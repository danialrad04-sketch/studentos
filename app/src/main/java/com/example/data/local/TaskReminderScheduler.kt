package com.example.data.local

import android.content.Context
import androidx.work.*
import com.example.data.local.entity.TaskEntity
import com.example.data.repository.AppPreferencesRepository
import com.example.domain.engine.TaskPlanningEngine
import com.example.ui.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** WorkManager owns delivery across process death and reboot; no exact-alarm permission required. */
object TaskReminderScheduler {
    const val TAG = "student_task_reminders"
    fun signature(task: TaskEntity, generation: String): String = "task_sig_" + MessageDigest.getInstance("SHA-256")
        .digest("$generation|${task.id}|${task.dueDate}|${task.reminderTime}".toByteArray())
        .joinToString("") { "%02x".format(it) }

    suspend fun reconcile(context: Context, tasks: List<TaskEntity>, enabled: Boolean, generation: String,
        now: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        val manager = WorkManager.getInstance(context)
        val desired = if (enabled) tasks.filter { TaskPlanningEngine.reminderAt(it) != null }.associateBy { signature(it, generation) } else emptyMap()
        val pending = manager.getWorkInfosByTag(TAG).get().filter { !it.state.isFinished }
        pending.filter { work -> work.tags.none { it in desired } }.forEach { manager.cancelWorkById(it.id).result.get() }
        desired.forEach { (signature, task) ->
            // Keep OS-deferred pending jobs, but never replay an already delivered/past reminder.
            if (pending.any { signature in it.tags }) return@forEach
            val at = TaskPlanningEngine.reminderAt(task) ?: return@forEach
            if (at <= now) return@forEach
            val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
                .setInitialDelay(at - now, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("taskId" to task.id, "date" to task.dueDate, "time" to task.reminderTime, "generation" to generation))
                .addTag(TAG).addTag(signature).addTag(AcademicReminderWorker.TAG).build()
            manager.enqueueUniqueWork("student_task_${task.id}", ExistingWorkPolicy.REPLACE, request).result.get()
        }
    }
}

class TaskReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val prefs = AppPreferencesRepository.getInstance(applicationContext)
        val generation = inputData.getString("generation")
        if (generation != prefs.taskReminderGeneration.value || !prefs.notificationsEnabled.value) return Result.success()
        val dao = AppDatabase.getDatabase(applicationContext).studentDao()
        val task = dao.getTaskById(inputData.getLong("taskId", 0)) ?: return Result.success()
        if (task.isCompleted || task.reminderTime.isBlank() || task.dueDate != inputData.getString("date") || task.reminderTime != inputData.getString("time")) return Result.success()
        if (task.semesterId != null) {
            val semester = dao.getSemesterById(task.semesterId) ?: return Result.success()
            if (semester.isArchived || !semester.isCurrent) return Result.success()
        }
        if (task.courseId.isNotBlank()) {
            val course = dao.getCourseById(task.courseId) ?: return Result.success()
            if (course.isArchived || (course.semesterId != null && course.semesterId != dao.getCurrentSemesterSync()?.id)) return Result.success()
        }
        val reminderAt = TaskPlanningEngine.reminderAt(task) ?: return Result.success()
        if (reminderAt > System.currentTimeMillis()) return Result.retry()
        if (!isStopped && generation == prefs.taskReminderGeneration.value) NotificationHelper.showSystemNotification(applicationContext,
            "موعد تکلیف: ${task.title}", "${task.courseName.ifBlank { "تکلیف شخصی" }} · ${task.dueDate} ساعت ${task.reminderTime} به وقت تهران",
            notificationId = "task_${task.id}".hashCode(), destinationTab = "TASKS")
        return Result.success()
    }
}

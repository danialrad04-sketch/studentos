package com.example.data.local

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.workDataOf
import com.example.data.repository.AppPreferencesRepository
import com.example.ui.util.NotificationHelper
import java.util.concurrent.TimeUnit

/** Durable reminders survive process death/reboot. Android may defer delivery for battery saving. */
class AcademicReminderWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val prefs = AppPreferencesRepository.getInstance(applicationContext)
        if (inputData.getString("kind") == "focus") {
            val endsAt = inputData.getLong("endsAt", 0)
            if (prefs.focusSession.value.endsAtMillis != endsAt || !prefs.completeFocusSession(System.currentTimeMillis())) return Result.success()
            if (prefs.notificationsEnabled.value) NotificationHelper.showSystemNotification(applicationContext,
                "جلسه تمرکز تمام شد", "چند دقیقه استراحت کنید و سپس سراغ کار بعدی بروید.", notificationId = 41001)
            return Result.success()
        }
        val courseId = inputData.getString("courseId") ?: return Result.failure()
        val reminderId = "exam_$courseId"
        if (reminderId !in prefs.examReminderIds.value) return Result.success()
        val course = AppDatabase.getDatabase(applicationContext).studentDao().getCourseById(courseId)
        if (course == null || course.isArchived || course.examDate != inputData.getString("date") || course.examTime != inputData.getString("time")) {
            prefs.setExamReminder(reminderId, false)
            return Result.success()
        }
        prefs.setExamReminder(reminderId, false)
        if (prefs.notificationsEnabled.value) NotificationHelper.showSystemNotification(applicationContext,
            "یادآور امتحان ${course.name}", "${course.examDate} ساعت ${course.examTime} · ${course.examLocation.ifBlank { "محل هنوز مشخص نشده" }}",
            notificationId = reminderId.hashCode())
        return Result.success()
    }

    companion object {
        const val TAG = "student_academic_reminders"
        const val FOCUS_WORK = "student_focus_completion"
        fun scheduleFocus(context: Context, endsAt: Long) {
            val request = OneTimeWorkRequestBuilder<AcademicReminderWorker>()
                .setInitialDelay((endsAt - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("kind" to "focus", "endsAt" to endsAt)).addTag(TAG).build()
            WorkManager.getInstance(context).enqueueUniqueWork(FOCUS_WORK, ExistingWorkPolicy.REPLACE, request)
        }
        fun cancelFocus(context: Context) { WorkManager.getInstance(context).cancelUniqueWork(FOCUS_WORK) }
        fun cancelAll(context: Context) { WorkManager.getInstance(context).cancelAllWorkByTag(TAG) }
    }
}

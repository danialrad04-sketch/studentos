package com.example.data

import android.app.Application
import android.app.NotificationManager
import androidx.work.*
import androidx.work.testing.*
import com.example.data.local.*
import com.example.data.local.entity.*
import com.example.data.repository.AppPreferencesRepository
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TaskReminderDeliveryTest {
    private val context: Application get() = RuntimeEnvironment.getApplication()
    private val prefs get() = AppPreferencesRepository.getInstance(context)
    private val dao get() = AppDatabase.getDatabase(context).studentDao()
    private val manager get() = context.getSystemService(NotificationManager::class.java)
    private val past = TaskEntity(id = 42, title = "تمرین اصلی", dueDate = "1400/07/01", reminderTime = "09:30", repeatEveryDays = 7)
    @Before fun setup() = runBlocking {
        prefs.setNotificationsEnabled(true)
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        manager.cancelAll()
        dao.clearTasks()
    }
    private suspend fun deliver(task: TaskEntity, generation: String = prefs.taskReminderGeneration.value) =
        TestListenableWorkerBuilder<TaskReminderWorker>(context).setInputData(workDataOf(
            "taskId" to task.id, "date" to task.dueDate, "time" to task.reminderTime, "generation" to generation)).build().doWork()

    @Test fun workerReadsLatestTitleAndNotificationOpensTasks() = runBlocking {
        dao.insertTask(past.copy(title = "عنوان اصلاح‌شده"))
        assertEquals(ListenableWorker.Result.success(), deliver(past))
        val notification = manager.activeNotifications.single().notification
        assertTrue(notification.extras.getString("android.title")!!.contains("عنوان اصلاح‌شده"))
        assertEquals("TASKS", shadowOf(notification.contentIntent).savedIntent.getStringExtra("destination_tab"))
    }
    @Test fun deletedCompletedRescheduledOrOldAccountRemindersCannotDeliver() = runBlocking {
        deliver(past)
        assertTrue(manager.activeNotifications.isEmpty())
        dao.insertTask(past.copy(isCompleted = true))
        deliver(past)
        dao.insertTask(past.copy(reminderTime = "12:00"))
        deliver(past)
        dao.insertTask(past)
        val oldGeneration = prefs.taskReminderGeneration.value
        prefs.clearAcademicChoices()
        deliver(past, oldGeneration)
        prefs.setNotificationsEnabled(false)
        deliver(past)
        assertTrue(manager.activeNotifications.isEmpty())
    }
    @Test fun archivedSemestersAndCoursesCannotDeliver() = runBlocking {
        dao.insertSemester(SemesterEntity(id = "archived", title = "پیشین", isArchived = true, isCurrent = false))
        dao.insertTask(past.copy(semesterId = "archived"))
        deliver(past)
        dao.insertCourse(CourseEntity(id = "archived_course", name = "درس پیشین", isArchived = true))
        dao.insertTask(past.copy(courseId = "archived_course"))
        deliver(past)
        assertTrue(manager.activeNotifications.isEmpty())
    }
    @Test fun schedulerSurvivesReconciliationUpdatesTimeAndCancelsOnCompletionOrDisable() = runBlocking {
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().setExecutor(SynchronousExecutor()).build())
        val future = past.copy(dueDate = "1410/07/01")
        val generation = prefs.taskReminderGeneration.value
        val wm = WorkManager.getInstance(context)
        fun pending() = wm.getWorkInfosByTag(TaskReminderScheduler.TAG).get().filter { !it.state.isFinished }
        TaskReminderScheduler.reconcile(context, listOf(future), true, generation)
        val original = pending().single()
        TaskReminderScheduler.reconcile(context, listOf(future.copy(title = "عنوان تازه")), true, generation)
        assertEquals(original.id, pending().single().id)
        TaskReminderScheduler.reconcile(context, listOf(future.copy(reminderTime = "13:00")), true, generation)
        assertNotEquals(original.id, pending().single().id)
        TaskReminderScheduler.reconcile(context, listOf(future.copy(isCompleted = true)), true, generation)
        assertTrue(pending().isEmpty())
        TaskReminderScheduler.reconcile(context, listOf(future), true, generation)
        TaskReminderScheduler.reconcile(context, listOf(future), false, generation)
        assertTrue(pending().isEmpty())
        TaskReminderScheduler.reconcile(context, listOf(past), true, generation)
        assertTrue(pending().isEmpty())
    }
}

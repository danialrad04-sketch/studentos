package com.example

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.AppDatabase
import com.example.data.local.entity.TaskEntity
import com.example.data.repository.StudentRepository
import com.example.domain.util.JalaliDate
import com.example.ui.components.AddTaskDialog
import com.example.ui.theme.StudentOsTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudentPlannerDeviceTest {
    @get:Rule val rule = createComposeRule()

    @Test fun repeatingTaskAndReminderSurviveDatabaseCloseAndReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "device-planner-${java.util.UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val repository = StudentRepository(db.studentDao(), db.curriculumDao(), db)
            val task = repository.saveTask(TaskEntity(title = "تمرین هفتگی", dueDate = "1405/07/01", repeatEveryDays = 7, reminderTime = "۰۹:۳۰"))
            repository.toggleTaskCompletion(task, JalaliDate(1405, 7, 20))
            repository.toggleTaskCompletion(task, JalaliDate(1405, 7, 20))
            repository.toggleTaskCompletion(task, JalaliDate(1405, 7, 20))
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val saved = db.studentDao().getAllTasksSync()
            assertEquals(2, saved.size)
            assertTrue(saved.single { it.id == task.id }.isCompleted)
            val next = saved.single { it.recurrenceParentId == task.id }
            assertEquals("1405/07/22", next.dueDate)
            assertEquals("09:30", next.reminderTime)
            assertEquals(7, next.repeatEveryDays)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun taskFormPassesRealRepeatAndNormalizedPersianReminderValuesToSave() {
        var saved: Pair<Int, String>? = null
        rule.setContent { StudentOsTheme(darkTheme = false) { Surface(Modifier.fillMaxSize()) {
            AddTaskDialog(listOf("ریاضی"), {}, { _, _, _ -> }, initialTask = TaskEntity(title = "تمرین", dueDate = "1410/07/01"),
                onSavePlanned = { _, _, _, repeat, time -> saved = repeat to time })
        } } }
        rule.onNodeWithTag("task_repeat_7").performScrollTo().performClick()
        rule.onNodeWithTag("task_reminder_switch").performScrollTo().performClick()
        rule.onNodeWithTag("task_reminder_time").performScrollTo().performTextReplacement("۲۵:۰۰")
        rule.onNodeWithTag("save_task").assertIsNotEnabled()
        rule.onNodeWithTag("task_reminder_time").performTextReplacement("۰۹:۳۰")
        rule.onNodeWithTag("save_task").assertIsEnabled().performClick()
        assertEquals(7 to "09:30", saved)
    }
}

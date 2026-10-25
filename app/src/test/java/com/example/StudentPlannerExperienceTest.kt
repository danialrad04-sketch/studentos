package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.example.data.local.entity.TaskEntity
import com.example.ui.components.AddTaskDialog
import com.example.ui.theme.StudentOsTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [35])
class StudentPlannerExperienceTest {
    @get:Rule val rule = createComposeRule()
    private fun friday(hour: Int, minute: Int): Long = java.util.Calendar.getInstance().apply {
        clear(); set(2026, java.util.Calendar.OCTOBER, 30, hour, minute)
        assertEquals(java.util.Calendar.FRIDAY, get(java.util.Calendar.DAY_OF_WEEK))
    }.timeInMillis

    @Test fun unscheduledHomeNeverInventsAClassTimeOrRoomAndOpensSchedule() {
        var destination: com.example.ui.models.AppTab? = null
        rule.setContent { StudentOsTheme(darkTheme = false) {
            com.example.ui.components.DynamicIslandLiveActivity(
                courses = listOf(com.example.data.local.entity.CourseEntity(id = "math", name = "ریاضی", examLocation = "محل امتحان")),
                pomodoroSeconds = 1500, isPomodoroRunning = false, onNavigateTab = { destination = it }, nowMillisOverride = friday(10, 0))
        } }
        rule.onNodeWithText("زمان کلاس‌ها ثبت نشده").assertIsDisplayed()
        rule.onNodeWithText("۲۴:۱۸ مانده").assertDoesNotExist()
        rule.onNodeWithText("08:00", substring = true).assertDoesNotExist()
        rule.onNodeWithTag("live_activity").performClick()
        rule.onNodeWithText("۱۰۷", substring = true).assertDoesNotExist()
        rule.onNodeWithText("محل امتحان", substring = true).assertDoesNotExist()
        rule.onNodeWithTag("live_activity_schedule").performClick()
        assertEquals(com.example.ui.models.AppTab.SCHEDULE, destination)
    }

    @Test fun homeUsesFridaySessionAndUpdatesFromUpcomingToOngoingAndNextWeek() {
        var clock by androidx.compose.runtime.mutableLongStateOf(friday(10, 0))
        val course = com.example.data.local.entity.CourseEntity(id = "math", name = "ریاضی", examLocation = "محل امتحان")
        val scheduled = com.example.data.local.relation.CourseWithSessions(course, listOf(
            com.example.data.local.entity.CourseSessionEntity(id = "fri", courseId = course.id, day = 6, start = "10:30", end = "11:00")))
        rule.setContent { StudentOsTheme(darkTheme = false) {
            com.example.ui.components.DynamicIslandLiveActivity(coursesWithSessions = listOf(scheduled), pomodoroSeconds = 1500,
                isPomodoroRunning = false, onNavigateTab = {}, nowMillisOverride = clock)
        } }
        rule.onNodeWithText("30 دقیقه تا شروع").assertIsDisplayed()
        rule.onNodeWithTag("live_activity").performClick()
        rule.onNodeWithText("مکان کلاس ثبت نشده").assertIsDisplayed()
        rule.onNodeWithText("محل امتحان", substring = true).assertDoesNotExist()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/live-status-real-session-light.png")
        rule.runOnIdle { clock = friday(10, 40) }
        rule.onNodeWithText("در کلاس: ریاضی").assertIsDisplayed()
        rule.onNodeWithText("20 دقیقه تا پایان").assertIsDisplayed()
        rule.runOnIdle { clock = friday(11, 0) }
        rule.onNodeWithText("7 روز دیگر").assertIsDisplayed()
        rule.onNodeWithText("در کلاس: ریاضی").assertDoesNotExist()
    }

    @Test fun homeFocusShowsActualTimerAndDarkLargeTextKeepsDetailsReachable() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                StudentOsTheme(darkTheme = true) {
                    com.example.ui.components.DynamicIslandLiveActivity(pomodoroSeconds = 90, isPomodoroRunning = true,
                        onNavigateTab = {}, nowMillisOverride = friday(10, 0))
                }
            }
        }
        rule.onNodeWithText("تمرکز فعال پومودورو").assertIsDisplayed()
        rule.onNodeWithText("01:30 مانده").assertIsDisplayed()
        rule.onNodeWithTag("live_activity").performClick()
        rule.onNodeWithTag("live_activity_schedule").assertIsDisplayed()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/live-status-focus-dark-large.png")
    }

    @Test fun lightThemeSavesWeeklyRepeatAndPersianReminderTimeAndRejectsInvalidTime() {
        var saved: Pair<Int, String>? = null
        rule.setContent { StudentOsTheme(darkTheme = false) { Surface(Modifier.fillMaxSize()) {
            AddTaskDialog(listOf("ریاضی"), {}, { _, _, _ -> }, initialTask = TaskEntity(title = "تمرین هفتگی", dueDate = "1406/07/01"),
                onSavePlanned = { _, _, _, repeat, time -> saved = repeat to time })
        } } }
        rule.onNodeWithTag("task_repeat_7").performScrollTo().performClick()
        rule.onNodeWithTag("task_reminder_switch").performScrollTo().performClick()
        rule.onNodeWithTag("task_reminder_time").performScrollTo().performTextReplacement("۲۵:۰۰")
        rule.onNodeWithTag("save_task").assertIsNotEnabled()
        rule.onNodeWithTag("task_reminder_time").performTextReplacement("۰۹:۳۰")
        rule.onNodeWithTag("task_reminder_time").assertTextContains("۰۹:۳۰")
        rule.onNodeWithTag("save_task").assertIsEnabled()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/planner-light.png")
        rule.onNodeWithTag("save_task").assertIsEnabled().performClick()
        assertEquals(7 to "09:30", saved)
    }
    @Test fun darkLargeFontEditorKeepsSaveReachableAndCanDisableReminderWithoutLosingRepeat() {
        var saved: Pair<Int, String>? = null
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                StudentOsTheme(darkTheme = true) { Surface(Modifier.fillMaxSize()) {
                    AddTaskDialog(listOf("ریاضی"), {}, { _, _, _ -> }, initialTask = TaskEntity(title = "تمرین مفصل برای جلسهٔ بعد", dueDate = "1406/07/01", repeatEveryDays = 14, reminderTime = "12:30"),
                        onSavePlanned = { _, _, _, repeat, time -> saved = repeat to time })
                } }
            }
        }
        rule.onNodeWithTag("task_repeat_14").performScrollTo().assertIsSelected()
        rule.onNodeWithTag("task_reminder_switch").performScrollTo().performClick()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/planner-dark-large-font.png")
        rule.onNodeWithTag("save_task").assertIsDisplayed().performClick()
        assertEquals(14 to "", saved)
    }
}

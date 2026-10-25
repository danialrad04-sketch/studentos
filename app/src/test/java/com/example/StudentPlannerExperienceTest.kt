package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
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

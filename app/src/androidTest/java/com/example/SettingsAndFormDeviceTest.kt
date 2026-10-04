package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.entity.StudentProfileEntity
import com.example.ui.components.AddTaskDialog
import com.example.ui.components.SettingsAndRoadmapDialog
import com.example.ui.models.ThemeMode
import com.example.ui.theme.StudentOsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsAndFormDeviceTest {
    @get:Rule val rule = createComposeRule()
    @Test fun taskSaveRemainsReachableWhenKeyboardOpens() {
        var title = ""
        rule.setContent { StudentOsTheme { AddTaskDialog(listOf("ریاضی مهندسی"), {}, { value, _, _ -> title = value }) } }
        rule.onNodeWithTag("task_title").performClick().performTextInput("تمرین جدید")
        rule.onNodeWithTag("save_task").assertIsDisplayed().performClick()
        assertEquals("تمرین جدید", title)
    }
    @Test fun settingsRoutesToAppearanceAndBack() {
        var selected: ThemeMode? = null
        rule.setContent {
            StudentOsTheme {
                SettingsAndRoadmapDialog(StudentProfileEntity(), ThemeMode.SYSTEM, { selected = it }, true, {}, {}, {}, {}, {}, {}, {}, {}, {})
            }
        }
        rule.onNodeWithTag("setting_ظاهر برنامه").performClick()
        rule.onNodeWithText("تاریک").performClick()
        assertEquals(ThemeMode.DARK, selected)
        rule.onNodeWithContentDescription("بازگشت").performClick()
        rule.onNodeWithTag("setting_حساب کاربری").assertExists()
    }
}

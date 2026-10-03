package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.components.FloatingIslandNavigationBar
import com.example.ui.models.AppTab
import com.example.ui.theme.StudentOsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReleaseNavigationTest {
    @get:Rule val rule = createComposeRule()
    @Test fun gradesHasADirectNavigationEntry() {
        var selected = AppTab.DASHBOARD
        rule.setContent { StudentOsTheme { FloatingIslandNavigationBar(AppTab.DASHBOARD,{selected=it}) } }
        rule.onNodeWithText("کارنامه").assertHasClickAction().performClick()
        assertEquals(AppTab.GRADES,selected)
        rule.onNodeWithText("خانه").assertIsSelected()
    }
}

package com.example

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.ui.components.StudentTodayCommandStrip
import com.example.ui.models.AppTab
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class StudentTodayCommandStripTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTodaySignalsAndPrimaryActions() {
        var destination: AppTab? = null
        var focusStarts = 0
        composeRule.setContent {
            MyApplicationTheme {
                StudentTodayCommandStrip(
                    courses = emptyList<CourseEntity>(),
                    coursesWithSessions = emptyList<CourseWithSessions>(),
                    attendance = emptyList<AttendanceEntity>(),
                    tasks = listOf(
                        TaskEntity(
                            id = 1L,
                            title = "تحویل پروژه",
                            courseName = "",
                            dueDate = "",
                            isCompleted = false
                        ),
                        TaskEntity(
                            id = 2L,
                            title = "تمرین",
                            courseName = "",
                            dueDate = "",
                            isCompleted = true
                        )
                    ),
                    studyRecommendations = emptyList(),
                    onNavigateTab = { destination = it },
                    onStartFocus = { focusStarts++ }
                )
            }
        }

        assertTrue(composeRule.onAllNodesWithText("نبض امروز").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("کار باز").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("کلاس امروز").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("غیبت بحرانی").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("شروع تمرکز").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("کارها").fetchSemanticsNodes().isNotEmpty())
        assertTrue(composeRule.onAllNodesWithText("1").fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithText("شروع تمرکز").performClick()
        assertEquals(1, focusStarts)
        composeRule.onNodeWithText("کارها").performClick()
        assertEquals(AppTab.TASKS, destination)
    }
}

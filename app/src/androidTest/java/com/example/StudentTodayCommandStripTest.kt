package com.example

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.onNodeWithText
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

class StudentTodayCommandStripTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTodaySignalsAndPrimaryActions() {
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
                    onNavigateTab = {},
                    onStartFocus = {}
                )
            }
        }

        composeRule.onNodeWithText("امروز").assertExists()
        composeRule.onNodeWithText("کار باز").assertExists()
        composeRule.onNodeWithText("کلاس امروز").assertExists()
        composeRule.onNodeWithText("غیبت بحرانی").assertExists()
        composeRule.onNodeWithText("شروع تمرکز").assertExists()
        composeRule.onNodeWithText("کارها").assertExists()
        composeRule.onNodeWithText("1").assertExists()
    }
}

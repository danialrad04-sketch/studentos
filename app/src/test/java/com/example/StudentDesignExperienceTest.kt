package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.entity.TaskEntity
import com.example.ui.components.CourseWorkspaceDialogV2
import com.example.ui.components.StudentAdaptiveRow
import com.example.ui.theme.StudentOsTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h800dp-xxhdpi", sdk = [35])
class StudentDesignExperienceTest {
    @get:Rule val rule = createComposeRule()
    private val notes = "توضیحات کامل این درس، تمرین‌ها و منابع تکمیلی برای مرور پیش از امتحان. ".repeat(18)
    private val course = CourseEntity(id = "design-course", name = "ترمودینامیک مهندسی شیمی و بررسی سیستم‌های چندجزئی", professor = "دکتر کریمی", notes = notes)
    private val tasks = (1..11).map { TaskEntity(id = it.toLong(), title = "کار شماره $it", dueDate = "1405/07/20", courseId = course.id) }

    private fun workspace(dark: Boolean = false, scale: Float = 1f) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                StudentOsTheme(darkTheme = dark) {
                    CourseWorkspaceDialogV2(
                        course = course, attendance = null, grade = null, tasks = tasks, exam = null,
                        sessions = listOf(CourseSessionEntity(courseId = course.id, day = 0, start = "15:00", end = "16:00", location = "دانشکدهٔ فنی، طبقهٔ دوم، کلاس ۱۰۸")),
                        onDismiss = {}, onEditCourse = {}, onChangeAttendance = {},
                        onToggleTask = {}, onAddTask = { _, _ -> }, onStartFocus = {}
                    )
                }
            }
        }
        rule.waitForIdle()
    }

    @Test fun allCourseTasksHaveADiscoverableRoute() {
        workspace()
        rule.onNodeWithTag("course_workspace_list").performScrollToNode(hasText("نمایش همهٔ 11 کار"))
        rule.onNodeWithText("نمایش همهٔ 11 کار").performClick()
        rule.onNodeWithTag("course_workspace_list").performScrollToNode(hasText("کار شماره 11"))
        rule.onNodeWithText("کار شماره 11").assertIsDisplayed()
    }

    @Test fun longNotesOpenWithoutEnteringEditMode() {
        workspace()
        rule.onNodeWithTag("course_workspace_list").performScrollToNode(hasTestTag("readable-یادداشت درس"))
        rule.onNodeWithTag("readable-یادداشت درس").onChildren().filterToOne(hasClickAction()).performClick()
        rule.onNodeWithTag("student_modal").assertIsDisplayed()
        rule.onNodeWithContentDescription("بستن پنجره").performClick()
        rule.waitForIdle()
        rule.onNodeWithTag("student_modal").assertDoesNotExist()
    }

    @Test fun compactLargeTextActionsReflowInsteadOfClipping() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                StudentOsTheme {
                    StudentAdaptiveRow(Modifier.width(280.dp)) { cell ->
                        Button(onClick = {}, modifier = cell) { Text("شروع تمرکز") }
                        Button(onClick = {}, modifier = cell) { Text("کار جدید") }
                    }
                }
            }
        }
        val first = rule.onNode(hasText("شروع تمرکز") and hasClickAction()).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val second = rule.onNode(hasText("کار جدید") and hasClickAction()).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("Large-text actions must occupy separate reachable rows", first.bottom <= second.top)
    }

    @Test fun workspaceLightVisual() {
        workspace()
        rule.onNodeWithTag("course_workspace").captureRoboImage("build/outputs/visual-review/workspace-light-320.png")
    }

    @Test fun workspaceDarkLargeTextVisual() {
        workspace(dark = true, scale = 2f)
        rule.onNodeWithTag("course_workspace").captureRoboImage("build/outputs/visual-review/workspace-dark-320-2.0.png")
    }
}

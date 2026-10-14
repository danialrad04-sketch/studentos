package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import org.junit.Assert.assertTrue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.ui.components.FloatingIslandNavigationBar
import com.example.ui.components.ModernBentoDashboard
import com.example.ui.components.AccountGateV2
import com.example.ui.components.StudentAppScaffold
import com.example.ui.components.SubScreenHeaderSection
import com.example.ui.components.TasksScreen
import com.example.ui.models.AppTab
import com.example.ui.theme.StudentOsTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h852dp-xxhdpi", sdk = [35])
class ReleaseVisualTest {
    @get:Rule val rule = createComposeRule()
    private val courses = listOf(
        CourseEntity(id="thermo", name="ترمودینامیک مهندسی شیمی", professor="دکتر کریمی"),
        CourseEntity(id="math", name="ریاضی مهندسی", professor="دکتر صیفاری")
    )
    private fun dashboard(dark: Boolean, scale: Float = 1f, semester: Boolean = false, courseList: List<CourseEntity> = courses, onOpenCourse: (CourseEntity) -> Unit = {}) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                StudentOsTheme(darkTheme = dark) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        StudentAppScaffold(AppTab.DASHBOARD, {}) {
                            ModernBentoDashboard(
                                courses = courseList,
                                onOpenCourseWorkspace = onOpenCourse,
                                coursesWithSessions = courseList.mapIndexed { i, course ->
                                    CourseWithSessions(course,listOf(CourseSessionEntity(courseId=course.id, day=0, start="${10+i*2}:00", end="${12+i*2}:00", location="دانشکده فنی، کلاس ۱۰۸")))
                                },
                                attendanceList = emptyList(), tasks = emptyList(), grades = emptyList(),
                                gpa = "۱۷٫۸۵", passedUnits = 34, totalRequiredCredits = 140,
                                pomodoroSeconds = 1500, isPomodoroRunning = false,
                                onTogglePomodoro = {}, onNavigateTab = {},
                                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        if (semester) {
            rule.onNodeWithTag("desk_semester").performClick().assertIsSelected()
            rule.onNodeWithText("پیشرفت تحصیلی").assertIsDisplayed()
            rule.onNodeWithText("کلاس بعدی").assertDoesNotExist()
        }
        rule.onRoot().captureRoboImage("build/outputs/visual-review/dashboard-${if (courseList.size > 2) "many-courses-" else ""}${if (semester) "semester-" else ""}${if(dark) "dark" else "light"}-$scale.png")
        if (semester) {
            rule.onNodeWithTag("desk_today").performClick().assertIsSelected()
            rule.onNodeWithText("کلاس بعدی").assertIsDisplayed()
        }
    }
    @Test fun dashboardLight() = dashboard(false)
    @Test fun dashboardDark() = dashboard(true)
    @Test fun semesterViewKeepsDailyNavigationReachable() = dashboard(false, semester = true)
    @Test fun semesterViewDark() = dashboard(true, semester = true)
    @Test fun semesterShowsAndOpensCoursesBeyondThePreview() {
        var openedId: String? = null
        val manyCourses = (1..5).map { CourseEntity(id = "desk-$it", name = "درس شماره $it") }
        dashboard(false, courseList = manyCourses, onOpenCourse = { openedId = it.id })
        rule.onNodeWithTag("desk_semester").performClick()
        rule.onNodeWithText("نمایش همه").performScrollTo().performClick()
        rule.onNodeWithText("درس شماره 5").performScrollTo().performClick()
        org.junit.Assert.assertEquals("desk-5", openedId)
    }

    @Test @Config(fontScale = 1.5f) fun dashboardLargeText() = dashboard(false, 1.5f)
    private fun tasks(dark: Boolean, scale: Float) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                StudentOsTheme(darkTheme = dark) {
                    StudentAppScaffold(AppTab.TASKS, {}) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                            SubScreenHeaderSection(
                                currentTab = AppTab.TASKS, onBackToDashboard = {}, onOpenSearch = {},
                                onOpenNotifications = {}, notifCount = 1, isDarkTheme = dark, onToggleTheme = {}
                            )
                            TasksScreen(tasks = emptyList(), onAddTask = {}, onToggleTask = {}, onDeleteTask = {})
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/tasks-${if (dark) "dark" else "light"}-$scale.png")
        val add = rule.onNodeWithText("ثبت اولین تکلیف").performScrollTo().assertIsDisplayed()
        val buttonBounds = add.fetchSemanticsNode().boundsInRoot
        val navigationBounds = rule.onNodeWithTag("primary_navigation").fetchSemanticsNode().boundsInRoot
        assertTrue("The primary action must remain above navigation", buttonBounds.bottom <= navigationBounds.top)
        rule.onRoot().captureRoboImage("build/outputs/visual-review/tasks-bottom-${if (dark) "dark" else "light"}-$scale.png")
        rule.onNode(hasText("عقب‌افتاده") and hasClickAction()).performScrollTo().assertIsDisplayed()
        val chip = rule.onNode(hasText("عقب‌افتاده") and hasClickAction()).fetchSemanticsNode().boundsInRoot
        val viewport = rule.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Every filter must scroll into the viewport", chip.left >= viewport.left && chip.right <= viewport.right)
    }
    @Test @Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [35])
    fun compactTasks() = tasks(false, 1f)
    @Test @Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [35], fontScale = 1.5f)
    fun compactTasksLargeText() = tasks(false, 1.5f)
    @Test @Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [35])
    fun compactTasksDark() = tasks(true, 1f)
    @Test fun accountEntry() {
        rule.setContent {
            StudentOsTheme(darkTheme=false) {
                AccountGateV2(onSignIn={_,_,_->},onSignUp={_,_,_,_->},onForgotPassword={},onGoogleSignIn={},onContinueAsGuest={})
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/account-light.png")
    }
}

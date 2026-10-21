package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.StudentProfileEntity
import com.example.domain.model.StudentGamificationProfile
import com.example.ui.components.*
import com.example.ui.models.*
import com.example.ui.theme.StudentOsTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Render the feature surfaces in RTL, dark mode and large text with an empty workspace. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [35])
open class DestinationScreensSmokeTest {
    protected open val darkTheme = true
    protected open val fontScale = 1.5f
    protected open val populated = false
    private val sampleCourses get() = if (populated) listOf(com.example.data.local.entity.CourseEntity(id = "math", name = "ریاضی مهندسی", units = 3)) else emptyList()
    @get:Rule val rule = createComposeRule()
    private fun screen(tab: AppTab, content: @Composable () -> Unit) {
        var menuOpened = false
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                StudentOsTheme(darkTheme = darkTheme) {
                    Surface {
                        StudentAppScaffold(tab, {}, onOpenMore = { menuOpened = true }) {
                            Column(Modifier.fillMaxSize().padding(16.dp)) {
                                SubScreenHeaderSection(tab, {}, {}, {}, notifCount = 0, isDarkTheme = darkTheme, onToggleTheme = {}, onOpenMenu = { menuOpened = true })
                                if (tab == AppTab.COPILOT || tab == AppTab.SEMESTER_PLANNER || tab == AppTab.HISTORY) {
                                    Box(Modifier.weight(1f)) { content() }
                                } else {
                                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { content() }
                                }
                            }
                        }
                    }
                }
            }
        }
        rule.onNodeWithContentDescription("باز کردن منوی برنامه").assertIsDisplayed().performClick()
        org.junit.Assert.assertTrue(menuOpened)
        rule.onNodeWithTag("primary_navigation").assertIsDisplayed()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/destination-${tab.name.lowercase()}-${if (darkTheme) "dark-large" else "light-populated"}.png")
    }
    @Test fun history() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
        val store = androidx.lifecycle.ViewModelStore()
        val model = androidx.lifecycle.ViewModelProvider(store,
            androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app))[com.example.ui.StudentViewModel::class.java]
        try { screen(AppTab.HISTORY) { SemesterHistoryScreen(model) } }
        finally { rule.runOnIdle { store.clear() } }
    }
    @Test fun schedule() = screen(AppTab.SCHEDULE) { WeeklyScheduleScreen(sampleCourses, onAddCourse = {}, onEditCourse = {}) }
    @Test fun tasks() = screen(AppTab.TASKS) { TasksScreen(if (populated) listOf(com.example.data.local.entity.TaskEntity(title = "تمرین معادلات دیفرانسیل", courseName = "ریاضی مهندسی", dueDate = "1405/08/12")) else emptyList(), onAddTask = {}, onToggleTask = {}, onDeleteTask = {}) }
    @Test fun exams() = screen(AppTab.EXAMS) { ExamsScreen(emptyList(), onSetReminder = {}, onAddExam = {}) }
    @Test fun grades() = screen(AppTab.GRADES) { GradesScreen(if (populated) listOf(com.example.data.local.entity.GradeEntity(courseName = "ریاضی مهندسی", courseId = "math", midtermGrade = 6.0, finalGrade = 12.0, isRecorded = true)) else emptyList(), onUpdateGrade = { _, _, _ -> }, onAddCourse = {}) }
    @Test fun attendance() = screen(AppTab.ATTENDANCE) { AttendanceRadarScreen(if (populated) listOf(com.example.data.local.entity.AttendanceEntity("math", "ریاضی مهندسی", 4, 5)) else emptyList(), sampleCourses, { _, _ -> }, {}, onSetLimit = { _, _ -> }) }
    @Test fun focus() = screen(AppTab.POMODORO) { PomodoroAndNotesScreen(1500, isRunning = false, onTogglePomodoro = {}, onResetPomodoro = {}, notes = "", onSaveNotes = {}) }
    @Test fun copilot() = screen(AppTab.COPILOT) {
        AcademicCopilotScreen(StudentProfileEntity(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
            onNavigateTab = {}, onExecuteAction = {}, onOpenPastSemestersDialog = {})
    }
    @Test fun intelligence() = screen(AppTab.ACADEMIC_INTELLIGENCE) {
        AcademicIntelligenceScreen(0.0, 0, 0, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), {})
    }
    @Test fun passport() = screen(AppTab.PASSPORT) {
        AcademicPassportScreen(StudentProfileEntity(), AcademicProgressUiState.Empty, CurriculumMatchUiState.Empty, {})
    }
    @Test fun curriculum() = screen(AppTab.CURRICULUM) { CurriculumScreen(CurriculumMatchUiState.Empty, onOpenProfile = {}) }
    @Test fun planner() = screen(AppTab.SEMESTER_PLANNER) { SemesterPlannerScreen(CurriculumMatchUiState.Empty, emptyList(), {}, onOpenProfile = {}) }
    @Test fun achievements() = screen(AppTab.GAMIFICATION) {
        StudentGamificationHub(StudentGamificationProfile(1, 0, 100, "شروع مسیر", 0, 0, 0.0, 0, 0, emptyList(), emptyList()))
    }
}

/** The same navigation and rendering contract must hold in the populated light workspace. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [35])
class LightDestinationScreensSmokeTest : DestinationScreensSmokeTest() {
    override val darkTheme = false
    override val fontScale = 1.0f
    override val populated = true
}

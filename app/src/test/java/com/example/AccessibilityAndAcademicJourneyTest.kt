package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.local.entity.StudentProfileEntity
import com.example.domain.model.*
import com.example.ui.components.AcademicJourneyOverview
import com.example.ui.components.SettingsAndRoadmapDialog
import com.example.ui.models.AcademicProgressUiState
import com.example.ui.models.ThemeMode
import com.example.ui.theme.StudentOsTheme
import com.example.ui.theme.studentColors
import com.example.ui.theme.readableAccent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi", sdk = [35])
class AccessibilityAndAcademicJourneyTest {
    @get:Rule val rule = createComposeRule()

    @Test fun academicJourneyShowsOnlyProvidedFactsAndLabelsConfidence() {
        val progress = AcademicProgress(
            totalRequiredCredits = 140,
            passedCredits = 72,
            passedCreditsSource = DataSource.USER_DECLARED,
            currentCredits = 18,
            remainingCredits = 50,
            progressPercentage = 51.42f,
            gpaState = GpaState.Known(16.75, DataSource.USER_DECLARED),
            confidence = DataConfidence.CREDIT_ONLY,
            availableCount = 9,
            blockedCount = 3,
            plannedCount = 4
        )
        rule.setContent {
            StudentOsTheme {
                AcademicJourneyOverview(
                    state = AcademicProgressUiState.Partial(progress, "ریزسوابق ناقص است"),
                    targetGpa = 17.0,
                    recommendations = emptyList(),
                    dailyStudyGoalMinutes = 120,
                    onOpenPassport = {}, onOpenGrades = {}, onOpenFocus = {}
                )
            }
        }
        rule.onNodeWithText("بر پایه واحد اعلامی").assertIsDisplayed()
        rule.onNodeWithText("72 واحد").assertIsDisplayed()
        rule.onNodeWithText("18 واحد").assertIsDisplayed()
        rule.onNodeWithText("50 واحد").assertIsDisplayed()
        rule.onNodeWithText("16.75").assertIsDisplayed()
        rule.onNodeWithText("75٪").assertDoesNotExist()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/academic-journey-light.png")
    }

    @Test fun studySettingsPersistThroughCallbacksAndExposeSelectedValues() {
        var goal by mutableStateOf(120)
        var focus by mutableStateOf(25)
        rule.setContent {
            StudentOsTheme {
                SettingsAndRoadmapDialog(
                    profile = StudentProfileEntity(), themeMode = ThemeMode.SYSTEM,
                    onSelectThemeMode = {}, notificationsEnabled = true, onToggleNotifications = {},
                    dailyStudyGoalMinutes = goal, onDailyStudyGoalChanged = { goal = it },
                    defaultFocusMinutes = focus, onDefaultFocusMinutesChanged = { focus = it },
                    onOpenEditProfile = {}, onDismiss = {}, onLoadDemoData = {}, onClearToFreshSlate = {},
                    onReopenOnboarding = {}, onOpenPastSemesters = {}, onOpenApkInfo = {}, onTestNotification = {}
                )
            }
        }
        rule.onNodeWithText("مطالعه و تمرکز").performClick()
        rule.onNodeWithText("3 ساعت").performScrollTo().performClick()
        rule.onNodeWithText("45 دقیقه").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(180, goal); assertEquals(45, focus) }
        rule.onNodeWithText("بازهٔ پیش‌فرض: 45 دقیقه").performScrollTo().assertIsDisplayed()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/study-settings-light.png")
    }

    @Test fun darkLargeJourneyAndJournalKeepActionsReachable() {
        var opened = false
        val progress = AcademicProgress(140, 72, DataSource.USER_DECLARED, 18, 50, 51.42f,
            GpaState.Known(16.75, DataSource.USER_DECLARED), DataConfidence.CREDIT_ONLY, 9, 3, 4)
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                StudentOsTheme(darkTheme = true) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        AcademicJourneyOverview(AcademicProgressUiState.Ready(progress), 18.0, emptyList(), 120,
                            onOpenPassport = { opened = true }, onOpenGrades = {}, onOpenFocus = {})
                        com.example.ui.components.StudyActivityJournal(emptyList(), 120)
                    }
                }
            }
        }
        rule.onNodeWithText("جزئیات مسیر").performScrollTo().performClick()
        rule.runOnIdle { assertTrue(opened) }
        rule.onRoot().captureRoboImage("build/outputs/visual-review/academic-journey-dark-large.png")
        rule.onNodeWithText("گزارش مطالعهٔ هفت‌روزه").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("هدف روزانه: 120 دقیقه · قابل تغییر در تنظیمات مطالعه").performScrollTo().assertIsDisplayed()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/study-journal-dark-large.png")
    }

    @Test fun attendanceDoesNotInventASafeSummaryAndHonorsStoredLimits() {
        var populated by mutableStateOf(false)
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                StudentOsTheme(darkTheme = true) {
                    com.example.ui.components.AttendanceRadarScreen(
                        courses = if (populated) listOf(com.example.data.local.entity.CourseEntity(id = "math", name = "ریاضی")) else emptyList(),
                        attendanceList = if (populated) listOf(com.example.data.local.entity.AttendanceEntity("math", "ریاضی", 2, 5)) else emptyList(),
                        onChangeAttendance = { _, _ -> }, onEvaluateAlerts = {},
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }
        }
        rule.onNodeWithText("خلاصهٔ غیبت‌های ترم").assertDoesNotExist()
        rule.onNodeWithText("بررسی هشدارها").assertIsNotEnabled()
        rule.runOnIdle { populated = true }
        rule.onNodeWithText("از 5").assertIsDisplayed()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/attendance-populated-dark-large.png")
        rule.onNodeWithText("سقف مجاز: 5 جلسه (نظری)").performScrollTo().assertIsDisplayed()
    }

    @Test fun largeTextTaskSignalsUseTwoReadableRows() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                StudentOsTheme(darkTheme = true) {
                    com.example.ui.components.TaskContextSummaryV2(emptyList(), emptyList(), emptyList())
                }
            }
        }
        val first = rule.onNodeWithText("باز").fetchSemanticsNode().boundsInRoot
        val secondRow = rule.onNodeWithText("مرتبط با امتحان").fetchSemanticsNode().boundsInRoot
        assertTrue(secondRow.top > first.bottom)
        rule.onRoot().captureRoboImage("build/outputs/visual-review/task-signals-dark-large.png")
    }

    @Test fun semanticStatusColorsKeepWcagTextContrastInLightAndDarkThemes() {
        var dark by mutableStateOf(false)
        var ratios = emptyList<Float>()
        rule.setContent {
            StudentOsTheme(darkTheme = dark) {
                val background = MaterialTheme.colorScheme.surface
                val colors = MaterialTheme.studentColors
                val badge = MaterialTheme.colorScheme.surfaceContainerHigh
                SideEffect {
                    ratios = listOf(colors.attendanceSafe, colors.attendanceWarning, colors.attendanceCritical)
                        .map { contrast(it, background) }
                    ratios = ratios + listOf(Color.Yellow, Color.Cyan, Color.Blue, Color.Black, Color.White, Color.Red)
                        .map { contrast(readableAccent(it, badge), badge) }
                }
            }
        }
        rule.runOnIdle { assertTrue(ratios.all { it >= 4.5f }) }
        rule.runOnIdle { dark = true }
        rule.waitForIdle()
        rule.runOnIdle { assertTrue(ratios.all { it >= 4.5f }) }
    }

    private fun contrast(a: Color, b: Color): Float {
        val lighter = maxOf(a.luminance(), b.luminance())
        val darker = minOf(a.luminance(), b.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}

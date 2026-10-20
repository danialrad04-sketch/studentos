package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
    }

    @Test fun studySettingsPersistThroughCallbacksAndExposeSelectedValues() {
        var goal = 120
        var focus = 25
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
    }

    @Test fun semanticStatusColorsKeepWcagTextContrastInLightAndDarkThemes() {
        var dark by mutableStateOf(false)
        var ratios = emptyList<Float>()
        rule.setContent {
            StudentOsTheme(darkTheme = dark) {
                val background = MaterialTheme.colorScheme.surface
                val colors = MaterialTheme.studentColors
                SideEffect {
                    ratios = listOf(colors.attendanceSafe, colors.attendanceWarning, colors.attendanceCritical)
                        .map { contrast(it, background) }
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

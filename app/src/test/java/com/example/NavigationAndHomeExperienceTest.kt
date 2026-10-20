package com.example

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.local.entity.StudentProfileEntity
import com.example.domain.model.UserAccount
import com.example.ui.components.*
import com.example.ui.models.AppTab
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
class NavigationAndHomeExperienceTest {
    @get:Rule val rule = createComposeRule()

    @Test fun everyDestinationCanBeFoundSelectedAndDrawerCloses() {
        var open by mutableStateOf(true)
        var selected by mutableStateOf(AppTab.DASHBOARD)
        var settingsOpened = false
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                StudentOsTheme {
                    StudentNavigationDrawer(open, { open = it }, selected, StudentProfileEntity(), UserAccount(),
                        { selected = it }, {}, { settingsOpened = true }, {}) { Text("محتوای برنامه") }
                }
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/drawer-light-rtl.png")
        rule.onNodeWithText("تنظیمات").assertIsDisplayed().performClick()
        rule.runOnIdle { assertTrue(settingsOpened) }
        AppTab.entries.forEach { tab ->
            rule.runOnIdle { open = true }
            rule.waitForIdle()
            rule.onNodeWithTag("drawer_search").performTextReplacement(tab.title)
            rule.onNodeWithTag("drawer_${tab.name}").performScrollTo().performClick()
            rule.waitForIdle()
            rule.runOnIdle { assertEquals(tab, selected); assertFalse(open) }
        }
    }

    @Test fun darkLargeTextDrawerSearchAndSupportRemainReachable() {
        var open by mutableStateOf(true)
        var support = false
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f), LocalLayoutDirection provides LayoutDirection.Rtl) {
                StudentOsTheme(darkTheme = true) {
                    StudentNavigationDrawer(open, { open = it }, AppTab.COPILOT, StudentProfileEntity(), UserAccount(),
                        {}, {}, {}, { support = true }) { Text("محتوای برنامه") }
                }
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/drawer-dark-rtl-large.png")
        rule.onNodeWithTag("drawer_search").performTextReplacement("پیدا نمی‌شود")
        rule.onNodeWithText("صفحه‌ای پیدا نشد؛ نام دیگری جست‌وجو کنید.").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("پشتیبانی").assertIsDisplayed().performClick()
        rule.runOnIdle { assertTrue(support) }
    }

    @Test fun changingAccountClearsTheUnsentConversationInput() {
        var account by mutableStateOf(UserAccount(uid = "first-local-account"))
        rule.setContent {
            StudentOsTheme {
                AcademicCopilotScreen(StudentProfileEntity(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
                    onNavigateTab = {}, onExecuteAction = {}, onOpenPastSemestersDialog = {}, userAccount = account)
            }
        }
        rule.onNodeWithTag("copilot_input_field").performTextInput("یادداشت خصوصی حساب اول")
        rule.runOnIdle { account = UserAccount(uid = "second-local-account") }
        rule.waitForIdle()
        val input = rule.onNodeWithTag("copilot_input_field").fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text
        assertEquals("", input)
    }

    @Test fun newStudentHasWorkingScheduleAndCopilotEntriesWithoutCallbacks() {
        var selected: AppTab? = null
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                StudentOsTheme {
                    Surface {
                        ModernBentoDashboard(
                            courses = emptyList(), attendanceList = emptyList(), tasks = emptyList(), grades = emptyList(),
                            pomodoroSeconds = 1500, isPomodoroRunning = false, onTogglePomodoro = {},
                            onNavigateTab = { selected = it }, modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        }
        rule.onNodeWithText("افزودن درس و برنامه").performScrollTo().performClick()
        assertEquals(AppTab.SCHEDULE, selected)
        rule.onNodeWithText("از دستیار تحصیلی بپرس").performScrollTo().performClick()
        assertEquals(AppTab.COPILOT, selected)
        rule.onRoot().captureRoboImage("build/outputs/visual-review/home-new-student-rtl.png")
    }
}

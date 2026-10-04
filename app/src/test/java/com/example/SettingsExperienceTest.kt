package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.example.data.local.entity.StudentProfileEntity
import com.example.domain.model.UserAccount
import com.example.ui.components.*
import com.example.ui.models.AppTab
import com.example.ui.models.ThemeMode
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
class SettingsExperienceTest {
    @get:Rule val rule = createComposeRule()
    private val profile = StudentProfileEntity(name = "دانیال", university = "دانشگاه مراغه", major = "مهندسی شیمی")
    private fun settings(dark: Boolean = false, scale: Float = 1f, initial: String = "home", onClear: () -> Unit = {}) {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                StudentOsTheme(darkTheme = dark) {
                    var mode by remember { mutableStateOf(ThemeMode.SYSTEM) }
                    SettingsAndRoadmapDialog(profile, mode, { mode = it }, true, {}, {}, {}, {}, onClear, {}, {}, {}, {}, initialSection = initial)
                }
            }
        }
        rule.waitForIdle()
    }
    @Test fun compactSettingsAndAppearance() {
        settings()
        rule.onNodeWithTag("settings_page").captureRoboImage("build/outputs/visual-review/settings-light.png")
        rule.onNodeWithTag("setting_ظاهر برنامه").performScrollTo().performClick()
        rule.onNodeWithText("روشن").performClick().assertIsSelected()
        rule.onNodeWithTag("settings_page").captureRoboImage("build/outputs/visual-review/settings-appearance.png")
        rule.onNodeWithContentDescription("بازگشت").performClick()
        rule.onNodeWithTag("setting_حساب کاربری").assertExists()
    }
    @Test fun darkSettingsLargeText() {
        settings(dark = true, scale = 1.5f)
        rule.onNodeWithTag("setting_داده‌ها و پشتیبان").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag("settings_page").captureRoboImage("build/outputs/visual-review/settings-dark-large-text.png")
    }
    @Test fun destructiveActionRequiresExplicitConfirmation() {
        var clears = 0
        settings(initial = "data", onClear = { clears++ })
        rule.onNodeWithTag("setting_پاکسازی اطلاعات محلی").performScrollTo().performClick()
        assertEquals(0, clears)
        rule.onNodeWithText("انصراف").performClick()
        assertEquals(0, clears)
        rule.onNodeWithTag("setting_پاکسازی اطلاعات محلی").performScrollTo().performClick()
        rule.onNodeWithText("پاکسازی اطلاعات", substring = false).performClick()
        assertEquals(1, clears)
    }
    @Test fun aboutShowsActualBuildIdentity() {
        settings(initial = "about")
        rule.onNodeWithText("نسخهٔ ${BuildConfig.VERSION_NAME} · ساخت ${BuildConfig.VERSION_CODE}").assertExists()
    }
    @Test fun connectedAccountHandlesSyncFailureAndRecovery() {
        var callback: ((Boolean, String) -> Unit)? = null
        rule.setContent {
            StudentOsTheme(darkTheme = false) {
                AuthAccountDialog(UserAccount(isGuest = false, displayName = "دانیال", email = "student@example.com"), { _, _, _ -> }, { _, _, _, _ -> }, {}, {}, {}, {}, {}, { callback = it }, {})
            }
        }
        rule.onNodeWithText("همگام‌سازی اکنون").performScrollTo().performClick()
        rule.onNodeWithText("در حال همگام‌سازی…").assertIsNotEnabled()
        rule.runOnIdle { callback!!(false, "اتصال برقرار نشد") }
        rule.onNodeWithText("اتصال برقرار نشد").assertExists()
        rule.onNodeWithText("همگام‌سازی اکنون").assertIsEnabled().performClick()
        rule.runOnIdle { callback!!(true, "اطلاعات همگام شد") }
        rule.onNodeWithText("اطلاعات همگام شد").assertExists()
        rule.onNodeWithTag("settings_page").captureRoboImage("build/outputs/visual-review/account-connected.png")
    }
    @Test fun guestAccountReusesStartupForm() {
        rule.setContent { StudentOsTheme(darkTheme = false) { AuthAccountDialog(UserAccount(), { _, _, _ -> }, { _, _, _, _ -> }, {}, {}, {}, {}, {}, {}, {}) } }
        rule.onNodeWithText("همگام‌سازی اکنون").assertDoesNotExist()
        rule.onNodeWithTag("settings_page").captureRoboImage("build/outputs/visual-review/account-guest.png")
        rule.onNodeWithText("ورود یا ساخت حساب").performClick()
        rule.onNodeWithTag("primary_guest_entry").assertExists()
    }
    @Test fun successfulSignInReturnsToAccountCenter() {
        var user by mutableStateOf(UserAccount())
        rule.setContent { StudentOsTheme(darkTheme = false) { AuthAccountDialog(user, { _, _, _ -> }, { _, _, _, _ -> }, {}, {}, {}, {}, {}, {}, {}) } }
        rule.onNodeWithText("ورود یا ساخت حساب").performClick()
        rule.onNodeWithTag("primary_guest_entry").assertExists()
        rule.runOnIdle { user = UserAccount(isGuest = false, displayName = "دانیال", email = "student@example.com") }
        rule.onNodeWithText("ورود یا ساخت حساب").assertDoesNotExist()
        rule.onNodeWithTag("account_center").assertIsDisplayed()
    }
    @Test fun taskFormValidatesAndSavesTodayInsteadOfFixedDate() {
        var saved: List<String>? = null
        rule.setContent { StudentOsTheme(darkTheme = false) { AddTaskDialog(listOf("ترمودینامیک مهندسی شیمی", "ریاضی مهندسی"), {}, { title, course, date -> saved = listOf(title, course, date) }) } }
        rule.onNodeWithTag("save_task").assertIsNotEnabled()
        rule.onNodeWithTag("task_title").performTextInput("حل تمرین فصل سوم")
        rule.onNodeWithTag("student_modal").captureRoboImage("build/outputs/visual-review/task-form-light.png")
        rule.onNodeWithTag("save_task").assertIsEnabled().performClick()
        assertEquals("حل تمرین فصل سوم", saved?.first())
        assertEquals(com.example.ui.components.datepicker.JalaliCalendarUtil.today().format(), saved?.last())
    }
    @Test @Config(qualifiers = "w360dp-h480dp-xxhdpi", sdk = [35])
    fun shortWindowWithLargeTextKeepsTaskActionsReachable() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                StudentOsTheme(darkTheme = true) { AddTaskDialog(listOf("ترمودینامیک مهندسی شیمی"), {}, { _, _, _ -> }) }
            }
        }
        rule.onNodeWithTag("save_task").assertIsDisplayed()
        rule.onNodeWithTag("student_modal").captureRoboImage("build/outputs/visual-review/task-form-short-dark-large-text.png")
    }
    @Test fun profileFormKeepsSaveReachable() {
        rule.setContent { StudentOsTheme(darkTheme = false) { EditProfileDialog(profile, {}, { _, _, _, _, _, _, _, _ -> }) } }
        rule.onNodeWithTag("profile_editor").captureRoboImage("build/outputs/visual-review/profile-form.png")
        rule.onNodeWithTag("save_profile").assertIsDisplayed()
    }
    @Test fun courseFormRenders() {
        rule.setContent { StudentOsTheme(darkTheme = true) { AddEditCourseDialog(initialCourse = null, onDismiss = {}) } }
        rule.onNodeWithTag("student_modal").captureRoboImage("build/outputs/visual-review/course-form-dark.png")
    }
    @Test fun profileAcceptsPersianNumbersAndRejectsInvalidInput() {
        var passed: Int? = null
        rule.setContent { StudentOsTheme(darkTheme = false) { EditProfileDialog(profile, {}, { _, _, _, _, _, _, units, _ -> passed = units }) } }
        val field = rule.onNode(hasText("واحدهای گذرانده") and hasSetTextAction())
        field.performScrollTo().performTextClearance()
        field.performTextInput("-۱")
        rule.onNodeWithTag("save_profile").assertIsNotEnabled()
        field.performTextClearance()
        field.performTextInput("۳۴")
        rule.onNodeWithTag("save_profile").assertIsEnabled().performClick()
        assertEquals(34, passed)
    }
    @Test fun privacySheetRendersAndDismisses() {
        var dismissed = false
        rule.setContent { StudentOsTheme(darkTheme = false) { PrivacyPolicyDialog { dismissed = true } } }
        rule.onNodeWithTag("student_modal").captureRoboImage("build/outputs/visual-review/privacy-sheet.png")
        rule.onNodeWithContentDescription("بستن پنجره").performClick()
        rule.waitForIdle()
        assertTrue(dismissed)
    }
    @Test fun backupSheetRenders() {
        rule.setContent { StudentOsTheme(darkTheme = true) { BackupRestoreDialog(0L, { "{}" }, { _, _ -> }, onDismiss = {}) } }
        rule.onNodeWithTag("student_modal").captureRoboImage("build/outputs/visual-review/backup-sheet-dark.png")
    }
    @Test fun gradeSheetRenders() {
        rule.setContent { StudentOsTheme(darkTheme = false) { EditGradeDialog(com.example.data.local.entity.GradeEntity(courseName = "ترمودینامیک مهندسی شیمی"), {}, { _, _ -> }) } }
        rule.onNodeWithTag("student_modal").captureRoboImage("build/outputs/visual-review/grade-sheet.png")
    }
    @Test fun drawerExposesSettingsAndAccount() {
        var account = false
        rule.setContent { StudentOsTheme(darkTheme = false) { StudentNavigationDrawer(true, {}, AppTab.DASHBOARD, profile, UserAccount(), {}, { account = true }, {}, {}) { Text("داشبورد") } } }
        rule.onNodeWithText("تنظیمات").assertIsDisplayed()
        rule.onNodeWithTag("app_drawer").captureRoboImage("build/outputs/visual-review/navigation-drawer.png")
        rule.onNodeWithText("حساب کاربری").performClick()
        rule.waitForIdle()
        assertTrue(account)
    }
    @Test fun compactHeaderWithLargeTextKeepsMenuAndAccountAccessible() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                StudentOsTheme(darkTheme = false) { HeaderSection(profile, 3, {}, {}, {}, {}) }
            }
        }
        rule.onNodeWithContentDescription("باز کردن منوی برنامه").assertIsDisplayed()
        rule.onNodeWithContentDescription("حساب کاربری").assertIsDisplayed()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/header-compact-large-text.png")
    }
    @Test @Config(qualifiers = "w840dp-h900dp-xxhdpi", sdk = [35])
    fun tabletUsesRailInsteadOfPhoneNavigation() {
        rule.setContent { StudentOsTheme(darkTheme = false) { StudentAppScaffold(AppTab.DASHBOARD, {}) { Text("محتوای صفحه") } } }
        rule.onNodeWithTag("primary_navigation").assertDoesNotExist()
        rule.onNodeWithTag("navigation_rail").assertExists()
        rule.onNodeWithTag("rail_more").assertHasClickAction()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/tablet-navigation.png")
    }
}

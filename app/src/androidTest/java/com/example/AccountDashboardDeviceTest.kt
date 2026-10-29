package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.data.local.AppDatabase
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.repository.AppPreferencesRepository
import com.example.ui.models.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Production header → account → Room profile and persisted theme → Activity recreation. */
class AccountDashboardDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun accountEditsPersistAndSettingsReturnToTheDashboard() {
        rule.waitUntil(30000) {
            rule.onAllNodesWithTag("local_onboarding").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithTag("primary_navigation").fetchSemanticsNodes().isNotEmpty()
        }
        val preferences = AppPreferencesRepository.getInstance(rule.activity)
        val originalTheme = preferences.themeMode.value
        val originalNotifications = preferences.notificationsEnabled.value
        preferences.setNotificationsEnabled(false)
        val dao = AppDatabase.getDatabase(rule.activity).studentDao()
        runBlocking(Dispatchers.IO) {
            dao.insertProfile(
                StudentProfileEntity(
                    name = "دانشجوی حساب",
                    university = "دانشگاه آزمون",
                    major = "ریاضی",
                    entryYear = 1405,
                    currentSemester = 1,
                    isOnboardingCompleted = true,
                )
            )
        }
        try {
            rule.waitUntil(20000) {
                rule.onAllNodesWithTag("primary_navigation").fetchSemanticsNodes().isNotEmpty()
            }
            val vm =
                androidx.lifecycle
                    .ViewModelProvider(rule.activity)[com.example.ui.StudentViewModel::class.java]
            rule.runOnIdle { vm.selectTab(com.example.ui.models.AppTab.DASHBOARD) }
            rule.onNodeWithContentDescription("حساب کاربری").performClick()
            rule.onNodeWithTag("account_dashboard").assertExists()
            rule.onNodeWithTag("account_edit_profile").performScrollTo().performClick()
            rule
                .onNodeWithText("نام و نام خانوادگی")
                .performTextReplacement("نام ذخیره‌شده در حساب")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            rule.onNodeWithTag("save_profile").performClick()
            rule.waitUntil(15000) {
                rule.onAllNodesWithTag("profile_editor").fetchSemanticsNodes().isEmpty()
            }
            rule.onNodeWithText("نام ذخیره‌شده در حساب").assertExists()
            rule.onNodeWithTag("account_theme_DARK").performScrollTo().performClick()
            rule.runOnIdle { assertEquals(ThemeMode.DARK, preferences.themeMode.value) }
            rule.activityRule.scenario.recreate()
            rule.waitUntil(20000) {
                rule.onAllNodesWithTag("primary_navigation").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithContentDescription("حساب کاربری").performClick()
            rule.onNodeWithText("نام ذخیره‌شده در حساب").assertExists()
            rule.onNodeWithTag("account_theme_DARK").performScrollTo().assertIsSelected()
            rule.onNodeWithTag("setting_مطالعه و تمرکز").performScrollTo().performClick()
            rule.onNodeWithText("هدف مطالعهٔ روزانه").assertExists()
            rule.onNodeWithContentDescription("بازگشت").performClick()
            // Settings back first returns to its index, then to the owning account destination.
            rule.onNodeWithContentDescription("بازگشت").performClick()
            rule.onNodeWithTag("account_dashboard").assertExists()
        } finally {
            preferences.setThemeMode(originalTheme)
            preferences.setNotificationsEnabled(originalNotifications)
        }
    }
}

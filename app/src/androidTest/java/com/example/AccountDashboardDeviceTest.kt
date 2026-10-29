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
            rule.onNodeWithTag("account_edit_profile").scrollAboveNavigation(rule).performClick()
            rule
                .onNodeWithText("نام و نام خانوادگی")
                .performTextReplacement("نام ذخیره‌شده در حساب")
            androidx.test.espresso.Espresso.closeSoftKeyboard()
            rule.onNodeWithTag("save_profile").performClick()
            rule.waitUntil(15000) {
                rule.onAllNodesWithTag("profile_editor").fetchSemanticsNodes().isEmpty()
            }
            rule
                .onNode(
                    hasText("نام ذخیره‌شده در حساب") and
                        hasAnyAncestor(hasTestTag("account_dashboard"))
                )
                .assertExists()
            // Saving shows a transient Snackbar above navigation, over the theme cards.
            // Wait for that real feedback to appear and leave before touching a covered control.
            val savedFeedback = rule.onAllNodesWithText("مشخصات دانشجو ذخیره شد.")
            rule.waitUntil(15000) { savedFeedback.fetchSemanticsNodes().isNotEmpty() }
            rule.waitUntil(15000) { savedFeedback.fetchSemanticsNodes().isEmpty() }
            awaitAppWindowFocus()
            val darkChoice = rule.onNodeWithTag("account_theme_DARK").scrollAboveNavigation(rule)
            captureDevice("theme-before-touch")
            tapThroughAndroidInput(darkChoice)
            rule.waitUntil(5000) { preferences.themeMode.value == ThemeMode.DARK }
            captureDevice("theme-after-touch")
            darkChoice.assertIsSelected()
            rule.runOnIdle { assertEquals(ThemeMode.DARK, preferences.themeMode.value) }
            rule.activityRule.scenario.recreate()
            rule.waitUntil(20000) {
                rule.onAllNodesWithTag("account_dashboard").fetchSemanticsNodes().isNotEmpty()
            }
            rule
                .onNode(
                    hasText("نام ذخیره‌شده در حساب") and
                        hasAnyAncestor(hasTestTag("account_dashboard"))
                )
                .assertExists()
            awaitAppWindowFocus()
            rule.onNodeWithTag("account_theme_DARK").scrollAboveNavigation(rule).assertIsSelected()
            rule.onNodeWithTag("setting_مطالعه و تمرکز").scrollAboveNavigation(rule).performClick()
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

    private fun awaitAppWindowFocus() {
        rule.waitUntil(15000) {
            var focused = false
            rule.activityRule.scenario.onActivity { focused = it.hasWindowFocus() }
            focused
        }
        rule.waitForIdle()
    }

    private fun captureDevice(name: String) {
        val instrumentation =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        // Keep images outside application data: AGP uninstalls the target after the suite.
        val automation = instrumentation.uiAutomation
        val directory = "/sdcard/Download/studentos-navigation-diagnostics"
        for (command in listOf("mkdir -p $directory", "screencap -p $directory/$name.png")) {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                    automation.executeShellCommand(command)
                )
                .use { it.readBytes() }
        }
    }

    /** Exercise the real window/input pipeline, including touch-mode and focus hand-off. */
    private fun tapThroughAndroidInput(target: SemanticsNodeInteraction) {
        val node = target.fetchSemanticsNode()
        val center = node.positionOnScreen + node.boundsInRoot.center - node.positionInRoot
        android.util.Log.i(
            "StudentOsNavigationTest",
            "Native tap tag=account_theme_DARK; bounds=${node.boundsInRoot}; screen=$center",
        )
        val automation =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
        val properties =
            arrayOf(
                android.view.MotionEvent.PointerProperties().apply {
                    id = 0
                    toolType = android.view.MotionEvent.TOOL_TYPE_FINGER
                }
            )
        val coordinates =
            arrayOf(
                android.view.MotionEvent.PointerCoords().apply {
                    x = center.x
                    y = center.y
                    pressure = 1f
                    size = 1f
                }
            )
        val down = android.os.SystemClock.uptimeMillis()
        for (action in
            listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
            if (action == android.view.MotionEvent.ACTION_UP) coordinates[0].pressure = 0f
            val event =
                android.view.MotionEvent.obtain(
                    down,
                    android.os.SystemClock.uptimeMillis(),
                    action,
                    1,
                    properties,
                    coordinates,
                    0,
                    0,
                    1f,
                    1f,
                    0,
                    0,
                    android.view.InputDevice.SOURCE_TOUCHSCREEN,
                    0,
                )
            try {
                org.junit.Assert.assertTrue(
                    "Android must accept the physical touch event",
                    automation.injectInputEvent(event, true),
                )
            } finally {
                event.recycle()
            }
        }
        rule.waitForIdle()
    }
}

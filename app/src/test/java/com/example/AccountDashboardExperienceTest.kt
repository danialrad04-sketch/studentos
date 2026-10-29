package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
class AccountDashboardExperienceTest {
    @get:Rule val rule = createComposeRule()
    private val longName = "دانشجوی مهندسی کامپیوتر با نام خانوادگی طولانی"
    private var action = ""
    private var notifications by mutableStateOf(true)
    private var theme by mutableStateOf(ThemeMode.SYSTEM)

    private fun content(dark: Boolean, scale: Float = 1f) {
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(d.density, scale),
            ) {
                StudentOsTheme(darkTheme = dark) {
                    StudentAppScaffold(AppTab.ACCOUNT, {}) {
                        Column(
                            Modifier.fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            AccountDashboardScreen(
                                StudentProfileEntity(
                                    name = longName,
                                    university = "دانشگاه صنعتی",
                                    major = "مهندسی کامپیوتر",
                                ),
                                UserAccount(),
                                theme,
                                notifications,
                                5,
                                3,
                                120,
                                25,
                                { action = "profile" },
                                { theme = it },
                                { notifications = it },
                                { action = it },
                                { action = "account" },
                                { action = "backup" },
                                { action = "support" },
                                { action = "privacy" },
                                { action = "courses" },
                                { action = "tasks" },
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun lightDashboardHasRealRoutesAndSingleChoiceAppearanceControls() {
        content(false)
        rule.onRoot().captureRoboImage("build/outputs/visual-review/account-dashboard-light.png")
        rule.onNodeWithTag("account_edit_profile").performScrollTo().performClick()
        assertEquals("profile", action)
        rule.onNodeWithTag("account_theme_DARK").performScrollTo().performClick().assertIsSelected()
        rule.onNodeWithTag("account_theme_SYSTEM").assertIsNotSelected()
        rule.onNodeWithText("اعلان‌های درسی").performScrollTo().performClick()
        rule.runOnIdle { assertFalse(notifications) }
        rule.onNodeWithText("یادآورهای برنامه خاموش‌اند").assertExists()
        rule.onNodeWithTag("setting_مطالعه و تمرکز").performScrollTo().performClick()
        assertEquals("study", action)
        rule.onNodeWithTag("setting_پشتیبان و بازیابی").performScrollTo().performClick()
        assertEquals("backup", action)
        rule.onNodeWithTag("setting_همهٔ تنظیمات").performScrollTo().performClick()
        assertEquals("home", action)
    }

    @Test
    fun darkLargeTextReflowsIdentityAndKeepsEveryManagementActionReachable() {
        content(true, 1.5f)
        val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        rule.onNodeWithText(longName).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(layouts)
        }
        assertFalse(layouts.single().hasVisualOverflow)
        rule
            .onRoot()
            .captureRoboImage("build/outputs/visual-review/account-dashboard-dark-large.png")
        for ((title, expected) in
            listOf(
                "مدیریت حساب" to "account",
                "پشتیبانی" to "support",
                "حریم خصوصی" to "privacy",
            )) {
            rule
                .onNodeWithTag("setting_$title")
                .performScrollTo()
                .assertIsDisplayed()
                .performClick()
            assertEquals(expected, action)
        }
        rule
            .onRoot()
            .captureRoboImage("build/outputs/visual-review/account-management-dark-large.png")
    }

    @Test
    @Config(qualifiers = "w320dp-h700dp-xxhdpi", sdk = [35])
    fun narrowTwoHundredPercentNavigationKeepsFiveTargetsAboveSystemInsets() {
        var selected by mutableStateOf(AppTab.DASHBOARD)
        var more = false
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(d.density, 2f),
            ) {
                StudentOsTheme {
                    StudentAppScaffold(selected, { selected = it }, onOpenMore = { more = true }) {
                        Text("محتوا")
                    }
                }
            }
        }
        for (tab in listOf(AppTab.DASHBOARD, AppTab.SCHEDULE, AppTab.TASKS, AppTab.GRADES)) {
            rule.onNodeWithTag("nav_${tab.name}").performClick().assertIsSelected()
            val bounds = rule.onNodeWithTag("nav_${tab.name}").fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.width >= 48 * rule.density.density)
        }
        for (label in listOf("خانه", "برنامه", "کارها", "نمره", "بیشتر")) {
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            rule.onNodeWithText(label).performSemanticsAction(
                SemanticsActions.GetTextLayoutResult
            ) {
                it(layouts)
            }
            assertEquals(
                "A navigation label must not leave an orphan letter",
                1,
                layouts.single().lineCount,
            )
            assertFalse(layouts.single().hasVisualOverflow)
        }
        rule.onNodeWithTag("nav_more").assertIsDisplayed().performClick()
        assertTrue(more)
        rule.onRoot().captureRoboImage("build/outputs/visual-review/navigation-320-rtl-200.png")
    }

    @Test
    @Config(qualifiers = "w840dp-h900dp-xhdpi", sdk = [35])
    fun tabletRailExposesAccountAndToolHub() {
        var selected by mutableStateOf(AppTab.DASHBOARD)
        var more = false
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                StudentOsTheme(darkTheme = true) {
                    StudentAppScaffold(selected, { selected = it }, onOpenMore = { more = true }) {
                        assertEquals(
                            "Dark canvas content must inherit the semantic foreground",
                            MaterialTheme.colorScheme.onBackground,
                            LocalContentColor.current,
                        )
                        Text(selected.title)
                    }
                }
            }
        }
        rule.onNodeWithTag("navigation_rail").assertIsDisplayed()
        rule.onNodeWithText("حساب").performScrollTo().performClick()
        assertEquals(AppTab.ACCOUNT, selected)
        rule.onNodeWithTag("rail_more").performScrollTo().performClick()
        assertTrue(more)
        rule
            .onRoot()
            .captureRoboImage("build/outputs/visual-review/navigation-tablet-account-dark.png")
    }
}

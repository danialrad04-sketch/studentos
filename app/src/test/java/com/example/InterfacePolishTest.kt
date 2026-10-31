package com.example

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.AppEmojiType
import com.example.ui.components.StudentInlineHelp
import com.example.ui.theme.StudentOsTheme
import com.example.ui.v3.StudentV3Dialog
import com.example.ui.v3.V3Card
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w320dp-h800dp-xxhdpi")
class InterfacePolishTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun dismissedGuidanceStaysClosedWhenScreenReopensAndCanBeReopened() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context
            .getSharedPreferences("student_inline_help", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        var visible by mutableStateOf(true)
        rule.setContent {
            StudentOsTheme { if (visible) StudentInlineHelp("test", "راهنمای کوتاه") }
        }
        rule.onNodeWithText("راهنمای کوتاه").assertIsDisplayed()
        rule.onNodeWithTag("help_close_test").performClick()
        rule.onNodeWithText("راهنمای کوتاه").assertDoesNotExist()
        rule.runOnIdle { visible = false }
        rule.waitForIdle()
        rule.runOnIdle { visible = true }
        rule.onNodeWithText("راهنمای کوتاه").assertDoesNotExist()
        rule.onNodeWithTag("help_open_test").performClick()
        rule.onNodeWithText("راهنمای کوتاه").assertIsDisplayed()
    }

    @Test fun initialTourUsesReadableLightFields() {
        rule.setContent {
            StudentOsTheme(darkTheme = false) {
                com.example.ui.components.FirstTimeAppTourDialog(
                    com.example.data.local.entity.StudentProfileEntity(name = "دانشجو"),
                    { _, _, _, _ -> }, {},
                )
            }
        }
        rule.onNodeWithText("مرحله بعد").performScrollTo().performClick()
        rule.onNodeWithText("شماره دانشجویی").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("معدل هدف ترم (از 20)").performScrollTo().assertIsDisplayed()
        rule.onAllNodes(isDialog()).onFirst().captureRoboImage("build/outputs/visual-review/polish-tour-light.png")
    }

    private fun workspace(dark: Boolean) {
        org.robolectric.RuntimeEnvironment.setFontScale(2f)
        var renderedScale = 0f
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                StudentOsTheme(darkTheme = dark) {
                    StudentV3Dialog("کتابخانه و یادداشت‌های درس", {}) {
                        renderedScale = LocalDensity.current.fontScale
                        V3Card(
                            "جزوه‌های ترمودینامیک مهندسی",
                            "فایل‌ها و یادداشت‌های این درس",
                            AppEmojiType.BOOK,
                        ) {
                            OutlinedTextField(
                                "۱۲",
                                {},
                                label = { Text("شمارهٔ صفحه") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button({}, Modifier.fillMaxWidth()) { Text("ذخیرهٔ یادداشت") }
                        }
                    }
                }
            }
        }
        rule.onNodeWithText("ذخیرهٔ یادداشت").performScrollTo().assertIsDisplayed()
        rule.runOnIdle { org.junit.Assert.assertEquals(2f, renderedScale, 0.01f) }
        rule
            .onNodeWithTag("v3_dialog")
            .captureRoboImage(
                "build/outputs/visual-review/polish-${if(dark) "dark" else "light"}-320-large.png"
            )
    }

    @Test fun narrowLightWorkspaceWithLargeText() = workspace(false)

    @Test fun narrowDarkWorkspaceWithLargeText() = workspace(true)
}

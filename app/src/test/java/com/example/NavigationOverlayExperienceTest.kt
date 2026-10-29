package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.components.LocalStudentNavigationClearance
import com.example.ui.components.StudentAppScaffold
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
class NavigationOverlayExperienceTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun pagePaintsBehindNavigationAndItsFinalActionScrollsAboveTheCapsule() {
        val backdrop = Color(0xFF237D8C)
        var saved = false
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                StudentOsTheme {
                    StudentAppScaffold(AppTab.DASHBOARD, {}) {
                        Column(
                            Modifier.fillMaxSize()
                                .background(backdrop)
                                .testTag("scroll_viewport")
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                                .padding(bottom = LocalStudentNavigationClearance.current)
                        ) {
                            Text("محتوای صفحه تا پشت نوار ادامه دارد", color = Color.White)
                            Spacer(Modifier.height(900.dp))
                            Button(
                                onClick = { saved = true },
                                modifier = Modifier.testTag("last_action"),
                            ) {
                                Text("ذخیرهٔ آخرین بخش")
                            }
                        }
                    }
                }
            }
        }
        val nav = rule.onNodeWithTag("primary_navigation").fetchSemanticsNode().boundsInRoot
        val viewport = rule.onNodeWithTag("scroll_viewport").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "The page viewport must extend beneath the floating bar",
            viewport.bottom > nav.bottom,
        )
        val screenshot = "build/outputs/visual-review/navigation-overlay-content.png"
        rule
            .onRoot()
            .captureRoboImage(
                screenshot,
                roborazziOptions =
                    com.github.takahirom.roborazzi.RoborazziOptions(
                        taskType = com.github.takahirom.roborazzi.RoborazziTaskType.Record
                    ),
            )
        val bitmap = android.graphics.BitmapFactory.decodeFile(screenshot)
        val edge = Color(bitmap.getPixel(2, nav.center.y.toInt()))
        assertEquals(
            "No opaque full-width backplate outside the capsule",
            backdrop.red,
            edge.red,
            0.02f,
        )
        assertEquals(backdrop.green, edge.green, 0.02f)
        assertEquals(backdrop.blue, edge.blue, 0.02f)
        rule.onRoot().captureRoboImage("build/outputs/visual-review/navigation-overlay-content.png")
        rule.onNodeWithTag("last_action").scrollAboveNavigation(rule).assertIsDisplayed()
        val action = rule.onNodeWithTag("last_action").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Scroll-end clearance must keep the last button above navigation",
            action.bottom <= nav.top,
        )
        rule.onNodeWithTag("last_action").performClick()
        assertTrue(saved)
    }
}

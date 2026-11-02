package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.GradeEntity
import com.example.ui.components.CourseGradeCard
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
@Config(sdk = [35], qualifiers = "w320dp-h800dp-xxhdpi")
class GradeLayoutPolishTest {
    @get:Rule val rule = createComposeRule()
    private fun render(dark: Boolean, scale: Float) {
        val title = "روش‌های پیشرفتهٔ تحلیل عددی و کاربردهای مهندسی"
        var edited = false
        rule.setContent { StudentOsTheme(darkTheme = dark) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, scale)) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                        CourseGradeCard(GradeEntity(courseName = title, units = 3, midtermGrade = 6.0, finalGrade = 14.0), 20.0, true, { edited = true })
                    }
                }
            }
        } }
        val layout = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(title, useUnmergedTree = true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layout) }
        assertFalse(layout.single().hasVisualOverflow)
        rule.onNodeWithText("20.0", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("ویرایش نمره", useUnmergedTree = true).performScrollTo().performClick()
        assertTrue(edited)
        rule.onRoot().captureRoboImage("build/outputs/visual-review/grade-layout-$dark-$scale.png")
    }
    @Test fun lightNarrowCard() = render(false, 1f)
    @Test fun darkLargeCard() = render(true, 2f)
}

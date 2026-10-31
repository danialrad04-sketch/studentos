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
import com.example.data.local.entity.*
import com.example.ui.components.*
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
@Config(sdk = [35], qualifiers = "w320dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScheduleCardPolishTest {
    @get:Rule val rule = createComposeRule()
    private val course = CourseEntity(id = "a", name = "مبانی ترمودینامیک مهندسی و کاربردهای پیشرفته", professor = "دکتر محمدی", colorHex = "#FFFF99")
    private val session = CourseSessionEntity(courseId = "a", day = 0, start = "12:15", end = "13:15", location = "ساختمان مهندسی، طبقهٔ دوم، کلاس ۲۰۴")

    private fun cards(dark: Boolean, scale: Float) {
        var opened = false
        var toggled = false
        rule.setContent { StudentOsTheme(darkTheme = dark) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, scale)) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CourseCard(course, session, hasConflict = true, onClick = {}, onEdit = {}, onDelete = {}, onOpenWorkspace = { opened = true })
                        TaskCard(TaskEntity(id = 1, title = "حل تمرین فصل سوم و آماده‌سازی گزارش کامل آزمایشگاه", courseName = course.name, dueDate = "۱۴۰۵/۰۸/۱۵"), { toggled = true }, {})
                    }
                }
            }
        } }
        val layouts = mutableListOf<TextLayoutResult>()
        rule.onAllNodesWithText(course.name).onFirst().performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertFalse(layouts.first().hasVisualOverflow)
        rule.onNodeWithText("کارپوشهٔ درس").performScrollTo().performClick()
        assertTrue(opened)
        rule.onNode(isToggleable()).performScrollTo().performClick()
        assertTrue(toggled)
        rule.onRoot().captureRoboImage("build/outputs/visual-review/layout-cards-${dark}-${scale}.png")
    }
    @Test fun lightCards() = cards(false, 1f)
    @Test fun darkLargeCards() = cards(true, 2f)

    @Test fun gridIncludesMiddayAndEachConflictingCourseOpensItsOwnDetails() {
        val other = course.copy(id = "b", name = "آزمایشگاه انتقال حرارت")
        val items = listOf(ScheduleItem(course, session), ScheduleItem(other, session.copy(courseId = "b")))
        val ranges = weeklyGridSlots(items).map { it.second }
        assertTrue(ranges.any { it.first == 735 && it.second == 795 })
        var opened = ""
        rule.setContent { StudentOsTheme { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { WeeklyGridMatrixView(items) { opened = it.id } } } }
        rule.onAllNodesWithText(other.name).onFirst().performScrollTo().performClick()
        assertEquals("b", opened)
        rule.onRoot().captureRoboImage("build/outputs/visual-review/layout-grid-midday.png")
    }
}

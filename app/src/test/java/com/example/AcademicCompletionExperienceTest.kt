package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.example.ui.theme.StudentOsTheme
import com.example.ui.components.*
import com.example.data.local.entity.*
import com.example.data.parser.ParsedCourseDraft
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
class AcademicCompletionExperienceTest {
    @get:Rule val rule = createComposeRule()
    private val course = CourseEntity(id = "math", name = "ریاضی مهندسی", colorHex = "#59652F", units = 3)
    @Test fun unscheduledCoursesRemainReachableForCompletingTheirTimetable() {
        var edited: String? = null
        rule.setContent { StudentOsTheme { WeeklyScheduleScreen(listOf(course), onAddCourse = {}, onEditCourse = { edited = it.id }) } }
        rule.onNodeWithText("1 درس بدون زمان کلاس").assertIsDisplayed()
        rule.onNodeWithText("تعیین زمان ریاضی مهندسی").performClick()
        assertEquals(course.id, edited)
    }
    @Test fun revisitingSetupPrefillsSummaryAndAcceptsPersianNumbersWithoutResettingThem() {
        var saved: Pair<Int, Double?>? = null
        rule.setContent {
            StudentOsTheme {
                OnboardingScreen(
                    onCompleteQuickSetup = { _, _, _, _, _, _, units, gpa, _ -> saved = units to gpa },
                    initialName = "دانشجو", initialUniversity = "دانشگاه من", initialMajor = "رشته من",
                    initialEntryYear = 1403, initialSemester = 3, initialPassedCredits = 42, initialGpa = 17.25, revisiting = true
                )
            }
        }
        rule.onNodeWithText("مرحله بعد: استقرار برنامه ترم 🚀").performScrollTo().performClick()
        rule.onNode(hasText("معدل کل فعلی (از ۲۰)") and hasSetTextAction()).performScrollTo().assertTextContains("17.25").performTextReplacement("۲۱")
        rule.onNodeWithText("مرحله نهایی: بازبینی و ساخت سیستم‌عامل 🚀").performScrollTo().assertIsNotEnabled()
        rule.onNode(hasText("معدل کل فعلی (از ۲۰)") and hasSetTextAction()).performScrollTo().performTextReplacement("۱۸٫۵")
        rule.onNode(hasText("واحد پاس‌شده") and hasSetTextAction()).performScrollTo().assertTextContains("42").performTextReplacement("۵۴")
        rule.onRoot().captureRoboImage("build/outputs/visual-review/onboarding-revisit-light.png")
        rule.onNodeWithText("مرحله نهایی: بازبینی و ساخت سیستم‌عامل 🚀").performScrollTo().performClick()
        rule.onNodeWithText("ذخیره و ادامه").performScrollTo().performClick()
        assertEquals(54 to 18.5, saved)
    }
    @Test fun customAttendanceLimitIsEditableAndValidatedInLightTheme() {
        var saved: Int? = null
        rule.setContent { StudentOsTheme { AttendanceCard("ریاضی", 2, 3, {}, {}, onSetLimit = { saved = it }) } }
        rule.onNodeWithText("تنظیم سقف غیبت").performClick()
        val input = rule.onNode(hasText("تعداد جلسه (۱ تا ۱۶)") and hasSetTextAction())
        input.performTextReplacement("۰")
        rule.onNodeWithText("ذخیره سقف").assertIsNotEnabled()
        input.performTextReplacement("۵")
        rule.onNodeWithText("ذخیره سقف").performClick()
        assertEquals(5, saved)
    }
    @Test fun firstGradeSupportsPersianNumbersAndRejectsTotalAboveTwenty() {
        var saved: Pair<Double, Double>? = null
        rule.setContent { StudentOsTheme { RecordGradeDialog(course, null, {}, { _, mid, fin -> saved = mid to fin }) } }
        rule.onNodeWithTag("save_grade").assertIsNotEnabled()
        rule.onNodeWithTag("grade_mid").performTextInput("۶")
        rule.onNodeWithTag("grade_final").performTextInput("۱۵")
        rule.onNodeWithTag("save_grade").assertIsNotEnabled()
        rule.onNodeWithTag("grade_final").performTextReplacement("۱۳٫۵")
        rule.onNodeWithTag("save_grade").performClick()
        assertEquals(6.0 to 13.5, saved)
    }
    @Test fun completedTaskEditorPrefillsAndSavesActualChanges() {
        var saved: String? = null
        val task = TaskEntity(id = 4, title = "تمرین قبلی", courseName = course.name, dueDate = "1405/07/20", isCompleted = true)
        rule.setContent { StudentOsTheme { AddTaskDialog(listOf(course.name), {}, { title, _, _ -> saved = title }, initialTask = task) } }
        rule.onNodeWithTag("task_title").assertTextContains("تمرین قبلی")
        rule.onNodeWithTag("task_title").performTextReplacement("تمرین اصلاح‌شده")
        rule.onNodeWithTag("save_task").performClick()
        assertEquals("تمرین اصلاح‌شده", saved)
    }
    @Test fun emptyExamScreenOffersWorkingAddAction() {
        var opened = false
        rule.setContent { StudentOsTheme { ExamsScreen(emptyList(), onSetReminder = {}, onAddExam = { opened = true }) } }
        rule.onNodeWithTag("add_exam").performClick()
        assertTrue(opened)
        rule.onRoot().captureRoboImage("build/outputs/visual-review/exams-empty-light.png")
    }
    @Test fun largeFontDarkGradeFormKeepsSaveReachableAndRecordedZeroValid() {
        rule.setContent {
            val current = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(current.density, 1.5f)) {
                StudentOsTheme(darkTheme = true) { RecordGradeDialog(course, GradeEntity(courseId = course.id, isRecorded = true), {}, { _, _, _ -> }) }
            }
        }
        rule.onNodeWithTag("save_grade").assertIsEnabled().assertIsDisplayed()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/grade-form-dark-large-font.png")
    }
    @Test fun importPreviewCanEditCourseRatherThanOnlyDeleteIt() {
        var result: ParsedCourseDraft? = null
        val draft = ParsedCourseDraft(name = "نام اشتباه", startTime = "08:00", endTime = "10:00")
        rule.setContent { StudentOsTheme { ImportDraftEditor(draft, {}, { result = it }) } }
        rule.onNodeWithTag("import_draft_name").performTextReplacement("ریاضی مهندسی")
        rule.onNodeWithTag("save_import_draft").performClick()
        assertEquals("ریاضی مهندسی", result?.name)
        assertEquals(draft.tempId, result?.tempId)
    }
}

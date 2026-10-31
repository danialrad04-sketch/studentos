package com.example

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.v3.*
import com.example.ui.components.LocalStudentNavigationClearance
import com.example.ui.components.StudentAppScaffold
import com.example.ui.models.AppTab
import com.example.ui.theme.StudentOsTheme
import com.example.ui.v3.*
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi", application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StudentV3ExperienceTest {
    @get:Rule val rule = createComposeRule()
    @get:Rule(order = 2)
    val failureImage =
        object : org.junit.rules.TestWatcher() {
            override fun failed(error: Throwable, description: org.junit.runner.Description) {
                val path = "build/outputs/visual-review/v3-failure-${description.methodName}.png"
                runCatching { rule.onNodeWithTag("v3_attempt").captureRoboImage(path) }
                    .recoverCatching { rule.onRoot().captureRoboImage(path) }
            }
        }
    private lateinit var db: AppDatabase
    private val viewModels = androidx.lifecycle.ViewModelStore()
    private lateinit var vm: StudentV3ViewModel
    private val app: Application
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() = runBlocking {
        db =
            Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        db.studentDao()
            .insertCourse(CourseEntity(id = "math", name = "ریاضی مهندسی و کاربردهای تحلیلی"))
        vm = StudentV3ViewModel(app, canWrite = { true }, database = db)
        viewModels.put("v3", vm)
    }

    @After
    fun close() {
        viewModels.clear()
        db.close()
    }

    private fun mount(section: V3Section, dark: Boolean = false, font: Float = 1f) {
        rule.setContent {
            StudentOsTheme(darkTheme = dark) {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, font)
                ) {
                    StudentAppScaffold(AppTab.LIBRARY, {}) {
                        Column(
                            Modifier.fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                                .padding(bottom = LocalStudentNavigationClearance.current)
                        ) {
                            StudentV3WorkspaceContent(vm, {}, section)
                        }
                    }
                }
            }
        }
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("v3_search").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun practiceDurationShowsInlineErrorAndRejectsOutOfRangeValues() {
        mount(V3Section.PRACTICE)
        val duration = rule.onNodeWithText("زمان آزمون (دقیقه)")
        duration.scrollAboveNavigation(rule).performTextReplacement("۱۸۱")
        rule.onNodeWithText("عددی بین ۵ تا ۱۸۰ وارد کن.").assertExists()
        rule.onNodeWithTag("v3_start_practice").assertIsNotEnabled()
        duration.performTextReplacement("۲۵")
        rule.onNodeWithText("عددی بین ۵ تا ۱۸۰ وارد کن.").assertDoesNotExist()
    }

    @Test
    fun projectSaveAndStepCompletionWriteRealRoomData() {
        mount(V3Section.PROJECTS)
        rule.onNodeWithTag("v3_add_project").scrollAboveNavigation(rule).performClick()
        rule.onNodeWithText("نام پروژه").performTextInput("پروژهٔ تحلیل داده")
        rule.onNodeWithText("زمان لازم (دقیقه)").performTextInput("۶۰")
        rule.onNodeWithTag("v3_editor_save").assertIsDisplayed().performClick()
        rule.waitUntil(15000) {
            runBlocking(Dispatchers.IO) { db.studentDao().projectsSnapshot().size == 1 }
        }
        assertEquals(
            60,
            runBlocking { db.studentDao().projectsSnapshot().single().estimatedMinutes },
        )
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("v3_editor_save").fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithText("باز کردن پروژه").scrollAboveNavigation(rule).performClick()
        rule.onNodeWithTag("v3_add_step").scrollAboveNavigation(rule).performClick()
        rule.onNodeWithText("این مرحله چه کاری دارد؟").performTextInput("آماده‌کردن داده‌ها")
        rule.onNodeWithTag("v3_editor_save").assertIsDisplayed().performClick()
        rule.waitUntil(15000) {
            runBlocking(Dispatchers.IO) { db.studentDao().projectStepsSnapshot().isNotEmpty() }
        }
        val stepId = runBlocking { db.studentDao().projectStepsSnapshot().single().id }
        rule.onNodeWithTag("v3_step_$stepId").scrollAboveNavigation(rule).performClick()
        rule.waitUntil(15000) {
            runBlocking(Dispatchers.IO) {
                db.studentDao().projectStepsSnapshot().single().completedAt != null
            }
        }
        rule.waitUntil(15000) {
            rule
                .onNodeWithTag("v3_step_$stepId")
                .fetchSemanticsNode()
                .config[androidx.compose.ui.semantics.SemanticsProperties.ToggleableState] ==
                androidx.compose.ui.state.ToggleableState.On
        }
        rule
            .onNodeWithTag("v3_project_detail")
            .captureRoboImage("build/outputs/visual-review/v3-project-step-light.png")
    }

    @Test
    fun invalidQuestionKeepsItsDraftUntilAValidSave() {
        mount(V3Section.PRACTICE)
        rule.onNodeWithTag("v3_add_question").scrollAboveNavigation(rule).performClick()
        // Let the native sheet reach its anchor before issuing a physical touch.
        rule.mainClock.advanceTimeBy(500)
        rule.waitForIdle()
        rule.onNodeWithText("متن سؤال").performTextInput("قانون دوم چیست؟")
        rule.onNodeWithTag("v3_editor_save").assertIsDisplayed().performClick()
        rule
            .onNodeWithTag("student_modal")
            .captureRoboImage("build/outputs/visual-review/v3-invalid-question-after-touch.png")
        rule.waitUntil(15000) {
            rule.onAllNodesWithText("پاسخ مرجع را وارد کن.").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("پاسخ مرجع را وارد کن.").onLast().assertIsDisplayed()
        rule.onNodeWithText("قانون دوم چیست؟").assertExists()
        assertTrue(runBlocking { db.studentDao().practiceQuestionsSnapshot().isEmpty() })
        rule.onNodeWithText("پاسخ مرجع").performTextInput("پاسخ مرجع کامل")
        rule.onNodeWithTag("v3_editor_save").assertIsDisplayed().performClick()
        rule.waitUntil(15000) {
            runBlocking(Dispatchers.IO) { db.studentDao().practiceQuestionsSnapshot().size == 1 }
        }
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("v3_editor_save").fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithText("قانون دوم چیست؟").assertExists()
    }

    @Test
    fun readerAutomaticallySavesItsNoteAndRestoresTheOpenDocument() {
        val source = java.io.File(app.cacheDir, "reader-restoration.png")
        source.outputStream().use { output ->
            val bitmap =
                android.graphics.Bitmap.createBitmap(
                    200,
                    300,
                    android.graphics.Bitmap.Config.ARGB_8888,
                )
            bitmap.eraseColor(android.graphics.Color.WHITE)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
        }
        val row =
            runBlocking(Dispatchers.IO) {
                vm.files.import(android.net.Uri.fromFile(source), "math", null, { true }) {
                    vm.repository.saveFile(it)
                }
            }
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            StudentOsTheme(darkTheme = false) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                ) {
                    StudentV3WorkspaceContent(vm, {}, V3Section.LIBRARY)
                }
            }
        }
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("v3_open_${row.id}").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("v3_open_${row.id}").scrollAboveNavigation(rule).performClick()
        rule.waitUntil(15000) {
            rule
                .onAllNodesWithContentDescription("صفحهٔ 1 از ${row.title}")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        rule
            .onNodeWithTag("v3_page_note")
            .scrollAboveNavigation(rule)
            .performTextInput("نکتهٔ ذخیره‌شده هنگام خروج")
        rule.onNodeWithContentDescription("بازگشت").performClick()
        rule.waitUntil(15000) {
            runBlocking(Dispatchers.IO) {
                db.studentDao().pageBookmarksSnapshot().any {
                    it.attachmentId == row.id && it.note == "نکتهٔ ذخیره‌شده هنگام خروج"
                }
            }
        }
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("v3_reader").fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithTag("v3_open_${row.id}").scrollAboveNavigation(rule).performClick()
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("v3_reader").fetchSemanticsNodes().isNotEmpty()
        }
        restoration.emulateSavedInstanceStateRestore()
        rule.waitUntil(15000) {
            rule
                .onAllNodesWithContentDescription("صفحهٔ 1 از ${row.title}")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        rule
            .onNodeWithTag("v3_page_note")
            .scrollAboveNavigation(rule)
            .assertTextContains("نکتهٔ ذخیره‌شده هنگام خروج")
        rule
            .onNodeWithTag("v3_reader")
            .captureRoboImage("build/outputs/visual-review/v3-reader-restored-light.png")
    }

    @Test
    fun aWrongPracticeAnswerCanBeSentToTheRealCourseReview() {
        runBlocking {
            vm.repository.question(
                PracticeQuestionEntity(
                    "mistake",
                    "math",
                    "دو و دو؟",
                    StudentV3Json.choices(listOf("چهار", "پنج")),
                    "0",
                    "MCQ",
                )
            )
        }
        mount(V3Section.PRACTICE)
        rule.onNodeWithTag("v3_start_practice").scrollAboveNavigation(rule).performClick()
        rule.waitUntil(15000) { rule.onAllNodesWithText("پنج").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("پنج").performClick()
        rule.waitUntil(15000) {
            runBlocking(Dispatchers.IO) {
                db.studentDao().practiceAttemptsSnapshot().any {
                    StudentV3Json.answers(it.answersJson)["mistake"] == "1"
                }
            }
        }
        rule.waitUntil(15000) {
            !rule
                .onNodeWithTag("v3_finish_practice")
                .fetchSemanticsNode()
                .config
                .contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)
        }
        rule.onNodeWithTag("v3_finish_practice").scrollAboveNavigation(rule).performClick()
        rule.waitUntil(15000) {
            rule.onAllNodesWithText("آزمون پایان یابد؟").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("پایان آزمون").performClick()
        rule.waitUntil(15000) {
            rule.onAllNodesWithText("افزودن به مرور این درس").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("افزودن به مرور این درس").scrollAboveNavigation(rule).performClick()
        rule.waitUntil(15000) {
            runBlocking(Dispatchers.IO) {
                db.studentDao().reviewCardsSnapshot().any {
                    it.id == "practice:mistake" && it.answer == "چهار"
                }
            }
        }
        rule.waitUntil(15000) {
            rule.onAllNodesWithText("در مرور این درس ثبت شد.").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("در مرور این درس ثبت شد.").assertExists()
        rule.onNodeWithText("افزودن به مرور این درس").scrollAboveNavigation(rule).performClick()
        rule.waitUntil(15000) {
            !rule
                .onNodeWithText("افزودن به مرور این درس")
                .fetchSemanticsNode()
                .config
                .contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)
        }
        assertEquals(1, runBlocking(Dispatchers.IO) { db.studentDao().reviewCardsSnapshot().size })
        rule
            .onNodeWithTag("v3_attempt")
            .captureRoboImage("build/outputs/visual-review/v3-mistake-to-review-light.png")
    }

    @Test
    fun actualPracticeTouchPersistsAnswerAndProducesAnImmutableResult() {
        runBlocking {
            vm.repository.question(
                PracticeQuestionEntity(
                    "q1",
                    "math",
                    "دو و دو؟",
                    StudentV3Json.choices(listOf("چهار", "پنج")),
                    "0",
                    "MCQ",
                )
            )
        }
        mount(V3Section.PRACTICE)
        rule.onNodeWithTag("v3_start_practice").scrollAboveNavigation(rule).performClick()
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("v3_attempt").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("چهار").performClick()
        rule.waitUntil(15000) {
            runBlocking(Dispatchers.IO) {
                db.studentDao().practiceAttemptsSnapshot().firstOrNull()?.let {
                    StudentV3Json.answers(it.answersJson)["q1"] == "0"
                } == true
            }
        }
        rule.waitUntil(15000) {
            !rule
                .onNodeWithTag("v3_finish_practice")
                .fetchSemanticsNode()
                .config
                .contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)
        }
        rule.onNodeWithTag("v3_finish_practice").scrollAboveNavigation(rule).performClick()
        rule.waitUntil(15000) {
            rule.onAllNodesWithText("آزمون پایان یابد؟").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("پایان آزمون").performClick()
        rule.waitUntil(15000) {
            runBlocking(Dispatchers.IO) {
                db.studentDao().practiceAttemptsSnapshot().single().finishedAt != null
            }
        }
        rule.waitUntil(15000) {
            rule.onAllNodesWithText("نتیجهٔ ثبت‌شده").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("نتیجهٔ ثبت‌شده").assertExists()
        assertEquals(
            1,
            runBlocking(Dispatchers.IO) {
                db.studentDao().practiceAttemptsSnapshot().single().correctCount
            },
        )
        rule
            .onNodeWithTag("v3_attempt")
            .captureRoboImage("build/outputs/visual-review/v3-practice-result-light.png")
    }

    @Test
    @Config(qualifiers = "w320dp-h800dp-xxhdpi")
    fun darkLargeTextProjectWorkspaceRetainsFullTitlesAndNavigation() {
        runBlocking {
            vm.repository.project(
                ProjectEntity(
                    "p",
                    "math",
                    "پروژهٔ بلندمدت طراحی سامانه و بررسی نتیجه‌های آزمایش",
                    description = "مرحله‌های تحویل، فایل‌ها و تکلیف‌ها کنار هم",
                    estimatedMinutes = 90,
                )
            )
        }
        mount(V3Section.PROJECTS, dark = true, font = 2f)
        rule
            .onNodeWithText("پروژهٔ بلندمدت طراحی سامانه و بررسی نتیجه‌های آزمایش")
            .scrollAboveNavigation(rule)
            .assertIsDisplayed()
        rule.onNodeWithTag("nav_ACCOUNT").assertExists()
        rule.onRoot().captureRoboImage("build/outputs/visual-review/v3-projects-dark-200.png")
    }

    @Test
    fun ownerReplacementClearsAnUnsavedPrivateProjectDraft() {
        var model by mutableStateOf(vm)
        rule.setContent {
            StudentOsTheme {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                ) {
                    StudentV3WorkspaceContent(model, {}, V3Section.PROJECTS)
                }
            }
        }
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("v3_add_project").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("v3_add_project").scrollAboveNavigation(rule).performClick()
        rule.onNodeWithText("نام پروژه").performTextInput("عنوان خصوصی حساب قبلی")
        rule.runOnIdle {
            model =
                StudentV3ViewModel(app, canWrite = { true }, database = db).also {
                    viewModels.put("replacement", it)
                }
        }
        rule.onNodeWithText("عنوان خصوصی حساب قبلی").assertDoesNotExist()
        rule.onNodeWithTag("v3_editor_save").assertDoesNotExist()
    }
}

package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.AppPreferencesRepository
import com.example.ui.StudentViewModel
import com.example.ui.models.AppTab
import org.junit.Rule
import org.junit.Test

/** Exercises the production viewport, including the chat's finite list and actual IME. */
class MainWorkspaceDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private var previousImeSetting: String? = null

    @org.junit.Before
    fun enableSoftwareKeyboardForThisScenario() {
        previousImeSetting =
            shell("settings get secure show_ime_with_hard_keyboard").trim().takeIf {
                it == "0" || it == "1"
            }
        shell("settings put secure show_ime_with_hard_keyboard 1")
        shell("wm dismiss-keyguard")
        // System setup/keyboard windows can retain focus on a freshly booted emulator.
        // Bring this same Activity to the front; never type into an unfocused application.
        rule.activityRule.scenario.onActivity { activity ->
            activity.startActivity(
                android.content
                    .Intent(activity, MainActivity::class.java)
                    .addFlags(
                        android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
            )
        }
        awaitWindowCondition("Activity did not receive input focus before the scenario") {
            it.hasWindowFocus()
        }
    }

    @org.junit.After
    fun restoreSoftwareKeyboardSetting() {
        val previous = previousImeSetting
        shell(
            if (previous == null) "settings delete secure show_ime_with_hard_keyboard"
            else "settings put secure show_ime_with_hard_keyboard $previous"
        )
    }

    private fun shell(command: String): String {
        val descriptor =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                .uiAutomation
                .executeShellCommand(command)
        return android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)
            .bufferedReader()
            .use { it.readText() }
    }

    @Test
    fun chatComposerAndNewWorkspacePagesRemainReachableInTheActualApp() {
        rule.waitUntil(30000) {
            rule.onAllNodesWithTag("local_onboarding").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithTag("primary_navigation").fetchSemanticsNodes().isNotEmpty()
        }
        AppPreferencesRepository.getInstance(rule.activity).setNotificationsEnabled(false)
        val vm = ViewModelProvider(rule.activity)[StudentViewModel::class.java]
        val database = com.example.data.local.AppDatabase.getDatabase(rule.activity)
        kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            database
                .studentDao()
                .insertProfile(
                    com.example.data.local.entity.StudentProfileEntity(
                        name = "دانشجوی آزمون",
                        university = "دانشگاه آزمون",
                        major = "ریاضی",
                        entryYear = 1405,
                        currentSemester = 1,
                        isOnboardingCompleted = true,
                    )
                )
        }
        rule.waitUntil(20000) {
            rule.onAllNodesWithTag("primary_navigation").fetchSemanticsNodes().isNotEmpty()
        }
        rule.runOnIdle { vm.selectTab(AppTab.COPILOT) }
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("copilot_input_field").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("copilot_input_field").performClick().performTextInput("پرسش آزمون")
        rule.activityRule.scenario.onActivity {
            androidx.core.view.WindowCompat.getInsetsController(it.window, it.window.decorView)
                .show(androidx.core.view.WindowInsetsCompat.Type.ime())
        }
        awaitWindowCondition("Real software keyboard was not visible") {
            androidx.core.view.ViewCompat.getRootWindowInsets(it.window.decorView)
                ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
        }
        rule.onNodeWithTag("ai_send").assertIsDisplayed()
        rule.onNodeWithTag("copilot_input_field").assertIsDisplayed()
        hideKeyboardAndAwaitWindowFocus()
        rule.runOnIdle { vm.selectTab(AppTab.AGENDA) }
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("student_agenda_screen").fetchSemanticsNodes().isNotEmpty()
        }
        rule.runOnIdle { vm.selectTab(AppTab.NOTES) }
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("study_notes_screen").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("note_add").performScrollTo().performClick()
        rule.onNodeWithTag("note_title").performTextInput("یادداشت روی دستگاه")
        rule.onNodeWithTag("note_content").performTextInput("ذخیرهٔ واقعی و آفلاین")
        rule.onNodeWithTag("note_save").performScrollTo().performClick()
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("note_save").fetchSemanticsNodes().isEmpty()
        }
        rule.waitUntil(15000) {
            rule.onAllNodesWithText("یادداشت روی دستگاه").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("یادداشت روی دستگاه").assertExists()
        hideKeyboardAndAwaitWindowFocus()
        AppTab.entries.forEach { tab ->
            rule.runOnIdle { vm.selectTab(tab) }
            rule.waitUntil(15000) {
                rule.onAllNodesWithText(tab.title).fetchSemanticsNodes().isNotEmpty()
            }
            rule.waitForIdle()
            rule.onNodeWithTag("primary_navigation").assertExists()
        }
    }

    @Test
    fun studyGoalAndCourseReviewUseRealRoomThroughTheProductionNavigation() {
        rule.waitUntil(30000) {
            rule.onAllNodesWithTag("local_onboarding").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithTag("primary_navigation").fetchSemanticsNodes().isNotEmpty()
        }
        AppPreferencesRepository.getInstance(rule.activity).setNotificationsEnabled(false)
        val vm = ViewModelProvider(rule.activity)[StudentViewModel::class.java]
        val db = com.example.data.local.AppDatabase.getDatabase(rule.activity)
        val today = com.example.data.repository.learningToday()
        val courseId = "device-learning-course"
        kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            val dao = db.studentDao()
            dao.insertProfile(
                com.example.data.local.entity.StudentProfileEntity(
                    name = "دانشجوی مرور",
                    isOnboardingCompleted = true,
                )
            )
            val semester =
                dao.getCurrentSemesterSync()
                    ?: com.example.data.local.entity
                        .SemesterEntity("learning-term", "ترم آزمون", isCurrent = true)
                        .also { dao.insertSemester(it) }
            dao.deleteCourseById(courseId)
            dao.cleanupOrphans()
            dao.insertCourse(
                com.example.data.local.entity.CourseEntity(
                    id = courseId,
                    name = "درس مرور دستگاهی",
                    semesterId = semester.id,
                )
            )
            dao.insertCourseSession(
                com.example.data.local.entity.CourseSessionEntity(
                    id = "device-learning-class",
                    courseId = courseId,
                    day = 0,
                    start = "09:00",
                    end = "11:00",
                )
            )
            dao.upsertReviewCard(
                com.example.data.local.entity.ReviewCardEntity(
                    "device-review-card",
                    courseId,
                    "پرسش روی دستگاه",
                    "پاسخ محفوظ روی دستگاه",
                    today.format(),
                )
            )
        }
        rule.waitUntil(20000) {
            rule.onAllNodesWithTag("primary_navigation").fetchSemanticsNodes().isNotEmpty()
        }
        rule.waitUntil(15000) { vm.courses.value.any { it.id == courseId } }
        rule.runOnIdle { vm.openStudyPlanner() }
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("learning_planner").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("learning_add_goal").performScrollTo().performClick()
        rule.onNodeWithText("درس مرور دستگاهی").performScrollTo().performClick()
        rule.onNodeWithTag("learning_goal_minutes").performTextInput("۱۲۰")
        rule
            .onNodeWithTag("learning_goal_date")
            .performTextInput(
                com.example.domain.engine.StudentAgendaEngine.addDays(today, 7).toPersianDigits()
            )
        rule.onNodeWithTag("learning_goal_save").performScrollTo().performClick()
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("learning_goal_save").fetchSemanticsNodes().isEmpty()
        }
        hideKeyboardAndAwaitWindowFocus()
        kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            org.junit.Assert.assertEquals(
                120,
                db.studentDao().studyGoalsSnapshot().single { it.courseId == courseId }.totalMinutes,
            )
        }
        rule.runOnIdle { vm.selectTab(AppTab.SCHEDULE) }
        rule.waitUntil(15000) {
            rule.onAllNodesWithText("درس مرور دستگاهی").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("درس مرور دستگاهی").performScrollTo().performClick()
        rule.onNodeWithTag("course_review_$courseId").performScrollTo().performClick()
        rule.onNodeWithTag("review_reveal").performScrollTo().performClick()
        rule.onNodeWithTag("review_answer").assertTextEquals("پاسخ محفوظ روی دستگاه")
        rule.onNodeWithTag("review_rate_GOOD").performScrollTo().performClick()
        rule.waitUntil(15000) {
            kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                db.studentDao()
                    .reviewCardsSnapshot()
                    .single { it.id == "device-review-card" }
                    .repetitions == 1
            }
        }
        kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            val card =
                db.studentDao().reviewCardsSnapshot().single { it.id == "device-review-card" }
            org.junit.Assert.assertEquals(
                com.example.domain.engine.StudentAgendaEngine.addDays(today, 1).format(),
                card.dueDate,
            )
        }
    }

    private fun hideKeyboardAndAwaitWindowFocus() {
        rule.activityRule.scenario.onActivity {
            androidx.core.view.WindowCompat.getInsetsController(it.window, it.window.decorView)
                .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
        }
        awaitWindowCondition("Keyboard did not hide or Activity did not regain focus") {
            val imeVisible =
                androidx.core.view.ViewCompat.getRootWindowInsets(it.window.decorView)
                    ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime()) == true
            !imeVisible && it.hasWindowFocus()
        }
    }

    private fun awaitWindowCondition(message: String, condition: (MainActivity) -> Boolean) {
        try {
            rule.waitUntil(15000) {
                var satisfied = false
                rule.activityRule.scenario.onActivity { satisfied = condition(it) }
                satisfied
            }
        } catch (error: androidx.compose.ui.test.ComposeTimeoutException) {
            val windows =
                shell("dumpsys window windows")
                    .lineSequence()
                    .filter {
                        it.contains("mCurrentFocus") ||
                            it.contains("mFocusedApp") ||
                            it.contains("mHasSurface") ||
                            it.contains("mObscuringWindow")
                    }
                    .take(20)
                    .joinToString("\n")
            throw AssertionError("$message\n$windows", error)
        }
    }
}

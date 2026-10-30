package com.example

import android.app.Activity
import android.app.Instrumentation
import android.content.ContentValues
import android.content.Intent
import android.content.IntentFilter
import android.graphics.pdf.PdfDocument
import android.provider.MediaStore
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.AppPreferencesRepository
import com.example.data.v3.*
import com.example.ui.StudentViewModel
import com.example.ui.models.AppTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** Uses the production activity, repositories, file store and platform PDF renderer. */
class StudentV3DeviceTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @get:Rule(order = 2)
    val diagnostics =
        object : org.junit.rules.TestWatcher() {
            override fun failed(error: Throwable, description: org.junit.runner.Description) {
                evidence("failure-${description.methodName}")
            }
        }
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val db
        get() = AppDatabase.getDatabase(rule.activity)

    private val courseId = "v3-device-course"
    private var previousNotifications = true
    private var previousBackup = true

    @Before
    fun prepare() {
        val prefs = AppPreferencesRepository.getInstance(rule.activity)
        previousNotifications = prefs.notificationsEnabled.value
        previousBackup = prefs.isAutoBackupEnabled.value
        prefs.setAutoBackupEnabled(false)
        prefs.setNotificationsEnabled(false)
        runBlocking(Dispatchers.IO) {
            db.studentDao()
                .insertProfile(
                    StudentProfileEntity(name = "دانشجوی نسخهٔ سه", isOnboardingCompleted = true)
                )
            db.studentDao().insertCourse(CourseEntity(id = courseId, name = "درس آزمون نسخهٔ سه"))
        }
        rule.waitUntil(30000) {
            rule.onAllNodesWithTag("primary_navigation").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @After
    fun cleanup() {
        AppPreferencesRepository.getInstance(rule.activity).apply {
            setNotificationsEnabled(previousNotifications)
            setAutoBackupEnabled(previousBackup)
        }
    }

    private fun select(tab: AppTab) {
        rule.runOnIdle {
            ViewModelProvider(rule.activity)[StudentViewModel::class.java].selectTab(tab)
        }
    }

    private fun chooseCourse() {
        rule.waitUntil(15000) {
            rule.onAllNodesWithText("درس آزمون نسخهٔ سه").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("درس آزمون نسخهٔ سه").onFirst().performScrollTo().performClick()
    }

    private fun evidence(name: String) {
        val directory =
            java.io.File("/sdcard/Download/studentos-navigation-diagnostics").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            java.io.File(directory, "v3-$name.png").outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
    }

    @Test
    fun selectedPdfRendersAndPageNoteSurvivesActivityRecreation() {
        val resolver = rule.activity.contentResolver
        val uri =
            checkNotNull(
                resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, "StudentOS-device-v3.pdf")
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    },
                )
            )
        try {
            val document = PdfDocument()
            try {
                repeat(2) { index ->
                    val page =
                        document.startPage(
                            PdfDocument.PageInfo.Builder(400, 500, index + 1).create()
                        )
                    page.canvas.drawText(
                        "V3 native page ${index+1}",
                        30f,
                        100f,
                        android.graphics.Paint().apply { textSize = 24f },
                    )
                    document.finishPage(page)
                }
                resolver.openOutputStream(uri)!!.use(document::writeTo)
            } finally {
                document.close()
            }
            resolver.update(
                uri,
                ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
                null,
                null,
            )
            select(AppTab.LIBRARY)
            chooseCourse()
            // The selected document is explicit. OEM picker interaction is covered by signed smoke.
            val filter =
                IntentFilter(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    addDataType("*/*")
                }
            val monitor =
                instrumentation.addMonitor(
                    filter,
                    Instrumentation.ActivityResult(
                        Activity.RESULT_OK,
                        Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                    ),
                    true,
                )
            try {
                rule.onNodeWithTag("v3_import_file").scrollAboveNavigation(rule).performClick()
                rule.waitUntil(20000) {
                    runBlocking(Dispatchers.IO) {
                        db.studentDao().attachmentsSnapshot().any {
                            it.courseId == courseId && it.title == "StudentOS-device-v3.pdf"
                        }
                    }
                }
                assertEquals(1, monitor.hits)
            } finally {
                instrumentation.removeMonitor(monitor)
            }
            val file =
                runBlocking(Dispatchers.IO) {
                    db.studentDao().attachmentsSnapshot().first {
                        it.courseId == courseId && it.title == "StudentOS-device-v3.pdf"
                    }
                }
            assertEquals(2, file.pageCount)
            rule.onNodeWithTag("v3_open_${file.id}").scrollAboveNavigation(rule).performClick()
            rule.waitUntil(15000) {
                rule
                    .onAllNodesWithContentDescription("صفحهٔ 1 از ${file.title}")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            rule.onNodeWithText("صفحهٔ بعد").performClick()
            rule.waitUntil(15000) {
                rule
                    .onAllNodesWithContentDescription("صفحهٔ 2 از ${file.title}")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            rule
                .onNodeWithTag("v3_page_note")
                .performScrollTo()
                .performTextInput("نکتهٔ پایدار صفحهٔ دوم")
            rule.onNodeWithTag("v3_save_bookmark").performScrollTo().performClick()
            rule.waitUntil(15000) {
                runBlocking(Dispatchers.IO) {
                    db.studentDao().pageBookmarksSnapshot().any {
                        it.attachmentId == file.id &&
                            it.page == 1 &&
                            it.note == "نکتهٔ پایدار صفحهٔ دوم"
                    }
                }
            }
            rule.activityRule.scenario.recreate()
            // Both the open reader and its saved page must be restored by the real Activity.
            rule.waitUntil(20000) {
                rule
                    .onAllNodesWithContentDescription("صفحهٔ 2 از ${file.title}")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            rule
                .onNodeWithTag("v3_page_note")
                .performScrollTo()
                .assertTextContains("نکتهٔ پایدار صفحهٔ دوم")
            assertEquals(
                1,
                runBlocking(Dispatchers.IO) {
                    db.studentDao().attachmentsSnapshot().first { it.id == file.id }.lastPage
                },
            )
            evidence("pdf-restored")
        } finally {
            resolver.delete(uri, null, null)
        }
    }

    @Test
    fun practiceAnswerAndResultPersistThroughTheRealExamPage() {
        runBlocking(Dispatchers.IO) {
            StudentV3Repository(db)
                .question(
                    PracticeQuestionEntity(
                        "v3-device-question",
                        courseId,
                        "پاسخ درست کدام است؟",
                        StudentV3Json.choices(listOf("گزینهٔ درست دستگاه", "گزینهٔ اشتباه")),
                        "0",
                        "MCQ",
                    )
                )
        }
        select(AppTab.EXAMS)
        rule.onNodeWithText("تمرین و آزمون").scrollAboveNavigation(rule).performClick()
        chooseCourse()
        rule.onNodeWithTag("v3_start_practice").scrollAboveNavigation(rule).performClick()
        rule.waitUntil(15000) {
            rule.onAllNodesWithTag("v3_attempt").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("گزینهٔ درست دستگاه").performClick()
        rule.waitUntil(15000) {
            runBlocking(Dispatchers.IO) {
                db.studentDao().practiceAttemptsSnapshot().any {
                    it.courseId == courseId &&
                        StudentV3Json.answers(it.answersJson)["v3-device-question"] == "0"
                }
            }
        }
        rule.onNodeWithTag("v3_finish_practice").performScrollTo().performClick()
        rule.onNodeWithText("پایان آزمون").performClick()
        rule.waitUntil(15000) {
            runBlocking(Dispatchers.IO) {
                db.studentDao().practiceAttemptsSnapshot().any {
                    it.courseId == courseId && it.finishedAt != null && it.correctCount == 1
                }
            }
        }
        rule.onNodeWithText("نتیجهٔ ثبت‌شده").assertExists()
        evidence("practice-result")
    }
}

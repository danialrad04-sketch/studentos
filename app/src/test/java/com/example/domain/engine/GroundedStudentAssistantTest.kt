package com.example.domain.engine

import com.example.data.local.entity.*
import com.example.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GroundedStudentAssistantTest {
    private val profile = StudentProfileEntity(studentId = "PRIVATE-ID", notes = "PRIVATE-NOTES")
    private val course = CourseEntity(id = "math", name = "ریاضی", units = 3, colorHex = "#FFFFFF")
    private val task = TaskEntity(id = 42, title = "تمرین انتگرال", dueDate = "1405/08/04")
    private fun answer(q: String, kind: String = "chat", tasks: List<TaskEntity> = listOf(task)) =
        GroundedStudentAssistant.respond(q, kind, profile, listOf(course), emptyList(), emptyList(), emptyList(), tasks, emptyList())

    @Test fun taskQuestionsNeverCompleteAnArbitraryTask() {
        assertTrue(answer("تکالیف من را بگو")!!.proposedAction!!.payload is CopilotPayload.NavigateToTab)
        assertNull(GroundedStudentAssistant.completionProposal("تکلیف را تکمیل کن", listOf(task)))
        assertNull(GroundedStudentAssistant.completionProposal("آیا تمرین انتگرال را تکمیل کنم؟", listOf(task)))
        assertNull(GroundedStudentAssistant.completionProposal("تمرین انتگرال را تکمیل نکن", listOf(task)))
        assertNull(GroundedStudentAssistant.completionProposal("تمرین انتگرال را انجام دادم", listOf(task, task.copy(id = 43))))
        val proposal = GroundedStudentAssistant.completionProposal("تمرین انتگرال را انجام دادم", listOf(task))!!
        assertEquals("42", (proposal.payload as CopilotPayload.CompleteTask).taskId)
        assertEquals(ActionImpactType.REQUIRES_CONFIRMATION, proposal.impactType)
    }
    @Test fun conceptualQuestionsAreNotMisclassifiedAsPersonalData() {
        assertNull(answer("کلاس در پایتون چیست؟"))
        assertNull(answer("مفهوم انتگرال را توضیح بده"))
    }
    @Test fun contextDistinguishesMissingGradesFromRecordedZeroAndRejectsInvalidScores() {
        val missing = GradeEntity(courseId = course.id)
        val zero = missing.copy(isRecorded = true)
        fun context(grades: List<GradeEntity>) = JSONObject(AcademicAssistantContext.build(profile, listOf(course), emptyList(), emptyList(), grades, emptyList(), emptyList()))
        assertTrue(context(listOf(missing)).isNull("gpaOfRecordedGrades"))
        assertEquals(0.0, context(listOf(zero)).getDouble("gpaOfRecordedGrades"), 0.0)
        assertTrue(context(listOf(zero.copy(finalGrade = Double.NaN), zero.copy(finalGrade = 30.0))).isNull("gpaOfRecordedGrades"))
        val json = context(listOf(zero)).toString()
        assertFalse(json.contains("PRIVATE-ID")); assertFalse(json.contains("PRIVATE-NOTES"))
    }
    @Test fun contextRemainsValidAndBoundedForLargeRecords() {
        val json = AcademicAssistantContext.build(profile, (1..200).map { course.copy(id = "$it", name = "درس".repeat(500)) }, emptyList(), emptyList(), emptyList(), (1..200).map { task.copy(id = it.toLong(), title = "عنوان".repeat(500)) }, emptyList())
        assertTrue(json.length <= 16000)
        assertEquals(200, JSONObject(json).getJSONObject("recordCounts").getInt("openTasks"))
    }
    @Test fun persianStudyBudgetIsRespectedAndNoCalendarWriteIsClaimed() {
        val text = answer("برای هفت روز برنامه مطالعه بساز روزی ۳۰ دقیقه", "coach")!!.text
        assertTrue(text.contains("30 دقیقه"))
        assertFalse(text.contains(": 120 دقیقه"))
        assertTrue(text.contains("هیچ رویدادی ذخیره نشده"))
        assertEquals(7, Regex("\\*\\*[0-9]{4}/[0-9]{2}/[0-9]{2}\\*\\*").findAll(text).count())
    }
    @Test fun tomorrowExamQueryNeverReturnsADifferentDaysExam() {
        val now = java.util.Calendar.getInstance().apply { clear(); set(2026, 9, 25, 12, 0) }.timeInMillis
        val exams = listOf(ExamItem(id = "a", courseName = "امروز", solarDate = "1405/08/03", time = "15:00", location = "", units = 3),
            ExamItem(id = "b", courseName = "فردا", solarDate = "1405/08/04", time = "09:00", location = "", units = 3))
        fun result(rows: List<ExamItem>) = GroundedStudentAssistant.respond("فردا چه امتحانی دارم؟", "chat", profile, listOf(course), emptyList(), emptyList(), emptyList(), emptyList(), rows, now = now)!!.text
        assertTrue(result(exams).contains("مرور فردا"))
        assertFalse(result(exams).contains("مرور امروز"))
        assertTrue(result(exams.take(1)).contains("امتحانی با تاریخ معتبر ثبت نشده"))
    }
    @Test fun localOnlyNeverCallsModelAndFailureNeverClaimsOnlineSuccess() = runBlocking {
        var calls = 0
        val provider: suspend (String, String, List<Pair<String,String>>, String) -> Result<String> = { _, _, _, _ -> calls++; Result.failure(IllegalStateException("unavailable")) }
        val local = AcademicCopilotEngine.processUserQueryAsync("تکالیف من را بگو", profile, listOf(course), emptyList(), emptyList(), listOf(task), emptyList(), localOnly = true, adviceProvider = provider)
        assertEquals(0, calls); assertEquals(CopilotResponseSource.LOCAL_ANALYSIS, local.responseSource)
        val failed = AcademicCopilotEngine.processUserQueryAsync("انتگرال چیست؟", profile, listOf(course), emptyList(), emptyList(), emptyList(), emptyList(), adviceProvider = provider)
        assertEquals(1, calls); assertEquals(CopilotResponseSource.ONLINE_UNAVAILABLE, failed.responseSource)
    }
    @Test fun onlyRealModelAnswersEnterModelHistory() = runBlocking {
        val history = listOf(
            CopilotMessage("1", CopilotSender.COPILOT, "welcome", "00:00"),
            CopilotMessage("2", CopilotSender.USER, "پرسش قبلی", "00:01"),
            CopilotMessage("3", CopilotSender.COPILOT, "پاسخ مدل", "00:02", responseSource = CopilotResponseSource.ONLINE_MODEL),
            CopilotMessage("4", CopilotSender.COPILOT, "خطای اتصال", "00:03", responseSource = CopilotResponseSource.ONLINE_UNAVAILABLE),
            CopilotMessage("5", CopilotSender.COPILOT, "برنامهٔ پیشنهادی محلی", "00:04", responseSource = CopilotResponseSource.LOCAL_ANALYSIS))
        val result = AcademicCopilotEngine.processUserQueryAsync("انتگرال چیست؟", profile, listOf(course), emptyList(), emptyList(), emptyList(), emptyList(), conversationHistory = history,
            adviceProvider = { _, context, turns, _ ->
                assertTrue(JSONObject(context).isNull("gpaOfRecordedGrades"))
                assertEquals("برنامهٔ پیشنهادی محلی", JSONObject(context).getJSONArray("recentLocalAnalysis").getString(0))
                assertEquals(listOf("user" to "پرسش قبلی", "model" to "پاسخ مدل"), turns)
                Result.success("توضیح مدل با مثال")
            })
        assertEquals(CopilotResponseSource.ONLINE_MODEL, result.responseSource)
    }
}

package com.example

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.engine.AcademicRiskEngine
import com.example.domain.engine.SemesterPlannerEngine
import com.example.domain.engine.StudyPlannerEngine
import com.example.domain.engine.WorkloadEngine
import com.example.domain.model.AcademicRiskType
import com.example.domain.model.CourseState
import com.example.domain.model.EvaluatedCurriculumCourse
import com.example.domain.model.RiskSeverity
import com.example.ui.models.ExamItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test Suite for Advanced Roadmap Engines:
 * - AcademicRiskEngine (Phase 21)
 * - WorkloadEngine (Phase 20)
 * - SemesterPlannerEngine (Phase 12 & 13)
 * - StudyPlannerEngine (Phase 28)
 */
class RoadmapEnginesComprehensiveTest {

    @Test
    fun `test AcademicRiskEngine detects attendance breach when absences reach limit`() {
        val riskyCourse = CourseEntity(
            id = "c1",
            name = "ترمودینامیک ۱",
            colorHex = "#4F46E5",
            units = 3
        )
        val safeCourse = CourseEntity(
            id = "c2",
            name = "معادلات دیفرانسیل",
            colorHex = "#4F46E5",
            units = 3
        )

        val attendances = listOf(
            AttendanceEntity(courseId = "c1", courseName = "ترمودینامیک ۱", absentCount = 3, maxAllowed = 3),
            AttendanceEntity(courseId = "c2", courseName = "معادلات دیفرانسیل", absentCount = 0, maxAllowed = 3)
        )

        val risks = AcademicRiskEngine.evaluateRisks(
            courses = listOf(riskyCourse, safeCourse),
            attendanceList = attendances,
            tasks = emptyList(),
            exams = emptyList()
        )

        assertEquals(1, risks.size)
        val risk = risks.first()
        assertEquals(RiskSeverity.CRITICAL, risk.severity)
        assertEquals(AcademicRiskType.ATTENDANCE_BREACH, risk.riskType)
        assertTrue(risk.title.contains("ترمودینامیک ۱"))
        assertFalse(risk.description.isBlank())
    }

    @Test
    fun `test AcademicRiskEngine detects exam collision on same date`() {
        val exam1 = ExamItem(
            id = "e1",
            courseName = "مکانیک سیالات",
            solarDate = "۱۴۰۳/۱۰/۱۵",
            time = "۰۹:۰۰",
            location = "سالن امتحانات",
            units = 3
        )
        val exam2 = ExamItem(
            id = "e2",
            courseName = "ترمودینامیک ۱",
            solarDate = "۱۴۰۳/۱۰/۱۵",
            time = "۱۴:۰۰",
            location = "سالن امتحانات",
            units = 3
        )

        val risks = AcademicRiskEngine.evaluateRisks(
            courses = emptyList(),
            attendanceList = emptyList(),
            tasks = emptyList(),
            exams = listOf(exam1, exam2)
        )

        val examRisk = risks.find { it.riskType == AcademicRiskType.EXAM_COLLISION }
        assertTrue(examRisk != null)
        assertEquals(RiskSeverity.MEDIUM, examRisk?.severity)
        assertTrue(examRisk?.description?.contains("مکانیک سیالات") == true)
    }

    @Test
    fun `test WorkloadEngine calculates lecture and self-study hours accurately`() {
        val courses = listOf(
            CourseEntity(id = "c1", name = "انتقال حرارت", colorHex = "", units = 3),
            CourseEntity(id = "c2", name = "آزمایشگاه فیزیک", colorHex = "", units = 1)
        )
        val tasks = listOf(
            TaskEntity(id = 1L, title = "پروژه هیت اکسچنجر", courseName = "انتقال حرارت", dueDate = "۱۴۰۳/۰۹/۲۰", isCompleted = false)
        )

        val workload = WorkloadEngine.calculateWeeklyWorkload(courses, tasks)

        // total units = 4 -> lecture = 4 * 1.5 = 6.0
        assertEquals(6.0, workload.classLectureHours, 0.01)
        // 1 lab course = 2.5
        assertEquals(2.5, workload.laboratoryHours, 0.01)
        // 1 active task = 1.5
        assertEquals(1.5, workload.assignmentHours, 0.01)
        // self study = 4 * 2.0 = 8.0
        assertEquals(8.0, workload.estimatedSelfStudyHours, 0.01)
        // total = 6.0 + 2.5 + 1.5 + 8.0 = 18.0
        assertEquals(18.0, workload.totalEstimatedWeeklyHours, 0.01)
        assertTrue(workload.isEstimateOnly)
    }

    @Test
    fun `test SemesterPlannerEngine builds candidate plans and compares trade-offs`() {
        val availableCourses = listOf(
            EvaluatedCurriculumCourse("c1", "MATH101", "ریاضی عمومی ۱", 3, "پایه", 1, CourseState.AVAILABLE),
            EvaluatedCurriculumCourse("c2", "PHYS101", "فیزیک ۱", 3, "پایه", 1, CourseState.AVAILABLE),
            EvaluatedCurriculumCourse("c3", "CHEM101", "شیمی عمومی", 3, "پایه", 1, CourseState.AVAILABLE),
            EvaluatedCurriculumCourse("c4", "PROG101", "برنامه‌نویسی کامپیوتر", 3, "پایه", 1, CourseState.AVAILABLE),
            EvaluatedCurriculumCourse("c5", "DRAW101", "نقشه‌کشی صنعتی", 2, "اصلی", 1, CourseState.AVAILABLE),
            EvaluatedCurriculumCourse("c6", "GEN101", "فارسی عمومی", 2, "عمومی", 1, CourseState.AVAILABLE),
            EvaluatedCurriculumCourse("c7", "ENG101", "زبان خارجی", 2, "عمومی", 1, CourseState.AVAILABLE),
            EvaluatedCurriculumCourse("c8", "ISL101", "اندیشه اسلامی ۱", 2, "عمومی", 1, CourseState.AVAILABLE)
        )

        val plans = SemesterPlannerEngine.generateCandidatePlans(availableCourses)
        assertEquals(2, plans.size)

        val planA = plans[0] // Balanced (<= 18 units)
        val planB = plans[1] // Accelerated (<= 21 units)

        assertTrue(planA.totalCredits <= 18)
        assertTrue(planB.totalCredits <= 21)
        assertTrue(planB.totalCredits >= planA.totalCredits)

        val comparison = SemesterPlannerEngine.comparePlans(planA, planB)
        assertEquals(planB.totalCredits - planA.totalCredits, comparison.creditDifference)
        assertFalse(comparison.summaryTradeOff.isBlank())
    }

    @Test
    fun `test StudyPlannerEngine prioritizes exams and pending tasks deterministically`() {
        val exams = listOf(
            ExamItem(id = "e1", courseName = "کنترل فرایند", solarDate = "۱۴۰۳/۱۰/۲۰", time = "۰۸:۳۰", location = "کلاس ۳۰۱", units = 3)
        )
        val tasks = listOf(
            TaskEntity(id = 1L, title = "حل تمرین سری ۴", courseName = "کنترل فرایند", dueDate = "فردا", isCompleted = false),
            TaskEntity(id = 2L, title = "گزارش کار", courseName = "آزمایشگاه عملیات", dueDate = "پس‌فردا", isCompleted = true)
        )

        val studySessions = StudyPlannerEngine.generateStudyPlan(exams, tasks)
        // 1 for exam, 1 for incomplete task (completed is excluded)
        assertEquals(2, studySessions.size)
        assertTrue(studySessions.any { it.courseName == "کنترل فرایند" && it.targetType == "آمادگی آزمون" })
        assertTrue(studySessions.any { it.courseName == "کنترل فرایند" && it.targetType == "تکمیل تکلیف" })
    }
    @Test
    fun `test StudyPlannerEngine removes duplicate recommendations and sorts exams`() {
        val exams = listOf(
            ExamItem(id = "e2", courseName = "فیزیک", solarDate = "۱۴۰۳/۱۰/۲۰", time = "۱۰:۰۰", location = "۲", units = 3),
            ExamItem(id = "e1", courseName = "ریاضی", solarDate = "۱۴۰۳/۱۰/۱۰", time = "۰۹:۰۰", location = "۱", units = 3),
            ExamItem(id = "e1", courseName = "ریاضی", solarDate = "۱۴۰۳/۱۰/۱۰", time = "۰۹:۰۰", location = "۱", units = 3)
        )
        val tasks = listOf(
            TaskEntity(id = 10L, title = "تمرین", courseName = "ریاضی", dueDate = "۱۴۰۳/۱۰/۰۸", isCompleted = false),
            TaskEntity(id = 10L, title = "تمرین", courseName = "ریاضی", dueDate = "۱۴۰۳/۱۰/۰۸", isCompleted = false)
        )

        val sessions = StudyPlannerEngine.generateStudyPlan(exams, tasks)

        assertEquals(3, sessions.size)
        assertEquals(3, sessions.map { it.id }.distinct().size)
        assertEquals("ریاضی", sessions[0].courseName)
        assertEquals("آمادگی آزمون", sessions[0].targetType)
        assertEquals("فیزیک", sessions[1].courseName)
        assertEquals("تکمیل تکلیف", sessions[2].targetType)
    }
}

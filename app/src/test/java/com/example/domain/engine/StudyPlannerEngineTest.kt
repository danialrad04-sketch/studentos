package com.example.domain.engine

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.model.ExamItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyPlannerEngineTest {

    @Test
    fun ranksUrgentTaskBeforeExamAndOrdinaryTask() {
        val recommendations = StudyPlannerEngine.generateStudyPlan(
            exams = listOf(
                ExamItem(
                    id = "exam-1",
                    courseName = "فیزیک",
                    solarDate = "1405/07/25",
                    time = "09:00",
                    location = "سالن",
                    units = 3
                )
            ),
            tasks = listOf(
                TaskEntity(
                    id = 1L,
                    title = "پروژه عقب افتاده",
                    courseName = "فیزیک",
                    dueDate = "1405/07/01",
                    courseId = "physics",
                    isCompleted = false
                ),
                TaskEntity(
                    id = 2L,
                    title = "کار عادی",
                    courseName = "شیمی",
                    dueDate = "1405/08/01",
                    courseId = "chem",
                    isCompleted = false
                )
            ),
            attendanceList = listOf(
                AttendanceEntity(
                    courseId = "physics",
                    courseName = "فیزیک",
                    absentCount = 3,
                    maxAllowed = 3
                )
            ),
            todayDate = "1405/07/21"
        )

        assertTrue(recommendations.size >= 3)
        assertEquals("فیزیک", recommendations.first().courseName)
        assertTrue(recommendations.first().priorityReason.contains("عقب"))
        assertTrue(recommendations.first().priorityReason.contains("غیبت"))
        assertEquals("آمادگی آزمون", recommendations[1].targetType)
    }

    @Test
    fun pastExamIsExcludedFromRecommendations() {
        val recommendations = StudyPlannerEngine.generateStudyPlan(
            exams = listOf(
                ExamItem(
                    id = "past",
                    courseName = "ریاضی",
                    solarDate = "1405/07/01",
                    time = "10:00",
                    location = "سالن",
                    units = 3
                ),
                ExamItem(
                    id = "future",
                    courseName = "فیزیک",
                    solarDate = "1405/08/01",
                    time = "10:00",
                    location = "سالن",
                    units = 3
                )
            ),
            tasks = emptyList(),
            attendanceList = emptyList(),
            todayDate = "1405/07/21"
        )

        assertTrue(recommendations.none { it.courseName == "ریاضی" })
        assertTrue(recommendations.any { it.courseName == "فیزیک" })
    }

    @Test
    fun oldTwoArgumentApiStillReturnsRecommendations() {
        val recommendations = StudyPlannerEngine.generateStudyPlan(
            exams = listOf(
                ExamItem(
                    id = "exam-2",
                    courseName = "ریاضی",
                    solarDate = "1405/07/30",
                    time = "10:00",
                    location = "سالن",
                    units = 3
                )
            ),
            tasks = emptyList()
        )

        assertEquals(1, recommendations.size)
        assertEquals("ریاضی", recommendations.first().courseName)
    }
}

package com.example.domain.engine

import com.example.data.local.entity.TaskEntity
import com.example.domain.model.ExamItem
import com.example.domain.model.AcademicRiskType
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicRiskPlanningIntegrationTest {

    @Test
    fun planningCollisionBecomesAcademicRisk() {
        val tasks = listOf(
            TaskEntity(
                id = 1L,
                title = "تحویل پروژه",
                courseName = "فیزیک",
                dueDate = "1405/07/25",
                courseId = "physics",
                isCompleted = false
            ),
            TaskEntity(
                id = 2L,
                title = "گزارش",
                courseName = "شیمی",
                dueDate = "1405/07/25",
                courseId = "chem",
                isCompleted = false
            )
        )
        val exams = listOf(
            ExamItem(
                id = "exam-1",
                courseName = "فیزیک",
                solarDate = "1405/07/25",
                time = "09:00",
                location = "سالن",
                units = 3
            )
        )

        val risks = AcademicRiskEngine.evaluateRisks(
            courses = emptyList(),
            attendanceList = emptyList(),
            tasks = tasks,
            exams = exams
        )

        assertTrue(risks.any { it.riskType == AcademicRiskType.UPCOMING_DEADLINE })
        assertTrue(risks.any { it.title.contains("امتحان") || it.title.contains("کار") })
    }
}

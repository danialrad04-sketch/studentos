package com.example.domain.engine

import com.example.data.local.entity.TaskEntity
import com.example.domain.model.AcademicPlanningCollisionSeverity
import com.example.domain.model.AcademicPlanningCollisionType
import com.example.domain.model.ExamItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicPlanningCollisionEngineTest {

    @Test
    fun detectsTaskExamAndSameDayTaskCollisions() {
        val tasks = listOf(
            TaskEntity(
                id = 1L,
                title = "تمرین فیزیک",
                courseName = "فیزیک",
                dueDate = "1405/07/25",
                courseId = "physics",
                isCompleted = false
            ),
            TaskEntity(
                id = 2L,
                title = "گزارش آزمایش",
                courseName = "شیمی",
                dueDate = "1405/07/25",
                courseId = "chem",
                isCompleted = false
            )
        )

        val exams = listOf(
            ExamItem(
                id = "exam-physics",
                courseName = "فیزیک",
                solarDate = "1405/07/25",
                time = "09:00",
                location = "سالن",
                units = 3
            )
        )

        val collisions = AcademicPlanningCollisionEngine.detect(tasks, exams)

        assertEquals(2, collisions.size)
        assertTrue(collisions.any { it.type == AcademicPlanningCollisionType.TASK_EXAM })
        assertTrue(collisions.any {
            it.type == AcademicPlanningCollisionType.TASK_TASK &&
                it.severity == AcademicPlanningCollisionSeverity.WARNING
        })
    }
}

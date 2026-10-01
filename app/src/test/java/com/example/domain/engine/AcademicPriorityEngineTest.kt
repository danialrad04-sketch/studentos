package com.example.domain.engine

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.model.AcademicPriorityKind
import com.example.domain.model.ExamItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicPriorityEngineTest {

    @Test
    fun criticalAttendanceBeatsOverdueTaskAndExamLinkedTask() {
        val course = CourseEntity(
            id = "c1",
            name = "ریاضی مهندسی",
            units = 3
        )
        val tasks = listOf(
            TaskEntity(
                id = 1L,
                title = "پروژه",
                courseName = "ریاضی مهندسی",
                dueDate = "1405/06/20",
                courseId = "c1",
                isCompleted = false
            ),
            TaskEntity(
                id = 2L,
                title = "تمرین",
                courseName = "ریاضی مهندسی",
                dueDate = "1405/07/20",
                courseId = "c1",
                isCompleted = false
            )
        )

        val priorities = AcademicPriorityEngine.rank(
            courses = listOf(course),
            coursesWithSessions = emptyList<CourseWithSessions>(),
            attendance = listOf(
                AttendanceEntity(
                    courseId = "c1",
                    courseName = "ریاضی مهندسی",
                    absentCount = 3,
                    maxAllowed = 3
                )
            ),
            tasks = tasks,
            exams = listOf(
                ExamItem(
                    id = "e1",
                    courseName = "ریاضی مهندسی",
                    solarDate = "1405/07/25",
                    time = "09:00",
                    location = "سالن",
                    units = 3
                )
            ),
            todayWeekdayIndex = 0,
            todayDate = "1405/07/21"
        )

        assertTrue(priorities.isNotEmpty())
        assertEquals(AcademicPriorityKind.CRITICAL_ATTENDANCE, priorities.first().kind)
        assertEquals(AcademicPriorityKind.OVERDUE_TASK, priorities[1].kind)
        assertTrue(priorities.any { it.kind == AcademicPriorityKind.EXAM_LINKED_TASK })
    }

    @Test
    fun emptyAcademicStateProducesNoPriority() {
        val priorities = AcademicPriorityEngine.rank(
            courses = emptyList(),
            coursesWithSessions = emptyList(),
            attendance = emptyList(),
            tasks = emptyList(),
            exams = emptyList(),
            todayWeekdayIndex = 0,
            todayDate = "1405/07/21"
        )

        assertTrue(priorities.isEmpty())
    }
}

package com.example.domain.engine

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.model.ExamItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicContextGraphBuilderTest {

    @Test
    fun linksTasksAttendanceSessionsAndExamToStableCourseContext() {
        val course = CourseEntity(
            id = "physics",
            name = "فیزیک",
            units = 3
        )

        val task = TaskEntity(
            id = 1L,
            title = "پروژه",
            courseName = "فیزیک",
            dueDate = "1405/07/20",
            courseId = "physics",
            isCompleted = false
        )

        val exam = ExamItem(
            id = "exam-physics",
            courseName = "فیزیک",
            solarDate = "1405/08/01",
            time = "09:00",
            location = "سالن",
            units = 3
        )

        val graph = AcademicContextGraphBuilder.build(
            courses = listOf(course),
            coursesWithSessions = listOf(
                CourseWithSessions(
                    course = course,
                    sessions = listOf(
                        CourseSessionEntity(
                            id = "physics-1",
                            courseId = "physics",
                            day = 0,
                            start = "08:00",
                            end = "09:30",
                            location = "101"
                        )
                    )
                )
            ),
            attendance = listOf(
                AttendanceEntity(
                    courseId = "physics",
                    courseName = "فیزیک",
                    absentCount = 1,
                    maxAllowed = 3
                )
            ),
            tasks = listOf(task),
            exams = listOf(exam)
        )

        val context = graph.courses.single()
        assertEquals("physics", context.course.id)
        assertEquals(1, context.sessions.size)
        assertEquals(1, context.tasks.size)
        assertEquals(1, context.exams.size)
        assertEquals(1, context.attendance?.absentCount)
        assertTrue(graph.orphanTasks.isEmpty())
        assertTrue(graph.orphanExams.isEmpty())
    }
}

package com.example.domain.engine

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.model.AcademicContextAction
import com.example.domain.model.ExamItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AcademicContextEngineTest {

    @Test
    fun buildsUnifiedSnapshotWithPrioritySignals() {
        val course = CourseEntity(
            id = "physics",
            name = "فیزیک",
            units = 3
        )
        val relation = CourseWithSessions(
            course = course,
            sessions = listOf(
                CourseSessionEntity(
                    id = "physics-1",
                    courseId = "physics",
                    day = 0,
                    start = "08:30",
                    end = "10:00",
                    location = "کلاس ۱۰۱"
                )
            )
        )

        val snapshot = AcademicContextEngine.buildSnapshot(
            courses = listOf(course),
            coursesWithSessions = listOf(relation),
            attendance = listOf(
                AttendanceEntity(
                    courseId = "physics",
                    courseName = "فیزیک",
                    absentCount = 3,
                    maxAllowed = 3
                )
            ),
            tasks = listOf(
                TaskEntity(
                    id = 1L,
                    title = "تمرین فصل اول",
                    courseName = "فیزیک",
                    dueDate = "1405/07/20",
                    courseId = "physics",
                    isCompleted = false
                )
            ),
            exams = listOf(
                ExamItem(
                    id = "exam-1",
                    courseName = "فیزیک",
                    solarDate = "1405/07/25",
                    time = "09:00",
                    location = "سالن ۲",
                    units = 3
                )
            ),
            todayWeekdayIndex = 0,
            todayDate = "1405/07/21"
        )

        assertEquals(1, snapshot.todaySessionCount)
        assertEquals(90, snapshot.todayClassMinutes)
        assertEquals(1, snapshot.openTaskCount)
        assertEquals(1, snapshot.overdueTaskCount)
        assertEquals(1, snapshot.criticalAttendanceCount)
        assertEquals(1, snapshot.examsWithOpenTasksCount)
        assertEquals(AcademicContextAction.OPEN_ATTENDANCE, snapshot.primaryAction)
        assertNotNull(snapshot.nextCourseId)
        assertEquals("physics", snapshot.nextCourseId)
    }

    @Test
    fun completedClassesAreNotPresentedAsNextOrUrgent() {
        val course = CourseEntity(id = "past", name = "ریاضی")
        val relation = CourseWithSessions(course, listOf(
            CourseSessionEntity(id = "past-session", courseId = course.id, day = 0, start = "08:00", end = "10:00")
        ))
        val snapshot = AcademicContextEngine.buildSnapshot(
            listOf(course), listOf(relation), emptyList(), emptyList(), emptyList(),
            todayWeekdayIndex = 0, todayDate = "1405/07/11", minuteOfDay = 600
        )
        assertEquals(1, snapshot.todaySessionCount)
        org.junit.Assert.assertNull(snapshot.nextCourseId)
        org.junit.Assert.assertNull(snapshot.topPriority)
    }

    @Test
    fun currentSessionWinsOverAnEarlierCompletedClass() {
        val course = CourseEntity(id = "current", name = "فیزیک")
        val relation = CourseWithSessions(course, listOf(
            CourseSessionEntity(id = "early", courseId = course.id, day = 0, start = "08:00", end = "09:00"),
            CourseSessionEntity(id = "live", courseId = course.id, day = 0, start = "۱۰:۰۰", end = "۱۱:۰۰")
        ))
        val snapshot = AcademicContextEngine.buildSnapshot(
            listOf(course), listOf(relation), emptyList(), emptyList(), emptyList(),
            todayWeekdayIndex = 0, todayDate = "1405/07/11", minuteOfDay = 630
        )
        assertEquals("۱۰:۰۰", snapshot.nextCourseStart)
        assertEquals("next-class:live", snapshot.topPriority?.id)
    }
}

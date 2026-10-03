package com.example.domain.engine

import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.relation.CourseWithSessions
import org.junit.Assert.*
import org.junit.Test

class NextClassEngineTest {
    private fun course(day: Int, start: String, end: String, id: String = "physics") =
        CourseWithSessions(CourseEntity(id = id, name = id), listOf(
            CourseSessionEntity(courseId = id, day = day, start = start, end = end)
        ))

    @Test fun doesNotInventClassesForEmptySchedule() {
        assertNull(NextClassEngine.next(emptyList(), 0, 480))
    }
    @Test fun ongoingClassWinsAndUsesActualProgress() {
        val result = NextClassEngine.next(listOf(course(0,"08:00","10:00"), course(0,"11:00","12:00","math")),0,540)!!
        assertEquals("physics", result.course.id)
        assertTrue(result.isOngoing)
        assertEquals(0.5f,result.progress,0.001f)
    }
    @Test fun completedClassDoesNotMaskNextClass() {
        val result = NextClassEngine.next(listOf(course(0,"08:00","10:00"),course(0,"11:00","12:00","math")),0,630)!!
        assertEquals("math",result.course.id)
        assertFalse(result.isOngoing)
    }
    @Test fun fridayWrapsToSaturdayWithoutTreatingFridayAsSaturday() {
        val result = NextClassEngine.next(listOf(course(0,"08:00","10:00")),6,600)!!
        assertEquals(1,result.daysAhead)
    }
    @Test fun invalidAndArchivedSessionsAreExcluded() {
        assertNull(NextClassEngine.next(listOf(course(-1,"08:00","10:00"),course(0,"10:00","08:00")),0,600))
        val archived = course(0,"08:00","10:00").let { it.copy(course=it.course.copy(isArchived=true)) }
        assertNull(NextClassEngine.next(listOf(archived),0,540))
    }
}

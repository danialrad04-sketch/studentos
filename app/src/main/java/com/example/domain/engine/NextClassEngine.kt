package com.example.domain.engine

import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.data.local.util.DateTimeNormalizer

object NextClassEngine {
    data class NextClass(
        val course: CourseEntity,
        val session: CourseSessionEntity,
        val daysAhead: Int,
        val isOngoing: Boolean,
        val progress: Float
    )

    fun next(courses: List<CourseWithSessions>, today: Int, minuteOfDay: Int): NextClass? {
        require(today in 0..6)
        require(minuteOfDay in 0..1439)
        return courses.filterNot { it.course.isArchived }.flatMap { course ->
            course.sessions.mapNotNull { session ->
                if (session.day !in 0..6 || session.start.isBlank() || session.end.isBlank()) return@mapNotNull null
                val start = DateTimeNormalizer.timeToMinutes(session.start)
                val end = DateTimeNormalizer.timeToMinutes(session.end)
                if (start !in 0..1439 || end !in 1..1440 || end <= start) return@mapNotNull null
                var days = (session.day - today + 7) % 7
                if (days == 0 && end <= minuteOfDay) days = 7
                val ongoing = days == 0 && minuteOfDay >= start
                NextClass(course.course, session, days, ongoing,
                    if (ongoing) ((minuteOfDay - start).toFloat() / (end - start)).coerceIn(0f, 1f) else 0f)
            }
        }.minWithOrNull(compareBy<NextClass>(
            { if (it.isOngoing) -1 else it.daysAhead * 1440 + DateTimeNormalizer.timeToMinutes(it.session.start) },
            { it.course.name }, { it.session.id }
        ))
    }
}

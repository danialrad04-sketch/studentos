package com.example.domain.engine

import com.example.data.local.relation.CourseWithSessions
import com.example.data.parser.ParsedCourseDraft
import com.example.domain.util.AcademicInputValidator

object ImportReviewEngine {
    fun conflicts(drafts: List<ParsedCourseDraft>, existing: List<CourseWithSessions> = emptyList()): List<String> {
        val messages = linkedSetOf<String>()
        fun overlap(dayA: Int, startA: String, endA: String, dayB: Int, startB: String, endB: String): Boolean {
            val a = AcademicInputValidator.time(startA) ?: return false
            val b = AcademicInputValidator.time(endA) ?: return false
            val c = AcademicInputValidator.time(startB) ?: return false
            val d = AcademicInputValidator.time(endB) ?: return false
            return dayA == dayB && a < d && c < b
        }
        drafts.forEachIndexed { index, a ->
            drafts.drop(index + 1).forEach { b ->
                if (a.name.trim() != b.name.trim() && overlap(a.dayOfWeek, a.startTime, a.endTime, b.dayOfWeek, b.startTime, b.endTime)) messages += "${a.name} و ${b.name}"
            }
            existing.filter { it.course.name.trim() != a.name.trim() }.forEach { scheduled ->
                if (scheduled.sessions.any { overlap(a.dayOfWeek, a.startTime, a.endTime, it.day, it.start, it.end) }) messages += "${a.name} و ${scheduled.course.name}"
            }
        }
        return messages.toList()
    }
}

package com.example.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity

/**
 * 1-to-N relation between a canonical CourseEntity and its weekly CourseSessionEntity slots.
 */
data class CourseWithSessions(
    @Embedded
    val course: CourseEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "courseId"
    )
    val sessions: List<CourseSessionEntity> = emptyList()
) {
    val id: String get() = course.id
    val name: String get() = course.name
    val units: Int get() = course.units
    val colorHex: String get() = course.colorHex
    val semesterId: String get() = course.semesterId
    val isArchived: Boolean get() = course.isArchived
    val courseCode: String get() = course.courseCode
    val professorId: String? get() = course.professorId
    val professor: String get() = course.professor
    val examDate: String get() = course.examDate
    val examTime: String get() = course.examTime
    val examLocation: String get() = course.examLocation
    val notes: String get() = course.notes

    // Primary or fallback session accessors
    val primarySession: CourseSessionEntity? get() = sessions.firstOrNull()
    val day: Int get() = primarySession?.day ?: 0
    val start: String get() = primarySession?.start ?: "08:00"
    val end: String get() = primarySession?.end ?: "10:00"
    val location: String get() = primarySession?.location ?: ""
}

package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a single weekly class session/time-slot for a course.
 * Decouples weekly recurring class sessions from the canonical CourseEntity.
 */
@Entity(
    tableName = "course_sessions",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("courseId"),
        Index("day")
    ]
)
data class CourseSessionEntity(
    @PrimaryKey
    val id: String = "sess_${UUID.randomUUID().toString().take(8)}",
    val courseId: String,
    val day: Int = -1, // -1 = unset; 0 = شنبه, 1 = یکشنبه, 2 = دوشنبه, 3 = سه‌شنبه, 4 = چهارشنبه, 5 = پنج‌شنبه
    val start: String = "",
    val end: String = "",
    val location: String = ""
)

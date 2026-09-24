package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * CourseEntity for Student OS.
 * Represents a logical course (name, units, code, professor, exams, color, semester).
 * Class sessions and weekly meeting times live exclusively in CourseSessionEntity (1:N relation).
 */
@Entity(
    tableName = "courses",
    indices = [
        Index(value = ["semesterId"]),
        Index(value = ["courseCode"]),
        Index(value = ["professorId"])
    ]
)
data class CourseEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#0D9488",
    val units: Int = 3,
    val semesterId: String = "current",
    val courseCode: String = "",
    val professorId: String? = null,
    val professor: String = "",
    val examDate: String = "",
    val examTime: String = "",
    val examLocation: String = "",
    val notes: String = "",
    val isArchived: Boolean = false
)

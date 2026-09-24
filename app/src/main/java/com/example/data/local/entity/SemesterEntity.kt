package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Semester entity representing an academic term in the Student OS.
 * Enables multi-semester isolation, past semester transcripts, and archive/active management.
 */
@Entity(
    tableName = "semesters",
    indices = [
        Index(value = ["isCurrent"]),
        Index(value = ["academicYear", "termNumber"])
    ]
)
data class SemesterEntity(
    @PrimaryKey val id: String, // e.g. "sem_1403_1", "sem_1", UUID
    val title: String, // e.g. "ترم ۱ (پاییز ۱۴۰۲)", "ترم ۳ (پاییز ۱۴۰۳)"
    val year: Int = 1403,
    val academicYear: Int = 1403,
    val semesterNumber: Int = 1, // 1 to 8
    val termNumber: Int = 1,
    val startDate: String = "", // e.g. "1403/07/01"
    val endDate: String = "",   // e.g. "1403/11/15"
    val status: String = "ACTIVE", // "ACTIVE", "ARCHIVED", "UPCOMING"
    val isCurrent: Boolean = false,
    val isArchived: Boolean = false,
    val totalUnits: Int = 0,
    val gpa: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

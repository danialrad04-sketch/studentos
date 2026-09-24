package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Historical and planned course attempts made by the student.
 * Supports multiple attempts (e.g. FAILED -> PASSED in next semester).
 */
@Entity(
    tableName = "student_course_attempts",
    indices = [Index("courseId"), Index("semesterIndex"), Index("profileId")]
)
data class StudentCourseAttemptEntity(
    @PrimaryKey val id: String,          // Unique UUID
    val profileId: Int = 1,              // Foreign link to student_profile
    val courseId: String?,               // Stable course ID if matched with curriculum
    val courseName: String,              // Display name entered by user or imported
    val units: Int,                      // Course units
    val attemptNumber: Int = 1,          // 1 for first try, 2 for retake, etc.
    val semesterIndex: Int,              // Academic semester number (e.g. 1 to 8)
    val status: String,                  // "PASSED", "FAILED", "DROPPED", "PLANNED"
    val grade: Double? = null,           // Genuine grade, nullable, zero fake defaults
    val source: String                   // "USER_INPUT", "IMPORTED_REGISTRATION", "QUICK_SETUP"
)

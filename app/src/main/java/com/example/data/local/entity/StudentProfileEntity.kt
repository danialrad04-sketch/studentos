package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_profile")
data class StudentProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "دانشجو",
    val studentId: String = "",
    val faculty: String = "دانشکده مهندسی",
    val term: String = "ترم ۱",
    val university: String = "دانشگاه",
    val major: String = "مهندسی",
    val entryYear: Int = 1403,
    val currentSemester: Int = 1,
    val activeUnits: Int = 0,
    val passedUnits: Int = 0,
    val notes: String = "",
    val isOnboardingCompleted: Boolean = false,
    val universityId: String? = null,
    val facultyId: String? = null,
    val majorId: String? = null,
    val declaredPassedCredits: Int? = null,
    val declaredGpa: Double? = null,
    val updatedAt: Long = 0L
)

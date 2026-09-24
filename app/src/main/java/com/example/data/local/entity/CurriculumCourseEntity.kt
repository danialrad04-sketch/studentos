package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "curriculum_courses")
data class CurriculumCourseEntity(
    @PrimaryKey val id: String,
    val majorId: String = "CHEM_ENG",
    val code: String,
    val name: String,
    val units: Int,
    val courseType: String, // "تخصصی", "پایه", "عمومی", "آزمایشگاهی"
    val recommendedSemester: Int,
    val prerequisites: String = "",
    val coRequisites: String = ""
)

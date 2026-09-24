package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * GradeEntity for course grade calculations and simulator.
 * Linked to course via stable courseId.
 */
@Entity(
    tableName = "grades",
    indices = [
        Index(value = ["courseId"])
    ]
)
data class GradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseName: String = "",
    val units: Int = 3,
    val midtermGrade: Double = 0.0,
    val finalGrade: Double = 0.0,
    val courseId: String = ""
)

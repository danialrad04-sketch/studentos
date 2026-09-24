package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Exam entity mapped to a specific Course via stable courseId.
 * Persisted in Room database so exams are dynamic, conflict-checked, and survive semester archiving.
 */
@Entity(
    tableName = "exams",
    indices = [
        Index(value = ["courseId"])
    ]
)
data class ExamEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val courseName: String = "",
    val date: String = "", // Solar date e.g. "1403/10/23"
    val time: String = "", // e.g. "08:30"
    val location: String = "",
    val notes: String = ""
)

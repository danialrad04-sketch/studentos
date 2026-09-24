package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AttendanceEntity for tracking course absences.
 * Uses stable courseId as PrimaryKey, eliminating fragile name-based coupling.
 */
@Entity(
    tableName = "attendance",
    indices = [
        Index(value = ["courseId"], unique = true)
    ]
)
data class AttendanceEntity(
    @PrimaryKey val courseId: String = "",
    val courseName: String = "",
    val absentCount: Int = 0,
    val maxAllowed: Int = 3
)

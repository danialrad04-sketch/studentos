package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * TaskEntity for task and sprint management.
 * Linked to course via stable courseId and semesterId.
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["courseId"]),
        Index(value = ["semesterId"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val courseName: String = "",
    val dueDate: String,
    val isCompleted: Boolean = false,
    val courseId: String = "",
    val semesterId: String? = null
)

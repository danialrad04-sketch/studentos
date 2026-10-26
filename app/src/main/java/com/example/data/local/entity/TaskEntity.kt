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
        Index(value = ["semesterId"]),
        Index(value = ["recurrenceParentId"], unique = true)
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val courseName: String = "",
    val dueDate: String,
    val isCompleted: Boolean = false,
    val courseId: String = "",
    val semesterId: String? = null,
    @androidx.room.ColumnInfo(defaultValue = "0") val repeatEveryDays: Int = 0,
    @androidx.room.ColumnInfo(defaultValue = "''") val reminderTime: String = "",
    @androidx.room.ColumnInfo(defaultValue = "NULL") val recurrenceParentId: Long? = null
)

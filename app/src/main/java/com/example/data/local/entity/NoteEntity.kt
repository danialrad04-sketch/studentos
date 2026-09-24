package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Note entity for academic formulas, reminders, and course notes.
 * Can be attached to a specific course via nullable courseId or be general.
 */
@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["courseId"])
    ]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val courseId: String? = null,
    val title: String = "",
    val content: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

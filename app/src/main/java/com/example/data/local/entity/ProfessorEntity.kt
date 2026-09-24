package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Professor entity providing stable reference for faculty members.
 * Supports course relation via professorId without fragile string coupling.
 */
@Entity(tableName = "professors")
data class ProfessorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String = "",
    val office: String = "",
    val department: String = "",
    val notes: String = ""
)

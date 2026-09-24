package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "course_aliases",
    indices = [Index("aliasNormalized"), Index("courseId")]
)
data class CourseAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: String,
    val aliasRaw: String,
    val aliasNormalized: String
)

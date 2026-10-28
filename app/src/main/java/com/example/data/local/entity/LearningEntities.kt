package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "study_goals")
data class StudyGoalEntity(
    @PrimaryKey val courseId: String,
    val targetDate: String,
    val totalMinutes: Int,
    val difficulty: Int = 2,
    val enabled: Boolean = true,
    val updatedAt: Long = 0,
) : java.io.Serializable

@Entity(tableName = "study_availability")
data class StudyAvailabilityEntity(
    @PrimaryKey val weekday: Int,
    val startMinute: Int,
    val endMinute: Int,
    val budgetMinutes: Int,
    val updatedAt: Long = 0,
) : java.io.Serializable

@Entity(tableName = "study_completions", indices = [Index("courseId")])
data class StudyCompletionEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val date: String,
    val startMinute: Int,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false,
) : java.io.Serializable

@Entity(tableName = "review_cards", indices = [Index("courseId")])
data class ReviewCardEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val question: String,
    val answer: String,
    val dueDate: String,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val lastReviewedDate: String = "",
    val isArchived: Boolean = false,
    val updatedAt: Long = 0,
) : java.io.Serializable

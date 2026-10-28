package com.example.domain.model

/** Stable payload shared by the planner, saved editor state and persisted focus timer. */
data class LearningBlock(
    val id: String,
    val courseId: String,
    val date: String,
    val startMinute: Int,
    val minutes: Int
) : java.io.Serializable

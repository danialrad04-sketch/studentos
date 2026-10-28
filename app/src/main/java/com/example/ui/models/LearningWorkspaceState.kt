package com.example.ui.models

import com.example.data.local.entity.*

data class LearningWorkspaceState(
    val goals: List<StudyGoalEntity> = emptyList(),
    val availability: List<StudyAvailabilityEntity> = emptyList(),
    val completions: List<StudyCompletionEntity> = emptyList(),
    val cards: List<ReviewCardEntity> = emptyList(),
    val loaded: Boolean = false,
    val error: Boolean = false,
)

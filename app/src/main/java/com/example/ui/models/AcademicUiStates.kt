package com.example.ui.models

import com.example.domain.engine.CurriculumMatchOutput
import com.example.domain.engine.ResolutionFailureReason
import com.example.domain.model.AcademicProgress
import com.example.domain.model.EvaluatedCurriculumCourse

/**
 * Clean UI State representations for Academic Intelligence features.
 * Adheres to Step 6: Loading, Ready, Partial, Empty, Error.
 */
sealed interface AcademicProgressUiState {
    data object Loading : AcademicProgressUiState
    data object Empty : AcademicProgressUiState
    data class Partial(val progress: AcademicProgress, val note: String) : AcademicProgressUiState
    data class Ready(val progress: AcademicProgress) : AcademicProgressUiState
    data class Error(val message: String) : AcademicProgressUiState
}

sealed interface CurriculumMatchUiState {
    data object Loading : CurriculumMatchUiState
    data object Empty : CurriculumMatchUiState
    data class Ready(
        val output: CurriculumMatchOutput,
        val groupedBySemester: Map<Int, List<EvaluatedCurriculumCourse>>
    ) : CurriculumMatchUiState
    data class NotFound(val reason: ResolutionFailureReason, val explanation: String) : CurriculumMatchUiState
    data class Error(val message: String) : CurriculumMatchUiState
}

sealed interface SyncUiState {
    data object Idle : SyncUiState
    data object Syncing : SyncUiState
    data class Success(val message: String, val completedAt: Long) : SyncUiState
    data class Error(val message: String, val failedAt: Long) : SyncUiState
}
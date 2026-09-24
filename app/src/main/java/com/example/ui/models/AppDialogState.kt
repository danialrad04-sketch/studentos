package com.example.ui.models

import com.example.data.local.entity.CourseEntity
import com.example.ui.components.export.ExportSourcePayload

/**
 * Unified Sealed Class for Dialog & Modal State Management (Bug 4 Resolution).
 * Eliminates 15+ individual boolean mutableStateOf variables in MainAppScreen,
 * preventing memory leaks, unnecessary recompositions, and backhandler fragmentation.
 */
sealed interface AppDialogState {
    data object None : AppDialogState
    data object Profile : AppDialogState
    data object SettingsAndRoadmap : AppDialogState
    data object PastSemesters : AppDialogState
    data object Notifications : AppDialogState
    data object ApkInfo : AppDialogState
    data object AddCourse : AppDialogState
    data class EditCourse(val course: CourseEntity) : AppDialogState
    data object AddTask : AppDialogState
    data object CommandCenter : AppDialogState
    data object Copilot : AppDialogState
    data object OcrImport : AppDialogState
    data object Auth : AppDialogState
    data object Upgrade : AppDialogState
    data object BackupRestore : AppDialogState
    data class CourseWorkspace(val courseId: String) : AppDialogState
    data class ExportShare(val payload: ExportSourcePayload) : AppDialogState
    data object PrivacyPolicy : AppDialogState
    data object SupportTickets : AppDialogState
}

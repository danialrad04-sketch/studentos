package com.example.domain.model

enum class ActionImpactType {
    SAFE_QUERY,            // Read-only analysis, simulations (zero DB modification)
    REQUIRES_CONFIRMATION, // Modifies DB state: adds task, updates past semester, logs attendance, etc.
    PROTECTED_READONLY     // System safety boundaries (student identity, official regulations)
}

sealed class CopilotPayload {
    data class AddTask(val title: String, val courseName: String, val dueDate: String) : CopilotPayload()
    data class CompleteTask(val taskId: String, val taskTitle: String) : CopilotPayload()
    data class UpdatePastSemesterSummary(val semesterIndex: Int, val units: Int, val gpa: Double?) : CopilotPayload()
    data class ChangeCurrentSemester(val newSemesterIndex: Int) : CopilotPayload()
    data class QuickEnrollCourse(val courseName: String, val units: Int, val dayOfWeek: String, val startTime: String, val endTime: String) : CopilotPayload()
    data class ClearAttendanceWarning(val courseName: String) : CopilotPayload()
    data class SimulateGpaTarget(val targetGpa: Double, val requiredAverage: Double) : CopilotPayload()
    data class NavigateToTab(val tabName: String) : CopilotPayload()
}

data class CopilotActionProposal(
    val id: String,
    val title: String,
    val description: String,
    val impactType: ActionImpactType,
    val payload: CopilotPayload,
    val buttonLabel: String = "اعمال تغییرات",
    val isDestructive: Boolean = false
)

enum class CopilotSender {
    USER,
    COPILOT,
    SYSTEM
}

data class CopilotMessage(
    val id: String,
    val sender: CopilotSender,
    val text: String,
    val timestamp: String,
    val proposedAction: CopilotActionProposal? = null,
    val isActionApplied: Boolean = false,
    val isActionDismissed: Boolean = false,
    val suggestedQuickReplies: List<String> = emptyList(),
    val confidenceBadge: String? = null
)

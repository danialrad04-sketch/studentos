package com.example.domain.model

data class AcademicPlanningCollision(
    val id: String,
    val type: AcademicPlanningCollisionType,
    val severity: AcademicPlanningCollisionSeverity,
    val date: String,
    val title: String,
    val description: String
)

enum class AcademicPlanningCollisionType {
    TASK_TASK,
    TASK_EXAM
}

enum class AcademicPlanningCollisionSeverity {
    WARNING,
    HIGH
}

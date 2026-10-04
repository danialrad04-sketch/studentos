package com.example.domain.model

data class AcademicPriorityItem(
    val id: String,
    val kind: AcademicPriorityKind,
    val title: String,
    val reason: String,
    val score: Int,
    val courseId: String? = null,
    val courseName: String? = null,
    val dueDate: String? = null
)

enum class AcademicPriorityKind {
    CRITICAL_ATTENDANCE,
    OVERDUE_TASK,
    EXAM_TODAY,
    TASK_TODAY,
    EXAM_LINKED_TASK,
    NEXT_CLASS,
    OPEN_TASK,
    NONE
}

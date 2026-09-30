package com.example.domain.model

data class AcademicContextSnapshot(
    val todayWeekdayIndex: Int,
    val todaySessionCount: Int,
    val todayClassMinutes: Int,
    val openTaskCount: Int,
    val overdueTaskCount: Int,
    val criticalAttendanceCount: Int,
    val examsWithOpenTasksCount: Int,
    val totalActiveCourseCount: Int,
    val nextCourseId: String? = null,
    val nextCourseName: String? = null,
    val nextCourseStart: String? = null,
    val nextCourseEnd: String? = null,
    val nextCourseLocation: String? = null,
    val primaryAction: AcademicContextAction = AcademicContextAction.NONE
)

enum class AcademicContextAction {
    OPEN_OVERDUE_TASKS,
    OPEN_ATTENDANCE,
    OPEN_NEXT_CLASS,
    START_FOCUS,
    REVIEW_EXAM,
    NONE
}

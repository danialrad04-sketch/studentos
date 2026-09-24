package com.example.domain.model

/**
 * Domain Models for Roadmap Systems:
 * - Phase 12 & 13: Semester Planner & Plan Comparison
 * - Phase 20: Academic Workload Engine
 * - Phase 21: Academic Risk Engine
 * - Phase 24: Global Search / Command Bar
 * - Phase 28: Smart Study Planner
 */

/**
 * An Academic Semester Plan for course selection (Build My Semester).
 */
data class SemesterPlan(
    val id: String,
    val name: String,
    val totalCredits: Int,
    val courses: List<EvaluatedCurriculumCourse>,
    val scheduleConflictCount: Int = 0,
    val estimatedWeeklyWorkloadHours: Double = 0.0,
    val isDraft: Boolean = true,
    val isActive: Boolean = false,
    val tradeOffs: List<String> = emptyList()
)

/**
 * Comparative evaluation between two candidate Semester Plans.
 */
data class SemesterPlanComparison(
    val planA: SemesterPlan,
    val planB: SemesterPlan,
    val creditDifference: Int, // planB.credits - planA.credits
    val workloadDifferenceHours: Double,
    val summaryTradeOff: String
)

/**
 * Calculated weekly academic workload (برآورد مطالعه).
 */
data class WeeklyAcademicWorkload(
    val classLectureHours: Double,
    val laboratoryHours: Double,
    val assignmentHours: Double,
    val estimatedSelfStudyHours: Double,
    val totalEstimatedWeeklyHours: Double,
    val isEstimateOnly: Boolean = true
)

/**
 * Severity of detected academic risks.
 */
enum class RiskSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Types of academic risks analyzed by Academic Risk Engine.
 */
enum class AcademicRiskType {
    ATTENDANCE_BREACH,
    UPCOMING_DEADLINE,
    EXAM_COLLISION,
    SCHEDULE_OVERLAP,
    CREDIT_OVERLOAD,
    PREREQUISITE_DEFICIT,
    CURRICULUM_DELAY
}

/**
 * An identified academic risk with root cause and actionable advice.
 */
data class AcademicRisk(
    val id: String,
    val title: String,
    val description: String,
    val severity: RiskSeverity,
    val riskType: AcademicRiskType,
    val recommendedAction: String
)

/**
 * Unified Search Result for Global Command Bar (Phase 24).
 */
sealed interface GlobalSearchResult {
    data class CourseItem(
        val courseId: String,
        val name: String,
        val code: String,
        val units: Int,
        val state: CourseState
    ) : GlobalSearchResult

    data class TaskItem(
        val taskId: Long,
        val title: String,
        val courseName: String,
        val deadline: String,
        val isCompleted: Boolean
    ) : GlobalSearchResult

    data class ExamItem(
        val examId: Long,
        val courseName: String,
        val examDate: String,
        val examTime: String
    ) : GlobalSearchResult

    data class NoteFormulaItem(
        val title: String,
        val courseName: String,
        val content: String
    ) : GlobalSearchResult
}

/**
 * Deterministic Study Plan session recommendation (Phase 28).
 */
data class StudySessionRecommendation(
    val id: String,
    val courseName: String,
    val recommendedDurationMinutes: Int,
    val priorityReason: String,
    val targetType: String // e.g. "آمادگی آزمون", "تکمیل تمرین", "مرور هفتگی"
)

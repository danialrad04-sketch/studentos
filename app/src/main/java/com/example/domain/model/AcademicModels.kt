package com.example.domain.model

/**
 * Domain-level representations of Academic Intelligence Foundation (Phase 4).
 * Pure Kotlin data structures with zero Android framework or Room dependencies.
 */

/**
 * Concrete academic state of a specific course for a student.
 */
enum class CourseState {
    PASSED,    // Successfully completed with passing grade
    CURRENT,   // Enrolled in the active ongoing semester
    PLANNED,   // Marked for future semester enrollment
    FAILED,    // Taken but received a failing grade (requires retake)
    DROPPED,   // Withdrawn / dropped during the semester
    AVAILABLE, // All prerequisites and unit requirements met; eligible to enroll
    BLOCKED,   // Blocked due to unmet prerequisites, co-requisites, or credit deficit
    UNKNOWN    // Insufficient historical records to definitively determine eligibility
}

/**
 * Concrete origin and provenance of academic data.
 */
enum class DataSource {
    USER_DECLARED,       // Directly input by the user (e.g. in Quick Setup)
    COURSE_RECORDS_SUM,  // Formally aggregated from logged student course attempts
    IMPORTED_DOCUMENT,   // Extracted from enrollment / transcript text
    CURRICULUM_REFERENCE // Extracted directly from accredited university curriculum
}

/**
 * Confidence level regarding completeness of student course history.
 */
enum class DataConfidence {
    UNKNOWN,          // No academic records established
    CREDIT_ONLY,      // Total credits passed is known, but granular course history is unknown
    PARTIAL_HISTORY,  // Granular courses partially logged, but discrepancy exists with total credits
    VERIFIED_FULL     // Complete 1-to-1 match between course attempts and curriculum requirements
}

/**
 * Pure GPA state representation without fake or fabricated default values.
 */
sealed interface GpaState {
    data class Known(val value: Double, val source: DataSource) : GpaState
    data object Unknown : GpaState
    data class PartiallyCalculated(val currentSemesterGpa: Double) : GpaState
}

/**
 * Structured explanation of why a course is blocked from enrollment.
 */
data class BlockedReason(
    val missingPrerequisiteNames: List<String> = emptyList(),
    val requiredMinUnits: Int? = null,
    val currentPassedUnits: Int? = null,
    val explanation: String
)

/**
 * Composite Prerequisite Condition tree structure (Future-proof: supports (A AND B) OR C, co-requisites, etc.).
 */
sealed interface PrerequisiteCondition {
    data class CoursePassed(val courseId: String, val canonicalName: String) : PrerequisiteCondition
    data class CoRequisite(val courseId: String, val canonicalName: String) : PrerequisiteCondition
    data class MinimumTotalPassedCredits(val requiredCredits: Int) : PrerequisiteCondition
    data class AndGroup(val conditions: List<PrerequisiteCondition>) : PrerequisiteCondition
    data class OrGroup(val conditions: List<PrerequisiteCondition>) : PrerequisiteCondition
}

data class CoursePrerequisiteRule(
    val rootCondition: PrerequisiteCondition? = null
)

/**
 * Evaluated curriculum course with its verified domain state.
 */
data class EvaluatedCurriculumCourse(
    val courseId: String,
    val courseCode: String,
    val name: String,
    val units: Int,
    val courseType: String,
    val recommendedSemester: Int,
    val state: CourseState,
    val blockedReason: BlockedReason? = null
)

/**
 * Comprehensive academic progress derived strictly from CurriculumVersion and user records.
 */
data class AcademicProgress(
    val totalRequiredCredits: Int,         // Dynamic from accredited CurriculumVersion
    val passedCredits: Int,                // Units completed
    val passedCreditsSource: DataSource,   // Provenance of credits calculation
    val currentCredits: Int,               // Units enrolled in active semester
    val remainingCredits: Int,             // max(0, total - passed - current)
    val progressPercentage: Float,         // Calculated strictly as (passed / total) * 100
    val gpaState: GpaState,                // Genuine GPA state
    val confidence: DataConfidence,        // Data verification status
    val availableCount: Int,
    val blockedCount: Int,
    val plannedCount: Int
)

/**
 * Clean domain model for the student's academic profile.
 */
data class AcademicProfile(
    val profileId: Int = 1,
    val universityId: String?,
    val universityName: String,
    val facultyId: String?,
    val facultyName: String,
    val majorId: String?,
    val majorName: String,
    val entryYear: Int?,
    val currentSemester: Int?,
    val studentId: String?,
    val studentName: String?,
    val curriculumVersionId: String?,
    val declaredPassedCredits: Int?,
    val gpaState: GpaState
)


data class ExamItem(
    val id: String,
    val courseName: String,
    val solarDate: String,
    val time: String,
    val location: String,
    val units: Int
)

package com.example.domain.engine

import com.example.domain.model.AcademicProgress
import com.example.domain.model.CourseState
import com.example.domain.model.DataConfidence
import com.example.domain.model.DataSource
import com.example.domain.model.EvaluatedCurriculumCourse
import com.example.domain.model.GpaState

data class CurriculumMatchOutput(
    val version: ResolvedCurriculumVersion,
    val evaluatedCourses: List<EvaluatedCurriculumCourse>,
    val passedCourses: List<EvaluatedCurriculumCourse>,
    val currentCourses: List<EvaluatedCurriculumCourse>,
    val availableCourses: List<EvaluatedCurriculumCourse>,
    val blockedCourses: List<EvaluatedCurriculumCourse>,
    val plannedCourses: List<EvaluatedCurriculumCourse>,
    val remainingCourses: List<EvaluatedCurriculumCourse>, // Derived Set: All courses not PASSED and not CURRENT
    val semesterMatrix: Map<Int, List<EvaluatedCurriculumCourse>>
)

/**
 * Clean domain matcher between student profile and curriculum version.
 */
object CurriculumMatcher {

    fun match(
        version: ResolvedCurriculumVersion,
        curriculumCourses: List<ResolvableCurriculumCourse>,
        enrolledCurrentCourseIds: Set<String>,
        enrolledCurrentCourseNames: Set<String>,
        studentAttempts: List<StudentCourseAttempt>,
        declaredPassedCredits: Int?
    ): CurriculumMatchOutput {
        // Collect passed course IDs
        val passedCourseIds = mutableSetOf<String>()
        studentAttempts.filter { it.status.equals("PASSED", ignoreCase = true) }.forEach { attempt ->
            if (attempt.courseId != null) {
                passedCourseIds.add(attempt.courseId)
            } else {
                val norm = CourseIdentityNormalizer.normalize(attempt.courseName)
                curriculumCourses.firstOrNull { CourseIdentityNormalizer.normalize(it.canonicalName) == norm }?.let {
                    passedCourseIds.add(it.id)
                }
            }
        }

        // Determine passed credits for prerequisite check
        val recordsPassedCredits = studentAttempts
            .filter { it.status.equals("PASSED", ignoreCase = true) }
            .sumOf { it.units }
        val effectivePassedCredits = declaredPassedCredits ?: recordsPassedCredits

        // Evaluate all curriculum courses
        val evaluated = curriculumCourses.map { course ->
            CourseStateResolver.resolve(
                course = course,
                enrolledCurrentCourseIds = enrolledCurrentCourseIds,
                enrolledCurrentCourseNames = enrolledCurrentCourseNames,
                studentAttempts = studentAttempts,
                passedCourseIds = passedCourseIds,
                passedCredits = effectivePassedCredits
            )
        }

        val passed = evaluated.filter { it.state == CourseState.PASSED }
        val current = evaluated.filter { it.state == CourseState.CURRENT }
        val available = evaluated.filter { it.state == CourseState.AVAILABLE }
        val blocked = evaluated.filter { it.state == CourseState.BLOCKED }
        val planned = evaluated.filter { it.state == CourseState.PLANNED }

        // REMAINING DERIVED SET: All courses not PASSED and not CURRENT
        val remaining = evaluated.filter { it.state != CourseState.PASSED && it.state != CourseState.CURRENT }

        val matrix = evaluated.groupBy { it.recommendedSemester }

        return CurriculumMatchOutput(
            version = version,
            evaluatedCourses = evaluated,
            passedCourses = passed,
            currentCourses = current,
            availableCourses = available,
            blockedCourses = blocked,
            plannedCourses = planned,
            remainingCourses = remaining,
            semesterMatrix = matrix
        )
    }

    /**
     * Aggregates academic progress strictly from version.totalCreditsRequired and student data.
     * Zero magic numbers, zero fake GPA defaults.
     */
    fun computeProgress(
        version: ResolvedCurriculumVersion?,
        declaredPassedCredits: Int?,
        declaredGpa: Double?,
        studentAttempts: List<StudentCourseAttempt>,
        currentEnrolledUnits: Int,
        matchOutput: CurriculumMatchOutput?
    ): AcademicProgress {
        val totalRequired = version?.totalCreditsRequired ?: 0

        val passedFromRecords = studentAttempts
            .filter { it.status.equals("PASSED", ignoreCase = true) }
            .sumOf { it.units }

        val hasPassedRecords = studentAttempts.any { it.status.equals("PASSED", ignoreCase = true) }

        val (passedCredits, passedSource, confidence) = when {
            hasPassedRecords -> {
                if (declaredPassedCredits != null && declaredPassedCredits != passedFromRecords) {
                    Triple(passedFromRecords, DataSource.COURSE_RECORDS_SUM, DataConfidence.PARTIAL_HISTORY)
                } else {
                    Triple(passedFromRecords, DataSource.COURSE_RECORDS_SUM, DataConfidence.VERIFIED_FULL)
                }
            }
            declaredPassedCredits != null -> {
                Triple(declaredPassedCredits, DataSource.USER_DECLARED, DataConfidence.CREDIT_ONLY)
            }
            else -> {
                Triple(0, DataSource.USER_DECLARED, DataConfidence.UNKNOWN)
            }
        }

        val remainingCredits = if (totalRequired > 0) {
            (totalRequired - passedCredits - currentEnrolledUnits).coerceAtLeast(0)
        } else {
            0
        }

        val progressPercentage = if (totalRequired > 0) {
            ((passedCredits.toFloat() / totalRequired.toFloat()) * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }

        val gpaState = if (declaredGpa != null) {
            GpaState.Known(declaredGpa, DataSource.USER_DECLARED)
        } else {
            GpaState.Unknown
        }

        return AcademicProgress(
            totalRequiredCredits = totalRequired,
            passedCredits = passedCredits,
            passedCreditsSource = passedSource,
            currentCredits = currentEnrolledUnits,
            remainingCredits = remainingCredits,
            progressPercentage = progressPercentage,
            gpaState = gpaState,
            confidence = confidence,
            availableCount = matchOutput?.availableCourses?.size ?: 0,
            blockedCount = matchOutput?.blockedCourses?.size ?: 0,
            plannedCount = matchOutput?.plannedCourses?.size ?: 0
        )
    }
}

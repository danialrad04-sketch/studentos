package com.example.domain.engine

import com.example.domain.model.CoursePrerequisiteRule
import com.example.domain.model.CourseState
import com.example.domain.model.EvaluatedCurriculumCourse

/**
 * Domain entity representation for a course in the accredited curriculum.
 */
data class ResolvableCurriculumCourse(
    val id: String,
    val curriculumVersionId: String,
    val code: String,
    val canonicalName: String,
    val units: Int,
    val courseType: String,
    val recommendedSemester: Int,
    val rule: CoursePrerequisiteRule = CoursePrerequisiteRule()
)

/**
 * Domain entity representing a student's enrolled course or completed course attempt.
 */
data class StudentCourseAttempt(
    val courseId: String?,
    val courseName: String,
    val units: Int,
    val status: String, // "PASSED", "FAILED", "DROPPED", "PLANNED"
    val grade: Double? = null
)

/**
 * Deterministic Course State Resolver.
 * Computes the exact CourseState for every curriculum course based on user attempts and prerequisites.
 */
object CourseStateResolver {

    /**
     * Resolves course state following a prioritized decision tree:
     * 1. CURRENT: Enrolled in the active semester
     * 2. PASSED / FAILED / DROPPED / PLANNED: From student course attempts
     * 3. BLOCKED / AVAILABLE: From prerequisite engine evaluation
     * 4. UNKNOWN: If information is insufficient
     */
    fun resolve(
        course: ResolvableCurriculumCourse,
        enrolledCurrentCourseIds: Set<String>,
        enrolledCurrentCourseNames: Set<String>,
        studentAttempts: List<StudentCourseAttempt>,
        passedCourseIds: Set<String>,
        passedCredits: Int
    ): EvaluatedCurriculumCourse {
        val normCanonical = CourseIdentityNormalizer.normalize(course.canonicalName)

        // 1. Is this course currently enrolled in the active semester?
        val isCurrent = enrolledCurrentCourseIds.contains(course.id) ||
                enrolledCurrentCourseNames.any { CourseIdentityNormalizer.normalize(it) == normCanonical }
        if (isCurrent) {
            return EvaluatedCurriculumCourse(
                courseId = course.id,
                courseCode = course.code,
                name = course.canonicalName,
                units = course.units,
                courseType = course.courseType,
                recommendedSemester = course.recommendedSemester,
                state = CourseState.CURRENT
            )
        }

        // 2. Check student's historical attempts (latest attempt takes priority)
        val matchingAttempts = studentAttempts.filter { attempt ->
            (attempt.courseId != null && attempt.courseId == course.id) ||
                    CourseIdentityNormalizer.normalize(attempt.courseName) == normCanonical
        }

        val latestAttempt = matchingAttempts.lastOrNull()
        if (latestAttempt != null) {
            val state = when (latestAttempt.status.uppercase()) {
                "PASSED" -> CourseState.PASSED
                "FAILED" -> CourseState.FAILED
                "DROPPED" -> CourseState.DROPPED
                "PLANNED" -> CourseState.PLANNED
                else -> null
            }
            if (state != null) {
                return EvaluatedCurriculumCourse(
                    courseId = course.id,
                    courseCode = course.code,
                    name = course.canonicalName,
                    units = course.units,
                    courseType = course.courseType,
                    recommendedSemester = course.recommendedSemester,
                    state = state
                )
            }
        }

        // 3. Evaluate prerequisites using PrerequisiteEngine
        when (val eval = PrerequisiteEngine.evaluate(course.rule, passedCourseIds, enrolledCurrentCourseIds, passedCredits)) {
            is PrerequisiteEngine.EvaluationResult.Satisfied -> {
                return EvaluatedCurriculumCourse(
                    courseId = course.id,
                    courseCode = course.code,
                    name = course.canonicalName,
                    units = course.units,
                    courseType = course.courseType,
                    recommendedSemester = course.recommendedSemester,
                    state = CourseState.AVAILABLE
                )
            }
            is PrerequisiteEngine.EvaluationResult.Unsatisfied -> {
                return EvaluatedCurriculumCourse(
                    courseId = course.id,
                    courseCode = course.code,
                    name = course.canonicalName,
                    units = course.units,
                    courseType = course.courseType,
                    recommendedSemester = course.recommendedSemester,
                    state = CourseState.BLOCKED,
                    blockedReason = eval.blockedReason
                )
            }
        }
    }
}

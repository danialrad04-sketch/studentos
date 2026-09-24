package com.example.domain.engine

import com.example.domain.model.BlockedReason
import com.example.domain.model.CoursePrerequisiteRule
import com.example.domain.model.PrerequisiteCondition

/**
 * Pure evaluation engine for prerequisite rules.
 * Evaluates Composite PrerequisiteCondition trees (AndGroup, OrGroup, CoursePassed, CoRequisite, MinimumTotalPassedCredits).
 */
object PrerequisiteEngine {

    sealed interface EvaluationResult {
        data object Satisfied : EvaluationResult
        data class Unsatisfied(val blockedReason: BlockedReason) : EvaluationResult
    }

    /**
     * Evaluates a course prerequisite rule against student's academic record.
     *
     * @param rule Composite rule for the course
     * @param passedCourseIds Set of stable course IDs successfully passed by the student
     * @param currentCourseIds Set of course IDs enrolled in current semester (for CoRequisite verification)
     * @param passedCredits Total completed credits to evaluate minimum credit prerequisites
     */
    fun evaluate(
        rule: CoursePrerequisiteRule,
        passedCourseIds: Set<String>,
        currentCourseIds: Set<String>,
        passedCredits: Int
    ): EvaluationResult {
        val root = rule.rootCondition ?: return EvaluationResult.Satisfied

        val missingPrereqNames = mutableListOf<String>()
        var requiredMinUnits: Int? = null
        val unsatisfiedReasons = mutableListOf<String>()

        val isSatisfied = evaluateCondition(
            condition = root,
            passedCourseIds = passedCourseIds,
            currentCourseIds = currentCourseIds,
            passedCredits = passedCredits,
            outMissingNames = missingPrereqNames,
            outMinUnits = { req -> requiredMinUnits = req },
            outReasons = unsatisfiedReasons
        )

        return if (isSatisfied) {
            EvaluationResult.Satisfied
        } else {
            EvaluationResult.Unsatisfied(
                BlockedReason(
                    missingPrerequisiteNames = missingPrereqNames.distinct(),
                    requiredMinUnits = requiredMinUnits,
                    currentPassedUnits = passedCredits,
                    explanation = unsatisfiedReasons.joinToString("؛ ")
                )
            )
        }
    }

    private fun evaluateCondition(
        condition: PrerequisiteCondition,
        passedCourseIds: Set<String>,
        currentCourseIds: Set<String>,
        passedCredits: Int,
        outMissingNames: MutableList<String>,
        outMinUnits: (Int) -> Unit,
        outReasons: MutableList<String>
    ): Boolean {
        return when (condition) {
            is PrerequisiteCondition.CoursePassed -> {
                val passed = passedCourseIds.contains(condition.courseId)
                if (!passed) {
                    outMissingNames.add(condition.canonicalName)
                    outReasons.add("گذراندن پیش‌نیاز «${condition.canonicalName}» الزامی است")
                }
                passed
            }
            is PrerequisiteCondition.CoRequisite -> {
                val satisfied = passedCourseIds.contains(condition.courseId) || currentCourseIds.contains(condition.courseId)
                if (!satisfied) {
                    outMissingNames.add(condition.canonicalName)
                    outReasons.add("اخذ همزمان یا گذراندن هم‌نیاز «${condition.canonicalName}» الزامی است")
                }
                satisfied
            }
            is PrerequisiteCondition.MinimumTotalPassedCredits -> {
                val satisfied = passedCredits >= condition.requiredCredits
                if (!satisfied) {
                    outMinUnits(condition.requiredCredits)
                    val deficit = condition.requiredCredits - passedCredits
                    outReasons.add("نیازمند گذراندن حداقل ${condition.requiredCredits} واحد (کسری: $deficit واحد)")
                }
                satisfied
            }
            is PrerequisiteCondition.AndGroup -> {
                var allPassed = true
                for (c in condition.conditions) {
                    val res = evaluateCondition(c, passedCourseIds, currentCourseIds, passedCredits, outMissingNames, outMinUnits, outReasons)
                    if (!res) allPassed = false
                }
                allPassed
            }
            is PrerequisiteCondition.OrGroup -> {
                val tempMissing = mutableListOf<String>()
                val tempReasons = mutableListOf<String>()
                var anyPassed = false
                for (c in condition.conditions) {
                    val localMissing = mutableListOf<String>()
                    val localReasons = mutableListOf<String>()
                    if (evaluateCondition(c, passedCourseIds, currentCourseIds, passedCredits, localMissing, outMinUnits, localReasons)) {
                        anyPassed = true
                        break
                    } else {
                        tempMissing.addAll(localMissing)
                        tempReasons.addAll(localReasons)
                    }
                }
                if (!anyPassed) {
                    outMissingNames.addAll(tempMissing)
                    outReasons.add("حداقل یکی از شروط زیر باید برقرار باشد: " + tempReasons.joinToString(" یا "))
                }
                anyPassed
            }
        }
    }
}

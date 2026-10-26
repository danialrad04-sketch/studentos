package com.example.domain.engine

/** A recorded grade is a fixed total out of 20; null means the result is unknown. */
data class GradeGoalCourse(val units: Int, val score: Double?)

data class GradeGoalResult(
    val totalUnits: Int,
    val remainingUnits: Int,
    val recordedAverage: Double?,
    val requiredAverage: Double?,
    val maximumAverage: Double?,
    val achievable: Boolean,
    val invalidCourses: Int
)

object GradeGoalEngine {
    fun calculate(courses: List<GradeGoalCourse>, target: Double): GradeGoalResult {
        require(target.isFinite() && target in 0.0..20.0)
        val valid = courses.filter { it.units > 0 && (it.score == null || it.score.isFinite() && it.score in 0.0..20.0) }
        val units = valid.sumOf { it.units }
        val remaining = valid.filter { it.score == null }.sumOf { it.units }
        val weighted = valid.sumOf { (it.score ?: 0.0) * it.units }
        val recorded = units - remaining
        val maximum = if (units > 0) (weighted + remaining * 20.0) / units else null
        return GradeGoalResult(
            totalUnits = units, remainingUnits = remaining,
            recordedAverage = if (recorded > 0) weighted / recorded else null,
            requiredAverage = if (remaining > 0) ((target * units - weighted) / remaining).coerceAtLeast(0.0) else null,
            maximumAverage = maximum,
            achievable = maximum != null && maximum + 1e-9 >= target,
            invalidCourses = courses.size - valid.size
        )
    }
}

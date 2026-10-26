package com.example.domain.engine

data class ExamScoreResult(val required: Double, val achievable: Boolean, val maximumTotal: Double)

/** Explicit instructor weighting; never infer exam weights from a stored partial grade. */
object ExamScoreEngine {
    fun calculate(earned: Double, finalMaximum: Double, target: Double): ExamScoreResult? {
        if (!earned.isFinite() || !finalMaximum.isFinite() || !target.isFinite() ||
            finalMaximum <= 0.0 || finalMaximum > 20.0 || earned < 0.0 ||
            earned > 20.0 - finalMaximum + 1e-9 || target !in 0.0..20.0) return null
        val required = (target - earned).coerceAtLeast(0.0)
        return ExamScoreResult(required, required <= finalMaximum + 1e-9, earned + finalMaximum)
    }
}

package com.example.ui.components.export

import com.example.domain.model.GpaState
import java.util.Locale

/** Reports contain recorded facts; absent grades are never rendered as invented zeroes. */
fun academicGpaText(payload: ExportSourcePayload): String {
    val value = when (payload) {
        is ExportSourcePayload.Grades -> {
            val recorded = payload.grades.filter { it.units > 0 && it.hasRecordedScore() }
            if (recorded.isEmpty()) null else recorded.sumOf { (it.midtermGrade + it.finalGrade) * it.units } / recorded.sumOf { it.units }
        }
        is ExportSourcePayload.Passport -> when (val state = payload.progress?.gpaState) {
            is GpaState.Known -> state.value
            is GpaState.PartiallyCalculated -> state.currentSemesterGpa
            else -> payload.profile.declaredGpa
        }
    }
    return value?.takeIf { it.isFinite() && it in 0.0..20.0 }?.let { String.format(Locale.US, "%.2f", it) } ?: "—"
}

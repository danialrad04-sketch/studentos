package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.DataConfidence
import com.example.domain.model.GpaState
import com.example.domain.model.StudySessionRecommendation
import com.example.ui.models.AcademicProgressUiState
import java.util.Locale

/** A factual overview: every number is derived from the student's stored academic records. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AcademicJourneyOverview(
    state: AcademicProgressUiState?,
    targetGpa: Double?,
    recommendations: List<StudySessionRecommendation>,
    dailyStudyGoalMinutes: Int,
    studiedTodayMinutes: Int = 0,
    onOpenPassport: () -> Unit,
    onOpenGrades: () -> Unit,
    onOpenFocus: () -> Unit,
    modifier: Modifier = Modifier,
    declaredPassedUnits: Int = 0,
    declaredGpa: String? = null,
    onCompleteRecords: (() -> Unit)? = null
) {
    val progress = when (state) {
        is AcademicProgressUiState.Ready -> state.progress
        is AcademicProgressUiState.Partial -> state.progress
        else -> null
    }
    val confidence = progress?.confidence
    val confidenceLabel = when (confidence) {
        DataConfidence.VERIFIED_FULL -> "سوابق کامل"
        DataConfidence.PARTIAL_HISTORY -> "سوابق نیمه‌کامل"
        DataConfidence.CREDIT_ONLY -> "بر پایه واحد اعلامی"
        else -> "نیازمند تکمیل اطلاعات"
    }
    val gpaLabel = when (val gpa = progress?.gpaState) {
        is GpaState.Known -> String.format(Locale.US, "%.2f", gpa.value)
        is GpaState.PartiallyCalculated -> String.format(Locale.US, "%.2f", gpa.currentSemesterGpa)
        else -> "ثبت نشده"
    }
    val plannedMinutes = recommendations.sumOf { it.recommendedDurationMinutes }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Rounded.Route, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(10.dp).size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("مسیر تحصیلی من", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(confidenceLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (progress != null) Text("${progress.progressPercentage.toInt().coerceIn(0, 100)}٪", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            }

            if (progress == null) {
                Text(
                    when (state) {
                        is AcademicProgressUiState.Error -> state.message
                        AcademicProgressUiState.Loading -> "در حال محاسبهٔ مسیر تحصیلی…"
                        else -> "برای محاسبهٔ دقیق پیشرفت، چارت رشته و سوابق ترم‌ها را کامل کنید."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (declaredPassedUnits > 0 || declaredGpa != null) {
                    Text("اطلاعات اعلام‌شدهٔ شما", style = MaterialTheme.typography.titleSmall)
                    Text("$declaredPassedUnits واحد گذرانده · معدل ${declaredGpa ?: "ثبت نشده"}",
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("تا تعیین چارت رشته، درصد و واحد باقی‌مانده محاسبه نمی‌شود.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                onCompleteRecords?.let { action ->
                    FilledTonalButton(onClick = action, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("تکمیل چارت و سوابق") }
                }
            } else {
                LinearProgressIndicator(
                    progress = { (progress.progressPercentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = if (LocalDensity.current.fontScale >= 1.3f) 1 else 3,
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    JourneyFact(Icons.Rounded.CheckCircle, "گذرانده", "${progress.passedCredits} واحد", Modifier.weight(1f))
                    JourneyFact(Icons.Rounded.School, "این ترم", "${progress.currentCredits} واحد", Modifier.weight(1f))
                    JourneyFact(Icons.Rounded.Flag, "باقی‌مانده", "${progress.remainingCredits} واحد", Modifier.weight(1f))
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(if (progress.gpaState is GpaState.PartiallyCalculated) "معدل محاسبه‌شدهٔ این ترم" else "معدل ثبت‌شده", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(gpaLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    targetGpa?.let {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("هدف معدل", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.US, "%.2f", it), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (dailyStudyGoalMinutes > 0) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("مطالعهٔ ثبت‌شدهٔ امروز", fontWeight = FontWeight.Bold)
                            Text("$studiedTodayMinutes از $dailyStudyGoalMinutes دقیقه", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        LinearProgressIndicator(
                            progress = { (studiedTodayMinutes.toFloat() / dailyStudyGoalMinutes).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        if (plannedMinutes > 0) Text("پیشنهاد آماده برای ادامه: $plannedMinutes دقیقه", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = if (LocalDensity.current.fontScale >= 1.3f) 1 else 2,
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onOpenPassport, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("جزئیات مسیر") }
                OutlinedButton(onClick = if (recommendations.isEmpty()) onOpenGrades else onOpenFocus, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text(if (recommendations.isEmpty()) "نمرات" else "شروع مطالعه")
                }
            }
        }
    }
}

@Composable
private fun JourneyFact(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            Spacer(Modifier.height(5.dp))
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

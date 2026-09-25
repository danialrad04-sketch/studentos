package com.example.ui.components

import androidx.compose.material3.Text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.model.AcademicRisk
import com.example.domain.model.RiskSeverity
import com.example.ui.models.AppTab
import com.example.ui.theme.AcademicNavy
import com.example.ui.theme.AcademicOlive
import com.example.ui.theme.StudentShapeTokens
import com.example.ui.theme.StudentSpacing

@Composable
fun AcademicIntelligenceScreen(
    gpa: Double,
    passedUnits: Int,
    totalRequiredCredits: Int,
    courses: List<CourseEntity>,
    attendance: List<AttendanceEntity>,
    tasks: List<TaskEntity>,
    grades: List<GradeEntity>,
    risks: List<AcademicRisk>,
    onOpenTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val completion = if (totalRequiredCredits > 0) {
        (passedUnits.toFloat() / totalRequiredCredits.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val status = when {
        courses.isEmpty() -> "داده کافی برای ارزیابی وجود ندارد"
        risks.any { it.severity == RiskSeverity.CRITICAL } -> "نیازمند توجه فوری"
        risks.isNotEmpty() -> "نیازمند توجه"
        else -> "وضعیت تحصیلی پایدار"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(StudentSpacing.Lg)
    ) {
        AcademicCard {
            Column(modifier = Modifier.padding(StudentSpacing.Xxl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = StudentShapeTokens.Compact, color = AcademicNavy.copy(alpha = 0.10f)) {
                        Icon(
                            Icons.Outlined.Analytics,
                            contentDescription = null,
                            tint = AcademicNavy,
                            modifier = Modifier.padding(StudentSpacing.Sm).size(20.dp)
                        )
                    }
                    Spacer(Modifier.size(StudentSpacing.Md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("وضعیت کلی تحصیلی", style = MaterialTheme.typography.headlineSmall)
                        Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(StudentSpacing.Xxl))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(StudentSpacing.Md)
                ) {
                    IntelligenceMetric("معدل", if (grades.isEmpty()) "—" else String.format("%.2f", gpa), Modifier.weight(1f))
                    IntelligenceMetric("واحد", "$passedUnits/$totalRequiredCredits", Modifier.weight(1f))
                    IntelligenceMetric("درس فعال", courses.distinctBy { it.id }.size.toString(), Modifier.weight(1f))
                }

                Spacer(Modifier.height(StudentSpacing.Xl))
                Text("پیشرفت چارت", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(StudentSpacing.Sm))
                LinearProgressIndicator(
                    progress = { completion },
                    modifier = Modifier.fillMaxWidth(),
                    color = AcademicOlive
                )
                Spacer(Modifier.height(StudentSpacing.Xs))
                Text(
                    "${(completion * 100).toInt()}٪ تکمیل",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AcademicSectionHeader(
            title = "تحلیل‌های قابل توجه",
            subtitle = "خلاصه تحلیل بر اساس داده‌های ثبت‌شده و موتورهای قطعی Student OS."
        )

        if (risks.isEmpty()) {
            AcademicCard {
                Column(modifier = Modifier.padding(StudentSpacing.Xxl)) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = AcademicOlive)
                    Spacer(Modifier.height(StudentSpacing.Md))
                    Text("مورد مهمی شناسایی نشد", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "بر اساس داده‌های فعلی، موتور ارزیابی مورد قابل توجهی گزارش نکرده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            risks.take(5).forEach { risk ->
                AcademicCard {
                    Column(modifier = Modifier.padding(StudentSpacing.Xl)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val critical = risk.severity == RiskSeverity.CRITICAL
                            Icon(
                                if (critical) Icons.Outlined.ErrorOutline else Icons.Outlined.Info,
                                contentDescription = null,
                                tint = if (critical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.size(StudentSpacing.Sm))
                            Text(risk.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(StudentSpacing.Sm))
                        Text(
                            risk.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(StudentSpacing.Md))
                        Text("پیشنهاد: ${risk.recommendedAction}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        AcademicCard(onClick = { onOpenTab(AppTab.TASKS) }) {
            Row(
                modifier = Modifier.padding(StudentSpacing.Xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.TrendingUp, contentDescription = null, tint = AcademicNavy)
                Spacer(Modifier.size(StudentSpacing.Md))
                Column(modifier = Modifier.weight(1f)) {
                    Text("گام پیشنهادی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        if (tasks.count { !it.isCompleted } > 0) "تکالیف باز را مرور و بر اساس موعد مرتب کنید."
                        else "برای حفظ روند فعلی، یک هدف مطالعه مشخص کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun IntelligenceMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AcademicRisk
import com.example.domain.model.RiskSeverity
import com.example.domain.model.WeeklyAcademicWorkload
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Rose500
import com.example.ui.theme.StudentOsShapes
import com.example.ui.theme.StudentOsSpacing

/**
 * Modern Academic Command Center Tiles (Phases 20 & 21).
 * Displays Academic Risks and Weekly Workload analysis with constructive, actionable tone.
 */
@Composable
fun AcademicIntelligenceBanner(
    risks: List<AcademicRisk>,
    workload: WeeklyAcademicWorkload,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(StudentOsSpacing.md)
    ) {
        // 1. Academic Risk Alert (if any detected)
        if (risks.isNotEmpty()) {
            val topRisk = risks.maxByOrNull { it.severity } ?: risks.first()
            val (riskColor, riskBg) = when (topRisk.severity) {
                RiskSeverity.CRITICAL -> Rose500 to Rose500.copy(alpha = 0.12f)
                RiskSeverity.HIGH -> Rose500 to Rose500.copy(alpha = 0.08f)
                RiskSeverity.MEDIUM -> Amber500 to Amber500.copy(alpha = 0.08f)
                RiskSeverity.LOW -> Emerald500 to Emerald500.copy(alpha = 0.08f)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(StudentOsShapes.mediumCard),
                colors = CardDefaults.cardColors(containerColor = riskBg),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(StudentOsSpacing.mdPlus),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(StudentOsSpacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(riskColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (topRisk.severity == RiskSeverity.CRITICAL) Icons.Default.Warning else Icons.Default.Info,
                            contentDescription = null,
                            tint = riskColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = topRisk.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (risks.size > 1) {
                                Surface(
                                    shape = RoundedCornerShape(StudentOsShapes.pill),
                                    color = riskColor.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "+${risks.size - 1} هشدار دیگر",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = riskColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = topRisk.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "💡 راهکار پیشنهادی: ${topRisk.recommendedAction}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // 2. Weekly Workload Estimate Card
        if (workload.totalEstimatedWeeklyHours > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(StudentOsShapes.mediumCard),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = StudentOsSpacing.mdPlus, vertical = StudentOsSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(StudentOsSpacing.sm)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "بار تحصیلی تخمینی هفته",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "شامل ${workload.classLectureHours.toInt()}h کلاس، ${workload.estimatedSelfStudyHours.toInt()}h مطالعه",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(StudentOsShapes.pill),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "≈ ${workload.totalEstimatedWeeklyHours.toInt()} ساعت",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

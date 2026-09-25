package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.SemesterPlannerEngine
import com.example.domain.model.CourseState
import com.example.domain.model.EvaluatedCurriculumCourse
import com.example.domain.model.SemesterPlan
import com.example.ui.models.CurriculumMatchUiState
import com.example.ui.theme.StudentOsColors
import com.example.ui.theme.StudentShapeTokens
import com.example.ui.theme.StudentSpacing
import com.example.ui.theme.StudentOsShapes
import com.example.ui.theme.StudentOsSpacing

/**
 * Phase 12 & 13: Build My Semester Planner Screen with Plan Comparison.
 * Evaluates prerequisites, credit load, weekly workload, and trade-offs.
 */
@Composable
fun SemesterPlannerScreen(
    matchState: CurriculumMatchUiState,
    candidatePlans: List<SemesterPlan>,
    onPlanSelected: (SemesterPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPlanId by remember(candidatePlans) {
        mutableStateOf(candidatePlans.firstOrNull()?.id ?: "")
    }
    var showComparison by remember { mutableStateOf(false) }

    val activePlan = candidatePlans.find { it.id == selectedPlanId } ?: candidatePlans.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = StudentOsSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(StudentOsSpacing.lg)
    ) {
        item {
            Spacer(modifier = Modifier.height(StudentOsSpacing.sm))
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "برنامه‌ریز ترم تحصیلی (Build My Semester)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "برنامه‌ریزی هوشمند دروس بر پایه پیش‌نیازها و سقف واحد",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (candidatePlans.size >= 2) {
                    Surface(
                        shape = StudentShapeTokens.Compact,
                        color = if (showComparison) StudentOsColors.LightIndigoSoft else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.tactileClickable(performHaptic = true) {
                            showComparison = !showComparison
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (showComparison) "بستن مقایسه" else "مقایسه سناریوها",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Comparison Banner if enabled
        if (showComparison && candidatePlans.size >= 2) {
            item {
                val comparison = remember(candidatePlans) {
                    SemesterPlannerEngine.comparePlans(candidatePlans[0], candidatePlans[1])
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = StudentShapeTokens.Card,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(StudentOsSpacing.mdPlus),
                        verticalArrangement = Arrangement.spacedBy(StudentOsSpacing.sm)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(StudentOsSpacing.xs)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "تحلیل مقایسه‌ای سناریوهای ترم آتی",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = comparison.summaryTradeOff,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Candidate Plan Selector Chips
        if (candidatePlans.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(StudentOsSpacing.sm)
                ) {
                    candidatePlans.forEach { plan ->
                        val isSelected = plan.id == selectedPlanId
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPlanId = plan.id },
                            label = {
                                Text(
                                    text = "${plan.name} (${plan.totalCredits} واحد)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Active Plan Overview Card
        if (activePlan != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = StudentShapeTokens.Hero,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(StudentOsSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(StudentOsSpacing.md)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = activePlan.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${activePlan.courses.size} درس انتخاب شده",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(StudentOsShapes.pill),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${activePlan.totalCredits} واحد",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Workload estimation badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(StudentOsShapes.button))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(StudentOsSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(StudentOsSpacing.xs)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "برآورد مطالعه هفتگی:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "حدود ${activePlan.estimatedWeeklyWorkloadHours.toInt()} ساعت در هفته",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Trade-offs list
                        if (activePlan.tradeOffs.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                activePlan.tradeOffs.forEach { tradeOff ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Text(
                                            text = tradeOff,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Activate / Save Plan as Primary Choice
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { onPlanSelected(activePlan) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(StudentOsShapes.button),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تایید و ثبت به عنوان برنامه هدف ترم",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Planned Courses List
            item {
                Text(
                    text = "دروس پیشنهادی این سناریو",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(activePlan.courses, key = { it.courseId }) { course ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(StudentOsShapes.mediumCard),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(StudentOsSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = course.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${course.courseType} · ترم پیشنهادی ${course.recommendedSemester}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(StudentOsShapes.pill),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${course.units} واحد",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(StudentOsShapes.largeContainer),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(StudentOsSpacing.hero),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(StudentOsSpacing.md)
                    ) {
                        Text(
                            text = "💡 درسی برای برنامه‌ریزی یافت نشد",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "جهت ارائه سناریوهای هوشمند ترم، ابتدا وضعیت پیشرفت چارت خود را در برگه شناسنامه تحصیلی بررسی نمایید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(StudentOsSpacing.hero))
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CurriculumCourseEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.ui.theme.Amber600
import com.example.ui.theme.BrandIndigo600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose600
import java.util.Locale

data class SemesterSummaryItem(
    val semesterIndex: Int,
    var units: String,
    var gpa: String
)

@Composable
fun PastSemestersHistoryDialog(
    profile: StudentProfileEntity,
    curriculumCourses: List<CurriculumCourseEntity>,
    onDismiss: () -> Unit,
    onSaveSummaryHistory: (selectedSemester: Int, totalPassedCredits: Int, overallGpa: Double, semesterSummaries: List<SemesterSummaryItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Detect suggested semester based on entry year & units
    val suggestedSemester = remember(profile.entryYear, profile.passedUnits) {
        val fromUnits = when {
            profile.passedUnits < 16 -> 1
            profile.passedUnits < 34 -> 2
            profile.passedUnits < 52 -> 3
            profile.passedUnits < 70 -> 4
            profile.passedUnits < 88 -> 5
            profile.passedUnits < 106 -> 6
            profile.passedUnits < 124 -> 7
            else -> 8
        }
        profile.currentSemester.coerceIn(1, 8)
    }

    var selectedSemester by remember { mutableIntStateOf(profile.currentSemester.coerceIn(1, 8)) }
    var selectedTabMode by remember { mutableIntStateOf(0) } // 0: خلاصه سریع ترم‌ها, 1: ثبت مجموع کلی

    // Past semesters inputs state
    val pastSummaries = remember(selectedSemester) {
        val map = mutableStateMapOf<Int, SemesterSummaryItem>()
        val count = selectedSemester - 1
        val avgUnitsPerTerm = if (count > 0) (profile.passedUnits / count).coerceIn(14, 20) else 17
        val defaultGpaStr = String.format(Locale.US, "%.2f", profile.declaredGpa ?: 16.5)

        for (i in 1..count) {
            map[i] = SemesterSummaryItem(
                semesterIndex = i,
                units = avgUnitsPerTerm.toString(),
                gpa = defaultGpaStr
            )
        }
        map
    }

    var totalDirectPassedUnits by remember { mutableStateOf(profile.passedUnits.toString()) }
    var overallDirectGpa by remember { mutableStateOf(String.format(Locale.US, "%.2f", profile.declaredGpa ?: 16.5)) }

    StudentGlassModalSheet(
        onDismiss = onDismiss,
        maxWidth = 600.dp
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
        ) {
            // Top bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandIndigo600.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = BrandIndigo600,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تنظیم ترم و سوابق گذشته",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "پشتیبانی از دانشجویان ترم ۱ تا ۸ و ورودی‌های مختلف",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن پنجره سوابق تحصیلی")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: Semester Selector
                Text(
                    text = "ترم تحصیلی فعلی شما چیست؟",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (term in 1..8) {
                        val isSelected = selectedSemester == term
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .semantics {
                                    role = Role.Tab
                                    contentDescription = "ترم $term، ${if (isSelected) "انتخاب شده" else "انتخاب نشده"}"
                                }
                                .clickable { selectedSemester = term },
                            color = if (isSelected) BrandIndigo600 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$term",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                    ),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Tabs
                TabRow(
                    selectedTabIndex = selectedTabMode,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    contentColor = BrandIndigo600,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTabMode == 0,
                        onClick = { selectedTabMode = 0 },
                        text = {
                            Text(
                                "ثبت ترم به ترم (${selectedSemester - 1} ترم قبل)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTabMode == 1,
                        onClick = { selectedTabMode = 1 },
                        text = {
                            Text(
                                "ثبت مجموعی سریع",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content area with spring-based AnimatedContent transition
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AnimatedContent(
                        targetState = selectedTabMode,
                        transitionSpec = {
                            fadeIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) togetherWith fadeOut(
                                animationSpec = spring(stiffness = Spring.StiffnessMedium)
                            )
                        },
                        label = "tab_mode_transition"
                    ) { tabMode ->
                        if (tabMode == 0) {
                            if (selectedSemester == 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "🌱 شما دانشجوی ترم ۱ (نوورود) هستید",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Emerald600
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "در ترم ۱ هنوز سابقه گذرانده‌ای ثبت نشده است و تمام تمرکز روی دروس فعال ترم اول خواهد بود.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items((1 until selectedSemester).toList()) { termIndex ->
                                        val item = pastSummaries[termIndex] ?: SemesterSummaryItem(termIndex, "17", "16.5")

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(CircleShape)
                                                            .background(BrandIndigo600.copy(alpha = 0.15f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "$termIndex",
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = BrandIndigo600
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "ترم $termIndex",
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedTextField(
                                                        value = item.units,
                                                        onValueChange = { newVal ->
                                                            pastSummaries[termIndex] = item.copy(units = newVal.filter { it.isDigit() })
                                                        },
                                                        label = { Text("واحد", style = MaterialTheme.typography.labelSmall) },
                                                        modifier = Modifier.width(76.dp),
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )

                                                    OutlinedTextField(
                                                        value = item.gpa,
                                                        onValueChange = { newVal ->
                                                            pastSummaries[termIndex] = item.copy(gpa = newVal)
                                                        },
                                                        label = { Text("معدل", style = MaterialTheme.typography.labelSmall) },
                                                        modifier = Modifier.width(84.dp),
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Quick total entry
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "اگر مایل نیستید ریز ترم‌ها را ثبت کنید، فقط مجموع واحد پاس‌شده و معدل کل خود را وارد کنید:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = totalDirectPassedUnits,
                                    onValueChange = { totalDirectPassedUnits = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("مجموع واحدهای پاس‌شده تا کنون (مثلاً ۵۴)", style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                OutlinedTextField(
                                    value = overallDirectGpa,
                                    onValueChange = { overallDirectGpa = it },
                                    label = { Text("معدل کل تخمینی یا اعلامی (از ۲۰)", style = MaterialTheme.typography.labelMedium) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(12.dp))

                // Bottom actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            var totalUnits = 0
                            var totalWeightedScore = 0.0
                            val summariesList = mutableListOf<SemesterSummaryItem>()

                            if (selectedTabMode == 0 && selectedSemester > 1) {
                                for (i in 1 until selectedSemester) {
                                    val item = pastSummaries[i] ?: SemesterSummaryItem(i, "17", "16.5")
                                    val u = item.units.toIntOrNull() ?: 17
                                    val g = item.gpa.toDoubleOrNull() ?: 16.5
                                    totalUnits += u
                                    totalWeightedScore += (u * g)
                                    summariesList.add(item)
                                }
                            } else if (selectedTabMode == 1 || selectedSemester == 1) {
                                totalUnits = totalDirectPassedUnits.toIntOrNull() ?: 0
                                val g = overallDirectGpa.toDoubleOrNull() ?: 16.5
                                totalWeightedScore = totalUnits * g
                            }

                            val computedGpa = if (totalUnits > 0) totalWeightedScore / totalUnits.toDouble() else (overallDirectGpa.toDoubleOrNull() ?: 16.5)

                            onSaveSummaryHistory(
                                selectedSemester,
                                totalUnits,
                                computedGpa,
                                summariesList
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandIndigo600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .semantics {
                                role = Role.Button
                                contentDescription = "ذخیره و همگام‌سازی چارت با سوابق تحصیلی ترم‌ها"
                            }
                            .testTag("save_semester_history_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ذخیره و همگام‌سازی چارت", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
    }
}

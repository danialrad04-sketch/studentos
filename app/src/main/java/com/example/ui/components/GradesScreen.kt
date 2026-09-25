package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.example.ui.theme.studentColors
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GradeEntity
import com.example.ui.theme.Amber500
import com.example.ui.theme.BrandIndigo600
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.Emerald50
import com.example.ui.theme.Emerald600
import com.example.ui.theme.NumericBadgeText
import com.example.ui.theme.NumericDisplayHero
import com.example.ui.theme.NumericDisplayStat
import com.example.ui.theme.Rose50
import com.example.ui.theme.Rose600
import com.example.ui.theme.Sky50
import com.example.ui.theme.Sky600
import com.example.ui.theme.StudentOsColors
import com.example.ui.theme.StudentShapeTokens
import com.example.ui.theme.StudentSpacing
import com.example.ui.theme.StudentOsGlassTokens
import java.util.Locale

@Composable
fun GradesScreen(
    grades: List<GradeEntity>,
    onUpdateGrade: (GradeEntity, Double, Double) -> Unit,
    onOpenExport: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var editingGrade by remember { mutableStateOf<GradeEntity?>(null) }

    // Weighted GPA calculation
    val totalUnits = grades.sumOf { it.units }
    val totalWeightedScore = grades.sumOf { (it.midtermGrade + it.finalGrade) * it.units }
    val gpa = if (totalUnits > 0) totalWeightedScore / totalUnits else 0.0
    val gpaFormatted = String.format(Locale.US, "%.2f", gpa)

    val isHonors = gpa >= 17.0
    val isNormal = gpa in 12.0..<17.0
    val passedCoursesCount = grades.count { (it.midtermGrade + it.finalGrade) >= 10.0 }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header with Export Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "کارنامه و شبیه‌ساز سقف ترم ۴",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "نقشه وضعیت نمرات، شبیه‌ساز معدل الف و مدیریت سقف انتخاب واحد",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onOpenExport != null) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenExport()
                    },
                    shape = StudentShapeTokens.Compact,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "پاسپورت تحصیلی 📸",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. CUMULATIVE STATUS HERO CARD (مهر اصالت وضعیت تحصیلی)
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = StudentShapeTokens.Hero,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = if (isDark) 0.dp else 1.5.dp,
            border = BorderStroke(
                1.dp,
                when {
                    isHonors -> Emerald600.copy(alpha = 0.6f)
                    isNormal -> if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
                    else -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Seal of status badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isHonors -> Emerald600.copy(alpha = 0.16f)
                                isNormal -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isHonors) Icons.Default.Star else Icons.Default.School,
                                    contentDescription = null,
                                    tint = when {
                                        isHonors -> Emerald600
                                        isNormal -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.error
                                    },
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        isHonors -> "مهر اصالت: دانشجوی ممتاز الف ✨"
                                        isNormal -> "مهر اصالت: وضعیت عادی تحصیلی 📘"
                                        else -> "مهر اصالت: در خطر مشروطی آموزش ⚠️"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isHonors -> Emerald600
                                        isNormal -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            isHonors -> "معدل شما بالای ۱۷ است. مجاز به اخذ سقف حداکثری ۲۴ واحد در ترم بعدی هستید."
                            isNormal -> "معدل بین ۱۲ تا ۱۶.۹۹ است. سقف انتخاب واحد استاندارد ۲۰ واحد مجاز می‌باشد."
                            else -> "معدل کمتر از ۱۲ است. طبق آیین‌نامه، سقف انتخاب واحد ترم بعدی ۱۴ واحد خواهد بود."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = "سقف مجاز: ${if (isHonors) "۲۴ واحد" else if (isNormal) "۲۰ واحد" else "۱۴ واحد"}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = "$passedCoursesCount از ${grades.size} درس قبول",
                                style = MaterialTheme.typography.labelMedium,
                                color = Emerald600,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Prominent GPA Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "معدل کل",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = gpaFormatted,
                        style = NumericDisplayHero,
                        fontSize = 28.sp,
                        color = when {
                            isHonors -> Emerald600
                            isNormal -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = "$totalUnits واحد ثبت‌شده",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. TARGET WHAT-IF SIMULATOR CARD (پیش‌بینی معدل الف ۱۷ به بالا)
        GradeWhatIfSimulatorCard(
            grades = grades,
            totalUnits = totalUnits,
            onApplySimulation = { simulatedPairs ->
                simulatedPairs.forEach { (g, mid, fin) ->
                    onUpdateGrade(g, mid, fin)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Section Title: Course Grade Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "کارت‌های عملکرد دروس (${grades.size} درس)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "برای تغییر نمرات هر درس لمس کنید",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (grades.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = StudentShapeTokens.Card,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowElevation = if (isDark) 0.dp else 1.5.dp,
                border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "هنوز درسی در کارنامه ثبت نشده است",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "با افزودن دروس به برنامه یا ثبت نمرات در بخش دروس، محاسبات معدل و شبیه‌ساز فعال می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // 3. INDIVIDUAL COURSE GRADE CARDS
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                grades.forEach { grade ->
                    val totalScore = grade.midtermGrade + grade.finalGrade
                    val isPassed = totalScore >= 10.0

                    CourseGradeCard(
                        grade = grade,
                        totalScore = totalScore,
                        isPassed = isPassed,
                        onEdit = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            editingGrade = grade
                        }
                    )
                }
            }
        }
    }

    // Edit Grade Dialog
    editingGrade?.let { target ->
        EditGradeDialog(
            grade = target,
            onDismiss = { editingGrade = null },
            onSave = { mid, fin ->
                onUpdateGrade(target, mid, fin)
                editingGrade = null
            }
        )
    }
}

@Composable
private fun CourseGradeCard(
    grade: GradeEntity,
    totalScore: Double,
    isPassed: Boolean,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = if (isDark) 0.dp else 1.5.dp,
        border = BorderStroke(
            1.dp,
            if (isPassed) {
                if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)
            } else {
                MaterialTheme.colorScheme.error.copy(alpha = 0.65f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Course Name, Units Badge, Pass Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = grade.courseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "${grade.units} واحد",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPassed) Emerald600.copy(alpha = 0.16f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (isPassed) "قبولی قطعی ✅" else "مردود ❌",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPassed) Emerald600 else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Score Metrics Row: Midterm (6) | Final (14) | Total (20)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Midterm Score Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(StudentShapeTokens.Compact)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "بارم میان‌ترم",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", grade.midtermGrade),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Final Score Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "بارم پایان‌ترم",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", grade.finalGrade),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Total Final Grade Pill
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isPassed) Emerald600.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                        .border(
                            0.8.dp,
                            if (isPassed) Emerald600.copy(alpha = 0.3f) else MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "نمره نهایی",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isPassed) Emerald600 else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.1f", totalScore)} از ۲۰",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = if (isPassed) Emerald600 else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun EditGradeDialog(
    grade: GradeEntity,
    onDismiss: () -> Unit,
    onSave: (mid: Double, fin: Double) -> Unit
) {
    var mid by remember { mutableStateOf(grade.midtermGrade) }
    var fin by remember { mutableStateOf(grade.finalGrade) }

    StudentGlassModalSheet(
        onDismiss = onDismiss,
        maxHeightPercent = 0.70f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "تنظیم نمرات: ${grade.courseName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Midterm Stepper (out of 6)
            Text(
                text = "میان‌ترم (از ۶ نمره):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (mid > 0.0) mid = (mid - 0.25).coerceAtLeast(0.0) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "کاهش میان‌ترم",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.2f", mid),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { if (mid < 6.0) mid = (mid + 0.25).coerceAtMost(6.0) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "افزایش میان‌ترم",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Final Stepper (out of 14)
            Text(
                text = "پایان‌ترم (از ۱۴ نمره):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (fin > 0.0) fin = (fin - 0.5).coerceAtLeast(0.0) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "کاهش پایان‌ترم",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.2f", fin),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { if (fin < 14.0) fin = (fin + 0.5).coerceAtMost(14.0) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "افزایش پایان‌ترم",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preview total
            val totalScore = mid + fin
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (totalScore >= 10) Emerald600.copy(alpha = 0.12f) else Rose600.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, if (totalScore >= 10) Emerald600.copy(alpha = 0.3f) else Rose600.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "نمره نهایی برآوردشده:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.2f", totalScore)} از ۲۰",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = if (totalScore >= 10) Emerald600 else Rose600
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(text = "انصراف", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = { onSave(mid, fin) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = "ثبت نمره",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Phase 18: Grade What-If Simulator (شبیه‌ساز سناریوهای نمره و حداقل‌های معدل الف)
 * Real-time dynamic simulator calculating required remaining final exam points
 * to achieve honors status (GPA >= 17.0) or custom target GPA.
 */
@Composable
fun GradeWhatIfSimulatorCard(
    grades: List<GradeEntity>,
    totalUnits: Int,
    onApplySimulation: (List<Triple<GradeEntity, Double, Double>>) -> Unit,
    modifier: Modifier = Modifier
) {
    var targetGpa by remember { mutableStateOf(17.0f) }

    val earnedMidtermPoints = grades.sumOf { it.midtermGrade * it.units }
    val targetTotalPoints = targetGpa * totalUnits
    val neededFinalPoints = targetTotalPoints - earnedMidtermPoints
    val requiredFinalAverage = if (totalUnits > 0) neededFinalPoints / totalUnits else 0.0

    val isFeasible = requiredFinalAverage <= 20.0
    val isAlreadyPassed = requiredFinalAverage <= 0.0

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = StudentShapeTokens.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "شبیه‌ساز سناریوهای نمره (What-If)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "هدف: ${String.format(Locale.US, "%.1f", targetGpa)}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "اسلایدر را تغییر دهید تا مشخص شود برای رسیدن به این معدل، پایان‌ترم دروس را میانگین چند باید بگیرید:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Target Slider
            Slider(
                value = targetGpa,
                onValueChange = { targetGpa = it },
                valueRange = 12.0f..19.5f,
                steps = 74, // 0.1 step increments
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "۱۲.۰ (حداقل قبولی)", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "۱۷.۰ (شرط الف / ۲۴ واحد)", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(text = "۱۹.۵ (سقف عالی)", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Result Callout Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = when {
                    isAlreadyPassed -> Emerald600.copy(alpha = 0.12f)
                    requiredFinalAverage in 0.0..14.0 -> Emerald600.copy(alpha = 0.12f)
                    requiredFinalAverage in 14.0..17.5 -> Sky600.copy(alpha = 0.12f)
                    requiredFinalAverage in 17.5..20.0 -> Amber500.copy(alpha = 0.12f)
                    else -> Rose600.copy(alpha = 0.12f)
                }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                isAlreadyPassed -> "🎉 قبلاً حاصل شده است!"
                                requiredFinalAverage in 0.0..14.0 -> "🟢 هدف کاملاً در دسترس و آسان"
                                requiredFinalAverage in 14.0..17.5 -> "🟡 نیازمند تسلط و مطالعه منظم"
                                requiredFinalAverage in 17.5..20.0 -> "🟠 چالش‌برانگیز - نیازمند نمرات بسیار بالا"
                                else -> "🔴 غیرممکن ریاضی در این ترم"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isAlreadyPassed || requiredFinalAverage in 0.0..14.0 -> Emerald600
                                requiredFinalAverage in 14.0..17.5 -> Sky600
                                requiredFinalAverage in 17.5..20.0 -> Amber500
                                else -> Rose600
                            }
                        )

                        Text(
                            text = if (isFeasible) {
                                "میانگین پایان‌ترم: ${String.format(Locale.US, "%.1f", requiredFinalAverage.coerceAtLeast(0.0))}"
                            } else {
                                "نیاز به > ۲۰.۰"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = when {
                                isAlreadyPassed || requiredFinalAverage in 0.0..14.0 -> Emerald600
                                requiredFinalAverage in 14.0..17.5 -> Sky600
                                requiredFinalAverage in 17.5..20.0 -> Amber500
                                else -> Rose600
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isFeasible) {
                            "اگر میانگین نمرات پایان‌ترم شما به ${String.format(Locale.US, "%.1f", requiredFinalAverage.coerceAtLeast(0.0))} از ۱۲ نمره باقی‌مانده برسد، معدل کل ترم شما دقیقاً ${String.format(Locale.US, "%.1f", targetGpa)} خواهد شد."
                        } else {
                            "مجموع نمرات میان‌ترم کسب‌شده برای این ترم به گونه‌ای است که حتی با نمره ۲۰ در تمامی امتحانات پایان‌ترم، دستیابی به این معدل امکان‌پذیر نیست."
                        },
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }

            if (isFeasible) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val simulated = grades.map { g ->
                            val simulatedFinal = (requiredFinalAverage).coerceIn(0.0, 14.0)
                            Triple(g, g.midtermGrade, simulatedFinal)
                        }
                        onApplySimulation(simulated)
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "اعمال آزمایشی این سناریو در جدول کارنامه",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

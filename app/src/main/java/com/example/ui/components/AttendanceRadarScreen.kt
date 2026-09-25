package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.ui.theme.Amber500
import com.example.ui.theme.BrandIndigo600
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.NumericBadgeText
import com.example.ui.theme.NumericDisplayStat
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose600
import com.example.ui.theme.StudentOsColors
import com.example.ui.theme.StudentShapeTokens
import com.example.ui.theme.StudentSpacing
import com.example.ui.theme.StudentOsGlassTokens

@Composable
fun AttendanceRadarScreen(
    attendanceList: List<AttendanceEntity>,
    courses: List<CourseEntity>,
    onChangeAttendance: (courseName: String, delta: Int) -> Unit,
    onEvaluateAlerts: () -> Unit,
    onOpenCourseWorkspace: ((CourseEntity) -> Unit)? = null,
    onSetAttendance: ((courseName: String, count: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("all") } // all, warning, safe
    var courseForDirectEdit by remember { mutableStateOf<Pair<String, Int>?>(null) }

    // Generate unified list of courses to track attendance
    val allCourseNames = remember(courses, attendanceList) {
        val names = courses.map { it.name }.distinct().toMutableList()
        attendanceList.forEach { if (!names.contains(it.courseName)) names.add(it.courseName) }
        names
    }

    val dangerCount = attendanceList.count { it.absentCount >= it.maxAllowed && it.maxAllowed > 0 }
    val warningCount = attendanceList.count { it.absentCount == it.maxAllowed - 1 && it.maxAllowed > 0 }
    val totalAbsences = attendanceList.sumOf { it.absentCount }
    val totalAllowedSemester = remember(allCourseNames) {
        allCourseNames.sumOf { name ->
            if (name.contains("آزمایشگاه") || name.contains("کارگاه")) 2 else 3
        }.coerceAtLeast(1)
    }

    val filteredCourseNames = remember(allCourseNames, attendanceList, selectedFilter) {
        when (selectedFilter) {
            "danger" -> allCourseNames.filter { name ->
                val rec = attendanceList.find { it.courseName == name }
                val maxAllowed = if (name.contains("آزمایشگاه") || name.contains("کارگاه")) 2 else 3
                val cur = rec?.absentCount ?: 0
                cur >= maxAllowed
            }
            "warning" -> allCourseNames.filter { name ->
                val rec = attendanceList.find { it.courseName == name }
                val maxAllowed = if (name.contains("آزمایشگاه") || name.contains("کارگاه")) 2 else 3
                val cur = rec?.absentCount ?: 0
                cur == maxAllowed - 1
            }
            "safe" -> allCourseNames.filter { name ->
                val rec = attendanceList.find { it.courseName == name }
                val maxAllowed = if (name.contains("آزمایشگاه") || name.contains("کارگاه")) 2 else 3
                val cur = rec?.absentCount ?: 0
                cur < maxAllowed - 1
            }
            else -> allCourseNames
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "رادار پایش غیبت‌ها (قانون ۳/۱۶ و ماده ۳۵)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "مدیریت استرس غیبت و محافظت از کارت حضور در جلسه امتحانات",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    onEvaluateAlerts()
                },
                shape = StudentShapeTokens.Compact,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = StudentOsColors.CyanAccent,
                    modifier = Modifier.padding(end = 4.dp).size(16.dp)
                )
                Text(
                    text = "بررسی هوشمند",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. CONCENTRIC SEMESTER RADAR GAUGE CARD
        AttendanceSemesterConcentricGauge(
            totalAbsences = totalAbsences,
            totalAllowed = totalAllowedSemester,
            dangerCount = dangerCount,
            warningCount = warningCount
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Danger banner if any
        AnimatedVisibility(visible = dangerCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Rose500.copy(alpha = 0.12f))
                    .border(1.dp, Rose600.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Rose600),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "اخطار ماده ۳۵: در آستانه حذف آموزشی در $dangerCount درس!",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.5.sp,
                            color = Rose500
                        )
                        Text(
                            text = "تعداد غیبت‌های شما در این دروس به سقف قانونی دانشگاه رسیده است.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Filter chips bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "all",
                onClick = {
                    selectedFilter = "all"
                },
                label = { Text("همه دروس (${allCourseNames.size})", fontSize = 11.5.sp) },
                shape = StudentShapeTokens.Compact,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = StudentOsColors.CyanAccent,
                    selectedLabelColor = Color.Black
                )
            )
            if (dangerCount > 0) {
                FilterChip(
                    selected = selectedFilter == "danger",
                    onClick = {
                        selectedFilter = "danger"
                    },
                    label = { Text("حذف / خطر ($dangerCount)", fontSize = 11.5.sp) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Rose600,
                        selectedLabelColor = Color.White
                    )
                )
            }
            if (warningCount > 0) {
                FilterChip(
                    selected = selectedFilter == "warning",
                    onClick = {
                        selectedFilter = "warning"
                    },
                    label = { Text("هشدار ($warningCount)", fontSize = 11.5.sp) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Amber500,
                        selectedLabelColor = Color.White
                    )
                )
            }
            FilterChip(
                selected = selectedFilter == "safe",
                onClick = {
                    selectedFilter = "safe"
                },
                label = { Text("امن", fontSize = 11.5.sp) },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Emerald600,
                    selectedLabelColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Attendance Cards
        if (filteredCourseNames.isEmpty()) {
            ActionableEmptyState(
                icon = Icons.AutoMirrored.Filled.Rule,
                title = if (selectedFilter == "danger") "وضعیت تمامی دروس امن است 🟢" else "هیچ درسی برای این فیلتر ثبت نشده است",
                description = if (selectedFilter == "danger") "خوشبختانه غیبت‌های هیچ درسی به مرز بحرانی نرسیده است." else "می‌توانید فیلتر را به حالت «همه» بازگردانید.",
                primaryActionTitle = if (selectedFilter != "all") "مشاهده همه دروس" else null,
                onPrimaryAction = if (selectedFilter != "all") { { selectedFilter = "all" } } else null
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredCourseNames.forEach { courseName ->
                    val record = attendanceList.find { it.courseName == courseName }
                    val matchedCourse = courses.find { it.name == courseName }
                    val maxAllowed = if (courseName.contains("آزمایشگاه") || courseName.contains("کارگاه")) 2 else 3
                    val curAbsents = record?.absentCount ?: 0

                    AttendanceCard(
                        courseName = courseName,
                        absentCount = curAbsents,
                        maxAllowed = maxAllowed,
                        units = matchedCourse?.units ?: 3,
                        onAdd = {
                            onChangeAttendance(courseName, 1)
                        },
                        onRemove = {
                            onChangeAttendance(courseName, -1)
                        },
                        onDirectEdit = {
                            courseForDirectEdit = courseName to curAbsents
                        },
                        onOpenWorkspace = if (matchedCourse != null && onOpenCourseWorkspace != null) {
                            { onOpenCourseWorkspace(matchedCourse) }
                        } else null
                    )
                }
            }
        }
    }

    // Direct Number Input Dialog for Attendance (Enables recording higher numbers accurately)
    val directEdit = courseForDirectEdit
    if (directEdit != null) {
        val courseName = directEdit.first
        val currentCount = directEdit.second
        var inputNumberText by remember { mutableStateOf(currentCount.toString()) }

        AlertDialog(
            onDismissRequest = { courseForDirectEdit = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = StudentShapeTokens.Card,
            title = {
                Text(
                    text = "ثبت تعداد غیبت درس",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "درس: $courseName\nمی‌توانید هر تعداد جلسه غیبت را تایپ کرده یا از گزینه‌های سریع انتخاب کنید:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = inputNumberText,
                        onValueChange = { str ->
                            if (str.all { it.isDigit() } && str.length <= 3) {
                                inputNumberText = str
                            }
                        },
                        label = { Text("تعداد جلسات غیبت") },
                        placeholder = { Text("مثال: ۴") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Chips for Numbers
                    Text(
                        text = "انتخاب سریع اعداد:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 1, 2, 3, 4, 5, 6, 8, 10).chunked(5).firstOrNull()?.forEach { n ->
                            SuggestionChip(
                                onClick = { inputNumberText = n.toString() },
                                label = { Text("$n", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = inputNumberText.toIntOrNull() ?: currentCount
                        if (onSetAttendance != null) {
                            onSetAttendance(courseName, parsed)
                        } else {
                            val delta = parsed - currentCount
                            onChangeAttendance(courseName, delta)
                        }
                        courseForDirectEdit = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StudentOsColors.CyanAccent)
                ) {
                    Text("ثبت عدد", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { courseForDirectEdit = null }
                ) {
                    Text("انصراف", fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
private fun AttendanceSemesterConcentricGauge(
    totalAbsences: Int,
    totalAllowed: Int,
    dangerCount: Int,
    warningCount: Int,
    modifier: Modifier = Modifier
) {
    val ratio = (totalAbsences.toFloat() / totalAllowed.toFloat()).coerceIn(0f, 1f)
    val percentage = (ratio * 100).toInt()

    val gaugeColor = when {
        dangerCount > 0 || ratio > 0.75f -> Rose500
        warningCount > 0 || ratio > 0.5f -> Amber500
        else -> StudentOsColors.CyanAccent
    }

    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Textual Information & Insights
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(gaugeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = gaugeColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "گیج وضعیت رادار ترم",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when {
                        dangerCount > 0 -> "⚠️ اخطار جدی ماده ۳۵: $dangerCount درس در آستانه حذف آموزشی هستند."
                        warningCount > 0 -> "⚠️ احتیاط: $warningCount درس تنها ۱ جلسه تا سقف غیبت فاصله دارند."
                        else -> "✅ وضعیت امن: حاشیه اطمینان بسیار خوب در تمامی دروس."
                    },
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = gaugeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "$percentage٪ مصرف سقف",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = gaugeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${totalAllowed - totalAbsences} جلسه مجاز باقیمانده",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Concentric Circular Gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(86.dp)
            ) {
                Canvas(modifier = Modifier.size(80.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    // Track background circle
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = strokeWidth)
                    )
                    // Active Arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                gaugeColor.copy(alpha = 0.6f),
                                gaugeColor,
                                gaugeColor
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * ratio,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$totalAbsences",
                        style = NumericDisplayStat,
                        color = gaugeColor,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "از $totalAllowed",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceCard(
    courseName: String,
    absentCount: Int,
    maxAllowed: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    units: Int = 3,
    onDirectEdit: (() -> Unit)? = null,
    onOpenWorkspace: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDanger = absentCount >= maxAllowed
    val isWarning = absentCount == maxAllowed - 1

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val cardBorder = when {
        isDanger -> Rose500.copy(alpha = 0.85f)
        isWarning -> Amber500.copy(alpha = 0.75f)
        else -> if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row: Course Name, units badge, workspace link, count badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = courseName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StudentOsColors.CyanAccent.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "$units واحد",
                                style = NumericBadgeText,
                                color = StudentOsColors.CyanAccent,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (onOpenWorkspace != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BrandIndigo600.copy(alpha = 0.15f),
                                modifier = Modifier.clickable { onOpenWorkspace() }
                            ) {
                                Text(
                                    text = "میزکار ↗",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudentOsColors.CyanAccent,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "سقف مجاز: $maxAllowed جلسه (${if (maxAllowed == 2) "عملی/کارگاهی" else "نظری"})",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = StudentShapeTokens.Compact,
                    color = when {
                        isDanger -> Rose500.copy(alpha = 0.2f)
                        isWarning -> Amber500.copy(alpha = 0.18f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = if (onDirectEdit != null) Modifier.clickable { onDirectEdit() } else Modifier
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$absentCount / $maxAllowed",
                            style = NumericBadgeText,
                            color = when {
                                isDanger -> Rose500
                                isWarning -> Amber500
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (onDirectEdit != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تغییر مستقیم عدد",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SEGMENTED PILLS (3 or 2 capsules that light up with status color)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in 1..maxAllowed) {
                    val isLit = i <= absentCount
                    val pillColor = when {
                        !isLit -> MaterialTheme.colorScheme.surfaceVariant
                        i == maxAllowed -> Rose500
                        i == maxAllowed - 1 -> Amber500
                        else -> Emerald500
                    }
                    val pillBorder = when {
                        !isLit -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        i == maxAllowed -> Rose600
                        i == maxAllowed - 1 -> Amber500
                        else -> Emerald600
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(pillColor)
                            .border(0.8.dp, pillBorder, RoundedCornerShape(6.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status message & High-Contrast 48x48dp Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isDanger -> "⚠️ اخطار ماده ۳۵: در آستانه حذف"
                        isWarning -> "⚠️ وضعیت هشدار (تنها ۱ جلسه باقیمانده)"
                        else -> "✅ وضعیت امن و مجاز"
                    },
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        isDanger -> Rose500
                        isWarning -> Amber500
                        else -> Emerald500
                    }
                )

                // Inline quick buttons (48x48dp minimum touch target compliant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decrease button (-1)
                    Surface(
                        onClick = onRemove,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFCBD5E1))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "کاهش غیبت",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Increase button (+1)
                    Surface(
                        onClick = onAdd,
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDanger) Rose600 else StudentOsColors.CyanAccent,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDanger) Rose500 else StudentOsColors.CyanAccent.copy(alpha = 0.6f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "ثبت غیبت",
                                tint = if (isDanger) Color.White else Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

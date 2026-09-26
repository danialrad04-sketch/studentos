package com.example.ui.components

import com.example.ui.theme.StudentShapeTokens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.datepicker.JalaliDatePickerField
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.TaskEntity
import com.example.ui.models.ExamItem
import com.example.ui.theme.studentColors

private val WEEKDAY_NAMES = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه")

/**
 * Course Workspace Pro (فضای اختصاصی ۳۶۰ درجه درس - نسخه ارتقایافته)
 * Bento-style, Glassmorphism-ready comprehensive hub for each course.
 * Includes attendance radar with risk gauge, smart grade simulator, full task sprint manager,
 * syllabus & formula memos, study session launcher, and direct course drop/edit controls.
 */
@Composable
fun CourseWorkspaceDialog(
    course: CourseEntity,
    attendance: AttendanceEntity?,
    grade: GradeEntity?,
    tasks: List<TaskEntity>,
    exam: ExamItem?,
    sessions: List<CourseSessionEntity> = emptyList(),
    onDismiss: () -> Unit,
    onEditCourse: (CourseEntity) -> Unit,
    onDeleteCourse: ((String) -> Unit)? = null,
    onChangeAttendance: (delta: Int) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onAddTask: (title: String, dueDate: String) -> Unit,
    onDeleteTask: ((TaskEntity) -> Unit)? = null,
    onStartFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSection by remember { mutableStateOf("OVERVIEW") }
    var showNewTaskInput by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDate by remember { mutableStateOf("1405/10/25") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Knowledge & Formula memo notes
    var notesMemo by remember(course.id, course.notes) {
        mutableStateOf(
            course.notes?.ifBlank { null }
                ?: "📌 فرمول‌های کلیدی و سرفصل‌ها:\n- فصل ۱: مفاهیم پایه و تعاریف اصلی\n- فصل ۲ و ۳: حل مسائل تحلیلی و نمونه سوالات امتحانی سال‌های قبل\n- پروژه درسی: موعد تحویل تا جلسه چهاردهم"
        )
    }
    var isEditingNotes by remember { mutableStateOf(false) }

    // Grade Simulator State (Interactive Target Score Simulation)
    var simulatedMidterm by remember(grade) { mutableDoubleStateOf(grade?.midtermGrade ?: 6.0) }
    var simulatedFinal by remember(grade) { mutableDoubleStateOf(grade?.finalGrade ?: 10.0) }
    var showGradeSimulator by remember { mutableStateOf(false) }

    val courseColor = remember(course.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(course.colorHex))
        } catch (_: Exception) {
            Color(0xFF6366F1)
        }
    }

    val isDarkWorkspace = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = modifier
                    .fillMaxWidth(0.95f)
                    .heightIn(max = maxHeight * 0.90f)
                    .clip(RoundedCornerShape(28.dp)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(
                    1.2.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                )
            ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Bar with Color Indicator, Edit, Delete, and Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(courseColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "کارپوشه ۳۶۰° درس (Course Workspace Pro)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onDeleteCourse != null) {
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "حذف درس",
                                    tint = MaterialTheme.studentColors.attendanceCritical,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { onEditCourse(course) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "ویرایش مشخصات",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Course Header Bento Card with dynamic gradient
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = StudentShapeTokens.Card,
                    colors = CardDefaults.cardColors(
                        containerColor = courseColor.copy(alpha = 0.08f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, courseColor.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = course.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "شناسه درس: ${course.id}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = StudentShapeTokens.Compact,
                                color = courseColor.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, courseColor.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${course.units} واحد تخصصی",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = courseColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (sessions.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                sessions.forEach { sess ->
                                    val dayText = WEEKDAY_NAMES.getOrNull(sess.day) ?: "روز نامشخص"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarToday,
                                                    contentDescription = null,
                                                    tint = courseColor,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "$dayText (${sess.start} - ${sess.end})",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        if (sess.location.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.LocationOn,
                                                        contentDescription = null,
                                                        tint = courseColor,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = sess.location,
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = courseColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "مشاهده جلسات در برنامه هفتگی",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Workspace Section Selector Chips (4-in-1 Tabs)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "OVERVIEW" to "مرور و وضعیت",
                        "SIMULATOR" to "شبیه‌ساز نمره",
                        "TASKS" to "تکالیف (${tasks.size})",
                        "KNOWLEDGE" to "فایل و سرفصل",
                        "INSIGHTS" to "تمرکز و تحلیل"
                    ).forEach { (secKey, secLabel) ->
                        val isSelected = activeSection == secKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { activeSection = secKey },
                            label = {
                                Text(
                                    text = secLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = courseColor.copy(alpha = 0.18f),
                                selectedLabelColor = courseColor
                            ),
                            shape = StudentShapeTokens.Compact
                        )
                    }
                }

                // Section 1: OVERVIEW (Attendance Radar, Grades, Exams)
                if (activeSection == "OVERVIEW") {
                    // Attendance Radar Tile with Danger Gauge
                    val absences = attendance?.absentCount ?: 0
                    val maxAllowed = attendance?.maxAllowed ?: 3
                    val isCritical = absences >= maxAllowed && maxAllowed > 0
                    val isWarning = absences == maxAllowed - 1 && maxAllowed > 0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = StudentShapeTokens.Card,
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isCritical -> MaterialTheme.studentColors.attendanceCritical.copy(alpha = 0.08f)
                                isWarning -> MaterialTheme.studentColors.streakFire.copy(alpha = 0.08f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            }
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when {
                                isCritical -> MaterialTheme.studentColors.attendanceCritical.copy(alpha = 0.4f)
                                isWarning -> MaterialTheme.studentColors.streakFire.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "رادار حضور و غیبت ۳/۱۶", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        if (isCritical) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.studentColors.attendanceCritical
                                            ) {
                                                Text(
                                                    text = "خطر حذف آموزشی",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "$absences غیبت از سقف $maxAllowed جلسه مجاز آیین‌نامه",
                                        fontSize = 11.sp,
                                        color = if (isCritical) MaterialTheme.studentColors.attendanceCritical else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { onChangeAttendance(-1) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(34.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                                    ) {
                                        Text(text = "- حضور", fontSize = 10.5.sp)
                                    }
                                    Button(
                                        onClick = { onChangeAttendance(1) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isCritical) MaterialTheme.studentColors.attendanceCritical else MaterialTheme.studentColors.streakFire
                                        ),
                                        modifier = Modifier.height(34.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                                    ) {
                                        Text(text = "+ غیبت", fontSize = 10.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Linear Visual Gauge for Attendance
                            val progress = if (maxAllowed > 0) (absences.toFloat() / maxAllowed).coerceIn(0f, 1f) else 0f
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = when {
                                    isCritical -> MaterialTheme.studentColors.attendanceCritical
                                    isWarning -> MaterialTheme.studentColors.streakFire
                                    else -> MaterialTheme.studentColors.attendanceSafe
                                },
                                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        }
                    }

                    // Grade Standing Tile
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkWorkspace) Color(0x33FFFFFF) else Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "وضعیت نمرات کارنامه", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                val midterm = grade?.midtermGrade ?: 0.0
                                val finalExam = grade?.finalGrade ?: 0.0
                                Text(
                                    text = "میان‌ترم: $midterm از ۸ · پایان‌ترم: $finalExam از ۱۲",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val score = (grade?.midtermGrade ?: 0.0) + (grade?.finalGrade ?: 0.0)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (score >= 17.0) MaterialTheme.studentColors.passedUnitBadge.copy(alpha = 0.15f) else courseColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f", score),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (score >= 17.0) MaterialTheme.studentColors.passedUnitBadge else courseColor,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Exam Countdown Tile
                    if (exam != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.studentColors.streakFire.copy(alpha = 0.08f)),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.studentColors.streakFire.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = MaterialTheme.studentColors.streakFire,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "امتحان پایان‌ترم رسمی",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "تاریخ: ${exam.solarDate} · ساعت: ${exam.time} · سالن: ${exam.location}",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: SMART GRADE SIMULATOR
                if (activeSection == "SIMULATOR") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, courseColor.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = courseColor, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "شبیه‌ساز هوشمند سناریوهای نمره (Grade Simulator)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "با تغییر نمره احتمالی میان‌ترم و پایان‌ترم، برآورد قبولی و تاثیر در معدل را بررسی کنید:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = if (simulatedMidterm == 0.0) "" else simulatedMidterm.toString(),
                                    onValueChange = { simulatedMidterm = it.toDoubleOrNull() ?: 0.0 },
                                    label = { Text("میان‌ترم (از ۶ یا ۸)", fontSize = 10.5.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = if (simulatedFinal == 0.0) "" else simulatedFinal.toString(),
                                    onValueChange = { simulatedFinal = it.toDoubleOrNull() ?: 0.0 },
                                    label = { Text("پایان‌ترم (از ۱۲ یا ۱۴)", fontSize = 10.5.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            val predictedTotal = simulatedMidterm + simulatedFinal
                            val isPassing = predictedTotal >= 10.0
                            val isHonor = predictedTotal >= 17.0

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = StudentShapeTokens.Compact,
                                color = if (isPassing) MaterialTheme.studentColors.passedUnitBadge.copy(alpha = 0.12f) else MaterialTheme.studentColors.attendanceCritical.copy(alpha = 0.12f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isPassing) MaterialTheme.studentColors.passedUnitBadge.copy(alpha = 0.3f) else MaterialTheme.studentColors.attendanceCritical.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (isHonor) "وضعیت: ممتاز (A)" else if (isPassing) "وضعیت: قبولی تضمین‌شده" else "وضعیت: در خطر افتادن (نیاز به مطالعه بیشتر)",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPassing) MaterialTheme.studentColors.passedUnitBadge else MaterialTheme.studentColors.attendanceCritical
                                        )
                                        Text(
                                            text = "حداقل نمره قبولی آیین‌نامه آموزش دانشگاه: ۱۰ از ۲۰",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.1f", predictedTotal),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isPassing) MaterialTheme.studentColors.passedUnitBadge else MaterialTheme.studentColors.attendanceCritical
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: TASKS (Sprint Manager with Add & Delete)
                if (activeSection == "TASKS") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "تکالیف و پروژه‌های این درس", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { showNewTaskInput = !showNewTaskInput },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = courseColor),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "تکلیف جدید", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (showNewTaskInput) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, courseColor.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = newTaskTitle,
                                        onValueChange = { newTaskTitle = it },
                                        placeholder = { Text("عنوان تکلیف (مثلاً تمرین سری ۳ یا فاز اول پروژه)", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        JalaliDatePickerField(
                                            value = newTaskDate,
                                            onValueChange = { newTaskDate = it },
                                            placeholder = "موعد تحویل",
                                            modifier = Modifier.weight(1f)
                                        )

                                        Button(
                                            onClick = {
                                                if (newTaskTitle.isNotBlank()) {
                                                    onAddTask(newTaskTitle, newTaskDate)
                                                    newTaskTitle = ""
                                                    showNewTaskInput = false
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = courseColor),
                                            enabled = newTaskTitle.isNotBlank()
                                        ) {
                                            Text(text = "ثبت تکلیف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        if (tasks.isEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "هیچ تکلیف ثبت‌شده‌ای برای این درس وجود ندارد.",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            tasks.forEach { task ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.8.dp,
                                        if (task.isCompleted) MaterialTheme.studentColors.passedUnitBadge.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onToggleTask(task) },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (task.isCompleted) Icons.Default.Check else Icons.Default.BookmarkBorder,
                                                contentDescription = null,
                                                tint = if (task.isCompleted) MaterialTheme.studentColors.passedUnitBadge else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = task.title,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (task.dueDate.isNotBlank()) {
                                                    Text(text = "مهلت تحویل: ${task.dueDate}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }

                                        if (onDeleteTask != null) {
                                            IconButton(
                                                onClick = { onDeleteTask(task) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "حذف تکلیف",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: KNOWLEDGE & FILES (Notes, Formula Sheet, Class Links)
                if (activeSection == "KNOWLEDGE") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "یادداشت‌ها، سرفصل و فرمول‌نامه درس", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            TextButton(
                                onClick = {
                                    if (isEditingNotes) {
                                        onEditCourse(course.copy(notes = notesMemo))
                                        isEditingNotes = false
                                    } else {
                                        isEditingNotes = true
                                    }
                                }
                            ) {
                                Text(text = if (isEditingNotes) "ذخیره" else "ویرایش متن", fontSize = 11.sp, color = courseColor, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isEditingNotes) {
                            OutlinedTextField(
                                value = notesMemo,
                                onValueChange = { notesMemo = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                shape = RoundedCornerShape(14.dp)
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkWorkspace) Color(0x33FFFFFF) else Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = notesMemo,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(14.dp),
                                    lineHeight = 19.sp
                                )
                            }
                        }

                        // Reference files card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkWorkspace) Color(0x33FFFFFF) else Color(0xFFCBD5E1))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = "🔗 فایل‌ها و مراجع درسی فعال:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = courseColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "اسلایدهای جلسات اول تا دوازدهم (PDF)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = MaterialTheme.studentColors.passedUnitBadge, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "سامانه مدیریت آموزش الکترونیکی و ثبت تمرین", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                // Section 5: INSIGHTS & FOCUS (Pomodoro & Workload Analysis)
                if (activeSection == "INSIGHTS") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(text = "📊 تحلیل حجم و زمان مطالعه بهینه (Workload)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                val estimatedStudy = course.units * 2.0
                                Text(
                                    text = "این درس دارای ${course.units} واحد است. برآورد استاندارد برای تسلط کامل، حدود $estimatedStudy ساعت مطالعه در هفته است.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onStartFocus()
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = courseColor)
                        ) {
                            Icon(imageVector = Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "شروع جلسه تمرکز پومودورو ۲۵ دقیقه‌ای برای ${course.name}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                // Bottom Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDeleteCourse != null) {
                        TextButton(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.studentColors.attendanceCritical)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حذف از برنامه هفتگی", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    TextButton(onClick = onDismiss) {
                        Text(text = "بستن کارپوشه", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    }

    // Deletion confirmation
    if (showDeleteConfirm && onDeleteCourse != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.studentColors.attendanceCritical,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "حذف و اضافه · حذف درس «${course.name}»",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Text(
                    text = "با حذف این درس، تمامی رکوردهای غیبت و تمرین‌های این درس از کارنامه حذف خواهد شد.",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteCourse(course.id)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.studentColors.attendanceCritical),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("بله، حذف کن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("انصراف", fontSize = 11.sp)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

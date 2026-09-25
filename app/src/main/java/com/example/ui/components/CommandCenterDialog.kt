package com.example.ui.components

import com.example.ui.theme.AcademicOlive

import com.example.ui.theme.AcademicNavy

import com.example.ui.theme.StudentSpacing

import com.example.ui.theme.StudentShapeTokens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.TaskEntity
import com.example.ui.models.ExamItem
import com.example.ui.theme.Amber500
import com.example.ui.theme.AcademicNavy
import com.example.ui.theme.AcademicOlive
import com.example.ui.theme.Rose600

/**
 * Phase 15 & 24: Command Center (مرکز فرمان سریع و جستجوی جامع تحصیلی)
 * High-speed spotlight command palette integrating global search across
 * courses, instructors, tasks, exams, notes, and instant 1-tap action shortcuts.
 */
@Composable
fun CommandCenterDialog(
    courses: List<CourseEntity>,
    tasks: List<TaskEntity>,
    exams: List<ExamItem>,
    notes: String,
    onDismiss: () -> Unit,
    onSelectCourse: (CourseEntity) -> Unit,
    onQuickAddTask: () -> Unit,
    onQuickAddCourse: () -> Unit,
    onQuickAttendance: () -> Unit,
    onQuickPomodoro: () -> Unit,
    onOpenCopilot: () -> Unit,
    onOpenOcrImport: () -> Unit,
    onOpenTextImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredCourses = remember(searchQuery, courses) {
        if (searchQuery.isBlank()) emptyList()
        else courses.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.courseCode.contains(searchQuery, ignoreCase = true) ||
            it.professor.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredTasks = remember(searchQuery, tasks) {
        if (searchQuery.isBlank()) emptyList()
        else tasks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.courseName.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredExams = remember(searchQuery, exams) {
        if (searchQuery.isBlank()) emptyList()
        else exams.filter {
            it.courseName.contains(searchQuery, ignoreCase = true) ||
            it.location.contains(searchQuery, ignoreCase = true)
        }
    }

    val hasNotesMatch = remember(searchQuery, notes) {
        searchQuery.isNotBlank() && notes.contains(searchQuery, ignoreCase = true)
    }

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
                    .clip(RoundedCornerShape(26.dp)),
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
                // Header with Terminal/Flash Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مرکز فرمان سریع (Command Center)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "جستجوی جامع + کلیدهای میانبر عملیاتی OS",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }

                // Spotlight Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجو در نام درس، استاد، تکلیف، امتحان، جزوه یا فرمول...", fontSize = 11.5.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = StudentShapeTokens.Card,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    )
                )

                // If searching, show match results
                if (searchQuery.isNotBlank()) {
                    val totalMatches = filteredCourses.size + filteredTasks.size + filteredExams.size + (if (hasNotesMatch) 1 else 0)

                    Text(
                        text = "نتایج جستجو ($totalMatches مورد):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (totalMatches == 0) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = StudentShapeTokens.Compact,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "موردی با عنوان «$searchQuery» یافت نشد.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Matching Courses
                            filteredCourses.forEach { course ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .tactileClickable {
                                            onSelectCourse(course)
                                            onDismiss()
                                        },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "درس: ${course.name}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            val detail = if (course.professor.isNotBlank()) "${course.units} واحد · ${course.professor}" else "${course.units} واحد"
                                            Text(text = detail, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(text = "ورود به Workspace ↗", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            // Matching Tasks
                            filteredTasks.forEach { task ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp)),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.BookmarkBorder, contentDescription = null, tint = Emerald600, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(text = "تکلیف: ${task.title}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            Text(text = "درس مربوطه: ${task.courseName} · مهلت: ${task.dueDate}", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            // Matching Exams
                            filteredExams.forEach { exam ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp)),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Alarm, contentDescription = null, tint = Amber500, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(text = "امتحان: ${exam.courseName}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            Text(text = "${exam.solarDate} ساعت ${exam.time} · مکان: ${exam.location}", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            // Matching Notes
                            if (hasNotesMatch) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp)),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(text = "یافت‌شده در یادداشت‌ها و فرمول‌ها", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                            Text(text = notes.take(70) + "...", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Quick Action Launchers (Spotlight Actions)
                Text(
                    text = "عملیات سریع سیستم‌عامل (Quick OS Actions):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CommandActionItem(
                        icon = Icons.Default.Add,
                        title = "ثبت تکلیف یا پروژه جدید",
                        subtitle = "تعیین عنوان، درس و مهلت تحویل در تسک‌لیست",
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            onDismiss()
                            onQuickAddTask()
                        }
                    )

                    CommandActionItem(
                        icon = Icons.Default.AutoAwesome,
                        title = "گفتگو با Academic Copilot",
                        subtitle = "تحلیل هوشمند فارغ‌التحصیلی، معدل الف و برنامه هفتگی",
                        iconColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            onDismiss()
                            onOpenCopilot()
                        }
                    )

                    CommandActionItem(
                        icon = Icons.Default.HourglassTop,
                        title = "شروع جلسه تمرکز عمیق (پومودورو)",
                        subtitle = "فعال‌سازی تایمر ۲۵ دقیقه‌ای دیپ‌ورک",
                        iconColor = CyanNeon,
                        onClick = {
                            onDismiss()
                            onQuickPomodoro()
                        }
                    )

                    CommandActionItem(
                        icon = Icons.Default.Image,
                        title = "استخراج انتخاب واحد از تصویر و اسکرین‌شات (OCR)",
                        subtitle = "پردازش خودکار کارنامه تصویری و ذخیره در برنامه هفتگی",
                        iconColor = Emerald600,
                        onClick = {
                            onDismiss()
                            onOpenOcrImport()
                        }
                    )

                    CommandActionItem(
                        icon = Icons.Default.ContentPaste,
                        title = "چسباندن متن انتخاب واحد (گلستان/آموزش)",
                        subtitle = "پارس خودکار متن کپی‌شده به تفکیک درس، واحد و ساعت",
                        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            onDismiss()
                            onOpenTextImport()
                        }
                    )

                    CommandActionItem(
                        icon = Icons.Default.WarningAmber,
                        title = "بررسی و ثبت وضعیت غیبت‌ها",
                        subtitle = "ثبت و اصلاح غیبت‌ها در رادار حضور و غیبت ۳/۱۶",
                        iconColor = Rose600,
                        onClick = {
                            onDismiss()
                            onQuickAttendance()
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "بستن مرکز فرمان", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
}

@Composable
private fun CommandActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .tactileClickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = "اجرا ↵", fontSize = 10.sp, color = iconColor, fontWeight = FontWeight.Bold)
        }
    }
}

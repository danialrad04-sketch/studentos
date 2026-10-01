package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.TaskEntity
import com.example.ui.models.ExamItem
import com.example.ui.theme.AcademicOlive
import com.example.ui.theme.studentColors

private val WORKSPACE_DAYS = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه")

@Composable
fun CourseWorkspaceDialogV2(
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
    var showAddTask by remember { mutableStateOf(false) }
    var taskTitle by remember { mutableStateOf("") }
    var taskDate by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val accent = remember(course.colorHex) {
        runCatching {
            Color(android.graphics.Color.parseColor(course.colorHex))
        }.getOrDefault(AcademicOlive)
    }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val openTasks = tasks.count { !it.isCompleted }
    val completedTasks = tasks.count { it.isCompleted }
    val nextTask = tasks.asSequence()
        .filter { !it.isCompleted && it.dueDate.isNotBlank() }
        .minByOrNull { it.dueDate }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(10.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(13.dp),
                        color = accent.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MenuBook, "درس", tint = accent, modifier = Modifier.size(22.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("فضای درس", style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
                        Text(
                            course.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onEditCourse(course) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.Edit, "ویرایش درس")
                    }
                    if (onDeleteCourse != null) {
                        IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                "حذف درس",
                                tint = MaterialTheme.studentColors.attendanceCritical
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.ArrowBack, "بستن")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = accent.copy(alpha = if (isDark) 0.11f else 0.065f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.24f))
                        ) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    WorkspaceMetric(course.units.toString(), "واحد", Modifier.weight(1f))
                                    WorkspaceMetric(openTasks.toString(), "کار باز", Modifier.weight(1f))
                                    WorkspaceMetric(sessions.size.toString(), "جلسه", Modifier.weight(1f))
                                }
                                Text(
                                    text = when {
                                        nextTask != null -> "گام بعدی: " + nextTask.title
                                        exam != null -> "امتحان بعدی: " + exam.solarDate
                                        sessions.isNotEmpty() -> "جلسه بعدی: " + (WORKSPACE_DAYS.getOrNull(sessions.first().day) ?: "روز") + " · " + sessions.first().start
                                        else -> "برای این درس هنوز فعالیتی ثبت نشده است."
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = onStartFocus,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, null, Modifier.size(17.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("شروع تمرکز")
                                    }
                                    OutlinedButton(
                                        onClick = { showAddTask = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.AddTask, null, Modifier.size(17.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("کار جدید")
                                    }
                                }
                            }
                        }
                    }

                    item { WorkspaceSectionTitleV2("برنامه کلاس", Icons.Default.Schedule) }
                    if (sessions.isEmpty()) {
                        item { WorkspaceHintV2("جلسه‌ای برای این درس ثبت نشده است.") }
                    } else {
                        items(sessions) { session ->
                            WorkspaceSessionRow(session, accent)
                        }
                    }

                    item { WorkspaceSectionTitleV2("حضور و غیاب", Icons.Default.Event) }
                    item {
                        val absent = attendance?.absentCount ?: 0
                        val maxAllowed = attendance?.maxAllowed ?: 0
                        val progress = if (maxAllowed > 0) {
                            (absent.toFloat() / maxAllowed.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        val statusColor = when {
                            maxAllowed <= 0 -> accent
                            absent >= maxAllowed -> MaterialTheme.studentColors.attendanceCritical
                            absent >= maxAllowed * 0.75f -> MaterialTheme.studentColors.attendanceWarning
                            else -> MaterialTheme.studentColors.attendanceSafe
                        }
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(17.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "غیبت: " + absent + if (maxAllowed > 0) " از " + maxAllowed else "",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(
                                            onClick = { onChangeAttendance(-1) },
                                            enabled = absent > 0,
                                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 3.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) { Text("−") }
                                        Button(
                                            onClick = { onChangeAttendance(1) },
                                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 3.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) { Text("+") }
                                    }
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = statusColor,
                                    trackColor = statusColor.copy(alpha = 0.12f)
                                )
                            }
                        }
                    }

                    item { WorkspaceSectionTitleV2("کارهای این درس", Icons.Default.Assignment) }
                    if (tasks.isEmpty()) {
                        item { WorkspaceHintV2("هنوز کاری برای این درس ثبت نشده است.") }
                    } else {
                        items(tasks.take(10), key = { it.id }) { task ->
                            WorkspaceTaskRowV2(task, accent, { onToggleTask(task) }, onDeleteTask?.let { { it(task) } })
                        }
                    }

                    item { WorkspaceSectionTitleV2("امتحان", Icons.Default.CalendarToday) }
                    item {
                        if (exam == null) {
                            WorkspaceHintV2("امتحانی برای این درس ثبت نشده است.")
                        } else {
                            Surface(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(17.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(exam.solarDate, fontWeight = FontWeight.Black)
                                    Text("\\u200E" + exam.time + "\\u200E", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (exam.location.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                exam.location,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { WorkspaceSectionTitleV2("نمرات", Icons.Default.CheckCircle) }
                    item {
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(17.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                WorkspaceScoreV2("میان‌ترم", grade?.midtermGrade?.toString() ?: "—", Modifier.weight(1f))
                                WorkspaceScoreV2("پایان‌ترم", grade?.finalGrade?.toString() ?: "—", Modifier.weight(1f))
                                WorkspaceScoreV2("کار", completedTasks.toString() + "/" + tasks.size, Modifier.weight(1f))
                            }
                        }
                    }

                    item { WorkspaceSectionTitleV2("اطلاعات درس", Icons.Default.Person) }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (course.professor.isNotBlank()) {
                                Text("استاد: " + course.professor, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (!course.notes.isNullOrBlank()) {
                                Text(
                                    course.notes.orEmpty(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }

    if (showAddTask) {
        AlertDialog(
            onDismissRequest = { showAddTask = false },
            title = { Text("کار جدید برای " + course.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("عنوان کار") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = taskDate,
                        onValueChange = { taskDate = it },
                        label = { Text("تاریخ سررسید") },
                        supportingText = { Text("مثلاً ۱۴۰۵/۰۷/۱۵") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            onAddTask(taskTitle.trim(), taskDate.trim())
                            taskTitle = ""
                            taskDate = ""
                            showAddTask = false
                        }
                    }
                ) { Text("ثبت") }
            },
            dismissButton = { TextButton(onClick = { showAddTask = false }) { Text("انصراف") } }
        )
    }

    if (showDeleteConfirm && onDeleteCourse != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(Icons.Default.WarningAmber, null, tint = MaterialTheme.studentColors.attendanceCritical)
            },
            title = { Text("حذف این درس؟") },
            text = { Text("حذف درس را فقط وقتی انجام دهید که مطمئن هستید داده‌های مرتبط دیگر لازم نیستند.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteCourse(course.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.studentColors.attendanceCritical)
                ) { Text("حذف درس") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun WorkspaceMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WorkspaceScoreV2(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun WorkspaceSectionTitleV2(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun WorkspaceHintV2(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun WorkspaceSessionRow(session: CourseSessionEntity, accent: Color) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(10.dp), color = accent.copy(alpha = 0.10f)) {
                Text(
                    WORKSPACE_DAYS.getOrNull(session.day) ?: "روز",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
            Spacer(Modifier.width(10.dp))
            Text("\\u200E" + session.start + " — " + session.end + "\\u200E", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            if (session.location.isNotBlank()) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(3.dp))
                Text(session.location, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun WorkspaceTaskRowV2(
    task: TaskEntity,
    accent: Color,
    onToggle: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = if (task.isCompleted) "انجام شده" else "انجام نشده",
                    tint = if (task.isCompleted) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    fontWeight = if (task.isCompleted) FontWeight.Medium else FontWeight.Bold,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.dueDate.isNotBlank()) {
                    Text("موعد: " + task.dueDate, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        "حذف کار",
                        tint = MaterialTheme.studentColors.attendanceCritical
                    )
                }
            }
        }
    }
}

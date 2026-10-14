package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilledTonalButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StudentDialog as Dialog
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
    onRecordGrade: (() -> Unit)? = null,
    onEditExam: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showAddTask by rememberSaveable(course.id) { mutableStateOf(false) }
    var taskTitle by remember { mutableStateOf("") }
    var taskDate by remember { mutableStateOf("") }
    var showDeleteConfirm by rememberSaveable(course.id) { mutableStateOf(false) }

    var showActions by remember { mutableStateOf(false) }
    var showAllTasks by rememberSaveable(course.id) { mutableStateOf(false) }

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
        val dismissWindow = LocalStudentModalDismiss.current ?: onDismiss

        Surface(
            modifier = modifier
                .fillMaxSize().testTag("course_workspace")
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
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = dismissWindow) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "بستن فضای درس")
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text("فضای درس", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Box {
                        IconButton(onClick = { showActions = true }) {
                            Icon(Icons.Default.MoreVert, "گزینه‌های درس")
                        }
                        DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                            DropdownMenuItem(text = { Text("ویرایش درس") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { showActions = false; onEditCourse(course) })
                            if (onDeleteCourse != null) DropdownMenuItem(text = { Text("حذف درس") }, leadingIcon = { Icon(Icons.Default.DeleteOutline, null) }, onClick = { showActions = false; showDeleteConfirm = true })
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("course_workspace_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                            Column {
                                Box(Modifier.fillMaxWidth().height(3.dp).background(accent))
                                StudentReadableText(course.name, detailTitle = "عنوان کامل درس",
                                    style = MaterialTheme.typography.headlineSmall, maxLines = 2,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp))
                            }
                        }
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surface, accent, if (isDark) 0.16f else 0.08f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.24f))
                        ) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StudentAdaptiveRow(minimumRowWidth = 216.dp) { cell ->
                                    WorkspaceMetric(course.units.toString(), "واحد", cell)
                                    WorkspaceMetric(openTasks.toString(), "کار باز", cell)
                                    WorkspaceMetric(sessions.size.toString(), "جلسه", cell)
                                }
                                StudentCardBody(
                                    text = when {
                                        nextTask != null -> "گام بعدی: " + nextTask.title
                                        exam != null -> "امتحان بعدی: " + exam.solarDate
                                        sessions.isNotEmpty() -> "جلسه بعدی: " + (WORKSPACE_DAYS.getOrNull(sessions.first().day) ?: "روز") + " · " + sessions.first().start
                                        else -> "برای این درس هنوز فعالیتی ثبت نشده است."
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                StudentAdaptiveRow { cell ->
                                    Button(
                                        onClick = onStartFocus,
                                        modifier = cell.heightIn(min = 48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, null, Modifier.size(17.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("شروع تمرکز")
                                    }
                                    OutlinedButton(
                                        onClick = { showAddTask = true },
                                        modifier = cell.heightIn(min = 48.dp),
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
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StudentAdaptiveRow(minimumRowWidth = 280.dp) { cell ->
                                    Text(
                                        "غیبت: " + absent + if (maxAllowed > 0) " از " + maxAllowed else "",
                                        fontWeight = FontWeight.Bold, modifier = cell
                                    )
                                    Row(modifier = cell, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        items(if (showAllTasks) tasks else tasks.take(10), key = { it.id }) { task ->
                            WorkspaceTaskRowV2(task, accent, { onToggleTask(task) }, onDeleteTask?.let { { it(task) } })
                        }
                        if (tasks.size > 10) item {
                            TextButton(onClick = { showAllTasks = !showAllTasks }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (showAllTasks) "نمایش خلاصهٔ کارها" else "نمایش همهٔ ${tasks.size} کار")
                            }
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
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StudentCardMeta(exam.solarDate, fontWeight = FontWeight.Black)
                                    StudentCardMeta("\u200E" + exam.time + "\u200E", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    if (exam.location.isNotBlank()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(Modifier.width(5.dp))
                                            StudentReadableText(
                                                exam.location,
                                                detailTitle = "محل امتحان",
                                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                                maxLines = 2,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            onEditExam?.let { action -> OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("ثبت یا ویرایش برنامه امتحان") } }
                            WorkspaceSectionTitleV2("نمرات", Icons.Default.CheckCircle)
                            onRecordGrade?.let { action -> FilledTonalButton(onClick = action, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (grade == null) "ثبت اولین نمره" else "ویرایش نمره") } }
                        }
                    }
                    item {
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(17.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            StudentAdaptiveRow(Modifier.padding(14.dp), minimumRowWidth = 216.dp) { cell ->
                                WorkspaceScoreV2("میان‌ترم", grade?.midtermGrade?.toString() ?: "—", cell)
                                WorkspaceScoreV2("پایان‌ترم", grade?.finalGrade?.toString() ?: "—", cell)
                                WorkspaceScoreV2("کار", completedTasks.toString() + "/" + tasks.size, cell)
                            }
                        }
                    }

                    item { WorkspaceSectionTitleV2("اطلاعات درس", Icons.Default.Person) }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (course.professor.isNotBlank()) {
                                StudentReadableText("استاد: " + course.professor, detailTitle = "استاد درس", maxLines = 2)
                            }
                            if (!course.notes.isNullOrBlank()) {
                                StudentReadableText(
                                    text = course.notes.orEmpty(), detailTitle = "یادداشت درس",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3,
                                    modifier = Modifier.fillMaxWidth()
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
        AddTaskDialog(listOf(course.name), { showAddTask = false }, { title, _, date -> onAddTask(title, date); showAddTask = false })
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
    BoxWithConstraints(modifier) {
        // Stacked metrics at large font scale become compact value/label rows.
        if (maxWidth >= 140.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                StudentCardMeta(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WorkspaceScoreV2(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        StudentCardMeta(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WorkspaceSectionTitleV2(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        StudentCardTitle(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun WorkspaceHintV2(text: String) {
    StudentCardBody(text, maxLines = 2, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun WorkspaceSessionRow(session: CourseSessionEntity, accent: Color) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StudentAdaptiveRow(minimumRowWidth = 240.dp) { cell ->
                Text(WORKSPACE_DAYS.getOrNull(session.day) ?: "روز", modifier = cell, style = MaterialTheme.typography.labelLarge, color = accent)
                Text("\u200E${session.start} — ${session.end}\u200E", modifier = cell, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            if (session.location.isNotBlank()) StudentReadableText(session.location, detailTitle = "محل کلاس", style = MaterialTheme.typography.bodySmall, maxLines = 2)
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
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                StudentReadableText(task.title, detailTitle = "عنوان کامل کار", style = MaterialTheme.typography.titleSmall, maxLines = 2)
                if (task.dueDate.isNotBlank()) {
                    StudentCardMeta("موعد: " + task.dueDate, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
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

package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.models.ExamItem
import com.example.data.local.entity.TaskEntity
import com.example.domain.util.AcademicInputValidator

@Composable
fun ExamsScreen(exams: List<ExamItem>, tasks: List<TaskEntity> = emptyList(), onSetReminder: (ExamItem) -> Unit,
    modifier: Modifier = Modifier, onAddExam: (() -> Unit)? = null, onEditExam: ((ExamItem) -> Unit)? = null,
    onScheduleReminder: ((ExamItem, Int) -> Unit)? = null, reminderIds: Set<String> = emptySet(), onCancelReminder: ((String) -> Unit)? = null) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("برنامه امتحانات", style = MaterialTheme.typography.titleLarge)
        Text("${exams.size} آزمون · تاریخ، ساعت و یادآور قابل مدیریت", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        onAddExam?.let { action -> Button(onClick = action, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("add_exam")) { Text("ثبت برنامه امتحان") } }
        if (exams.isEmpty()) ActionableEmptyState(icon = Icons.Default.Alarm, title = "امتحانی ثبت نشده",
            description = "زمان آزمون را برای یکی از درس‌ها ثبت کنید تا برنامه و یادآور در دسترس باشد.",
            primaryActionTitle = if (onAddExam != null) "ثبت اولین امتحان" else null, onPrimaryAction = onAddExam)
        else exams.sortedBy { AcademicInputValidator.examTimestamp(it.solarDate, it.time) ?: Long.MAX_VALUE }.forEachIndexed { index, exam ->
            ExamCard(exam, index + 1, tasks.filter { it.courseId == exam.id.removePrefix("exam_") || (it.courseId.isBlank() && it.courseName == exam.courseName) },
                { onSetReminder(exam) }, onEdit = onEditExam?.let { { it(exam) } },
                onSchedule = onScheduleReminder?.let { { minutes -> it(exam, minutes) } },
                reminderEnabled = exam.id in reminderIds, onCancel = onCancelReminder?.let { { it(exam.id) } })
        }
    }
}

@Composable
fun ExamCard(exam: ExamItem, index: Int, relatedTasks: List<TaskEntity> = emptyList(), onSetReminder: () -> Unit,
    modifier: Modifier = Modifier, onEdit: (() -> Unit)? = null, onSchedule: ((Int) -> Unit)? = null,
    reminderEnabled: Boolean = false, onCancel: (() -> Unit)? = null) {
    var reminderMenu by remember { mutableStateOf(false) }
    Card(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(exam.courseName, style = MaterialTheme.typography.titleMedium)
            Text("${exam.solarDate} · ساعت ${exam.time}", style = MaterialTheme.typography.bodyLarge)
            Text("محل: ${exam.location}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ExamPreparationIndicator(relatedTasks)
            onEdit?.let { action -> OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("ویرایش برنامه امتحان") } }
            Box(Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = { if (reminderEnabled && onCancel != null) onCancel() else if (onSchedule != null) reminderMenu = true else onSetReminder() },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (reminderEnabled) "یادآور فعال · لغو یادآور" else "تنظیم یادآور")
                }
                DropdownMenu(expanded = reminderMenu, onDismissRequest = { reminderMenu = false }) {
                    listOf(15 to "۱۵ دقیقه قبل", 60 to "یک ساعت قبل", 1440 to "یک روز قبل").forEach { (minutes, title) ->
                        DropdownMenuItem(text = { Text(title) }, onClick = { reminderMenu = false; onSchedule?.invoke(minutes) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamPreparationIndicator(
    relatedTasks: List<TaskEntity>
) {
    val total = relatedTasks.size
    val completed = relatedTasks.count { it.isCompleted }
    val progress = if (total == 0) 0f else completed.toFloat() / total.toFloat()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "آمادگی بر اساس کارهای مرتبط",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (total == 0) "هنوز کاری تعریف نشده" else "$completed از $total انجام شده",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            )
        }
    }
}

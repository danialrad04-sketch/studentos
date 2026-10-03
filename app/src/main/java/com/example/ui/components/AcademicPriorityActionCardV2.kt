package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.engine.AcademicPriorityEngine
import com.example.domain.model.AcademicPriorityItem
import com.example.domain.model.AcademicPriorityKind
import com.example.domain.model.ExamItem
import com.example.ui.models.AppTab
import com.example.domain.util.JalaliCalendarUtil

@Composable
fun AcademicPriorityActionCardV2(
    courses: List<CourseEntity>,
    coursesWithSessions: List<CourseWithSessions>,
    attendance: List<AttendanceEntity>,
    tasks: List<TaskEntity>,
    exams: List<ExamItem>,
    onNavigateTab: (AppTab) -> Unit,
    onStartFocus: () -> Unit,
    modifier: Modifier = Modifier,
    priorityOverride: AcademicPriorityItem? = null
) {
    val clock = rememberAcademicClock()
    val priority = priorityOverride?.takeUnless { it.kind == AcademicPriorityKind.NEXT_CLASS } ?: remember(courses, coursesWithSessions, attendance, tasks, exams, clock) {
        AcademicPriorityEngine.topOrNull(
            courses = courses,
            coursesWithSessions = coursesWithSessions,
            attendance = attendance,
            tasks = tasks,
            exams = exams,
            todayWeekdayIndex = clock.get(java.util.Calendar.DAY_OF_WEEK) % 7,
            todayDate = JalaliCalendarUtil.today().format("/"),
        minuteOfDay = clock.get(java.util.Calendar.HOUR_OF_DAY) * 60 + clock.get(java.util.Calendar.MINUTE)
        )
    } ?: return

    val accent = when (priority.kind) {
        AcademicPriorityKind.CRITICAL_ATTENDANCE,
        AcademicPriorityKind.EXAM_TODAY -> MaterialTheme.colorScheme.error
        AcademicPriorityKind.OVERDUE_TASK,
        AcademicPriorityKind.TASK_TODAY -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    val destination = when (priority.kind) {
        AcademicPriorityKind.CRITICAL_ATTENDANCE -> AppTab.ATTENDANCE
        AcademicPriorityKind.EXAM_TODAY -> AppTab.EXAMS
        AcademicPriorityKind.OVERDUE_TASK,
        AcademicPriorityKind.TASK_TODAY,
        AcademicPriorityKind.EXAM_LINKED_TASK,
        AcademicPriorityKind.OPEN_TASK -> AppTab.TASKS
        AcademicPriorityKind.NEXT_CLASS -> AppTab.SCHEDULE
        AcademicPriorityKind.NONE -> null
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = accent.copy(alpha = 0.10f)
            ) {
                Icon(
                    when (priority.kind) {
                        AcademicPriorityKind.CRITICAL_ATTENDANCE -> Icons.Default.WarningAmber
                        AcademicPriorityKind.OVERDUE_TASK -> Icons.Default.CheckCircle
                        AcademicPriorityKind.EXAM_TODAY -> Icons.Default.Event
                        AcademicPriorityKind.TASK_TODAY -> Icons.Default.CalendarToday
                        else -> Icons.Default.Bolt
                    },
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(8.dp).size(18.dp)
                )
            }

            Column(Modifier.weight(1f)) {
                StudentCardMeta(
                    "الان مهم‌تر از همه",
                    color = accent,
                    fontWeight = FontWeight.Black
                )
                StudentCardTitle(
                    priority.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black
                )
                StudentCardBody(
                    priority.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            if (priority.kind == AcademicPriorityKind.OPEN_TASK ||
                priority.kind == AcademicPriorityKind.EXAM_LINKED_TASK
            ) {
                OutlinedButton(
                    onClick = onStartFocus,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("تمرکز")
                }
            } else if (destination != null) {
                Button(
                    onClick = { onNavigateTab(destination) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("باز کردن")
                }
            }
        }
    }
}

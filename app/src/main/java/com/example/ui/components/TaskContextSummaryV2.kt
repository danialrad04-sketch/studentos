package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.TaskEntity
import com.example.ui.models.ExamItem
import com.example.domain.engine.AcademicContextEngine
import com.example.domain.engine.AcademicPlanningCollisionEngine
import com.example.domain.util.JalaliCalendarUtil
import com.example.ui.theme.StudentSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskContextSummaryV2(
    tasks: List<TaskEntity>,
    exams: List<ExamItem>,
    courses: List<CourseEntity>,
    modifier: Modifier = Modifier
) {
    val clock = rememberAcademicClock()
    val snapshot = AcademicContextEngine.buildSnapshot(
        courses = courses,
        coursesWithSessions = emptyList(),
        attendance = emptyList(),
        tasks = tasks,
        exams = exams,
        todayWeekdayIndex = clock.get(java.util.Calendar.DAY_OF_WEEK) % 7,
        todayDate = JalaliCalendarUtil.today().format("/"),
        minuteOfDay = clock.get(java.util.Calendar.HOUR_OF_DAY) * 60 + clock.get(java.util.Calendar.MINUTE)
    )
    val open = snapshot.openTaskCount
    val overdue = snapshot.overdueTaskCount
    val examLinked = snapshot.examsWithOpenTasksCount
    val collisionCount = AcademicPlanningCollisionEngine.detect(
        tasks = tasks,
        exams = exams
    ).size

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Icon(
                        Icons.Default.Assignment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(7.dp).size(17.dp)
                    )
                }
                Spacer(Modifier.size(StudentSpacing.Sm))
                Column(Modifier.weight(1f)) {
                    Text(
                        "وضعیت کارها",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                    StudentCardBody(
                        text = when {
                            overdue > 0 -> overdue.toString() + " کار عقب‌افتاده داری؛ اول آن‌ها را تعیین تکلیف کن."
                            examLinked > 0 -> examLinked.toString() + " کار باز به امتحان‌های پیش‌رو مرتبط است."
                            open > 0 -> open.toString() + " کار باز داری."
                            else -> "کار بازی باقی نمانده؛ وضعیتت مرتب است."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            FlowRow(
                Modifier.fillMaxWidth(),
                maxItemsInEachRow = if (LocalDensity.current.fontScale >= 1.3f) 2 else 4,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskSignal(open.toString(), "باز", Modifier.weight(1f))
                TaskSignal(overdue.toString(), "عقب‌افتاده", Modifier.weight(1f))
                TaskSignal(examLinked.toString(), "مرتبط با امتحان", Modifier.weight(1f))
                TaskSignal(collisionCount.toString(), "تداخل برنامه", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TaskSignal(
    value: String,
    label: String,
    modifier: Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            Modifier.padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(label, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

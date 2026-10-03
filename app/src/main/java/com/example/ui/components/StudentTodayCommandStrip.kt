package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.engine.AcademicContextEngine
import com.example.domain.model.StudySessionRecommendation
import com.example.ui.models.AppTab
import com.example.domain.util.JalaliCalendarUtil

@Composable
fun StudentTodayCommandStrip(
    courses: List<CourseEntity>,
    coursesWithSessions: List<CourseWithSessions>,
    attendance: List<AttendanceEntity>,
    tasks: List<TaskEntity>,
    exams: List<com.example.ui.models.ExamItem> = emptyList(),
    studyRecommendations: List<StudySessionRecommendation>,
    onNavigateTab: (AppTab) -> Unit,
    onStartFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snapshot = AcademicContextEngine.buildSnapshot(
        courses = courses,
        coursesWithSessions = coursesWithSessions,
        attendance = attendance,
        tasks = tasks,
        exams = exams,
        todayWeekdayIndex = JalaliCalendarUtil.getTodayWeekdayIndex(),
        todayDate = JalaliCalendarUtil.today().format("/")
    )
    val openTasks = snapshot.openTaskCount
    val dangerAttendance = snapshot.criticalAttendanceCount
    val recommendation = studyRecommendations.firstOrNull()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        )
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp).size(18.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "امروز",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        when {
                            dangerAttendance > 0 -> dangerAttendance.toString() + " درس به توجه فوری نیاز دارد."
                            openTasks > 0 -> openTasks.toString() + " کار باز داری؛ یکی را جلو بینداز."
                            snapshot.nextCourseName != null -> "کلاس بعدی امروز را از دست نده."
                            else -> "برنامه امروز سبک است؛ برای جلو افتادن وقت خوبی است."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TodaySignal(Icons.Default.CheckCircle, openTasks.toString(), "کار باز", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                TodaySignal(Icons.Default.CalendarToday, snapshot.todaySessionCount.toString(), "کلاس امروز", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                TodaySignal(Icons.Default.Event, dangerAttendance.toString(), "غیبت بحرانی", MaterialTheme.colorScheme.error, Modifier.weight(1f))
            }

            if (snapshot.nextCourseName != null) {
                val courseName = snapshot.nextCourseName.orEmpty()
                Surface(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        Modifier.padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "کلاس امروز",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                courseName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "\u200E" + snapshot.nextCourseStart.orEmpty() + " — " + snapshot.nextCourseEnd.orEmpty() + "\u200E" +
                                    if (!snapshot.nextCourseLocation.isNullOrBlank()) " · " + snapshot.nextCourseLocation else "",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = { onNavigateTab(AppTab.SCHEDULE) },
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("برنامه") }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartFocus,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(11.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        recommendation?.let { "شروع " + it.recommendedDurationMinutes + " دقیقه" } ?: "شروع تمرکز"
                    )
                }
                OutlinedButton(
                    onClick = { onNavigateTab(AppTab.TASKS) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("کارها")
                }
            }
        }
    }
}

@Composable
private fun TodaySignal(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.07f)
    ) {
        Column(
            Modifier.padding(vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(3.dp))
            Text(value, fontWeight = FontWeight.Black)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

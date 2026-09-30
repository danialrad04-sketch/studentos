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
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CourseEntity
import com.example.domain.engine.CourseConflict
import com.example.data.local.relation.CourseWithSessions
import com.example.ui.theme.AcademicOlive
import com.example.ui.theme.AcademicNavy

@Composable
fun ScheduleContextStripV2(
    courses: List<CourseEntity>,
    coursesWithSessions: List<CourseWithSessions>,
    conflicts: List<CourseConflict>,
    selectedDay: Int,
    modifier: Modifier = Modifier
) {
    val sessions = coursesWithSessions.flatMap { relation ->
        relation.sessions.filter { it.day == selectedDay }.map { relation.course to it }
    }.sortedBy { it.second.start }

    val minutes = sessions.sumOf { sessionDurationMinutes(it.second.start, it.second.end) }
    val hours = minutes / 60
    val mins = minutes % 60
    val loadText = if (hours > 0) {
        "\u200E\${hours}س \${mins}د\u200E"
    } else {
        "\u200E\${mins}د\u200E"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AcademicOlive.copy(alpha = 0.12f)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = AcademicOlive,
                        modifier = Modifier.padding(7.dp).size(17.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "نمای امروز",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "\${sessions.size} جلسه · \${loadText} زمان کلاس",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (conflicts.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.WarningAmber,
                                null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "\${conflicts.size} تداخل",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScheduleContextMetric(
                    value = courses.distinctBy { it.id }.size.toString(),
                    label = "درس فعال",
                    accent = AcademicNavy,
                    modifier = Modifier.weight(1f)
                )
                ScheduleContextMetric(
                    value = sessions.size.toString(),
                    label = "جلسه امروز",
                    accent = AcademicOlive,
                    modifier = Modifier.weight(1f)
                )
                ScheduleContextMetric(
                    value = loadText,
                    label = "زمان کلاس",
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ScheduleContextMetric(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        color = accent.copy(alpha = 0.07f)
    ) {
        Column(
            Modifier.padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(
                label,
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun sessionDurationMinutes(start: String, end: String): Int {
    fun parse(value: String): Int? {
        val parts = value.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }

    val s = parse(start) ?: return 0
    val e = parse(end) ?: return 0
    return (e - s).coerceAtLeast(0)
}

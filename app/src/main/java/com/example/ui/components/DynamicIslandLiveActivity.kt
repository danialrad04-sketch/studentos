package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CourseEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.data.local.util.DateTimeNormalizer
import com.example.domain.engine.NextClassEngine
import com.example.domain.util.JalaliCalendarUtil
import com.example.ui.models.AppTab
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale

/** Home status comes only from recorded sessions and the running focus timer. */
@Composable
fun DynamicIslandLiveActivity(
    courses: List<CourseEntity> = emptyList(),
    coursesWithSessions: List<CourseWithSessions> = emptyList(),
    pomodoroSeconds: Int,
    isPomodoroRunning: Boolean,
    onNavigateTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    nowMillisOverride: Long? = null
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var liveNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(nowMillisOverride) {
        if (nowMillisOverride == null) while (true) {
            liveNow = System.currentTimeMillis()
            // Class schedules have minute precision. Refresh on the next minute,
            // including a day rollover, without a perpetual decorative animation.
            delay(60_000L - liveNow % 60_000L)
        }
    }
    val now = Calendar.getInstance().apply { timeInMillis = nowMillisOverride ?: liveNow }
    val minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val next = NextClassEngine.next(coursesWithSessions, now.get(Calendar.DAY_OF_WEEK) % 7, minute)
    val hasCourses = courses.any { !it.isArchived } || coursesWithSessions.any { !it.isArchived }
    val title = when {
        isPomodoroRunning -> "تمرکز فعال پومودورو"
        next?.isOngoing == true -> "در کلاس: ${next.course.name}"
        next != null -> "کلاس بعدی: ${next.course.name}"
        hasCourses -> "زمان کلاس‌ها ثبت نشده"
        else -> "هنوز کلاسی ثبت نشده"
    }
    val badge = when {
        isPomodoroRunning -> String.format(Locale.US, "%02d:%02d مانده", pomodoroSeconds.coerceAtLeast(0) / 60, pomodoroSeconds.coerceAtLeast(0) % 60)
        next?.isOngoing == true -> "${DateTimeNormalizer.timeToMinutes(next.session.end) - minute} دقیقه تا پایان"
        next?.daysAhead == 0 -> "${DateTimeNormalizer.timeToMinutes(next.session.start) - minute} دقیقه تا شروع"
        next?.daysAhead == 1 -> "فردا، ${next.session.start}"
        next != null -> "${next.daysAhead} روز دیگر"
        else -> "ثبت برنامه"
    }
    Card(
        modifier = modifier.fillMaxWidth().testTag("live_activity").animateContentSize()
            .semantics { stateDescription = if (isExpanded) "جزئیات باز است" else "نمای خلاصه" }
            .tactileClickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.large, modifier = Modifier.widthIn(max = 136.dp)) {
                    Text(badge, Modifier.padding(horizontal = 10.dp, vertical = 6.dp).testTag("live_activity_badge"),
                        style = MaterialTheme.typography.labelMedium)
                }
            }
            AnimatedVisibility(isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    if (next != null) {
                        Text("${JalaliCalendarUtil.getWeekdayName(next.session.day)} · ${next.session.start} تا ${next.session.end}",
                            style = MaterialTheme.typography.bodyMedium)
                        Text(next.session.location.ifBlank { "مکان کلاس ثبت نشده" },
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("برای نمایش کلاس بعدی، روز و ساعت کلاس را در برنامه ثبت کن.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { isExpanded = false; onNavigateTab(AppTab.SCHEDULE) }, modifier = Modifier.testTag("live_activity_schedule")) {
                        Text("برنامهٔ کلاس‌ها")
                    }
                }
            }
        }
    }
}

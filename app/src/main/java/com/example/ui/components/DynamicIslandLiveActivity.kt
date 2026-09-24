package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CourseEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.ui.models.AppTab
import java.util.Calendar

private data class LiveActivityCourseInfo(
    val name: String,
    val start: String,
    val end: String,
    val location: String
)

/**
 * 2026 Apple Dynamic Island / Samsung Now Bar - Live Academic Activity Capsule
 * Floats at the top of Student OS providing real-time ambient status,
 * live class alerts, and expandable quick actions.
 */
@Composable
fun DynamicIslandLiveActivity(
    courses: List<CourseEntity> = emptyList(),
    coursesWithSessions: List<CourseWithSessions> = emptyList(),
    pomodoroSeconds: Int,
    isPomodoroRunning: Boolean,
    onNavigateTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pill_pulse"
    )

    // Calculate nearest course session today
    val now = Calendar.getInstance()
    val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
    val dayIndex = when (dayOfWeek) {
        Calendar.SATURDAY -> 0
        Calendar.SUNDAY -> 1
        Calendar.MONDAY -> 2
        Calendar.TUESDAY -> 3
        Calendar.WEDNESDAY -> 4
        else -> 0
    }
    val allSessionItems = remember(coursesWithSessions, courses) {
        if (coursesWithSessions.isNotEmpty()) {
            coursesWithSessions.flatMap { cws ->
                cws.sessions.map { s ->
                    Triple(s.day, s.start, LiveActivityCourseInfo(cws.course.name, s.start, s.end, s.location))
                }
            }
        } else {
            courses.map { c ->
                Triple(0, "08:00", LiveActivityCourseInfo(c.name, "08:00", "10:00", c.examLocation))
            }
        }
    }
    val todayCourses = allSessionItems
        .filter { it.first == dayIndex }
        .sortedWith(compareBy({ com.example.data.local.util.DateTimeNormalizer.timeToMinutes(it.second) }, { it.third.name }))
        .map { it.third }
    val nextCourse = todayCourses.firstOrNull() ?: allSessionItems.firstOrNull()?.third

    val pomodoroMin = pomodoroSeconds / 60
    val pomodoroSec = pomodoroSeconds % 60
    val pomodoroTimeStr = String.format("%02d:%02d", pomodoroMin, pomodoroSec)

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val pillContainerColor = if (isDark) {
        Color(0xFF161C2D).copy(alpha = 0.88f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val pillBorderBrush = if (isDark) {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.04f)))
    } else {
        Brush.linearGradient(listOf(Color.Black.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.03f)))
    }

    Card(
        shape = RoundedCornerShape(if (isExpanded) 24.dp else 30.dp),
        colors = CardDefaults.cardColors(
            containerColor = pillContainerColor
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = pillBorderBrush
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 6.dp else 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            .tactileClickable(pressedScale = 0.98f) {
                isExpanded = !isExpanded
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Compact State (Dynamic Island Pill)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right status with live indicator & title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pulsing Neon Dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
                            .border(1.5.dp, Color(0xFF34D399), CircleShape)
                    )

                    Column {
                        Text(
                            text = if (isPomodoroRunning) "تمرکز فعال پومودورو"
                            else if (nextCourse != null) "${nextCourse.name} • ${nextCourse.location.ifEmpty { "۱۰۷ فنی" }}"
                            else "وضعیت زنده: بدون کلاس فوری",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Left Pill Badge (Remaining Time)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isPomodoroRunning) "$pomodoroTimeStr مانده"
                            else if (isExpanded) "بستن ✕"
                            else "۲۴:۱۸ مانده",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                        )
                    }
                }
            }

            // Expanded State (Rich Briefing Drawer)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (nextCourse != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = nextCourse.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "زمان: ${nextCourse.start} الی ${nextCourse.end} · مکان: ${nextCourse.location.ifEmpty { "کلاس ۱۰۷ دانشکده مهندسی" }}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF2563EB),
                                modifier = Modifier.tactileClickable {
                                    isExpanded = false
                                    onNavigateTab(AppTab.SCHEDULE)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "برنامه کامل",
                                        fontSize = 11.5.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "امروز درس دیگری در برنامه کلاسی ثبت نشده است. می‌توانید به مرور جزوات بپردازید.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

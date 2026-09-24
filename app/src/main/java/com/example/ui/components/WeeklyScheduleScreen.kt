package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.data.local.util.DateTimeNormalizer
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.NumericBadgeText
import com.example.ui.theme.NumericDisplayStat
import com.example.ui.theme.StudentOsColors
import com.example.ui.theme.StudentOsGlassTokens
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.engine.ConflictDetectionEngine
import com.example.domain.engine.CourseConflict
import com.example.ui.theme.studentColors

private val WEEKDAY_NAMES = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه")

data class ScheduleItem(
    val course: CourseEntity,
    val session: CourseSessionEntity,
    val isPrimary: Boolean = true
)

@Composable
fun WeeklyScheduleScreen(
    courses: List<CourseEntity>,
    coursesWithSessions: List<CourseWithSessions> = emptyList(),
    onAddCourse: () -> Unit,
    onEditCourse: (CourseEntity) -> Unit,
    onOpenCourseWorkspace: ((CourseEntity) -> Unit)? = null,
    onDeleteCourse: ((String) -> Unit)? = null,
    onOpenZeroSetup: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedDayTab by remember { mutableIntStateOf(0) }
    var viewMode by remember { mutableIntStateOf(0) } // 0: Day View, 1: Full Week List, 2: Matrix Grid (8:00 - 20:00)

    val totalUnits = remember(courses) { courses.distinctBy { it.id }.sumOf { it.units } }
    val effectiveCoursesWithSessions = remember(courses, coursesWithSessions) {
        val courseMap = courses.associateBy { it.id }
        val sessionMap = coursesWithSessions.associate { it.course.id to it.sessions }

        courses.mapIndexed { idx, course ->
            val existingSessions = sessionMap[course.id] ?: emptyList()
            val sessionsToUse = if (existingSessions.isNotEmpty()) {
                existingSessions
            } else {
                listOf(
                    CourseSessionEntity(
                        id = "sess_${course.id.take(8)}_default",
                        courseId = course.id,
                        day = (idx % 5),
                        start = if (idx % 2 == 0) "08:00" else "10:00",
                        end = if (idx % 2 == 0) "10:00" else "12:00",
                        location = "دانشکده"
                    )
                )
            }
            CourseWithSessions(course, sessionsToUse)
        }
    }
    val conflicts = remember(effectiveCoursesWithSessions) {
        ConflictDetectionEngine.checkAllConflictsWithSessions(effectiveCoursesWithSessions)
    }

    val allScheduleItems = remember(effectiveCoursesWithSessions) {
        effectiveCoursesWithSessions.flatMap { cWithS ->
            cWithS.sessions.mapIndexed { idx, s ->
                ScheduleItem(cWithS.course, s, isPrimary = (idx == 0))
            }
        }
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Column(modifier = modifier.fillMaxWidth()) {
        if (courses.isEmpty()) {
            ActionableEmptyState(
                title = "برنامه هفتگی ترم جاری هنوز خالی است",
                description = "می‌توانید دروس را دستی اضافه کنید، یا با Zero-Setup در چند ثانیه چارت و برنامه را استقرار دهید.",
                icon = Icons.Default.Schedule,
                primaryActionTitle = "افزودن درس دستی (حذف و اضافه)",
                onPrimaryAction = onAddCourse,
                secondaryActionTitle = "⚡ راه‌اندازی سریع با Zero-Setup",
                onSecondaryAction = onOpenZeroSetup
            )
            return@Column
        }

        // Conflict Warning Banner
        if (conflicts.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.studentColors.attendanceCritical.copy(alpha = 0.12f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.studentColors.attendanceCritical.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.studentColors.attendanceCritical,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "هشدار تداخل زمانی (${conflicts.size} تداخل)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.studentColors.attendanceCritical
                        )
                        Text(
                            text = "دو یا چند درس در یک روز و ساعت همزمان تنظیم شده‌اند.",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Modern Pro Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(width = 0.9.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "برنامه هفتگی و انتخاب واحد",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "$totalUnits واحد اخذ شده",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "مرتب‌سازی هوشمند به وقت شروع · مدیریت تداخل و حذف/اضافه",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // View mode toggle chips (Day / List / Matrix Grid)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                IconButton(
                                    onClick = { viewMode = 0 },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (viewMode == 0) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ViewAgenda,
                                        contentDescription = "روزانه",
                                        tint = if (viewMode == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewMode = 1 },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (viewMode == 1) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = "کل هفته",
                                        tint = if (viewMode == 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewMode = 2 },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (viewMode == 2) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "ماتریس ساعتی",
                                        tint = if (viewMode == 2) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onAddCourse,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "درس جدید", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (viewMode) {
            0 -> {
                // Day Selector Bento Capsules
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WEEKDAY_NAMES.forEachIndexed { index, dayName ->
                        val isSelected = selectedDayTab == index
                        val daySessionsCount = allScheduleItems.count { it.session.day == index }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    else if (isDark) Color(0x28FFFFFF) else Color(0xFFE2E8F0),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedDayTab = index }
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = dayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "($daySessionsCount)",
                                    style = NumericBadgeText,
                                    fontSize = 10.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2026 Live Time Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        .border(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تایم‌لاین زنده دانشگاه · روز فعال: ${WEEKDAY_NAMES.getOrNull(selectedDayTab) ?: ""}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = "زنده 🟢",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Day Agenda List
                val dayItems = allScheduleItems
                    .filter { it.session.day == selectedDayTab }
                    .sortedWith(compareBy({ DateTimeNormalizer.timeToMinutes(it.session.start) }, { it.course.name }))

                if (dayItems.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = "☕", fontSize = 28.sp)
                            Text(
                                text = "در این روز کلاسی ثبت نشده است.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            OutlinedButton(
                                onClick = onAddCourse,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("افزودن کلاس برای این روز", fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dayItems.forEach { item ->
                            val isConflicting = conflicts.any { it.course1Id == item.course.id || it.course2Id == item.course.id }
                            CourseCard(
                                course = item.course,
                                session = item.session,
                                isPrimarySession = item.isPrimary,
                                hasConflict = isConflicting,
                                onClick = { onOpenCourseWorkspace?.invoke(item.course) ?: onEditCourse(item.course) },
                                onEdit = { onEditCourse(item.course) },
                                onDelete = onDeleteCourse?.let { { it(item.course.id) } },
                                onOpenWorkspace = onOpenCourseWorkspace?.let { { it(item.course) } }
                            )
                        }
                    }
                }
            }
            1 -> {
                // Full Week Agenda List View
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    WEEKDAY_NAMES.forEachIndexed { dayIdx, dayName ->
                        val dayItems = allScheduleItems
                            .filter { it.session.day == dayIdx }
                            .sortedWith(compareBy({ DateTimeNormalizer.timeToMinutes(it.session.start) }, { it.course.name }))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(14.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = dayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "${dayItems.size} جلسه",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (dayItems.isEmpty()) {
                                    Text(
                                        text = "بدون کلاس در این روز",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        dayItems.forEach { item ->
                                            val isConflicting = conflicts.any { it.course1Id == item.course.id || it.course2Id == item.course.id }
                                            CourseCard(
                                                course = item.course,
                                                session = item.session,
                                                isPrimarySession = item.isPrimary,
                                                hasConflict = isConflicting,
                                                onClick = { onOpenCourseWorkspace?.invoke(item.course) ?: onEditCourse(item.course) },
                                                onEdit = { onEditCourse(item.course) },
                                                onDelete = onDeleteCourse?.let { { it(item.course.id) } },
                                                onOpenWorkspace = onOpenCourseWorkspace?.let { { it(item.course) } }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Hourly Grid Matrix View (8:00 - 20:00)
                WeeklyGridMatrixView(
                    scheduleItems = allScheduleItems,
                    onCourseClick = { course -> onOpenCourseWorkspace?.invoke(course) ?: onEditCourse(course) }
                )
            }
        }
    }
}

@Composable
fun CourseCard(
    course: CourseEntity,
    session: CourseSessionEntity? = null,
    isPrimarySession: Boolean = true,
    hasConflict: Boolean = false,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onOpenWorkspace: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val defaultPrimary = MaterialTheme.colorScheme.primary
    val accentColor = remember(course.colorHex, defaultPrimary) {
        try {
            Color(android.graphics.Color.parseColor(course.colorHex))
        } catch (_: Exception) {
            defaultPrimary
        }
    }

    val displayStart = session?.start ?: "08:00"
    val displayEnd = session?.end ?: "10:00"
    val displayLoc = session?.location ?: ""

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .tactileClickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 2.dp else 3.5.dp, pressedElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored accent line with rounded capsule
                Box(
                    modifier = Modifier
                        .width(4.5.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accentColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = course.name,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!isPrimarySession) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "جلسه ۲",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (hasConflict) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.studentColors.attendanceCritical.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.studentColors.attendanceCritical.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "⚠️ تداخل زمانی",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.studentColors.attendanceCritical,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Notion-Style Property Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(0.7.dp, accentColor.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "${course.units} واحد",
                                style = NumericBadgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Professor or Location badge
                        if (course.professor.isNotBlank()) {
                            Text(
                                text = "👨‍🏫 ${course.professor}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (displayLoc.isNotBlank()) {
                            Text(
                                text = "📍 $displayLoc",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Time slot capsule with Plus Jakarta Sans and LTR protection
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, accentColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.padding(end = 4.dp).size(13.dp)
                        )
                        Text(
                            text = "\u200E$displayStart - $displayEnd\u200E",
                            style = NumericDisplayStat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor
                        )
                    }
                }

                // Quick Action Context Menu (Edit / Delete / Open Workspace)
                if (onEdit != null || onDelete != null || onOpenWorkspace != null) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "عملیات درس",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (onOpenWorkspace != null) {
                                DropdownMenuItem(
                                    text = { Text("کارپوشه تخصصی ۳۶۰°", fontSize = 11.5.sp) },
                                    onClick = {
                                        showMenu = false
                                        onOpenWorkspace()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.OpenInFull, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                            if (onEdit != null) {
                                DropdownMenuItem(
                                    text = { Text("ویرایش درس (ساعت، مکان، رنگ)", fontSize = 11.5.sp) },
                                    onClick = {
                                        showMenu = false
                                        onEdit()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                            if (onDelete != null) {
                                DropdownMenuItem(
                                    text = { Text("حذف و اضافه (حذف درس)", fontSize = 11.5.sp, color = MaterialTheme.studentColors.attendanceCritical) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirm = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.studentColors.attendanceCritical, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.studentColors.attendanceCritical,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "حذف درس از برنامه هفتگی",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            },
            text = {
                Text(
                    text = "آیا می‌خواهید درس «${course.name}» را از برنامه ترم جاری حذف کنید؟",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.studentColors.attendanceCritical),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("حذف درس", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("انصراف", fontSize = 11.sp)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun WeeklyGridMatrixView(
    scheduleItems: List<ScheduleItem>,
    onCourseClick: (CourseEntity) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val timeSlots = listOf(
        "08:00 - 10:00" to (8 * 60 to 10 * 60),
        "10:00 - 12:00" to (10 * 60 to 12 * 60),
        "13:30 - 15:30" to (13 * 60 + 30 to 15 * 60 + 30),
        "15:30 - 17:30" to (15 * 60 + 30 to 17 * 60 + 30),
        "17:30 - 19:30" to (17 * 60 + 30 to 19 * 60 + 30)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 2.dp else 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // Header Row: Days
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Time label column header
                Box(
                    modifier = Modifier
                        .width(82.dp)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ساعت / روز",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                WEEKDAY_NAMES.forEach { dayName ->
                    Surface(
                        modifier = Modifier.width(118.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time Slot Rows
            timeSlots.forEach { (slotLabel, timeRange) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Time Label Cell
                    Surface(
                        modifier = Modifier
                            .width(82.dp)
                            .height(76.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "\u200E$slotLabel\u200E",
                                style = NumericDisplayStat,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // 5 Day Cells for this time slot
                    WEEKDAY_NAMES.forEachIndexed { dayIdx, _ ->
                        val matchingItems = scheduleItems.filter { item ->
                            if (item.session.day != dayIdx) return@filter false
                            val sMin = DateTimeNormalizer.timeToMinutes(item.session.start)
                            val eMin = DateTimeNormalizer.timeToMinutes(item.session.end)
                            // check overlap with slot
                            sMin < timeRange.second && eMin > timeRange.first
                        }

                        val firstItem = matchingItems.firstOrNull()
                        val courseColor = if (firstItem != null) {
                            try {
                                Color(android.graphics.Color.parseColor(firstItem.course.colorHex))
                            } catch (_: Exception) {
                                MaterialTheme.colorScheme.primary
                            }
                        } else {
                            MaterialTheme.colorScheme.primary
                        }

                        Surface(
                            modifier = Modifier
                                .width(118.dp)
                                .height(76.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (matchingItems.isNotEmpty()) {
                                courseColor.copy(alpha = if (isDark) 0.26f else 0.16f)
                            } else {
                                if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                if (matchingItems.isNotEmpty()) 1.4.dp else 1.dp,
                                if (matchingItems.isNotEmpty()) courseColor.copy(alpha = if (isDark) 0.85f else 0.70f)
                                else if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)
                            )
                        ) {
                            if (matchingItems.isEmpty()) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "—", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                        .clickable { onCourseClick(firstItem!!.course) },
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    matchingItems.forEach { item ->
                                        val itemColor = try {
                                            Color(android.graphics.Color.parseColor(item.course.colorHex))
                                        } catch (_: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(itemColor)
                                            )
                                            Text(
                                                text = item.course.name,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        val sessionLoc = item.session.location
                                        if (sessionLoc.isNotBlank()) {
                                            Text(
                                                text = "📍 $sessionLoc",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

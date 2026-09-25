package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CastForEducation
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.ui.models.AppTab
import com.example.domain.model.StudySessionRecommendation
import com.example.ui.theme.NumericBadgeText
import com.example.ui.theme.NumericDisplayStat
import com.example.ui.theme.AcademicNavy
import com.example.ui.theme.AcademicOlive
import com.example.ui.theme.StudentOsColors
import com.example.ui.theme.StudentOsGlassTokens
import com.example.ui.theme.StudentShapeTokens
import com.example.ui.theme.StudentSpacing
import com.example.ui.theme.studentColors
import java.util.Calendar
import java.util.Locale

private data class BentoClassScheduleItem(
    val course: CourseEntity,
    val day: Int,
    val start: String,
    val end: String,
    val location: String
)

/**
 * Modern Bento Dashboard matching exact design layout:
 * 1. 2x2 Bento Stat Tiles (معدل کل, واحد گذرانده, حضور و غیاب, تسک امروز)
 * 2. Hero Gradient Card ("کلاس بعدی")
 * 3. 5 Quick Action Circles (سنتر, تسک, حضور, پومودورو, کوپایلوت)
 * 4. Split 2-Card Row (برنامه امروز & هوش مصنوعی)
 * 5. Course Status List Card ("وضعیت درس‌ها")
 * 6. Bottom Curricular & Gamification Quick Access
 */
@Composable
fun ModernBentoDashboard(
    courses: List<CourseEntity>,
    coursesWithSessions: List<CourseWithSessions> = emptyList(),
    attendanceList: List<AttendanceEntity>,
    tasks: List<TaskEntity>,
    grades: List<GradeEntity>,
    passedUnits: Int = 0,
    gpa: String = "۰.۰۰",
    totalRequiredCredits: Int = 0,
    academicProgressState: com.example.ui.models.AcademicProgressUiState? = null,
    academicRisks: List<com.example.domain.model.AcademicRisk> = emptyList(),
    weeklyWorkload: com.example.domain.model.WeeklyAcademicWorkload? = null,
    studyRecommendations: List<StudySessionRecommendation> = emptyList(),
    pomodoroSeconds: Int,
    isPomodoroRunning: Boolean,
    onTogglePomodoro: () -> Unit,
    onNavigateTab: (AppTab) -> Unit,
    gamificationProfile: com.example.domain.model.StudentGamificationProfile? = null,
    onOpenCourseWorkspace: ((CourseEntity) -> Unit)? = null,
    onOpenCommandCenter: (() -> Unit)? = null,
    onOpenCopilot: (() -> Unit)? = null,
    onQuickAddTask: (() -> Unit)? = null,
    onOpenOcrImport: (() -> Unit)? = null,
    onOpenAppTour: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // App Tour Pro Launch Banner
        if (onOpenAppTour != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .tactileClickable { onOpenAppTour() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "راهنمای تعاملی تمام بخش‌ها (App Tour) 🚀",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "آشنایی با قابلیت‌های سیستم‌عامل و تکمیل اطلاعات ضروری",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 1. TOP 2x2 BENTO STAT TILES (معدل کل, واحد گذرانده, حضور و غیاب, تسک امروز)
        AnalyticsKpiSection(
            gpa = gpa,
            passedUnits = passedUnits,
            totalRequiredCredits = totalRequiredCredits.coerceAtLeast(0),
            attendanceList = attendanceList,
            tasks = tasks,
            onNavigateToGrades = { onNavigateTab(AppTab.GRADES) },
            onNavigateToPassport = { onNavigateTab(AppTab.PASSPORT) },
            onNavigateToAttendance = { onNavigateTab(AppTab.ATTENDANCE) },
            onNavigateToTasks = { onNavigateTab(AppTab.TASKS) }
        )

        // 2. HERO GRADIENT CARD ("کلاس بعدی")
        NextClassLiveBentoTile(
            courses = courses,
            coursesWithSessions = coursesWithSessions,
            onOpenWorkspace = {
                val featured = courses.firstOrNull()
                if (onOpenCourseWorkspace != null && featured != null) {
                    onOpenCourseWorkspace(featured)
                } else {
                    onNavigateTab(AppTab.SCHEDULE)
                }
            },
            onOpenSchedule = { onNavigateTab(AppTab.SCHEDULE) }
        )

        // 3. QUICK OPERATIONS 5 CIRCLES (سنتر, تسک, حضور, پومودورو, کوپایلوت)
        StudentOperationsHubCard(
            onOpenCommandCenter = { onOpenCommandCenter?.invoke() ?: onNavigateTab(AppTab.DASHBOARD) },
            onQuickAddTask = { onQuickAddTask?.invoke() ?: onNavigateTab(AppTab.TASKS) },
            onQuickAttendance = { onNavigateTab(AppTab.ATTENDANCE) },
            onQuickPomodoro = onTogglePomodoro,
            onOpenCopilot = { onOpenCopilot?.invoke() ?: onNavigateTab(AppTab.DASHBOARD) },
            onOpenOcrImport = { onOpenOcrImport?.invoke() ?: onNavigateTab(AppTab.SCHEDULE) }
        )

        // 4. SPLIT 2-CARD ROW: برنامه امروز (Today's Schedule) & هوش مصنوعی (AI Advice)
        TodayScheduleAndAiSection(
            courses = courses,
            coursesWithSessions = coursesWithSessions,
            onOpenSchedule = { onNavigateTab(AppTab.SCHEDULE) },
            onOpenCopilot = { onOpenCopilot?.invoke() ?: onNavigateTab(AppTab.DASHBOARD) }
        )

        // 5. COURSE STATUS LIST CARD ("وضعیت درس‌ها")
        CourseStatusListSection(
            courses = courses,
            coursesWithSessions = coursesWithSessions,
            onOpenCourse = { course ->
                if (onOpenCourseWorkspace != null) {
                    onOpenCourseWorkspace(course)
                } else {
                    onNavigateTab(AppTab.SCHEDULE)
                }
            }
        )

        if (studyRecommendations.isNotEmpty()) {
            StudyRecommendationsSection(
                recommendations = studyRecommendations,
                onOpenFocus = { onNavigateTab(AppTab.POMODORO) }
            )
        }

        // 6. GAMIFICATION / STREAK BANNER
        if (gamificationProfile != null) {
            BentoGamificationBanner(
                profile = gamificationProfile,
                onClick = { onNavigateTab(AppTab.GAMIFICATION) }
            )
        }

        // 7. EXTRA CURRICULUM & EXAMS QUICK ACTION BANNER
        val chartUnitsText = when (academicProgressState) {
            is com.example.ui.models.AcademicProgressUiState.Ready -> "${academicProgressState.progress.totalRequiredCredits} واحد مصوب"
            is com.example.ui.models.AcademicProgressUiState.Partial -> "${academicProgressState.progress.totalRequiredCredits} واحد مصوب"
            else -> "اطلاعات چارت ثبت نشده"
        }

        CurriculumAndExamActionBanner(
            chartUnitsText = chartUnitsText,
            onOpenCurriculum = { onNavigateTab(AppTab.CURRICULUM) },
            onOpenExams = { onNavigateTab(AppTab.EXAMS) }
        )
    }
}

@Composable
private fun StudyRecommendationsSection(
    recommendations: List<StudySessionRecommendation>,
    onOpenFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    AcademicCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(StudentSpacing.Xxl)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("پیشنهادهای مطالعه", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("اولویت‌بندی قطعی بر اساس امتحان‌ها و تکالیف باز", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onOpenFocus) { Text("شروع تمرکز") }
            }
            Spacer(Modifier.height(StudentSpacing.Md))
            recommendations.take(3).forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = StudentSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = StudentShapeTokens.Compact, color = AcademicNavy.copy(alpha = 0.10f)) {
                        Text("${item.recommendedDurationMinutes} دقیقه", style = NumericBadgeText, color = AcademicNavy, modifier = Modifier.padding(horizontal = StudentSpacing.Sm, vertical = StudentSpacing.Xs))
                    }
                    Spacer(Modifier.width(StudentSpacing.Md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.courseName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(item.priorityReason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
// -------------------------------------------------------------
// 1. 2x2 BENTO STAT TILES SECTION
// -------------------------------------------------------------
@Composable
private fun AnalyticsKpiSection(
    gpa: String,
    passedUnits: Int,
    totalRequiredCredits: Int,
    attendanceList: List<AttendanceEntity>,
    tasks: List<TaskEntity>,
    onNavigateToGrades: () -> Unit,
    onNavigateToPassport: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToTasks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayGpa = gpa.takeIf { it.isNotBlank() } ?: "۰.۰۰"
    val displayPassed = passedUnits.toString()
    val pendingTasksCount = tasks.count { !it.isCompleted }
    val displayTasks = pendingTasksCount.toString()

    val dangerCourses = attendanceList.count { it.absentCount >= it.maxAllowed && it.maxAllowed > 0 }
    val displayAttendance = if (attendanceList.isEmpty()) "—" else {
        val safeRatio = ((attendanceList.size - dangerCourses).toFloat() / attendanceList.size.toFloat() * 100).toInt()
        "$safeRatio٪"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: معدل کل & واحد گذرانده
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoKpiTile(
                title = "معدل کل",
                value = displayGpa,
                subtitle = "هدف: ۱۸.۵۰ 🎯",
                emojiType = AppEmojiType.CHART,
                accentColor = AcademicNavy,
                onClick = onNavigateToGrades,
                modifier = Modifier.weight(1f)
            )

            BentoKpiTile(
                title = "واحد گذرانده",
                value = "$displayPassed / $totalRequiredCredits",
                subtitle = "${((passedUnits.toFloat() / totalRequiredCredits.coerceAtLeast(1)) * 100).toInt()}% چارت 📈",
                emojiType = AppEmojiType.CHECK,
                accentColor = AcademicOlive,
                onClick = onNavigateToPassport,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: حضور و غیاب & تسک امروز
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BentoKpiTile(
                title = "حضور و غیاب",
                value = displayAttendance,
                subtitle = if (dangerCourses > 0) "$dangerCourses هشدار غیبت!" else if (attendanceList.isEmpty()) "هنوز داده‌ای ثبت نشده" else "وضعیت پایدار 🛡️",
                emojiType = AppEmojiType.CALENDAR,
                accentColor = if (dangerCourses > 0) StudentOsColors.CrimsonRose else AcademicOlive,
                onClick = onNavigateToAttendance,
                modifier = Modifier.weight(1f)
            )

            BentoKpiTile(
                title = "تسک امروز",
                value = displayTasks,
                subtitle = "تکالیف در جریان ⏳",
                emojiType = AppEmojiType.TARGET,
                accentColor = StudentOsColors.AmberGlow,
                onClick = onNavigateToTasks,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BentoKpiTile(
    title: String,
    value: String,
    subtitle: String = "",
    emojiType: AppEmojiType,
    accentColor: Color = StudentOsColors.CyanAccent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .tactileClickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.studentColors.glassSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.studentColors.glassBorderGradient),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                AppEmoji(
                    type = emojiType,
                    size = 32.dp,
                    shapeRadiusRatio = 0.28f,
                    elevation = 1.dp
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(0.6.dp, accentColor.copy(alpha = 0.35f))
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(accentColor, CircleShape)
                            .padding(2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = NumericDisplayStat,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 2. HERO NEXT CLASS CARD ("کلاس بعدی")
// -------------------------------------------------------------
@Composable
private fun NextClassLiveBentoTile(
    courses: List<CourseEntity>,
    coursesWithSessions: List<CourseWithSessions> = emptyList(),
    onOpenWorkspace: () -> Unit,
    onOpenSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = java.util.Calendar.getInstance()
    val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK)
    val dayIndex = when (dayOfWeek) {
        java.util.Calendar.SATURDAY -> 0
        java.util.Calendar.SUNDAY -> 1
        java.util.Calendar.MONDAY -> 2
        java.util.Calendar.TUESDAY -> 3
        java.util.Calendar.WEDNESDAY -> 4
        else -> 0
    }
    val allSessions = remember(coursesWithSessions, courses) {
        if (coursesWithSessions.isNotEmpty()) {
            coursesWithSessions.flatMap { cws ->
                cws.sessions.map { s ->
                    BentoClassScheduleItem(cws.course, s.day, s.start, s.end, s.location)
                }
            }
        } else {
            courses.map { BentoClassScheduleItem(it, 0, "08:00", "10:00", it.examLocation) }
        }
    }
    val todaySessions = allSessions
        .filter { it.day == dayIndex }
        .sortedWith(compareBy({ com.example.data.local.util.DateTimeNormalizer.timeToMinutes(it.start) }, { it.course.name }))
    val featuredSession = todaySessions.firstOrNull() ?: allSessions.firstOrNull()
    val featuredCourse = featuredSession?.course ?: courses.firstOrNull()
    val featuredStart = featuredSession?.start ?: "۰۸:۰۰"
    val featuredLocation = featuredSession?.location?.ifBlank { null } ?: featuredCourse?.examLocation?.ifBlank { "کلاس مصوب" } ?: "کلاس مصوب"

    val shape = RoundedCornerShape(24.dp)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .tactileClickable { onOpenWorkspace() },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            StudentOsColors.CyanAccent.copy(alpha = 0.12f),
                            Color(0x00000000)
                        ),
                        radius = 450f
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(StudentOsColors.CyberCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (featuredCourse != null) "کلاس زنده · $featuredStart" else "کلاس بعدی · بدون برنامه",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudentOsColors.CyberCyan
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StudentOsColors.CyberCyan.copy(alpha = 0.14f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, StudentOsColors.CyberCyan.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = if (featuredCourse != null) "${featuredCourse.units} واحد" else "برنامه هفتگی",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudentOsColors.CyberCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = featuredCourse?.name ?: "درسی برای امروز در سیستم ثبت نشده",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (featuredCourse != null) "$featuredLocation · ${featuredCourse.professor.ifBlank { "استاد درس" }}" else "جهت ثبت کلاس، از بخش برگه انتخاب واحد یا برنامه هفتگی اقدام کنید",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Cyan Glowing Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (featuredCourse != null) 0.65f else 0.15f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        StudentOsColors.CyberCyan,
                                        StudentOsColors.SkyCyan
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. SPLIT 2-CARD ROW: برنامه امروز & هوش مصنوعی
// -------------------------------------------------------------
@Composable
private fun TodayScheduleAndAiSection(
    courses: List<CourseEntity>,
    coursesWithSessions: List<CourseWithSessions> = emptyList(),
    onOpenSchedule: () -> Unit,
    onOpenCopilot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = java.util.Calendar.getInstance()
    val dayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK)
    val dayIndex = when (dayOfWeek) {
        java.util.Calendar.SATURDAY -> 0
        java.util.Calendar.SUNDAY -> 1
        java.util.Calendar.MONDAY -> 2
        java.util.Calendar.TUESDAY -> 3
        java.util.Calendar.WEDNESDAY -> 4
        else -> 0
    }
    val allSessions = remember(coursesWithSessions, courses) {
        if (coursesWithSessions.isNotEmpty()) {
            coursesWithSessions.flatMap { cws ->
                cws.sessions.map { s ->
                    BentoClassScheduleItem(cws.course, s.day, s.start, s.end, s.location)
                }
            }
        } else {
            courses.map { BentoClassScheduleItem(it, 0, "08:00", "10:00", it.examLocation) }
        }
    }
    val todaySessions = allSessions
        .filter { it.day == dayIndex }
        .sortedWith(compareBy({ com.example.data.local.util.DateTimeNormalizer.timeToMinutes(it.start) }, { it.course.name }))

    val cardShape = RoundedCornerShape(22.dp)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Right Card: برنامه امروز
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(cardShape)
                .tactileClickable { onOpenSchedule() },
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.studentColors.glassSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.studentColors.glassBorderGradient, cardShape)
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "برنامه امروز",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${todaySessions.size} کلاس",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudentOsColors.CyberCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    if (todaySessions.isEmpty()) {
                        Text(
                            text = "امروز کلاسی ندارید 🎉",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        todaySessions.take(3).forEachIndexed { idx, s ->
                            if (idx > 0) Spacer(modifier = Modifier.height(6.dp))
                            ScheduleBulletItem(time = s.start, name = s.course.name)
                        }
                    }
                }
            }
        }

        // Left Card: هوش مصنوعی
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(cardShape)
                .tactileClickable { onOpenCopilot() },
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.studentColors.glassSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.studentColors.glassBorderGradient, cardShape)
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "دستیار هوشمند",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تحلیل بار",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StudentOsColors.AmberGlow
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudentOsColors.AmberGlow.copy(alpha = 0.12f))
                            .border(0.8.dp, StudentOsColors.AmberGlow.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                            .padding(9.dp)
                    ) {
                        val annotatedString = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    color = StudentOsColors.AmberGlow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            ) {
                                append("نکته: ")
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 10.5.sp
                                )
                            ) {
                                append("توزیع ساعات کلاسی شما این هفته متعادل و پایدار است.")
                            }
                        }
                        Text(text = annotatedString, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleBulletItem(time: String, name: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(StudentOsColors.CyberCyan)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$time — $name",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// -------------------------------------------------------------
// 5. COURSE STATUS LIST CARD ("وضعیت درس‌ها")
// -------------------------------------------------------------
@Composable
private fun CourseStatusListSection(
    courses: List<CourseEntity>,
    coursesWithSessions: List<CourseWithSessions> = emptyList(),
    onOpenCourse: (CourseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(24.dp)
    val displayCourses = courses.take(4)
    val sessionMap = remember(coursesWithSessions) {
        coursesWithSessions.associate { it.course.id to it.sessions }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.studentColors.glassSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.studentColors.glassBorderGradient, cardShape)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "وضعیت درس‌های ترم",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${courses.size} درس فعال",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StudentOsColors.CyberCyan
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (displayCourses.isEmpty()) {
                    Text(
                        text = "درسی برای ترم جاری انتخاب نشده است.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    displayCourses.forEachIndexed { index, c ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                thickness = 0.8.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                        }
                        val sessions = sessionMap[c.id] ?: emptyList()
                        val scheduleSummary = if (sessions.isNotEmpty()) {
                            sessions.joinToString("، ") { sess ->
                                val d = when (sess.day) {
                                    0 -> "شنبه"
                                    1 -> "۱شنبه"
                                    2 -> "۲شنبه"
                                    3 -> "۳شنبه"
                                    4 -> "۴شنبه"
                                    else -> "طول هفته"
                                }
                                "$d ${sess.start}"
                            }
                        } else {
                            if (c.professor.isNotBlank()) c.professor else "مشاهده در برنامه"
                        }
                        CourseStatusRowItem(
                            name = c.name,
                            time = scheduleSummary,
                            badgeText = "${c.units} واحد",
                            badgeBg = StudentOsColors.CyberCyan.copy(alpha = 0.15f),
                            badgeColor = StudentOsColors.CyberCyan,
                            onClick = { onOpenCourse(c) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseStatusRowItem(
    name: String,
    time: String,
    badgeText: String,
    badgeBg: Color,
    badgeColor: Color,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tactileClickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = time,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(badgeBg)
                .border(0.8.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}

// -------------------------------------------------------------
// TILE 2: ATTENDANCE THREAT RADAR (1x1 Bento Tile)
// -------------------------------------------------------------
@Composable
private fun AttendanceRadarBentoTile(
    attendanceList: List<AttendanceEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalAbsences = attendanceList.sumOf { it.absentCount }
    val criticalCourses = attendanceList.count { it.absentCount >= it.maxAllowed && it.maxAllowed > 0 }
    val hasDanger = criticalCourses > 0

    Card(
        modifier = modifier
            .height(142.dp)
            .tactileClickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = if (hasDanger) MaterialTheme.studentColors.attendanceCritical.copy(alpha = 0.35f) else MaterialTheme.studentColors.attendanceSafe.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (hasDanger) MaterialTheme.studentColors.attendanceCritical.copy(alpha = 0.15f) else MaterialTheme.studentColors.attendanceSafe.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (hasDanger) "🚨" else "🛡️", fontSize = 16.sp)
                    }

                    Text(
                        text = if (hasDanger) "هشدار سقف!" else "وضعیت ایمن",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasDanger) MaterialTheme.studentColors.attendanceCritical else MaterialTheme.studentColors.attendanceSafe
                    )
                }

                Column {
                    Text(
                        text = "$totalAbsences جلسه",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (hasDanger) "$criticalCourses درس در لبه حذف ۳/۱۶" else "سقف غیبت مجاز رعایت شده",
                        fontSize = 10.5.sp,
                        color = if (hasDanger) MaterialTheme.studentColors.attendanceCritical else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TILE 3: MICRO POMODORO QUICK FOCUS TILE (1x1 Bento Tile)
// -------------------------------------------------------------
@Composable
private fun MicroPomodoroBentoTile(
    secondsRemaining: Int,
    isRunning: Boolean,
    onTogglePlay: () -> Unit,
    onOpenFull: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = secondsRemaining / 60
    val secs = secondsRemaining % 60
    val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, secs)

    val progress = (1500 - secondsRemaining).toFloat() / 1500f

    Card(
        modifier = modifier
            .height(142.dp)
            .tactileClickable { onOpenFull() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.5.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.studentColors.studyFocus.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.studentColors.studyFocus.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⏱️", fontSize = 16.sp)
                    }

                    // Direct interactive mini play/pause
                    Surface(
                        shape = CircleShape,
                        color = if (isRunning) MaterialTheme.studentColors.streakFire else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(32.dp)
                            .tactileClickable { onTogglePlay() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isRunning) "مکث" else "شروع",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = timeFormatted,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.studentColors.studyFocus,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isRunning) "تمرکز فعال روی جزوه..." else "لمس برای شروع پومودورو",
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TILE 4: ACADEMIC PULSE & GPA TARGET
// -------------------------------------------------------------
@Composable
private fun AcademicPulseBentoTile(
    grades: List<GradeEntity>,
    academicProgressState: com.example.ui.models.AcademicProgressUiState? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = when (academicProgressState) {
        is com.example.ui.models.AcademicProgressUiState.Ready -> academicProgressState.progress
        is com.example.ui.models.AcademicProgressUiState.Partial -> academicProgressState.progress
        else -> null
    }

    val displayValue = if (progress != null) {
        "${progress.passedCredits}/${progress.totalRequiredCredits}"
    } else {
        val totalUnits = grades.sumOf { it.units }
        val totalScore = grades.sumOf { (it.midtermGrade + it.finalGrade) * it.units }
        val gpa = if (totalUnits > 0) totalScore / totalUnits.toDouble() else 0.0
        String.format(Locale.US, "%.2f", gpa)
    }

    val subTitle = if (progress != null) {
        "واحد گذرانده از چارت"
    } else {
        "از مجموع ${grades.sumOf { it.units }} واحد ترم"
    }

    val badgeText = if (progress != null) {
        "${"%.0f".format(progress.progressPercentage)}٪ پیشرفت"
    } else {
        "رتبه الف (>۱۷)"
    }

    Card(
        modifier = modifier
            .height(142.dp)
            .tactileClickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.studentColors.passedUnitBadge.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(13.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📜 شناسنامه تحصیلی",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.studentColors.passedUnitBadge
                    )
                    Text(
                        text = badgeText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (progress != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = displayValue,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subTitle,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Mini visual bar preview
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.studentColors.passedUnitBadge.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "جزییات",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.studentColors.passedUnitBadge
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.studentColors.passedUnitBadge,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TILE 5: SPRINT TASKS MICRO-LIST TILE
// -------------------------------------------------------------
@Composable
private fun TasksSprintBentoTile(
    tasks: List<TaskEntity>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingCount = tasks.count { !it.isCompleted }
    val doneCount = tasks.count { it.isCompleted }

    Card(
        modifier = modifier
            .height(142.dp)
            .tactileClickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(13.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋 تکالیف و تسک‌ها",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$pendingCount باقی‌مانده",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column {
                    val nextTask = tasks.firstOrNull { !it.isCompleted }
                    if (nextTask != null) {
                        Text(
                            text = nextTask.title,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "درس: ${nextTask.courseName} · مهلت: ${nextTask.dueDate}",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "همه تکالیف انجام شده‌اند 🎉",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.studentColors.passedUnitBadge
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TILE 6: CURRICULUM & EXAM BANNER
// -------------------------------------------------------------
@Composable
private fun CurriculumAndExamActionBanner(
    chartUnitsText: String = "اطلاعات چارت ثبت نشده",
    onOpenCurriculum: () -> Unit,
    onOpenExams: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(20.dp)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Engineering Chart Card
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(cardShape)
                .tactileClickable { onOpenCurriculum() },
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.studentColors.glassSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.studentColors.glassBorderGradient, cardShape)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppEmoji(
                        type = AppEmojiType.BOOK,
                        size = 30.dp,
                        shapeRadiusRatio = 0.28f
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "چارت تحصیلی",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$chartUnitsText · مصوب",
                            fontSize = 9.5.sp,
                            color = StudentOsColors.CyberCyan
                        )
                    }
                }
            }
        }

        // Final Exams Card
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(cardShape)
                .tactileClickable { onOpenExams() },
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.studentColors.glassSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.studentColors.glassBorderGradient, cardShape)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppEmoji(
                        type = AppEmojiType.BELL,
                        size = 30.dp,
                        shapeRadiusRatio = 0.28f
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "برنامه امتحانات",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "تاریخ، ساعات و صندلی",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern Gamification Bento Tile: Level, Active Streak, and Achievement Badges.
 */
@Composable
fun BentoGamificationBanner(
    profile: com.example.domain.model.StudentGamificationProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unlockedCount = profile.badges.count { it.isUnlocked }
    val xpProgress = (profile.currentXp.toFloat() / profile.nextLevelXp.toFloat()).coerceIn(0f, 1f)
    val cardShape = RoundedCornerShape(22.dp)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .tactileClickable { onClick() },
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.studentColors.glassSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.studentColors.glassBorderGradient, cardShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            StudentOsColors.AmberGlow.copy(alpha = 0.12f),
                            Color(0x00000000)
                        ),
                        radius = 400f
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppEmoji(
                        type = AppEmojiType.FIRE,
                        size = 44.dp,
                        shapeRadiusRatio = 0.30f,
                        elevation = 2.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${profile.studyStreakDays} روز استریک پیوسته",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StudentOsColors.AmberGlow.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(0.6.dp, StudentOsColors.AmberGlow.copy(alpha = 0.40f))
                            ) {
                                Text(
                                    text = "سطح ${profile.level}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudentOsColors.AmberGlow,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${profile.levelTitle} · $unlockedCount مدال فعال از ${profile.badges.size}",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { xpProgress },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = StudentOsColors.AmberGlow,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مدال‌ها",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        AppEmoji(
                            type = AppEmojiType.TROPHY,
                            size = 18.dp,
                            shapeRadiusRatio = 0.25f
                        )
                    }
                }
            }
        }
    }
}

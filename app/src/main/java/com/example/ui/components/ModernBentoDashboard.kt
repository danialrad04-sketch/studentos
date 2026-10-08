package com.example.ui.components

import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.testTag
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import com.example.ui.theme.cardElevation
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

/** Study desk: daily actions and semester progress share one stable navigation model. */
@Composable
fun ModernBentoDashboard(
    courses: List<CourseEntity>,
    coursesWithSessions: List<CourseWithSessions> = emptyList(),
    attendanceList: List<AttendanceEntity>,
    tasks: List<TaskEntity>,
    exams: List<com.example.ui.models.ExamItem> = emptyList(),
    grades: List<GradeEntity>,
    passedUnits: Int = 0,
    gpa: String = "۰.۰۰",
    targetGpa: Double? = null,
    totalRequiredCredits: Int = 0,
    academicProgressState: com.example.ui.models.AcademicProgressUiState? = null,
    academicRisks: List<com.example.domain.model.AcademicRisk> = emptyList(),
    weeklyWorkload: com.example.domain.model.WeeklyAcademicWorkload? = null,
    studyRecommendations: List<StudySessionRecommendation> = emptyList(),
    primaryPriority: com.example.domain.model.AcademicPriorityItem? = null,
    acceptedStudyPlanIds: Set<String> = emptySet(),
    onAcceptStudyPlan: (StudySessionRecommendation) -> Unit = {},
    onStartStudyPlan: ((StudySessionRecommendation) -> Unit)? = null,
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
    var semesterView by rememberSaveable { mutableStateOf(false) }
    val startFocus: () -> Unit = {
        if (!isPomodoroRunning) {
            val recommendation = studyRecommendations.firstOrNull()
            if (recommendation != null && onStartStudyPlan != null) {
                onStartStudyPlan(recommendation)
            } else {
                onTogglePomodoro()
            }
        }
        onNavigateTab(AppTab.POMODORO)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("میز مطالعه", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("هر روز، یک قدم جلوتر", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        StudentAdaptiveRow(minimumRowWidth = 216.dp) { cell ->
            FilterChip(selected = !semesterView, onClick = { semesterView = false }, label = { Text("امروز") }, modifier = cell.testTag("desk_today"), leadingIcon = { Icon(Icons.Default.CalendarToday, null, Modifier.size(18.dp)) })
            FilterChip(selected = semesterView, onClick = { semesterView = true }, label = { Text("این ترم") }, modifier = cell.testTag("desk_semester"), leadingIcon = { Icon(Icons.Default.School, null, Modifier.size(18.dp)) })
        }
        if (!semesterView) {
            // Daily view: next class, actionable context, and tools.
            NextClassLiveBentoTile(
                courses = courses,
                coursesWithSessions = coursesWithSessions,
                onOpenWorkspace = { course ->
                    onOpenCourseWorkspace?.invoke(course) ?: onNavigateTab(AppTab.SCHEDULE)
                },
                onOpenSchedule = { onNavigateTab(AppTab.SCHEDULE) }
            )

            StudentTodayCommandStrip(
                courses = courses,
                coursesWithSessions = coursesWithSessions,
                attendance = attendanceList,
                tasks = tasks,
                exams = exams,
                studyRecommendations = studyRecommendations,
                onNavigateTab = onNavigateTab,
                onStartFocus = startFocus,
                showClassPreview = false
            )

            if (primaryPriority != null || tasks.any { !it.isCompleted } || exams.isNotEmpty()) AcademicPriorityActionCardV2(
                courses = courses,
                coursesWithSessions = coursesWithSessions,
                attendance = attendanceList,
                tasks = tasks,
                exams = exams,
                priorityOverride = primaryPriority,
                onNavigateTab = onNavigateTab,
                onStartFocus = startFocus
            )

            if (studyRecommendations.isNotEmpty()) StudyPlanPreviewCardV2(
                recommendations = studyRecommendations,
                acceptedRecommendationIds = acceptedStudyPlanIds,
                onAcceptRecommendation = onAcceptStudyPlan,
                onStartRecommendation = onStartStudyPlan,
                onStartFocus = startFocus
            )

            AcademicSectionHeader("جعبه‌ابزار", subtitle = "برنامه، کارها و پیگیری درس‌ها")
            listOf(
                Triple("برنامه هفتگی", Icons.Default.CalendarToday, AppTab.SCHEDULE), Triple("امتحانات", Icons.Default.School, AppTab.EXAMS),
                Triple("تکالیف", Icons.Default.CheckCircle, AppTab.TASKS), Triple("حضور و غیاب", Icons.Default.CastForEducation, AppTab.ATTENDANCE)
            ).chunked(if (LocalDensity.current.fontScale >= 1.3f) 1 else 2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (title, icon, tab) ->
                        QuickActionSquareTile(title, icon, { onNavigateTab(tab) }, Modifier.weight(1f))
                    }
                }
            }
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

        } else {
            AcademicSectionHeader("پیشرفت تحصیلی", subtitle = "خلاصهٔ اطلاعات ثبت‌شدهٔ شما")

            // Semester view: recorded progress and course workspaces.
            AnalyticsKpiSection(
                gpa = gpa,
                passedUnits = passedUnits,
                totalRequiredCredits = totalRequiredCredits.coerceAtLeast(0),
                attendanceList = attendanceList,
                tasks = tasks,
                targetGpa = targetGpa,
                onNavigateToGrades = { onNavigateTab(AppTab.GRADES) },
                onNavigateToPassport = { onNavigateTab(AppTab.PASSPORT) },
                onNavigateToAttendance = { onNavigateTab(AppTab.ATTENDANCE) },
                onNavigateToTasks = { onNavigateTab(AppTab.TASKS) }
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
        onOpenAppTour?.let { openTour ->
            TextButton(onClick = openTour, modifier = Modifier.fillMaxWidth()) {
                Text("راهنمای استفاده از Student OS")
            }
        }

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
                    StudentCardTitle("پیشنهادهای مطالعه", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    StudentCardMeta("اولویت‌بندی قطعی بر اساس امتحان‌ها و تکالیف باز", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onOpenFocus) { Text("شروع تمرکز") }
            }
            Spacer(Modifier.height(StudentSpacing.Md))
            recommendations.take(3).forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = StudentSpacing.Xs), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = StudentShapeTokens.Compact, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)) {
                        Text("${item.recommendedDurationMinutes} دقیقه", style = NumericBadgeText, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(horizontal = StudentSpacing.Sm, vertical = StudentSpacing.Xs))
                    }
                    Spacer(Modifier.width(StudentSpacing.Md))
                    Column(modifier = Modifier.weight(1f)) {
                        StudentCardTitle(item.courseName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        StudentCardBody(item.priorityReason, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    targetGpa: Double?,
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
        StudentAdaptiveRow(minimumRowWidth = 280.dp) { cell ->
            BentoKpiTile(
                title = "معدل کل",
                value = displayGpa,
                subtitle = targetGpa?.let { "هدف ثبت‌شده: ${String.format(Locale.US, "%.2f", it)}" } ?: "هدف معدل ثبت نشده",
                emojiType = AppEmojiType.CHART,
                accentColor = MaterialTheme.colorScheme.secondary,
                onClick = onNavigateToGrades,
                modifier = cell
            )

            BentoKpiTile(
                title = "واحد گذرانده",
                value = "$displayPassed از $totalRequiredCredits",
                subtitle = "${((passedUnits.toFloat() / totalRequiredCredits.coerceAtLeast(1)) * 100).toInt()}% از چارت",
                emojiType = AppEmojiType.CHECK,
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToPassport,
                modifier = cell
            )
        }

        // Row 2: حضور و غیاب & تسک امروز
        StudentAdaptiveRow(minimumRowWidth = 280.dp) { cell ->
            BentoKpiTile(
                title = "حضور و غیاب",
                value = displayAttendance,
                subtitle = if (dangerCourses > 0) "$dangerCourses هشدار غیبت!" else if (attendanceList.isEmpty()) "هنوز داده‌ای ثبت نشده" else "وضعیت مناسب",
                emojiType = AppEmojiType.CALENDAR,
                accentColor = if (dangerCourses > 0) StudentOsColors.CrimsonRose else MaterialTheme.colorScheme.primary,
                onClick = onNavigateToAttendance,
                modifier = cell
            )

            BentoKpiTile(
                title = "کارهای باز",
                value = displayTasks,
                subtitle = "برای امروز یک قدم بردار",
                emojiType = AppEmojiType.TARGET,
                accentColor = StudentOsColors.AmberGlow,
                onClick = onNavigateToTasks,
                modifier = cell
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
    accentColor: Color = MaterialTheme.colorScheme.secondary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .tactileClickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.surface, accentColor, 0.06f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.studentColors.glassBorderGradient),
        elevation = MaterialTheme.cardElevation
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
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
    onOpenWorkspace: (CourseEntity) -> Unit,
    onOpenSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clock by androidx.compose.runtime.produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            kotlinx.coroutines.delay(60_000L)
        }
    }
    val now = java.util.Calendar.getInstance().apply { timeInMillis = clock }
    val dayIndex = now.get(java.util.Calendar.DAY_OF_WEEK) % 7
    val featured = com.example.domain.engine.NextClassEngine.next(
        coursesWithSessions, dayIndex,
        now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE)
    )
    val featuredCourse = featured?.course
    val featuredStart = featured?.session?.start.orEmpty()
    val featuredLocation = featured?.session?.location?.ifBlank { "محل کلاس ثبت نشده" }.orEmpty()
    val classLabel = when {
        featured == null -> "کلاس بعدی · بدون برنامه"
        featured.isOngoing -> "در حال برگزاری · $featuredStart"
        featured.daysAhead == 0 -> "امروز · $featuredStart"
        featured.daysAhead == 1 -> "فردا · $featuredStart"
        else -> "${featured.daysAhead} روز دیگر · $featuredStart"
    }

    val heroColor = Color(0xFF193E43)
    val heroInk = Color(0xFFF4F4EA)
    val marker = Color(0xFFDCEAAB)
    Card(
        onClick = { featuredCourse?.let(onOpenWorkspace) ?: onOpenSchedule() },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 24.dp),
        colors = CardDefaults.cardColors(containerColor = heroColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF426267)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = marker, shape = RoundedCornerShape(6.dp)) {
                    Text("کلاس بعدی", color = heroColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
                Text(classLabel, Modifier.weight(1f), color = heroInk, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(featuredCourse?.name ?: "برنامه‌ات را بچین",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = heroInk, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(if (featuredCourse != null) featuredLocation else "با ثبت اولین درس، میز مطالعهٔ تو آماده می‌شود.",
                style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBDCDA), maxLines = 2, overflow = TextOverflow.Ellipsis)
            HorizontalDivider(color = heroInk.copy(alpha = 0.18f))
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    if (featured != null) Text("\u200E${featured.session.start} — ${featured.session.end}\u200E", color = marker, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (featuredCourse == null) "افزودن برنامهٔ هفتگی" else "باز کردن فضای درس", color = heroInk, style = MaterialTheme.typography.labelMedium)
                }
                Surface(color = marker, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.padding(12.dp).size(20.dp), tint = heroColor)
                }
            }
            if (featured?.isOngoing == true) LinearProgressIndicator(progress = { featured.progress }, modifier = Modifier.fillMaxWidth(), color = marker, trackColor = heroInk.copy(alpha = 0.2f))
        }
    }
}

// -------------------------------------------------------------
// 4. SPLIT 2-CARD ROW: برنامه امروز & هوش مصنوعی
// -------------------------------------------------------------
@Composable
private fun TodayScheduleAndAiSection(courses: List<CourseEntity>, coursesWithSessions: List<CourseWithSessions> = emptyList(), onOpenSchedule: () -> Unit, onOpenCopilot: () -> Unit, modifier: Modifier = Modifier) {
    val day = com.example.domain.util.JalaliCalendarUtil.getTodayWeekdayIndex()
    val today = coursesWithSessions.flatMap { item -> item.sessions.filter { it.day == day }.map { item.course.name to it.start } }
        .sortedBy { com.example.data.local.util.DateTimeNormalizer.timeToMinutes(it.second) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AcademicCard(onClick = onOpenSchedule, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("برنامه امروز · ${today.size} کلاس", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (today.isEmpty()) Text(if (courses.isEmpty()) "برای دیدن برنامهٔ امروز، ابتدا کلاس‌های خود را ثبت کنید." else "کلاسی برای امروز ثبت نشده است.", style = MaterialTheme.typography.bodyLarge)
                else today.take(3).forEach { (name, time) -> Text("$time · $name", style = MaterialTheme.typography.bodyLarge) }
                Text("مشاهدهٔ برنامهٔ هفتگی", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
        AcademicCard(onClick = onOpenCopilot, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("همراه هوشمند مطالعه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("اولویت‌های خود را مرور کنید؛ برای برنامهٔ هفت‌روزه و مرور امتحان از ابزارهای Student Pro کمک بگیرید.", style = MaterialTheme.typography.bodyLarge)
                Text("بازکردن دستیار", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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
                .background(MaterialTheme.colorScheme.secondary)
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
// 5. COURSE STATUS LIST ("دوره‌های من / درس‌های امروز")
// -------------------------------------------------------------
@Composable
private fun CourseStatusListSection(
    courses: List<CourseEntity>,
    coursesWithSessions: List<CourseWithSessions> = emptyList(),
    onOpenCourse: (CourseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAll by rememberSaveable { mutableStateOf(false) }
    val sessionMap = remember(coursesWithSessions) { coursesWithSessions.associate { it.course.id to it.sessions } }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AcademicSectionHeader("درس‌های من", subtitle = "${courses.size} درس ثبت‌شده",
            actionLabel = if (courses.size > 4) { if (showAll) "خلاصه" else "نمایش همه" } else null,
            onAction = if (courses.size > 4) { { showAll = !showAll } } else null)
        if (courses.isEmpty()) AcademicEmptyState("هنوز درسی ثبت نشده", "درس‌ها را از برنامهٔ هفتگی اضافه کن.")
        (if (showAll) courses else courses.take(4)).forEach { course ->
            val sessions = sessionMap[course.id].orEmpty()
            val session = sessions.firstOrNull()
            val sessionText = session?.let {
                val day = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه").getOrNull(it.day).orEmpty()
                "$day · \u200E${it.start} — ${it.end}\u200E"
            } ?: "جلسه‌ای ثبت نشده"
            ModernCourseCard(course, sessionText, sessions.size, onClick = { onOpenCourse(course) })
        }
    }
}

@Composable
private fun ModernCourseCard(course: CourseEntity, sessionText: String, sessionCount: Int, onClick: () -> Unit) {
    val fallback = MaterialTheme.colorScheme.primary
    val accent = remember(course.colorHex, fallback) { runCatching { Color(android.graphics.Color.parseColor(course.colorHex)) }.getOrDefault(fallback) }
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(4.dp).height(48.dp).background(accent, RoundedCornerShape(2.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(course.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${course.units} واحد · $sessionCount جلسه در هفته", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(sessionText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickActionSquareTile(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = when (title) {
        "برنامه هفتگی" -> MaterialTheme.colorScheme.secondary
        "امتحانات" -> MaterialTheme.colorScheme.tertiary
        "حضور و غیاب" -> MaterialTheme.studentColors.attendanceSafe
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(onClick = onClick, modifier = modifier.testTag("dashboard_tool_$title"), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.heightIn(min = 68.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = accent.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
                Icon(icon, null, Modifier.padding(9.dp).size(20.dp), tint = accent)
            }
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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
        elevation = MaterialTheme.cardElevation
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
        elevation = MaterialTheme.cardElevation
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
            elevation = MaterialTheme.cardElevation
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
                        StudentCardMeta(
                            text = "$chartUnitsText · مصوب",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
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
            elevation = MaterialTheme.cardElevation
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
                        StudentCardMeta(
                            text = "تاریخ، ساعات و صندلی",
                            style = MaterialTheme.typography.labelSmall
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
        elevation = MaterialTheme.cardElevation
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
                                text = profile.studyStreakDays.toString() + " روز استریک پیوسته",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                            text = profile.levelTitle + " · " + unlockedCount + " مدال فعال از " + profile.badges.size,
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

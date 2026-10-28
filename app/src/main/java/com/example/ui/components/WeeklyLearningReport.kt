package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.TaskEntity
import com.example.data.repository.learningToday
import com.example.domain.engine.*
import com.example.domain.model.StudyDay
import com.example.domain.util.JalaliCalendarUtil
import com.example.ui.models.LearningWorkspaceState
import com.example.ui.theme.studentStickerColors

@Composable
fun WeeklyLearningReport(
    state: LearningWorkspaceState,
    history: List<StudyDay>,
    todayKey: String,
    courseIds: Set<String>,
    tasks: List<TaskEntity>,
    onOpenPlanner: () -> Unit,
) {
    var details by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val stats = StudyStatisticsEngine.calculate(history, todayKey)
    val report =
        WeeklyLearningReportEngine.summarize(
            learningToday(),
            courseIds,
            state.completions,
            state.cards,
            tasks,
        )
    val colors = studentStickerColors(AppEmojiType.CHART)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.container,
        contentColor = colors.ink,
        modifier = Modifier.fillMaxWidth().testTag("weekly_learning_report"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                AppEmoji(AppEmojiType.CHART, size = 36.dp)
                Text("گزارش ۷ روز گذشته", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "${stats.lastSevenDaysMinutes} دقیقه با تایمر · ${stats.currentStreak} روز پیوسته",
                style = MaterialTheme.typography.bodyLarge,
            )
            val change = stats.lastSevenDaysMinutes - stats.previousSevenDaysMinutes
            Text(
                when {
                    stats.lastSevenDaysMinutes == 0 && stats.previousSevenDaysMinutes == 0 ->
                        "هنوز مطالعه‌ای با تایمر ثبت نشده است."
                    change > 0 -> "$change دقیقه بیشتر از هفت روز قبل"
                    change < 0 -> "${-change} دقیقه کمتر از هفت روز قبل"
                    else -> "زمان مطالعه با هفت روز قبل برابر است."
                },
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.loaded && !state.error) {
                Text(
                    "${report.confirmedSessions} جلسهٔ برنامه ثبت شده · ${report.declaredMinutes} دقیقه اعلامی"
                )
                Text(
                    "${report.uniqueReviewedCards} کارت مرورشده · ${report.overdueTasks} تکلیف با موعد گذشته"
                )
                TextButton(onClick = { details = !details }) {
                    Text(if (details) "بستن جزئیات محاسبه" else "جزئیات محاسبه")
                }
                if (details)
                    Text(
                        "زمان اعلامی جلسه‌ها با زمان تایمر جمع نشده است؛ ممکن است مربوط به یک مطالعه باشند. تعداد کارت‌ها، کارت‌های یکتای مرورشده است.",
                        style = MaterialTheme.typography.bodySmall,
                    )
            } else
                Text(
                    "جزئیات برنامه و مرور هنوز خوانده نشده است؛ عددی برای آن‌ها حدس نمی‌زنیم.",
                    style = MaterialTheme.typography.bodySmall,
                )
            OutlinedButton(onClick = onOpenPlanner) { Text("تنظیم برنامهٔ مطالعه") }
        }
    }
}

@Composable
fun LearningHomeSummary(
    state: LearningWorkspaceState,
    plan: AdaptiveLearningPlan,
    courses: List<com.example.data.local.entity.CourseEntity>,
    onOpenPlanner: () -> Unit,
    onOpenCourse: (com.example.data.local.entity.CourseEntity) -> Unit,
) {
    if (!state.loaded || state.error) return
    val today = learningToday()
    val next = plan.blocks.firstOrNull { it.date == today.format() }
    val course = courses.find { it.id == next?.courseId }
    val reviewCourse =
        courses.firstOrNull { c ->
            state.cards.any {
                it.courseId == c.id &&
                    !it.isArchived &&
                    JalaliCalendarUtil.parse(it.dueDate)?.let { d -> d <= today } == true
            }
        }
    val colors = studentStickerColors(AppEmojiType.POMODORO)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.container,
        contentColor = colors.ink,
        modifier = Modifier.fillMaxWidth().testTag("learning_home_summary"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("قدم بعدی مطالعه", style = MaterialTheme.typography.titleSmall)
            Text(
                if (next != null && course != null)
                    "${course.name} · ${next.minutes} دقیقه · ${AdaptiveLearningEngine.clock(next.startMinute)}"
                else "هدفت را با زمان آزاد این هفته هماهنگ کن."
            )
            TextButton(onClick = onOpenPlanner) {
                Text(if (next == null) "برنامهٔ مطالعه‌ام" else "دیدن برنامهٔ امروز")
            }
            if (reviewCourse != null)
                TextButton(onClick = { onOpenCourse(reviewCourse) }) {
                    Text("مرور آماده: ${reviewCourse.name}")
                }
        }
    }
}

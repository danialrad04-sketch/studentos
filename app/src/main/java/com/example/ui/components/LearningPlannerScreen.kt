package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.*
import com.example.data.repository.learningToday
import com.example.domain.engine.*
import com.example.domain.util.JalaliCalendarUtil
import com.example.ui.models.LearningWorkspaceState
import com.example.ui.theme.studentStickerColors

private val learningWeekdays =
    listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه")

internal fun learningNumber(raw: String) =
    com.example.data.local.util.DateTimeNormalizer.normalizeDigits(raw).trim().toIntOrNull()

@Composable
internal fun LearningStatus(state: LearningWorkspaceState, onRetry: () -> Unit): Boolean {
    if (!state.loaded) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text("در حال خواندن برنامهٔ مطالعه…")
        return false
    }
    if (state.error) {
        Text("برنامه خوانده نشد؛ اطلاعات ذخیره‌شده را تغییر ندادیم.")
        Button(onClick = onRetry) { Text("تلاش دوباره") }
        return false
    }
    return true
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LearningPlannerScreen(
    state: LearningWorkspaceState,
    plan: AdaptiveLearningPlan,
    courses: List<CourseEntity>,
    onRetry: () -> Unit,
    onSaveGoal: (StudyGoalEntity, (Boolean) -> Unit) -> Unit,
    onSaveAvailability: (StudyAvailabilityEntity, (Boolean) -> Unit) -> Unit,
    onComplete: (LearningBlock, Int, (Boolean) -> Unit) -> Unit,
    onUndo: (StudyCompletionEntity, (Boolean) -> Unit) -> Unit,
    onFocus: (LearningBlock) -> Unit,
    activeBlock: LearningBlock? = null,
    timerRunning: Boolean = false,
    onReleaseBlock: (LearningBlock) -> Unit = {},
    onReturnToFocus: () -> Unit = {},
) {
    var goalEditor by rememberSaveable { mutableStateOf<StudyGoalEntity?>(null) }
    var addGoal by rememberSaveable { mutableStateOf(false) }
    var showFree by rememberSaveable { mutableStateOf(false) }
    var selectedDay by rememberSaveable { mutableIntStateOf(0) }
    var completing by rememberSaveable { mutableStateOf<LearningBlock?>(null) }
    var undoing by rememberSaveable { mutableStateOf<StudyCompletionEntity?>(null) }
    var showGoals by rememberSaveable { mutableStateOf(false) }
    var history by rememberSaveable { mutableStateOf(false) }
    val today = learningToday()
    val names = courses.associate { it.id to it.name }
    val goals = state.goals.filter { it.courseId in names }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("learning_planner"),
    ) {
        Text("برنامهٔ مطالعه", style = MaterialTheme.typography.titleLarge)
        Text(
            "هدف درس و زمان آزادت را مشخص کن؛ برنامه با ثبت مطالعه تنظیم می‌شود.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (LearningStatus(state, onRetry)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = { showFree = true },
                    modifier = Modifier.testTag("learning_availability"),
                ) {
                    Text("زمان آزاد من")
                }
                Button(
                    onClick = {
                        if (goals.size >= courses.size) showGoals = true else addGoal = true
                    },
                    enabled = courses.isNotEmpty(),
                    modifier = Modifier.testTag("learning_add_goal"),
                ) {
                    Text("هدف درسی")
                }
            }
            activeBlock
                ?.takeIf {
                    it.courseId in names &&
                        state.completions.none { r -> r.id == it.id && !r.isDeleted }
                }
                ?.let { block ->
                    OutlinedCard {
                        Column(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("جلسهٔ متصل به تایمر", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${names[block.courseId]}${block.workTitle.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()} · ${block.date} · ${block.minutes} دقیقهٔ پیشنهادی"
                            )
                            Text(
                                if (timerRunning)
                                    "تایمر در حال اجراست؛ انجام جلسه هنوز ثبت نشده است."
                                else "پس از مطالعه، زمان واقعی را ثبت کن تا از هدف کم شود."
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = onReturnToFocus) { Text("رفتن به تایمر") }
                                OutlinedButton(
                                    enabled = !timerRunning,
                                    onClick = { completing = block },
                                ) {
                                    Text("ثبت این جلسه")
                                }
                                TextButton(onClick = { onReleaseBlock(block) }) {
                                    Text("کنار گذاشتن پیشنهاد")
                                }
                            }
                        }
                    }
                }
            if (courses.isEmpty())
                Text("اول یک درس به ترم اضافه کن؛ برنامهٔ مطالعه به همان درس‌ها متصل می‌شود.")
            if (goals.isEmpty() && courses.isNotEmpty())
                Text("از یک درس شروع کن: زمان موردنیاز و تاریخ هدفت را وارد کن.")
            if (plan.overdueGoals.isNotEmpty())
                TextButton(onClick = { showGoals = true }) {
                    Text("تاریخ ${plan.overdueGoals.size} هدف گذشته است؛ هدف‌ها را اصلاح کن.")
                }
            if (plan.workItems.isNotEmpty()) {
                com.example.ui.v3.V3Card("زمان پروژه و تکلیف", "زمان‌های ثبت‌شده در همین برنامه جا می‌گیرند؛ زمان کارهای بدون برآورد حدس زده نمی‌شود.") {
                    plan.workItems.take(8).forEach { item ->
                        Text(item.title, style = MaterialTheme.typography.titleSmall)
                        Text(when {
                            item.remainingMinutes == null -> "برای برنامه‌ریزی، مهلت و زمان لازم را در کار یا پروژه ثبت کن."
                            item.overdue -> "مهلت گذشته است؛ تاریخ تحویل را بررسی کن."
                            item.shortfallMinutes > 0 -> "${item.remainingMinutes} دقیقه مانده · ${item.shortfallMinutes} دقیقه کمبود ظرفیت"
                            else -> "${item.remainingMinutes} دقیقه مانده · مهلت ${item.dueDate}"
                        }, style=MaterialTheme.typography.bodyMedium)
                    }
                    if(plan.workItems.size>8) Text("${plan.workItems.size-8} کار دیگر؛ جزئیات در بخش کارها و پروژه‌ها")
                }
            }
            val crowded = plan.capacityShortfall.keys - plan.overdueGoals
            if (crowded.isNotEmpty())
                TextButton(onClick = { showGoals = true }) {
                    Text("برای ${crowded.size} هدف زمان کافی پیدا نشد؛ جزئیات را ببین.")
                }
            if (goals.isNotEmpty())
                TextButton(onClick = { showGoals = !showGoals }) {
                    Text(
                        if (showGoals) "بستن هدف‌ها" else "هدف‌ها (${goals.count{it.enabled}} فعال)"
                    )
                }
            if (showGoals)
                goals.forEach { g ->
                    val done =
                        state.completions
                            .filter { !it.isDeleted && it.courseId == g.courseId }
                            .sumOf { it.actualMinutes }
                    val colors = studentStickerColors(AppEmojiType.TARGET)
                    Surface(
                        color = colors.container,
                        contentColor = colors.ink,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                names[g.courseId].orEmpty(),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                "$done از ${g.totalMinutes} دقیقه ثبت‌شده · هدف ${g.targetDate}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            LinearProgressIndicator(
                                progress = { (done.toFloat() / g.totalMinutes).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (g.courseId in plan.overdueGoals)
                                Text("تاریخ این هدف گذشته است؛ تاریخ تازه‌ای انتخاب کن.")
                            plan.capacityShortfall[g.courseId]
                                ?.takeIf { g.courseId !in plan.overdueGoals }
                                ?.let {
                                    Text(
                                        "تا این تاریخ، برای $it دقیقه هنوز جای کافی پیدا نشد. زمان آزاد یا هدف را اصلاح کن."
                                    )
                                }
                            Row {
                                TextButton(onClick = { goalEditor = g }) { Text("ویرایش هدف") }
                                TextButton(
                                    onClick = { onSaveGoal(g.copy(enabled = !g.enabled)) {} }
                                ) {
                                    Text(if (g.enabled) "توقف هدف" else "ادامهٔ هدف")
                                }
                            }
                        }
                    }
                }
            if (goals.any { it.enabled } && state.availability.none { it.budgetMinutes > 0 })
                Text("برای زمان‌بندی، روزهای آزاد را مشخص کن.")
            Text("جلسه‌های پیشنهادی", style = MaterialTheme.typography.titleMedium)
            if (plan.unverifiedClassTimes)
                Text(
                    "زمان بعضی کلاس‌ها کامل نیست؛ قبل از شروع، تداخل با کلاس را خودت بررسی کن.",
                    style = MaterialTheme.typography.bodySmall,
                )
            Text(
                "امروز و شش روز آینده",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (0..6).forEach { offset ->
                    val d = StudentAgendaEngine.addDays(today, offset)
                    FilterChip(
                        selected = selectedDay == offset,
                        onClick = { selectedDay = offset },
                        label = {
                            Text(
                                if (offset == 0) "امروز"
                                else "${d.getWeekdayName()} ${d.month}/${d.day}"
                            )
                        },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("learning_day_$offset"),
                    )
                }
            }
            val day = StudentAgendaEngine.addDays(today, selectedDay).format()
            val rows = plan.blocks.filter { it.date == day }
            if (day in plan.examDays)
                Text(
                    "این روز امتحان ثبت شده است؛ چون ساعت پایان آن مشخص نیست، جلسهٔ مطالعه‌ای در این روز نمی‌چینیم."
                )
            else if (rows.isEmpty())
                Text(
                    "برای این روز جلسه‌ای پیشنهاد نشده؛ هدف‌ها و زمان آزاد را بررسی کن یا روز دیگری را ببین."
                )
            rows.forEach { block ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            names[block.courseId].orEmpty(),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            "${AdaptiveLearningEngine.clock(block.startMinute)} تا ${AdaptiveLearningEngine.clock(block.startMinute+block.minutes)} · ${block.minutes} دقیقه"
                        )
                        if (selectedDay == 0)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onFocus(block) },
                                    modifier = Modifier.testTag("learning_focus_${block.id}"),
                                ) {
                                    Text("شروع تمرکز")
                                }
                                OutlinedButton(onClick = { completing = block }) {
                                    Text("ثبت انجام جلسه")
                                }
                            }
                    }
                }
            }
            Text(
                "برای انتقال برنامه و فلش‌کارت‌ها به گوشی دیگر، از پشتیبان فایلی استفاده کن. ساعت‌ها به وقت تهران‌اند. کلاس‌ها از الگوی هفتگی ترم گرفته می‌شوند؛ تعطیلی و پایان ترم را خودت بررسی کن. بین جلسه‌ها پنج دقیقه فاصله می‌ماند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { history = !history }) {
                Text(if (history) "بستن جلسه‌های ثبت‌شده" else "جلسه‌های ثبت‌شده")
            }
            if (history) {
                val recorded =
                    state.completions
                        .filter { !it.isDeleted && it.courseId in names }
                        .sortedWith(
                            compareByDescending<StudyCompletionEntity> {
                                    JalaliCalendarUtil.parse(it.date)
                                }
                                .thenByDescending { it.startMinute }
                        )
                        .take(30)
                if (recorded.isEmpty()) Text("هنوز جلسه‌ای ثبت نکرده‌ای.")
                recorded.forEach { row ->
                    OutlinedCard {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(
                                "${names[row.courseId]} · ${row.date} · ${row.actualMinutes} دقیقه اعلامی"
                            )
                            TextButton(onClick = { undoing = row }) { Text("لغو ثبت") }
                        }
                    }
                }
                if (recorded.size == 30)
                    Text(
                        "۳۰ ثبت آخر نمایش داده شده است؛ همهٔ سوابق در پشتیبان نگه داشته می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                    )
            }
        }
    }
    if (showFree)
        LearningAvailabilityEditor(state.availability, { showFree = false }, onSaveAvailability)
    if (addGoal || goalEditor != null)
        LearningGoalEditor(
            goalEditor,
            if (goalEditor == null) courses.filter { c -> goals.none { it.courseId == c.id } }
            else courses,
            {
                goalEditor = null
                addGoal = false
            },
            onSaveGoal,
        )
    completing?.let { block ->
        var actual by rememberSaveable(block.id) { mutableStateOf(block.minutes.toString()) }
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf(false) }
        StudentGlassModalSheet(
            onDismiss = { if (!busy) completing = null },
            title = "زمان مطالعهٔ انجام‌شده",
            subtitle = "ثبت جداگانهٔ زمان واقعی",
        ) {
            Column(
                Modifier.fillMaxWidth().weight(1f, false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "چند دقیقه واقعاً برای این جلسه مطالعه کردی؟ این زمان اعلامی است و با تایمر جمع نمی‌شود."
                )
                OutlinedTextField(
                    actual,
                    { actual = it },
                    label = { Text("دقیقه، از ۱ تا ۲۴۰") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("learning_actual_minutes"),
                )
                if (error) Text("ثبت نشد؛ اطلاعاتت حفظ شده، دوباره تلاش کن.")
                Button(
                    enabled = !busy && learningNumber(actual) in 1..240,
                    onClick = {
                        busy = true
                        onComplete(block, learningNumber(actual)!!) { ok ->
                            busy = false
                            error = !ok
                            if (ok) completing = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("ثبت جلسه")
                }
                TextButton(
                    onClick = { completing = null },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("انصراف")
                }
            }
        }
    }

    undoing?.let { row ->
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!busy) undoing = null },
            title = { Text("لغو ثبت این جلسه؟") },
            text = {
                Text(
                    if (error) "تغییر ذخیره نشد؛ دوباره تلاش کن."
                    else "این زمان از پیشرفت هدف کم می‌شود. تایمر و سوابق دیگر تغییر نمی‌کنند."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        onUndo(row) { ok ->
                            busy = false
                            error = !ok
                            if (ok) undoing = null
                        }
                    },
                ) {
                    Text("لغو ثبت")
                }
            },
            dismissButton = {
                TextButton(onClick = { undoing = null }, enabled = !busy) { Text("بازگشت") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LearningAvailabilityEditor(
    rows: List<StudyAvailabilityEntity>,
    onDismiss: () -> Unit,
    onSave: (StudyAvailabilityEntity, (Boolean) -> Unit) -> Unit,
) {
    var day by rememberSaveable { mutableIntStateOf(0) }
    StudentGlassModalSheet(
        onDismiss,
        title = "زمان آزاد من",
        subtitle = "یک بازه و سقف مطالعه برای هر روز",
    ) {
        Column(
            Modifier.fillMaxWidth().weight(1f, false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                learningWeekdays.forEachIndexed { i, name ->
                    FilterChip(day == i, { day = i }, label = { Text(name) })
                }
            }
            key(day) {
                val old = rows.find { it.weekday == day }
                var start by rememberSaveable {
                    mutableStateOf(
                        old?.let { AdaptiveLearningEngine.clock(it.startMinute) } ?: "17:00"
                    )
                }
                var end by rememberSaveable {
                    mutableStateOf(
                        old?.let { AdaptiveLearningEngine.clock(it.endMinute) } ?: "19:00"
                    )
                }
                var budget by rememberSaveable {
                    mutableStateOf(old?.budgetMinutes?.toString() ?: "60")
                }
                var busy by remember { mutableStateOf(false) }
                var saved by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf(false) }
                OutlinedTextField(
                    start,
                    {
                        start = it
                        saved = false
                    },
                    label = { Text("شروع زمان آزاد، مثل ۱۷:۰۰") },
                    modifier = Modifier.fillMaxWidth().testTag("learning_free_start"),
                )
                OutlinedTextField(
                    end,
                    {
                        end = it
                        saved = false
                    },
                    label = { Text("پایان زمان آزاد، مثل ۱۹:۰۰") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    budget,
                    {
                        budget = it
                        saved = false
                    },
                    label = { Text("سقف مطالعهٔ این روز، دقیقه") },
                    supportingText = { Text("برای روز تعطیل صفر بنویس؛ بیشتر از طول بازه نباشد.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("learning_free_budget"),
                )
                val row =
                    StudyAvailabilityEntity(
                        day,
                        AdaptiveLearningEngine.minute(start) ?: -1,
                        AdaptiveLearningEngine.availabilityEnd(end) ?: -1,
                        learningNumber(budget) ?: -1,
                    )
                val valid = runCatching { LearningValidation.availability(row) }.isSuccess
                if (!valid) Text("بازهٔ ساعت و سقف صفر تا ۴۸۰ دقیقه را بررسی کن.")
                if (error) Text("ذخیره نشد؛ دوباره تلاش کن.")
                Button(
                    enabled = valid && !busy,
                    onClick = {
                        busy = true
                        onSave(row) { ok ->
                            busy = false
                            saved = ok
                            error = !ok
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("learning_free_save"),
                ) {
                    Text(
                        if (busy) "در حال ذخیره…"
                        else if (saved) "ذخیره شد" else "ذخیرهٔ ${learningWeekdays[day]}"
                    )
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("تمام") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LearningGoalEditor(
    old: StudyGoalEntity?,
    courses: List<CourseEntity>,
    onDismiss: () -> Unit,
    onSave: (StudyGoalEntity, (Boolean) -> Unit) -> Unit,
) {
    var courseId by
        rememberSaveable(old?.courseId) {
            mutableStateOf(old?.courseId ?: courses.firstOrNull()?.id.orEmpty())
        }
    var minutes by
        rememberSaveable(old?.courseId) { mutableStateOf(old?.totalMinutes?.toString().orEmpty()) }
    var date by rememberSaveable(old?.courseId) { mutableStateOf(old?.targetDate.orEmpty()) }
    var difficulty by rememberSaveable(old?.courseId) { mutableIntStateOf(old?.difficulty ?: 2) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    StudentGlassModalSheet(
        onDismiss,
        title = if (old == null) "هدف مطالعهٔ درس" else "ویرایش هدف",
        subtitle = "برای هر درس یک هدف فعال",
    ) {
        Column(
            Modifier.fillMaxWidth().weight(1f, false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            courses
                .filter { old == null || it.id == old.courseId }
                .forEach { course ->
                    FilterChip(
                        courseId == course.id,
                        { courseId = course.id },
                        label = { Text(course.name) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            OutlinedTextField(
                minutes,
                { minutes = it },
                label = { Text("کل زمان موردنیاز این هدف، دقیقه") },
                supportingText = {
                    Text("بین ۵ تا ۶۰۰۰ دقیقه؛ زمان ثبت‌شدهٔ قبلی هم بخشی از همین هدف است.")
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("learning_goal_minutes"),
            )
            OutlinedTextField(
                date,
                { date = it },
                label = { Text("تاریخ هدف شمسی، مثل ۱۴۰۵/۰۹/۲۰") },
                modifier = Modifier.fillMaxWidth().testTag("learning_goal_date"),
            )
            Text("دشواری این درس برای تو")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("معمولی", "سخت", "خیلی سخت").forEachIndexed { i, t ->
                    FilterChip(difficulty == i + 1, { difficulty = i + 1 }, label = { Text(t) })
                }
            }
            val parsed = JalaliCalendarUtil.parse(date)
            val today = learningToday()
            val row =
                StudyGoalEntity(
                    courseId,
                    parsed?.format().orEmpty(),
                    learningNumber(minutes) ?: 0,
                    difficulty,
                    old?.enabled ?: true,
                    updatedAt = old?.updatedAt ?: 0,
                )
            val valid =
                runCatching { LearningValidation.goal(row) }.isSuccess &&
                    parsed != null &&
                    parsed >= today &&
                    parsed <= StudentAgendaEngine.addDays(today, 90)
            if (!valid)
                Text(
                    "زمان معتبر و تاریخی از امروز تا ۹۰ روز آینده انتخاب کن.",
                    style = MaterialTheme.typography.bodySmall,
                )
            if (error) Text("ذخیره نشد؛ نوشته‌ها حفظ شده‌اند، دوباره تلاش کن.")
            Button(
                enabled = valid && !busy,
                onClick = {
                    busy = true
                    onSave(row) { ok ->
                        busy = false
                        error = !ok
                        if (ok) onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("learning_goal_save"),
            ) {
                Text(if (busy) "در حال ذخیره…" else "ذخیرهٔ هدف")
            }
            TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("انصراف")
            }
        }
    }
}

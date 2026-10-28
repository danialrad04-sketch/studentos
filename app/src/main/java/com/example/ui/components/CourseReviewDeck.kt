package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.*
import com.example.data.repository.learningToday
import com.example.domain.engine.*
import com.example.domain.util.JalaliCalendarUtil
import com.example.ui.theme.studentStickerColors

@Composable
fun CourseReviewEntry(
    course: CourseEntity,
    cards: List<ReviewCardEntity>,
    onSave: (ReviewCardEntity, (Boolean) -> Unit) -> Unit,
    onRate: (ReviewCardEntity, ReviewRating, (Boolean) -> Unit) -> Unit,
) {
    var open by rememberSaveable(course.id) { mutableStateOf(false) }
    val today = learningToday()
    val live = cards.filter { it.courseId == course.id && !it.isArchived }
    val due =
        live.count { JalaliCalendarUtil.parse(it.dueDate)?.let { date -> date <= today } == true }
    val colors = studentStickerColors(AppEmojiType.BOOK)
    Surface(
        onClick = { open = true },
        shape = MaterialTheme.shapes.large,
        color = colors.container,
        contentColor = colors.ink,
        modifier = Modifier.fillMaxWidth().testTag("course_review_${course.id}"),
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            AppEmoji(AppEmojiType.BOOK, size = 36.dp)
            Column(Modifier.weight(1f)) {
                Text("مرور این درس", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (live.isEmpty()) "فلش‌کارت بساز و مرور را شروع کن"
                    else "${live.size} کارت · $due آمادهٔ مرور",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    if (open) CourseReviewDeck(course, cards, { open = false }, onSave, onRate)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CourseReviewDeck(
    course: CourseEntity,
    cards: List<ReviewCardEntity>,
    onDismiss: () -> Unit,
    onSave: (ReviewCardEntity, (Boolean) -> Unit) -> Unit,
    onRate: (ReviewCardEntity, ReviewRating, (Boolean) -> Unit) -> Unit,
) {
    var manage by rememberSaveable(course.id) { mutableStateOf(false) }
    var archived by rememberSaveable(course.id) { mutableStateOf(false) }
    var query by rememberSaveable(course.id) { mutableStateOf("") }
    var limit by rememberSaveable(course.id) { mutableIntStateOf(20) }
    LaunchedEffect(query, archived) { limit = 20 }
    var seen by rememberSaveable(course.id) { mutableStateOf(emptyList<String>()) }
    var editor by rememberSaveable { mutableStateOf<ReviewCardEntity?>(null) }
    val today = learningToday()
    val owned = cards.filter { it.courseId == course.id }
    val due =
        owned
            .filter {
                !it.isArchived &&
                    JalaliCalendarUtil.parse(it.dueDate)?.let { d -> d <= today } == true
            }
            .sortedWith(compareBy({ JalaliCalendarUtil.parse(it.dueDate) }, { it.id }))
    val current = due.firstOrNull { it.id !in seen }
    StudentGlassModalSheet(onDismiss, title = "مرور درسی", subtitle = course.name) {
        Column(
            Modifier.fillMaxWidth()
                .weight(1f, false)
                .verticalScroll(rememberScrollState())
                .testTag("review_deck"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!manage, { manage = false }, label = { Text("مرور امروز") })
                FilterChip(manage, { manage = true }, label = { Text("کارت‌ها") })
            }
            Text(
                "${owned.count{!it.isArchived}} کارت فعال · ${due.size} آمادهٔ مرور",
                style = MaterialTheme.typography.bodySmall,
            )
            if (!manage) {
                if (current == null) {
                    Text(
                        if (seen.isEmpty()) "امروز کارت آمادهٔ مروری نداری."
                        else "مرور این نوبت تمام شد.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (due.isNotEmpty()) {
                        Text("${due.size} کارت هنوز برای تکرار امروز باقی مانده است.")
                        OutlinedButton(onClick = { seen = emptyList() }) { Text("شروع نوبت تازه") }
                    } else Text("کارت‌های بعدی در تاریخ مرورشان نمایش داده می‌شوند.")
                } else
                    key(current.id, current.updatedAt) {
                        var revealed by rememberSaveable { mutableStateOf(false) }
                        var busy by remember { mutableStateOf(false) }
                        var error by remember { mutableStateOf(false) }
                        val colors = studentStickerColors(AppEmojiType.BOOK)
                        Surface(
                            color = colors.container,
                            contentColor = colors.ink,
                            shape = MaterialTheme.shapes.large,
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Text("پرسش", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    current.question,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.testTag("review_question"),
                                )
                                if (revealed) {
                                    HorizontalDivider()
                                    Text("پاسخ", style = MaterialTheme.typography.labelMedium)
                                    Text(
                                        current.answer,
                                        modifier = Modifier.testTag("review_answer"),
                                    )
                                } else
                                    Button(
                                        onClick = { revealed = true },
                                        modifier = Modifier.fillMaxWidth().testTag("review_reveal"),
                                    ) {
                                        Text("نمایش پاسخ")
                                    }
                            }
                        }
                        if (revealed) {
                            Text("پیش از دیدن پاسخ چقدر یادت بود؟")
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                ReviewRating.entries.forEach { rating ->
                                    OutlinedButton(
                                        enabled = !busy,
                                        onClick = {
                                            busy = true
                                            error = false
                                            onRate(current, rating) { ok ->
                                                busy = false
                                                error = !ok
                                                if (ok) seen = seen + current.id
                                            }
                                        },
                                        modifier = Modifier.testTag("review_rate_${rating.name}"),
                                    ) {
                                        Text(rating.title)
                                    }
                                }
                            }
                            if (error)
                                Text(
                                    "مرور ذخیره نشد؛ کارت را از بخش کارت‌ها دوباره باز کن یا دوباره تلاش کن.",
                                    color = MaterialTheme.colorScheme.error,
                                )
                        }
                    }
                Text(
                    "با ارزیابی تو، مرور بعدی تنظیم می‌شود؛ «یادم نبود» برای تکرار امروز می‌ماند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FilterChip(
                    archived,
                    { archived = !archived },
                    label = { Text(if (archived) "بایگانی" else "کارت‌های فعال") },
                )
                OutlinedTextField(
                    query,
                    { query = it },
                    label = { Text("جست‌وجوی پرسش و پاسخ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val shown =
                    owned
                        .filter {
                            it.isArchived == archived &&
                                (it.question.contains(query.trim(), true) ||
                                    it.answer.contains(query.trim(), true))
                        }
                        .sortedBy { it.question }
                if (shown.isEmpty())
                    Text(
                        if (archived) "کارت بایگانی‌شده‌ای وجود ندارد."
                        else "اولین پرسش و پاسخ این درس را بساز."
                    )
                shown.take(limit).forEach { card ->
                    var busy by remember(card.id, card.updatedAt) { mutableStateOf(false) }
                    var error by remember(card.id, card.updatedAt) { mutableStateOf(false) }
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(card.question, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "مرور بعدی: ${card.dueDate} · ${card.repetitions} بار مرور",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { editor = card }, enabled = !busy) {
                                    Text("ویرایش")
                                }
                                TextButton(
                                    enabled = !busy,
                                    onClick = {
                                        busy = true
                                        onSave(card.copy(isArchived = !card.isArchived)) { ok ->
                                            busy = false
                                            error = !ok
                                        }
                                    },
                                ) {
                                    Text(if (card.isArchived) "بازیابی" else "بایگانی")
                                }
                            }
                            if (error) Text("ذخیره نشد؛ دوباره تلاش کن.")
                        }
                    }
                }
                if (shown.size > limit)
                    TextButton(onClick = { limit += 20 }) { Text("نمایش ۲۰ کارت بعدی") }
            }
            Button(
                onClick = {
                    editor =
                        ReviewCardEntity(
                            java.util.UUID.randomUUID().toString(),
                            course.id,
                            "",
                            "",
                            today.format(),
                        )
                },
                modifier = Modifier.fillMaxWidth().testTag("review_add"),
            ) {
                Text("ساخت فلش‌کارت")
            }
        }
    }
    editor?.let { card -> ReviewCardEditor(card, { editor = null }, onSave) }
}

@Composable
private fun ReviewCardEditor(
    card: ReviewCardEntity,
    onDismiss: () -> Unit,
    onSave: (ReviewCardEntity, (Boolean) -> Unit) -> Unit,
) {
    var question by rememberSaveable(card.id) { mutableStateOf(card.question) }
    var answer by rememberSaveable(card.id) { mutableStateOf(card.answer) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    StudentGlassModalSheet(
        onDismiss,
        title = if (card.question.isBlank()) "فلش‌کارت تازه" else "ویرایش فلش‌کارت",
        subtitle = "یک پرسش روشن، یک پاسخ کوتاه",
    ) {
        Column(
            Modifier.fillMaxWidth().weight(1f, false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                question,
                { question = it },
                label = { Text("پرسش") },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth().testTag("review_edit_question"),
            )
            OutlinedTextField(
                answer,
                { answer = it },
                label = { Text("پاسخ") },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth().testTag("review_edit_answer"),
            )
            Text(
                "پرسش تا ۱۰۰۰ و پاسخ تا ۵۰۰۰ نویسه؛ با تغییر متن، مرور کارت از امروز شروع می‌شود.",
                style = MaterialTheme.typography.bodySmall,
            )
            val changed = question.trim() != card.question || answer.trim() != card.answer
            if (error)
                Text(
                    "ذخیره نشد؛ متن حفظ شده است. اگر کارت تغییر کرده، نسخهٔ تازه را دوباره باز کن.",
                    color = MaterialTheme.colorScheme.error,
                )
            Button(
                enabled =
                    !busy &&
                        question.isNotBlank() &&
                        question.length <= 1000 &&
                        answer.isNotBlank() &&
                        answer.length <= 5000,
                onClick = {
                    busy = true
                    val updated =
                        card.copy(question = question, answer = answer).let {
                            if (changed)
                                it.copy(
                                    dueDate = learningToday().format(),
                                    intervalDays = 0,
                                    repetitions = 0,
                                    lapses = 0,
                                    lastReviewedDate = "",
                                )
                            else it
                        }
                    onSave(updated) { ok ->
                        busy = false
                        error = !ok
                        if (ok) onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("review_save"),
            ) {
                Text(if (busy) "در حال ذخیره…" else "ذخیرهٔ کارت")
            }
            TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("انصراف")
            }
        }
    }
}

@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.v3

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.*
import com.example.data.v3.*
import java.util.UUID
import kotlinx.coroutines.delay

@Composable
fun StudentPracticeContent(
    vm: StudentV3ViewModel,
    state: V3UiState,
    courseId: String,
    query: String,
    writable: Boolean,
) {
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    var active by rememberSaveable(courseId) { mutableStateOf("") }
    var minutes by rememberSaveable(courseId) { mutableStateOf("25") }
    var archived by rememberSaveable(courseId) { mutableStateOf(false) }
    val busy by vm.busy.collectAsStateWithLifecycle()
    val questions =
        state.data.questions.filter {
            it.courseId == courseId &&
                it.isArchived == archived &&
                normalizedStudentSearch(it.prompt).contains(normalizedStudentSearch(query))
        }
    V3Card(
        "بانک سؤال این درس",
        "سؤال اضافه کن، آزمون بده و نکته‌های دشوار را به مرور بفرست.",
        sticker = com.example.ui.components.AppEmojiType.TARGET,
    ) {
        FilledTonalButton(
            { editorId = UUID.randomUUID().toString() },
            enabled = writable,
            modifier = Modifier.testTag("v3_add_question"),
        ) {
            Text("افزودن سؤال")
        }
        FilterChip(archived, { archived = !archived }, { Text("سؤال‌های بایگانی‌شده") })
    }
    if (questions.isEmpty())
        V3Card(
            "سؤالی در این فهرست نیست",
            if (archived) "سؤال‌های بایگانی‌شده از اینجا بازیابی می‌شوند."
            else "از جزوه یا تمرین‌های درس، اولین سؤال را ثبت کن.",
        )
    V3Paged(questions, "questions:$courseId:$query:$archived") { q ->
        var reveal by rememberSaveable(q.id) { mutableStateOf(false) }
        V3Card(
            q.prompt,
            if (q.kind == "MCQ") "گزینه‌ای · یک پاسخ درست" else "پاسخ کوتاه · ارزیابی توسط خودت",
        ) {
            if (q.kind == "MCQ")
                StudentV3Json.choices(q.choicesJson).forEachIndexed { i, c -> Text("${i+1}. $c") }
            if (reveal)
                Text(
                    "پاسخ: ${StudentPracticeEngine.correctAnswer(q)}",
                    color = MaterialTheme.colorScheme.primary,
                )
            TextButton({ reveal = !reveal }) {
                Text(if (reveal) "پنهان کردن پاسخ" else "نمایش پاسخ")
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton({ editorId = q.id }, enabled = writable) { Text("ویرایش") }
                TextButton(
                    { vm.perform({ question(q.copy(isArchived = !q.isArchived)) }) },
                    enabled = writable,
                ) {
                    Text(if (q.isArchived) "بازیابی" else "بایگانی")
                }
            }
        }
    }
    val attempts = state.attempts.filter { it.courseId == courseId && !it.isDeleted }
    V3Card("آزمون تمرینی", "تا ۱۰۰ سؤال فعال؛ نتیجهٔ تمرین وارد نمره‌های رسمی نمی‌شود.") {
        OutlinedTextField(
            minutes,
            { minutes = it },
            label = { Text("زمان آزمون، ۵ تا ۱۸۰ دقیقه") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            {
                vm.perform({
                    active =
                        startPractice(
                            courseId,
                            com.example.data.local.util.DateTimeNormalizer.normalizeDigits(minutes)
                                .toIntOrNull() ?: 0,
                        )
                })
            },
            enabled =
                writable && state.data.questions.any { it.courseId == courseId && !it.isArchived },
            modifier = Modifier.testTag("v3_start_practice"),
        ) {
            Text("شروع آزمون")
        }
        V3Paged(attempts, "attempts:$courseId") { attempt ->
            val result =
                PracticeResult(attempt.correctCount, attempt.incorrectCount, attempt.ungradedCount)
            OutlinedButton({ active = attempt.id }, Modifier.fillMaxWidth()) {
                Text(
                    if (attempt.finishedAt == null) "ادامهٔ آزمون · ${attempt.questionCount} سؤال"
                    else
                        "نتیجه · ${result.correct} درست، ${result.incorrect} اشتباه یا بی‌پاسخ، ${result.ungraded} ارزیابی‌نشده"
                )
            }
        }
    }
    editorId?.let { id ->
        QuestionEditor(
            vm,
            state.data.questions.find { it.id == id }
                ?: PracticeQuestionEntity(id = id, courseId = courseId),
        ) {
            editorId = null
        }
    }
    if (attempts.any { it.id == active }) {
        val attempt by
            remember(active) { vm.repository.dao.observePracticeAttempt(active) }
                .collectAsStateWithLifecycle(initialValue = null)
        attempt?.let { a -> StudentPracticeAttempt(vm, a) { active = "" } }
    }
}

@Composable
private fun QuestionEditor(
    vm: StudentV3ViewModel,
    row: PracticeQuestionEntity,
    onDismiss: () -> Unit,
) {
    val saving by vm.busy.collectAsStateWithLifecycle()
    var prompt by rememberSaveable(row.id) { mutableStateOf(row.prompt) }
    var kind by rememberSaveable(row.id) { mutableStateOf(row.kind) }
    var choices by
        rememberSaveable(row.id) {
            mutableStateOf(StudentV3Json.choices(row.choicesJson).joinToString("\n"))
        }
    var answer by
        rememberSaveable(row.id) { mutableStateOf(if (row.kind == "SHORT") row.answer else "") }
    var correct by
        rememberSaveable(row.id) {
            mutableStateOf(
                if (row.kind == "MCQ") ((row.answer.toIntOrNull() ?: 0) + 1).toString() else "1"
            )
        }
    val revision by rememberSaveable(row.id) { mutableLongStateOf(row.updatedAt) }
    V3Editor(
        "سؤال تمرینی",
        vm,
        onDismiss,
        {
            vm.perform(
                {
                    val options =
                        if (kind == "MCQ")
                            choices.lines().map { it.trim() }.filter { it.isNotEmpty() }
                        else emptyList()
                    val solution =
                        if (kind == "MCQ")
                            ((com.example.data.local.util.DateTimeNormalizer.normalizeDigits(
                                        correct
                                    )
                                    .toIntOrNull() ?: 0) - 1)
                                .toString()
                        else answer
                    question(
                        row.copy(
                            updatedAt = revision,
                            prompt = prompt,
                            kind = kind,
                            choicesJson = StudentV3Json.choices(options),
                            answer = solution,
                        )
                    )
                },
                onDismiss,
            )
        },
    ) {
        V3PanelSelector(listOf("پاسخ کوتاه", "گزینه‌ای"), if (kind == "SHORT") 0 else 1) {
            kind = if (it == 0) "SHORT" else "MCQ"
        }
        OutlinedTextField(
            prompt,
            { prompt = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
            label = { Text("متن سؤال") },
            minLines = 2,
        )
        if (kind == "MCQ") {
            OutlinedTextField(
                choices,
                { choices = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                label = { Text("گزینه‌ها") },
                supportingText = { Text("هر گزینه را در یک خط بنویس.") },
                minLines = 4,
            )
            OutlinedTextField(
                correct,
                { correct = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                label = { Text("گزینهٔ درست") },
                supportingText = { Text("شمارهٔ گزینه را از ۱ وارد کن.") },
                singleLine = true,
            )
        } else
            OutlinedTextField(
                answer,
                { answer = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
                label = { Text("پاسخ مرجع") },
                minLines = 3,
            )
    }
}

@Composable
private fun StudentPracticeAttempt(
    vm: StudentV3ViewModel,
    attempt: PracticeAttemptEntity,
    onDismiss: () -> Unit,
) {
    val busy by vm.busy.collectAsStateWithLifecycle()
    val questions =
        remember(attempt.questionsJson) { StudentV3Json.questions(attempt.questionsJson) }
    val answers = remember(attempt.answersJson) { StudentV3Json.answers(attempt.answersJson) }
    val ratings = remember(attempt.ratingsJson) { StudentV3Json.ratings(attempt.ratingsJson) }
    var index by rememberSaveable(attempt.id) { mutableIntStateOf(0) }
    var confirm by rememberSaveable(attempt.id) { mutableStateOf(false) }
    var timeoutHandled by rememberSaveable(attempt.id) { mutableStateOf(false) }
    val q = questions[index.coerceIn(questions.indices)]
    var draft by rememberSaveable(attempt.id, q.id) { mutableStateOf(answers[q.id].orEmpty()) }
    val now by
        produceState(System.currentTimeMillis(), attempt.id, attempt.finishedAt) {
            while (attempt.finishedAt == null) {
                value = System.currentTimeMillis()
                delay(1000)
            }
        }
    LaunchedEffect(attempt.id, attempt.finishedAt, busy) {
        if (attempt.finishedAt == null && !busy && !timeoutHandled) {
            delay((attempt.endsAt - System.currentTimeMillis()).coerceAtLeast(0))
            timeoutHandled = true
            vm.perform({ finish(attempt.id) })
        }
    }
    fun move(target: Int) {
        if (!busy && target in questions.indices) {
            if (
                attempt.finishedAt == null &&
                    q.kind == "SHORT" &&
                    draft != answers[q.id].orEmpty() &&
                    System.currentTimeMillis() < attempt.endsAt
            )
                vm.perform({ answer(attempt.id, q.id, draft) }) { index = target }
            else index = target
        }
    }
    StudentV3Dialog(
        if (attempt.finishedAt == null) "آزمون تمرینی" else "نتیجهٔ تمرین",
        onDismiss,
        tag = "v3_attempt",
        requestClose = { close ->
            if (!busy) {
                if (
                    attempt.finishedAt == null &&
                        q.kind == "SHORT" &&
                        draft != answers[q.id].orEmpty() &&
                        System.currentTimeMillis() < attempt.endsAt
                )
                    vm.perform({ answer(attempt.id, q.id, draft) }) { close() }
                else close()
            }
        },
    ) {
        V3Status(vm)
        if (attempt.finishedAt == null) {
            val remaining = ((attempt.endsAt - now) / 1000).coerceAtLeast(0)
            Text(
                "زمان باقی‌مانده: ${remaining/60}:${(remaining%60).toString().padStart(2,'0')}",
                style = MaterialTheme.typography.titleLarge,
            )
            LinearProgressIndicator(
                progress = {
                    (remaining.toFloat() / ((attempt.endsAt - attempt.startedAt) / 1000)).coerceIn(
                        0f,
                        1f,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            V3Card("سؤال ${index+1} از ${questions.size}", q.prompt) {
                if (q.kind == "MCQ")
                    StudentV3Json.choices(q.choicesJson).forEachIndexed { i, c ->
                        Surface(
                            selected = answers[q.id] == i.toString(),
                            onClick = { vm.perform({ answer(attempt.id, q.id, i.toString()) }) },
                            enabled = !busy && now < attempt.endsAt,
                            shape = MaterialTheme.shapes.medium,
                            color =
                                if (answers[q.id] == i.toString())
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                RadioButton(answers[q.id] == i.toString(), null)
                                Text(c)
                            }
                        }
                    }
                else {
                    OutlinedTextField(
                        draft,
                        { draft = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy && now < attempt.endsAt,
                        label = { Text("پاسخ تو") },
                        minLines = 3,
                    )
                    Text(
                        "پاسخ را پیش از پایان زمان ثبت کن؛ با رفتن به سؤال بعد هم ذخیره می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    FilledTonalButton(
                        { vm.perform({ answer(attempt.id, q.id, draft) }) },
                        enabled = !busy && now < attempt.endsAt,
                        modifier = Modifier.testTag("v3_submit_answer"),
                    ) {
                        Text(
                            if (answers[q.id] == draft && draft.isNotBlank()) "پاسخ ثبت شده"
                            else "ثبت پاسخ"
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton({ move(index - 1) }, enabled = index > 0 && !busy) { Text("سؤال قبل") }
                TextButton({ move(index + 1) }, enabled = index < questions.lastIndex && !busy) {
                    Text("سؤال بعد")
                }
            }
            Button(
                { confirm = true },
                enabled = !busy,
                modifier = Modifier.testTag("v3_finish_practice"),
            ) {
                Text("پایان و بررسی پاسخ‌ها")
            }
        } else {
            val score = StudentPracticeEngine.score(attempt)
            V3Card(
                "نتیجهٔ ثبت‌شده",
                "${score.correct} درست · ${score.incorrect} اشتباه یا بی‌پاسخ · ${score.ungraded} تشریحی ارزیابی‌نشده",
            ) {
                Text(
                    "پاسخ‌های تشریحی را خودت با پاسخ مرجع مقایسه و ارزیابی کن. این نتیجه، نمرهٔ رسمی درس نیست."
                )
            }
            V3Paged(questions, "result:${attempt.id}") { question ->
                V3Card(question.prompt) {
                    val response = answers[question.id].orEmpty()
                    Text(
                        "پاسخ تو: " +
                            if (question.kind == "MCQ")
                                StudentV3Json.choices(question.choicesJson)
                                    .getOrNull(response.toIntOrNull() ?: -1)
                                    .orEmpty()
                                    .ifBlank { "ثبت نشده" }
                            else response.ifBlank { "ثبت نشده" }
                    )
                    Text("پاسخ مرجع: ${StudentPracticeEngine.correctAnswer(question)}")
                    val correct =
                        if (question.kind == "MCQ") response == question.answer
                        else ratings[question.id]
                    if (question.kind == "SHORT")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            FilterChip(
                                correct == true,
                                { vm.perform({ rateShortAnswer(attempt.id, question.id, true) }) },
                                { Text("درست پاسخ دادم") },
                                enabled = !busy,
                            )
                            FilterChip(
                                correct == false,
                                { vm.perform({ rateShortAnswer(attempt.id, question.id, false) }) },
                                { Text("نیاز به مرور") },
                                enabled = !busy,
                            )
                        }
                    else
                        Text(
                            if (correct == true) "پاسخ درست" else "اشتباه یا بی‌پاسخ",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    if (correct == false)
                        OutlinedButton(
                            { vm.perform({ addToReview(attempt.id, question.id) }) },
                            enabled = !busy,
                        ) {
                            Text("افزودن به مرور این درس")
                        }
                }
            }
        }
    }
    if (confirm)
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("آزمون پایان یابد؟") },
            text = {
                Text(
                    "پس از پایان، پاسخ‌های ثبت‌شده قابل تغییر نیستند. پاسخ تشریحی همین صفحه نیز در صورت باقی‌بودن زمان ثبت می‌شود."
                )
            },
            confirmButton = {
                TextButton(
                    {
                        vm.perform({
                            if (q.kind == "SHORT" && System.currentTimeMillis() < attempt.endsAt)
                                answer(attempt.id, q.id, draft)
                            finish(attempt.id)
                        }) {
                            confirm = false
                        }
                    },
                    enabled = !busy,
                ) {
                    Text("پایان آزمون")
                }
            },
            dismissButton = { TextButton({ confirm = false }) { Text("ادامهٔ آزمون") } },
        )
}

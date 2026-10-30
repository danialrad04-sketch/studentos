package com.example.data.v3

import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.*
import com.example.domain.util.JalaliCalendarUtil
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/** Included in the existing metadata snapshot; blob bytes are exported separately in ZIP. */
data class StudentV3Snapshot(
    val files: List<AttachmentEntity> = emptyList(),
    val bookmarks: List<PageBookmarkEntity> = emptyList(),
    val projects: List<ProjectEntity> = emptyList(),
    val steps: List<ProjectStepEntity> = emptyList(),
    val questions: List<PracticeQuestionEntity> = emptyList(),
    val attempts: List<PracticeAttemptEntity> = emptyList(),
)

object StudentV3Json {
    val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val strings =
        moshi.adapter<List<String>>(
            Types.newParameterizedType(List::class.java, String::class.java)
        )
    private val questions =
        moshi.adapter<List<PracticeQuestionEntity>>(
            Types.newParameterizedType(List::class.java, PracticeQuestionEntity::class.java)
        )
    private val answers =
        moshi.adapter<Map<String, String>>(
            Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        )
    private val ratings =
        moshi.adapter<Map<String, Boolean>>(
            Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                Boolean::class.javaObjectType,
            )
        )

    fun choices(json: String): List<String> = requireNotNull(strings.fromJson(json))

    fun choices(values: List<String>): String = strings.toJson(values)

    fun questions(json: String): List<PracticeQuestionEntity> =
        requireNotNull(questions.fromJson(json))

    fun questions(values: List<PracticeQuestionEntity>): String = questions.toJson(values)

    fun answers(json: String): Map<String, String> = requireNotNull(answers.fromJson(json))

    fun answers(values: Map<String, String>): String = answers.toJson(values)

    fun ratings(json: String): Map<String, Boolean> = requireNotNull(ratings.fromJson(json))

    fun ratings(values: Map<String, Boolean>): String = ratings.toJson(values)
}

object StudentV3Validation {
    fun text(value: String, max: Int = 200) {
        require(value.isNotBlank() && value.length <= max) { "متن خالی یا بیش از اندازه است." }
    }

    fun date(value: String) {
        require(value.isBlank() || JalaliCalendarUtil.parse(value) != null) {
            "تاریخ را به شکل ۱۴۰۵/۰۸/۱۰ وارد کنید."
        }
    }

    fun file(row: AttachmentEntity) {
        text(row.id)
        text(row.courseId)
        text(row.title)
        require(!row.isPurged || row.isDeleted)
        require(
            row.blobKey.matches(Regex("[a-f0-9]{64}")) &&
                row.byteCount in 1..StudentV3Files.MAX_FILE_BYTES &&
                row.pageCount in 1..100000 &&
                row.lastPage in 0 until row.pageCount
        ) {
            "اطلاعات فایل معتبر نیست."
        }
        require(row.mimeType in setOf("application/pdf", "image/jpeg", "image/png", "image/webp"))
    }

    fun project(row: ProjectEntity) {
        text(row.id)
        text(row.courseId)
        text(row.title)
        date(row.dueDate)
        require(
            row.description.length <= 10000 &&
                (row.estimatedMinutes == 0 || row.estimatedMinutes in 5..6000) &&
                (row.completedAt == null || row.completedAt >= 0)
        )
    }

    fun step(row: ProjectStepEntity) {
        text(row.id)
        text(row.projectId)
        text(row.title)
        date(row.dueDate)
        require(row.completedAt == null || row.completedAt >= 0)
    }

    fun question(row: PracticeQuestionEntity) {
        text(row.id)
        text(row.courseId)
        require(row.prompt.isNotBlank()) { "متن سؤال را وارد کن." }
        require(row.answer.isNotBlank()) { "پاسخ مرجع را وارد کن." }
        text(row.prompt, 4000)
        text(row.answer, 5000)
        val choices = StudentV3Json.choices(row.choicesJson)
        require(row.kind in setOf("MCQ", "SHORT"))
        if (row.kind == "MCQ")
            require(
                choices.size in 2..6 &&
                    choices.all { it.isNotBlank() && it.length <= 1000 } &&
                    row.answer.toIntOrNull() in choices.indices
            ) {
                "گزینه‌ها و شمارهٔ پاسخ درست را بررسی کنید."
            }
        else require(choices.isEmpty())
    }

    fun bookmark(row: PageBookmarkEntity) {
        text(row.id)
        text(row.attachmentId)
        require(row.page >= 0 && row.note.length <= 10000)
    }

    fun attempt(row: PracticeAttemptEntity) {
        text(row.id)
        text(row.courseId)
        require(
            row.startedAt >= 0 &&
                row.endsAt > row.startedAt &&
                row.endsAt - row.startedAt <= 180 * 60000L
        )
        require(row.finishedAt == null || row.finishedAt >= row.startedAt)
        val questions = StudentV3Json.questions(row.questionsJson)
        require(
            questions.size in 1..100 && questions.map { it.id }.distinct().size == questions.size
        )
        questions.forEach(::question)
        require(questions.all { it.courseId == row.courseId })
        require(row.questionCount == questions.size)
        if (row.finishedAt != null) {
            val score = StudentPracticeEngine.score(row)
            require(
                row.correctCount == score.correct &&
                    row.incorrectCount == score.incorrect &&
                    row.ungradedCount == score.ungraded
            ) {
                "خلاصهٔ نتیجه با پاسخ‌ها سازگار نیست."
            }
        }
        val ids = questions.map { it.id }.toSet()
        require(
            StudentV3Json.answers(row.answersJson).all { it.key in ids && it.value.length <= 5000 }
        )
        require(
            StudentV3Json.ratings(row.ratingsJson).keys.all { id ->
                questions.any { it.id == id && it.kind == "SHORT" }
            }
        )
    }

    fun snapshot(s: StudentV3Snapshot, courseIds: Set<String>) {
        val groups =
            listOf(
                s.files.map { it.id },
                s.bookmarks.map { it.id },
                s.projects.map { it.id },
                s.steps.map { it.id },
                s.questions.map { it.id },
                s.attempts.map { it.id },
            )
        require(groups.all { it.size == it.distinct().size }) {
            "شناسهٔ تکراری در پشتیبان وجود دارد."
        }
        s.files.forEach(::file)
        s.projects.forEach(::project)
        s.steps.forEach(::step)
        s.questions.forEach(::question)
        s.attempts.forEach(::attempt)
        s.bookmarks.forEach(::bookmark)
        require(
            s.files.all { it.isDeleted || it.courseId in courseIds } &&
                s.projects.all { it.isDeleted || it.courseId in courseIds } &&
                s.questions.all { it.isArchived || it.courseId in courseIds } &&
                s.attempts.all { it.isDeleted || it.courseId in courseIds }
        ) {
            "درس مرتبط با اطلاعات پیدا نشد."
        }
        require(s.steps.all { it.isDeleted || s.projects.any { p -> p.id == it.projectId } })
        require(
            s.bookmarks.all { b ->
                b.isDeleted || s.files.any { it.id == b.attachmentId && b.page < it.pageCount }
            }
        )
        require(
            s.files.all { f ->
                f.projectId == null ||
                    s.projects.any { it.id == f.projectId && it.courseId == f.courseId }
            }
        )
    }
}

suspend fun StudentDao.v3Snapshot() =
    StudentV3Snapshot(
        attachmentsSnapshot(),
        pageBookmarksSnapshot(),
        projectsSnapshot(),
        projectStepsSnapshot(),
        practiceQuestionsSnapshot(),
        practiceAttemptsSnapshot(),
    )

/** Newer local revisions and deletion markers win over an older exported file. */
suspend fun StudentDao.mergeV3(s: StudentV3Snapshot) {
    val old = v3Snapshot()
    s.files
        .filter { r -> r.updatedAt > (old.files.find { it.id == r.id }?.updatedAt ?: -1) }
        .forEach { upsertAttachment(it) }
    s.bookmarks
        .filter { r -> r.updatedAt > (old.bookmarks.find { it.id == r.id }?.updatedAt ?: -1) }
        .forEach { upsertPageBookmark(it) }
    s.projects
        .filter { r -> r.updatedAt > (old.projects.find { it.id == r.id }?.updatedAt ?: -1) }
        .forEach { upsertProject(it) }
    s.steps
        .filter { r -> r.updatedAt > (old.steps.find { it.id == r.id }?.updatedAt ?: -1) }
        .forEach { upsertProjectStep(it) }
    s.questions
        .filter { r -> r.updatedAt > (old.questions.find { it.id == r.id }?.updatedAt ?: -1) }
        .forEach { upsertPracticeQuestion(it) }
    s.attempts
        .filter { r -> r.updatedAt > (old.attempts.find { it.id == r.id }?.updatedAt ?: -1) }
        .forEach { upsertPracticeAttempt(it) }
}

fun normalizedStudentSearch(value: String): String =
    value
        .lowercase(java.util.Locale.ROOT)
        .replace('ي', 'ی')
        .replace('ى', 'ی')
        .replace('ك', 'ک')
        .replace('\u200c', ' ')
        .map { c ->
            when (c) {
                in '۰'..'۹' -> '0' + (c - '۰')
                in '٠'..'٩' -> '0' + (c - '٠')
                else -> c
            }
        }
        .joinToString("")
        .trim()

data class PracticeResult(val correct: Int, val incorrect: Int, val ungraded: Int)

object StudentPracticeEngine {
    fun correctAnswer(q: PracticeQuestionEntity): String =
        if (q.kind == "MCQ") StudentV3Json.choices(q.choicesJson)[q.answer.toInt()] else q.answer

    fun score(a: PracticeAttemptEntity): PracticeResult {
        val answers = StudentV3Json.answers(a.answersJson)
        val ratings = StudentV3Json.ratings(a.ratingsJson)
        var right = 0
        var wrong = 0
        var ungraded = 0
        StudentV3Json.questions(a.questionsJson).forEach { q ->
            val value = if (q.kind == "MCQ") answers[q.id] == q.answer else ratings[q.id]
            when (value) {
                true -> right++
                false -> wrong++
                null -> ungraded++
            }
        }
        return PracticeResult(right, wrong, ungraded)
    }
}

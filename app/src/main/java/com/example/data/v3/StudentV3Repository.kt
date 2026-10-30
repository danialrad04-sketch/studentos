package com.example.data.v3

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.learningToday
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StudentV3Repository(
    private val db: AppDatabase,
    private val canWrite: () -> Boolean = { true },
) {
    val dao = db.studentDao()

    private fun stamp(old: Long) = maxOf(System.currentTimeMillis(), old + 1)

    private fun owner() {
        check(canWrite()) { "نشست تغییر کرده؛ صفحه را دوباره باز کنید." }
    }

    private suspend fun course(id: String, allowArchived: Boolean = false) {
        require(
            dao.getAllCoursesIncludingArchivedSync().any {
                it.id == id && (allowArchived || !it.isArchived)
            }
        ) {
            "این درس دیگر در دسترس نیست."
        }
    }

    private fun revision(old: Long?, expected: Long) {
        require(old == null || old == expected) { "اطلاعات تغییر کرده؛ صفحه را دوباره باز کنید." }
    }

    private suspend fun <T> write(block: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                owner()
                val value = block()
                owner()
                value
            }
        }

    suspend fun saveFile(row: AttachmentEntity) = write {
        val old = dao.attachmentsSnapshot().find { it.id == row.id }
        revision(old?.updatedAt, row.updatedAt)
        course(row.courseId, allowArchived = old != null)
        StudentV3Validation.file(row)
        require(old == null || old.courseId == row.courseId)
        require(old?.isPurged != true || row.isPurged) {
            "فایل برای همیشه پاک شده است؛ دوباره آن را وارد کنید."
        }
        row.projectId?.let { id ->
            require(
                dao.projectsSnapshot().any {
                    it.id == id &&
                        it.courseId == row.courseId &&
                        (!it.isDeleted || old?.projectId == it.id)
                }
            )
        }
        dao.upsertAttachment(row.copy(title = row.title.trim(), updatedAt = stamp(row.updatedAt)))
    }

    suspend fun purgeFile(row: AttachmentEntity) = write {
        val old = dao.attachmentsSnapshot().find { it.id == row.id } ?: error("فایل پیدا نشد.")
        revision(old.updatedAt, row.updatedAt)
        require(old.isDeleted)
        dao.upsertAttachment(old.copy(isPurged = true, updatedAt = stamp(old.updatedAt)))
    }

    suspend fun savePage(id: String, page: Int) = write {
        val row =
            dao.attachmentsSnapshot().find { it.id == id && !it.isDeleted }
                ?: error("فایل حذف شده است.")
        course(row.courseId, allowArchived = true)
        require(page in 0 until row.pageCount)
        dao.upsertAttachment(row.copy(lastPage = page, updatedAt = stamp(row.updatedAt)))
    }

    suspend fun bookmark(id: String, page: Int, note: String) = write {
        val file =
            dao.attachmentsSnapshot().find { it.id == id && !it.isDeleted }
                ?: error("فایل حذف شده است.")
        course(file.courseId, allowArchived = true)
        require(page in 0 until file.pageCount && note.length <= 10000)
        val key = "$id:$page"
        val old = dao.pageBookmarksSnapshot().find { it.id == key }
        dao.upsertPageBookmark(
            PageBookmarkEntity(key, id, page, note.trim(), stamp(old?.updatedAt ?: 0))
        )
    }

    suspend fun removeBookmark(row: PageBookmarkEntity) = write {
        revision(dao.pageBookmarksSnapshot().find { it.id == row.id }?.updatedAt, row.updatedAt)
        dao.upsertPageBookmark(row.copy(isDeleted = true, updatedAt = stamp(row.updatedAt)))
    }

    suspend fun project(row: ProjectEntity) = write {
        StudentV3Validation.project(row)
        course(row.courseId)
        val old = dao.projectsSnapshot().find { it.id == row.id }
        revision(old?.updatedAt, row.updatedAt)
        require(old == null || old.courseId == row.courseId)
        dao.upsertProject(row.copy(title = row.title.trim(), updatedAt = stamp(row.updatedAt)))
    }

    suspend fun step(row: ProjectStepEntity) = write {
        StudentV3Validation.step(row)
        val project =
            dao.projectsSnapshot().find { it.id == row.projectId && !it.isDeleted }
                ?: error("پروژه در دسترس نیست.")
        course(project.courseId)
        val old = dao.projectStepsSnapshot().find { it.id == row.id }
        revision(old?.updatedAt, row.updatedAt)
        require(old == null || old.projectId == row.projectId)
        dao.upsertProjectStep(row.copy(title = row.title.trim(), updatedAt = stamp(row.updatedAt)))
    }

    suspend fun linkTask(taskId: Long, projectId: String?) = write {
        val task = dao.getTaskById(taskId) ?: error("تکلیف در دسترس نیست.")
        course(task.courseId)
        if (projectId != null)
            require(
                dao.projectsSnapshot().any {
                    it.id == projectId && !it.isDeleted && it.courseId == task.courseId
                }
            ) {
                "تکلیف و پروژه باید متعلق به یک درس باشند."
            }
        dao.updateTask(task.copy(projectId = projectId))
    }

    suspend fun question(row: PracticeQuestionEntity) = write {
        StudentV3Validation.question(row)
        course(row.courseId)
        val old = dao.practiceQuestionsSnapshot().find { it.id == row.id }
        revision(old?.updatedAt, row.updatedAt)
        require(old == null || old.courseId == row.courseId)
        dao.upsertPracticeQuestion(
            row.copy(
                prompt = row.prompt.trim(),
                answer = row.answer.trim(),
                updatedAt = stamp(row.updatedAt),
            )
        )
    }

    suspend fun startPractice(
        courseId: String,
        minutes: Int,
        now: Long = System.currentTimeMillis(),
    ): String = write {
        course(courseId)
        require(minutes in 5..180)
        require(
            dao.practiceAttemptsSnapshot().none {
                it.courseId == courseId && !it.isDeleted && it.finishedAt == null && it.endsAt > now
            }
        ) {
            "ابتدا آزمون نیمه‌تمام این درس را ادامه دهید."
        }
        val questions =
            dao.practiceQuestionsSnapshot()
                .filter { it.courseId == courseId && !it.isArchived }
                .sortedBy { it.id }
                .take(100)
        require(questions.isNotEmpty()) { "ابتدا سؤال اضافه کنید." }
        val id = UUID.randomUUID().toString()
        dao.upsertPracticeAttempt(
            PracticeAttemptEntity(
                id,
                courseId,
                now,
                now + minutes * 60000L,
                questionsJson = StudentV3Json.questions(questions),
                updatedAt = now,
                questionCount = questions.size,
            )
        )
        id
    }

    suspend fun answer(
        id: String,
        questionId: String,
        text: String,
        now: Long = System.currentTimeMillis(),
    ) = write {
        val row = attempt(id)
        require(row.finishedAt == null && now < row.endsAt) {
            "زمان آزمون تمام شده است؛ نتیجه را باز کنید."
        }
        val q =
            StudentV3Json.questions(row.questionsJson).firstOrNull { it.id == questionId }
                ?: error("سؤال پیدا نشد.")
        require(
            text.length <= 5000 &&
                (q.kind != "MCQ" ||
                    text.toIntOrNull() in StudentV3Json.choices(q.choicesJson).indices)
        )
        dao.upsertPracticeAttempt(
            row.copy(
                answersJson =
                    StudentV3Json.answers(
                        StudentV3Json.answers(row.answersJson) + (questionId to text)
                    ),
                updatedAt = stamp(row.updatedAt),
            )
        )
    }

    private suspend fun attempt(id: String): PracticeAttemptEntity {
        val row =
            dao.practiceAttemptsSnapshot().find { it.id == id && !it.isDeleted }
                ?: error("آزمون در دسترس نیست.")
        course(row.courseId, allowArchived = true)
        return row
    }

    suspend fun finish(id: String, now: Long = System.currentTimeMillis()) = write {
        val row = attempt(id)
        if (row.finishedAt == null)
            dao.upsertPracticeAttempt(
                summarized(
                    row.copy(
                        finishedAt = minOf(now, row.endsAt).coerceAtLeast(row.startedAt),
                        updatedAt = stamp(row.updatedAt),
                    )
                )
            )
    }

    suspend fun rateShortAnswer(id: String, questionId: String, correct: Boolean) = write {
        val row = attempt(id)
        require(row.finishedAt != null) { "ابتدا آزمون را پایان دهید." }
        require(
            StudentV3Json.questions(row.questionsJson).any {
                it.id == questionId && it.kind == "SHORT"
            }
        )
        dao.upsertPracticeAttempt(
            summarized(
                row.copy(
                    ratingsJson =
                        StudentV3Json.ratings(
                            StudentV3Json.ratings(row.ratingsJson) + (questionId to correct)
                        ),
                    updatedAt = stamp(row.updatedAt),
                )
            )
        )
    }

    private fun summarized(row: PracticeAttemptEntity): PracticeAttemptEntity {
        val score = StudentPracticeEngine.score(row)
        return row.copy(
            correctCount = score.correct,
            incorrectCount = score.incorrect,
            ungradedCount = score.ungraded,
        )
    }

    suspend fun addToReview(attemptId: String, questionId: String) = write {
        val row = attempt(attemptId)
        require(row.finishedAt != null)
        val q = StudentV3Json.questions(row.questionsJson).first { it.id == questionId }
        val id = "practice:$questionId"
        val old = dao.reviewCardsSnapshot().find { it.id == id }
        if (old == null)
            dao.upsertReviewCard(
                ReviewCardEntity(
                    id,
                    q.courseId,
                    q.prompt,
                    StudentPracticeEngine.correctAnswer(q),
                    learningToday().format(),
                    updatedAt = System.currentTimeMillis(),
                )
            )
        else require(!old.isArchived) { "کارت در مرور بایگانی شده است؛ از همان‌جا بازیابی کنید." }
    }
}

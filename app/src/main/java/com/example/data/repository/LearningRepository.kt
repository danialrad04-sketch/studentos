package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.*
import com.example.domain.engine.*
import com.example.domain.util.JalaliCalendarUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shares the existing Room/account boundary. No new server or second canonical store. */
class LearningRepository(
    private val dao: StudentDao,
    private val db: AppDatabase,
    private val canWrite: () -> Boolean = { true },
) {
    val goals = dao.observeStudyGoals()
    val availability = dao.observeStudyAvailability()
    val completions = dao.observeStudyCompletions()
    val cards = dao.observeReviewCards()

    private suspend fun courseExists(id: String) {
        require(dao.getAllCoursesIncludingArchivedSync().any { it.id == id && !it.isArchived }) {
            "این درس دیگر در دسترس نیست."
        }
    }

    suspend fun saveGoal(row: StudyGoalEntity) =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                check(canWrite()) { "نشست تغییر کرده؛ صفحه را دوباره باز کنید." }
                LearningValidation.goal(row)
                courseExists(row.courseId)
                val old = dao.studyGoalsSnapshot().find { it.courseId == row.courseId }
                require(old == null || old.updatedAt == row.updatedAt) {
                    "هدف تغییر کرده؛ دوباره آن را باز کنید."
                }
                dao.upsertStudyGoal(
                    row.copy(
                        updatedAt = maxOf(System.currentTimeMillis(), (old?.updatedAt ?: 0) + 1)
                    )
                )
            }
        }

    suspend fun saveAvailability(row: StudyAvailabilityEntity) =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                check(canWrite()) { "نشست تغییر کرده؛ صفحه را دوباره باز کنید." }
                LearningValidation.availability(row)
                val old = dao.studyAvailabilitySnapshot().find { it.weekday == row.weekday }
                dao.upsertStudyAvailability(
                    row.copy(
                        updatedAt = maxOf(System.currentTimeMillis(), (old?.updatedAt ?: 0) + 1)
                    )
                )
            }
        }

    suspend fun complete(block: LearningBlock, actualMinutes: Int) =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                check(canWrite()) { "نشست تغییر کرده؛ صفحه را دوباره باز کنید." }
                courseExists(block.courseId)
                block.workId?.let { work ->
                    require(if(work.startsWith("project:")) dao.projectsSnapshot().any { "project:${it.id}"==work && it.courseId==block.courseId && !it.isDeleted && it.completedAt==null } else dao.getAllTasksSync().any { "task:${it.id}"==work && it.courseId==block.courseId && !it.isCompleted }) { "کار مرتبط تغییر کرده؛ برنامه را دوباره باز کنید." }
                }
                require(JalaliCalendarUtil.parse(block.date)!! <= learningToday()) {
                    "جلسهٔ آینده هنوز انجام نشده است."
                }
                val previous = dao.studyCompletionsSnapshot().find { it.id == block.id }
                require(previous == null || previous.isDeleted) { "این جلسه قبلاً ثبت شده است." }
                val row =
                    StudyCompletionEntity(
                        block.id,
                        block.courseId,
                        block.date,
                        block.startMinute,
                        block.minutes,
                        actualMinutes,
                        maxOf(System.currentTimeMillis(), (previous?.updatedAt ?: 0) + 1),
                        workId = block.workId,
                    )
                LearningValidation.completion(row)
                dao.upsertStudyCompletion(row)
            }
        }

    suspend fun undo(row: StudyCompletionEntity) =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                check(canWrite()) { "نشست تغییر کرده؛ صفحه را دوباره باز کنید." }
                val current = dao.studyCompletionsSnapshot().find { it.id == row.id }
                require(current?.updatedAt == row.updatedAt) {
                    "اطلاعات جلسه تغییر کرده؛ دوباره بررسی کنید."
                }
                dao.upsertStudyCompletion(
                    row.copy(
                        isDeleted = true,
                        updatedAt = maxOf(System.currentTimeMillis(), row.updatedAt + 1),
                    )
                )
            }
        }

    suspend fun saveCard(row: ReviewCardEntity) =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                check(canWrite()) { "نشست تغییر کرده؛ صفحه را دوباره باز کنید." }
                LearningValidation.card(row)
                courseExists(row.courseId)
                val old = dao.reviewCardsSnapshot().find { it.id == row.id }
                require(old == null || old.updatedAt == row.updatedAt) {
                    "کارت تغییر کرده؛ دوباره آن را باز کنید."
                }
                dao.upsertReviewCard(
                    row.copy(
                        question = row.question.trim(),
                        answer = row.answer.trim(),
                        updatedAt = maxOf(System.currentTimeMillis(), row.updatedAt + 1),
                    )
                )
            }
        }

    suspend fun rate(row: ReviewCardEntity, rating: ReviewRating) =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                check(canWrite()) { "نشست تغییر کرده؛ صفحه را دوباره باز کنید." }
                courseExists(row.courseId)
                val current = dao.reviewCardsSnapshot().find { it.id == row.id }
                require(
                    current != null && !current.isArchived && current.updatedAt == row.updatedAt
                ) {
                    "این کارت تغییر کرده؛ مرور را دوباره باز کنید."
                }
                require(JalaliCalendarUtil.parse(current.dueDate)!! <= learningToday()) {
                    "زمان مرور این کارت هنوز نرسیده است."
                }
                dao.upsertReviewCard(
                    SpacedReviewEngine.rate(
                        current,
                        rating,
                        learningToday(),
                        System.currentTimeMillis(),
                    )
                )
            }
        }
}

fun learningToday(): com.example.domain.util.JalaliDate {
    val c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tehran"))
    return JalaliCalendarUtil.gregorianToJalali(
        c.get(java.util.Calendar.YEAR),
        c.get(java.util.Calendar.MONTH) + 1,
        c.get(java.util.Calendar.DAY_OF_MONTH),
    )
}

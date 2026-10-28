package com.example.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.*
import com.example.domain.engine.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class LearningPersistenceTest {
    private lateinit var db: AppDatabase
    private lateinit var learning: LearningRepository
    private lateinit var repo: StudentRepository
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun create() = runBlocking {
        db =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        learning = LearningRepository(db.studentDao(), db)
        repo = StudentRepository(db.studentDao(), db.curriculumDao(), db)
        db.studentDao().insertCourse(CourseEntity(id = "math", name = "ریاضی"))
    }

    @After
    fun close() {
        db.close()
    }

    private suspend fun seed() {
        learning.saveGoal(
            StudyGoalEntity("math", StudentAgendaEngine.addDays(learningToday(), 7).format(), 120)
        )
        learning.saveAvailability(StudyAvailabilityEntity(0, 540, 720, 60))
        learning.complete(LearningBlock("block", "math", learningToday().format(), 540, 25), 19)
        learning.saveCard(
            ReviewCardEntity("card", "math", "  پرسش  ", "پاسخ", learningToday().format())
        )
    }

    @Test
    fun allLearningDataRoundTripsThroughTheExistingBackup() = runBlocking {
        seed()
        val json = repo.exportFullBackupJson()
        db.studentDao().clearAllUserData()
        assertTrue(repo.restoreFullBackupJson(json).isSuccess)
        assertEquals(120, db.studentDao().studyGoalsSnapshot().single().totalMinutes)
        assertEquals(60, db.studentDao().studyAvailabilitySnapshot().single().budgetMinutes)
        assertEquals(19, db.studentDao().studyCompletionsSnapshot().single().actualMinutes)
        assertEquals("پرسش", db.studentDao().reviewCardsSnapshot().single().question)
    }

    @Test
    fun legacyBackupsDoNotEraseNewLearningData() = runBlocking {
        seed()
        assertTrue(repo.restoreFullBackupJson("""{"notes":[]}""").isSuccess)
        assertEquals(1, db.studentDao().reviewCardsSnapshot().size)
        assertEquals(1, db.studentDao().studyGoalsSnapshot().size)
    }

    @Test
    fun replayAndStaleReviewRequestsCannotDoubleAdvanceTheSchedule() = runBlocking {
        seed()
        val card = db.studentDao().reviewCardsSnapshot().single()
        learning.rate(card, ReviewRating.GOOD)
        assertTrue(runCatching { learning.rate(card, ReviewRating.GOOD) }.isFailure)
        assertTrue(runCatching { learning.saveCard(card.copy(answer = "قدیمی")) }.isFailure)
        assertEquals(1, db.studentDao().reviewCardsSnapshot().single().repetitions)
    }

    @Test
    fun olderBackupsCannotUndoArchiveOrResurrectAnUndoneSession() = runBlocking {
        seed()
        val backup = repo.exportFullBackupJson()
        val card = db.studentDao().reviewCardsSnapshot().single()
        learning.saveCard(card.copy(isArchived = true))
        val done = db.studentDao().studyCompletionsSnapshot().single()
        learning.undo(done)
        assertTrue(repo.restoreFullBackupJson(backup).isSuccess)
        assertTrue(db.studentDao().reviewCardsSnapshot().single().isArchived)
        assertTrue(db.studentDao().studyCompletionsSnapshot().single().isDeleted)
    }

    @Test
    fun duplicateCompletionIsRejectedAndUndoCanBeReactivatedIntentionally() = runBlocking {
        val block = LearningBlock("same", "math", learningToday().format(), 540, 25)
        learning.complete(block, 20)
        assertTrue(runCatching { learning.complete(block, 20) }.isFailure)
        learning.undo(db.studentDao().studyCompletionsSnapshot().single())
        learning.complete(block, 15)
        assertEquals(1, db.studentDao().studyCompletionsSnapshot().size)
        assertEquals(15, db.studentDao().studyCompletionsSnapshot().single().actualMinutes)
    }

    @Test
    fun changingAccountGenerationPreventsAStaleEditorFromWriting() = runBlocking {
        var allowed = true
        val scoped = LearningRepository(db.studentDao(), db) { allowed }
        allowed = false
        assertTrue(
            runCatching { scoped.saveGoal(StudyGoalEntity("math", learningToday().format(), 60)) }
                .isFailure
        )
        assertTrue(db.studentDao().studyGoalsSnapshot().isEmpty())
    }

    @Test
    fun deletionAndResetCannotLeaveLearningRecordsInAnotherAccount() = runBlocking {
        seed()
        db.studentDao().deleteCourseById("math")
        db.studentDao().cleanupOrphans()
        assertTrue(db.studentDao().reviewCardsSnapshot().isEmpty())
        assertTrue(db.studentDao().studyGoalsSnapshot().isEmpty())
        assertTrue(db.studentDao().studyCompletionsSnapshot().isEmpty())
        db.studentDao().clearAllUserData()
        assertTrue(db.studentDao().studyAvailabilitySnapshot().isEmpty())
    }

    @Test
    fun invalidLearningBackupIsRejectedBeforeChangingTheProfile() = runBlocking {
        db.studentDao().insertProfile(StudentProfileEntity(name = "محفوظ"))
        val result =
            repo.restoreFullBackupJson(
                """{"profile":{"name":"تغییر"},"studyGoals":[{"courseId":"math","targetDate":"bad","totalMinutes":120}]}"""
            )
        assertTrue(result.isFailure)
        assertEquals("محفوظ", db.studentDao().getProfileSync()!!.name)
    }

    @Test
    fun linkedStudySessionSurvivesTimerRecreationAndNeverAutoCompletesTheGoal() = runBlocking {
        val prefs = AppPreferencesRepository(context)
        val block = LearningBlock("timer-block", "math", learningToday().format(), 540, 5)
        val deadline = System.currentTimeMillis() + 300000
        prefs.saveFocusSession(
            com.example.domain.model.FocusSession(300, 300, deadline, "ریاضی", block)
        )
        val reopened = AppPreferencesRepository(context)
        assertEquals(block, reopened.focusSession.value.learningBlock)
        assertTrue(reopened.completeFocusSession(deadline + 1))
        assertEquals(block, AppPreferencesRepository(context).focusSession.value.learningBlock)
        assertTrue(db.studentDao().studyCompletionsSnapshot().isEmpty())
        reopened.clearAcademicChoices()
        assertNull(reopened.focusSession.value.learningBlock)
    }

    @Test
    fun publishedSchemaTwelveMigratesAndAllNewDataSurvivesReopen() = runBlocking {
        val name = "learning-upgrade-${java.util.UUID.randomUUID()}.db"
        val schema =
            org.json
                .JSONObject(
                    javaClass.classLoader!!
                        .getResourceAsStream("com.example.data.local.AppDatabase/12.json")!!
                        .bufferedReader()
                        .use { it.readText() }
                )
                .getJSONObject("database")
        val helper =
            androidx.sqlite.db.framework
                .FrameworkSQLiteOpenHelperFactory()
                .create(
                    androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                        .name(name)
                        .callback(
                            object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(12) {
                                override fun onCreate(
                                    raw: androidx.sqlite.db.SupportSQLiteDatabase
                                ) {
                                    val entities = schema.getJSONArray("entities")
                                    for (i in 0 until entities.length()) {
                                        val e = entities.getJSONObject(i)
                                        val table = e.getString("tableName")
                                        raw.execSQL(
                                            e.getString("createSql")
                                                .replace("\${TABLE_NAME}", table)
                                        )
                                        val indices =
                                            e.optJSONArray("indices") ?: org.json.JSONArray()
                                        for (j in 0 until indices.length()) raw.execSQL(
                                            indices
                                                .getJSONObject(j)
                                                .getString("createSql")
                                                .replace("\${TABLE_NAME}", table)
                                        )
                                    }
                                    val setup = schema.getJSONArray("setupQueries")
                                    for (i in 0 until setup.length()) raw.execSQL(
                                        setup.getString(i)
                                    )
                                    raw.execSQL(
                                        "INSERT INTO notes(id,courseId,title,content,updatedAt,isArchived) VALUES('old',NULL,'قدیمی','محفوظ',123,1)"
                                    )
                                }

                                override fun onUpgrade(
                                    raw: androidx.sqlite.db.SupportSQLiteDatabase,
                                    oldVersion: Int,
                                    newVersion: Int,
                                ) {}
                            }
                        )
                        .build()
                )
        helper.writableDatabase
        helper.close()
        try {
            var upgraded =
                Room.databaseBuilder(context, AppDatabase::class.java, name)
                    .addMigrations(AppDatabase.MIGRATION_12_13)
                    .allowMainThreadQueries()
                    .build()
            assertEquals("محفوظ", upgraded.studentDao().getNoteById("old")!!.content)
            upgraded.studentDao().insertCourse(CourseEntity(id = "math", name = "ریاضی"))
            LearningRepository(upgraded.studentDao(), upgraded)
                .saveCard(
                    ReviewCardEntity("saved", "math", "سؤال", "جواب", learningToday().format())
                )
            upgraded.close()
            upgraded =
                Room.databaseBuilder(context, AppDatabase::class.java, name)
                    .allowMainThreadQueries()
                    .build()
            assertEquals("جواب", upgraded.studentDao().reviewCardsSnapshot().single().answer)
            assertTrue(upgraded.studentDao().getNoteById("old")!!.isArchived)
            upgraded.close()
        } finally {
            context.deleteDatabase(name)
        }
    }
}

package com.example.data

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.StudentRepository
import com.example.data.v3.*
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StudentV3PersistenceTest {
    private lateinit var db: AppDatabase
    private lateinit var v3: StudentV3Repository
    private lateinit var repository: StudentRepository
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() = runBlocking {
        db =
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        v3 = StudentV3Repository(db)
        repository = StudentRepository(db.studentDao(), db.curriculumDao(), db)
        db.studentDao().insertCourse(CourseEntity(id = "math", name = "ریاضی"))
    }

    @After
    fun close() {
        db.close()
    }

    private fun question(id: String = "q", kind: String = "MCQ") =
        PracticeQuestionEntity(
            id,
            "math",
            "جمع دو و دو؟",
            if (kind == "MCQ") StudentV3Json.choices(listOf("چهار", "پنج")) else "[]",
            if (kind == "MCQ") "0" else "چهار",
            kind,
        )

    @Test
    fun immutableAttemptsSurviveQuestionEditsAndDuplicateReviewActions() = runBlocking {
        v3.question(question())
        v3.question(question("short", "SHORT"))
        val attempt = v3.startPractice("math", 5, 1000)
        v3.answer(attempt, "q", "0", 2000)
        v3.answer(attempt, "short", "چهار", 2200)
        val old = db.studentDao().practiceQuestionsSnapshot().first { it.id == "q" }
        v3.question(old.copy(answer = "1"))
        v3.finish(attempt, 3000)
        v3.finish(attempt, 4000)
        v3.rateShortAnswer(attempt, "short", true)
        val result = db.studentDao().practiceAttemptsSnapshot().single()
        assertEquals(3000L, result.finishedAt)
        assertEquals(PracticeResult(2, 0, 0), StudentPracticeEngine.score(result))
        assertEquals(2, result.correctCount)
        v3.addToReview(attempt, "q")
        v3.addToReview(attempt, "q")
        assertEquals("چهار", db.studentDao().reviewCardsSnapshot().single().answer)
        assertTrue(runCatching { v3.answer(attempt, "q", "1", 4000) }.isFailure)
    }

    @Test
    fun expiredAttemptRejectsLateAnswersAndRetainsItsOriginalDeadline() = runBlocking {
        v3.question(question())
        val id = v3.startPractice("math", 5, 1000)
        assertTrue(runCatching { v3.answer(id, "q", "0", 301000) }.isFailure)
        v3.finish(id, 400000)
        assertEquals(301000L, db.studentDao().practiceAttemptsSnapshot().single().finishedAt)
        assertEquals(
            PracticeResult(0, 1, 0),
            StudentPracticeEngine.score(db.studentDao().practiceAttemptsSnapshot().single()),
        )
    }

    @Test
    fun projectTaskAndQuestionDataRoundTripWithoutResurrectingNewerArchives() = runBlocking {
        v3.project(ProjectEntity("p", "math", "گزارش", "1405/09/01", estimatedMinutes = 60))
        v3.step(ProjectStepEntity("s", "p", "طرح اولیه"))
        val taskId =
            db.studentDao()
                .insertTask(
                    TaskEntity(
                        title = "تمرین",
                        courseId = "math",
                        dueDate = "1405/09/01",
                        estimatedMinutes = 30,
                    )
                )
        v3.linkTask(taskId, "p")
        v3.question(question())
        val original = repository.exportFullBackupJson()
        v3.project(db.studentDao().projectsSnapshot().single().copy(isDeleted = true))
        v3.question(db.studentDao().practiceQuestionsSnapshot().single().copy(isArchived = true))
        assertTrue(repository.restoreFullBackupJson(original).isSuccess)
        assertTrue(db.studentDao().projectsSnapshot().single().isDeleted)
        assertTrue(db.studentDao().practiceQuestionsSnapshot().single().isArchived)
        val latest = repository.exportFullBackupJson()
        db.studentDao().clearAllUserData()
        assertTrue(repository.restoreFullBackupJson(latest).isSuccess)
        assertEquals("p", db.studentDao().getTaskById(taskId)?.projectId)
        assertEquals(30, db.studentDao().getTaskById(taskId)?.estimatedMinutes)
        assertEquals(1, db.studentDao().projectStepsSnapshot().size)
        assertTrue(repository.restoreFullBackupJson("""{"version":1,"notes":[]}""").isSuccess)
        assertEquals(1, db.studentDao().projectsSnapshot().size)
    }

    @Test
    fun staleEditorsAndChangedOwnersCannotOverwriteRecords() = runBlocking {
        v3.project(ProjectEntity("p", "math", "نسخه اول"))
        val row = db.studentDao().projectsSnapshot().single()
        v3.project(row.copy(title = "نسخه تازه"))
        assertTrue(runCatching { v3.project(row.copy(title = "ویرایش قدیمی")) }.isFailure)
        val unavailable = StudentV3Repository(db) { false }
        assertTrue(runCatching { unavailable.question(question()) }.isFailure)
        assertTrue(db.studentDao().practiceQuestionsSnapshot().isEmpty())
    }

    @Test
    fun crossCourseTaskLinkAndInvalidBackupAreRejectedBeforeWrites() = runBlocking {
        db.studentDao().insertCourse(CourseEntity(id = "other", name = "درس دیگر"))
        v3.project(ProjectEntity("p", "math", "پروژه"))
        val task =
            db.studentDao()
                .insertTask(TaskEntity(title = "تمرین", courseId = "other", dueDate = "1405/09/01"))
        assertTrue(runCatching { v3.linkTask(task, "p") }.isFailure)
        db.studentDao().insertProfile(StudentProfileEntity(name = "محفوظ"))
        val broken =
            """{"version":2,"profile":{"name":"خراب"},"v3":{"questions":[{"id":"q","courseId":"math","prompt":"پرسش","kind":"MCQ","answer":"5","choicesJson":"[\"الف\",\"ب\"]"}]}}"""
        assertTrue(repository.restoreFullBackupJson(broken).isFailure)
        assertEquals("محفوظ", db.studentDao().getProfileSync()?.name)
    }

    private suspend fun importImage(files: StudentV3Files): AttachmentEntity {
        val source = File(context.cacheDir, "test-library.png")
        source.outputStream().use {
            Bitmap.createBitmap(20, 30, Bitmap.Config.ARGB_8888)
                .compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return files.import(Uri.fromFile(source), "math", null, { true }) { v3.saveFile(it) }
    }

    @Test
    fun fullZipRestoresActualBlobAndBookmarksAndPreservesNewerDeletion() = runBlocking {
        val files = StudentV3Files(context)
        val row = importImage(files)
        v3.bookmark(row.id, 0, "نکتهٔ صفحه")
        val archive = File(context.cacheDir, "roundtrip.zip")
        val portable = StudentPortableBackup(files)
        portable.export(Uri.fromFile(archive), { repository.exportFullBackupJson() }, { true })
        db.studentDao().clearAllUserData()
        files.clear()
        portable.import(Uri.fromFile(archive), { true }) {
            repository.restoreFullBackupJson(it).getOrThrow()
        }
        assertEquals(row.blobKey, db.studentDao().attachmentsSnapshot().single().blobKey)
        assertEquals("نکتهٔ صفحه", db.studentDao().pageBookmarksSnapshot().single().note)
        assertEquals(row.blobKey, StudentV3Files.digest(files.blob(row.blobKey)))
        v3.saveFile(db.studentDao().attachmentsSnapshot().single().copy(isDeleted = true))
        portable.import(Uri.fromFile(archive), { true }) {
            repository.restoreFullBackupJson(it).getOrThrow()
        }
        assertTrue(db.studentDao().attachmentsSnapshot().single().isDeleted)
    }

    @Test
    fun olderPortableBackupCannotResurrectPermanentlyPurgedBytes() = runBlocking {
        val files = StudentV3Files(context)
        val row = importImage(files)
        val archive = File(context.cacheDir, "before-purge.zip")
        val portable = StudentPortableBackup(files) { db.studentDao().attachmentsSnapshot() }
        portable.export(Uri.fromFile(archive), { repository.exportFullBackupJson() }, { true })
        v3.saveFile(db.studentDao().attachmentsSnapshot().single().copy(isDeleted = true))
        v3.purgeFile(db.studentDao().attachmentsSnapshot().single())
        files.pruneUnreferenced {
            db.studentDao().attachmentsSnapshot().filter { !it.isPurged }.map { it.blobKey }.toSet()
        }
        assertFalse(files.exists(row.blobKey))
        portable.import(Uri.fromFile(archive), { true }) {
            repository.restoreFullBackupJson(it).getOrThrow()
        }
        assertTrue(db.studentDao().attachmentsSnapshot().single().isPurged)
        assertFalse(files.exists(row.blobKey))
    }

    @Test
    fun validPortableBackupRepairsAnExistingCorruptBlob() = runBlocking {
        val files = StudentV3Files(context)
        val row = importImage(files)
        val archive = File(context.cacheDir, "repair.zip")
        val portable = StudentPortableBackup(files) { db.studentDao().attachmentsSnapshot() }
        portable.export(Uri.fromFile(archive), { repository.exportFullBackupJson() }, { true })
        files.blob(row.blobKey).writeText("damaged bytes")
        portable.import(Uri.fromFile(archive), { true }) {
            repository.restoreFullBackupJson(it).getOrThrow()
        }
        assertEquals(row.blobKey, StudentV3Files.digest(files.blob(row.blobKey)))
        val bitmap = files.render(db.studentDao().attachmentsSnapshot().single(), 0)
        assertEquals(20, bitmap.width)
        assertEquals(30, bitmap.height)
        bitmap.recycle()
    }

    @Test
    fun recoverySnapshotRejectsAnOldWorkspaceWriterAndReader() = runBlocking {
        val prefs = com.example.data.repository.AppPreferencesRepository.getInstance(context)
        val owner = prefs.taskReminderGeneration.value
        val json = repository.exportFullBackupJson()
        assertTrue(
            com.example.data.backup.LocalDataBackupManager.saveLocalSnapshot(context, json, owner)
        )
        assertEquals(
            json,
            com.example.data.backup.LocalDataBackupManager.loadLatestLocalSnapshot(context),
        )
        prefs.clearAcademicChoices()
        assertNotEquals(owner, prefs.taskReminderGeneration.value)
        assertNull(com.example.data.backup.LocalDataBackupManager.loadLatestLocalSnapshot(context))
        assertFalse(
            com.example.data.backup.LocalDataBackupManager.saveLocalSnapshot(context, json, owner)
        )
        assertTrue(com.example.data.backup.LocalDataBackupManager.saveLocalSnapshot(context, json))
        com.example.data.backup.LocalDataBackupManager.discardStaleSnapshot(context)
        assertEquals(
            json,
            com.example.data.backup.LocalDataBackupManager.loadLatestLocalSnapshot(context),
        )
    }

    @Test
    fun corruptArchiveNeverCallsTheMetadataWriter() = runBlocking {
        val files = StudentV3Files(context)
        importImage(files)
        val archive = File(context.cacheDir, "good.zip")
        val corrupt = File(context.cacheDir, "bad.zip")
        StudentPortableBackup(files)
            .export(Uri.fromFile(archive), { repository.exportFullBackupJson() }, { true })
        ZipFile(archive).use { input ->
            ZipOutputStream(corrupt.outputStream()).use { output ->
                input.entries().asSequence().forEach { entry ->
                    output.putNextEntry(ZipEntry(entry.name))
                    if (entry.name == "metadata.json") output.write("{}".toByteArray())
                    else input.getInputStream(entry).use { it.copyTo(output) }
                    output.closeEntry()
                }
            }
        }
        var committed = false
        assertTrue(
            runCatching {
                    StudentPortableBackup(files).import(Uri.fromFile(corrupt), { true }) {
                        committed = true
                    }
                }
                .isFailure
        )
        assertFalse(committed)
        assertEquals(1, db.studentDao().attachmentsSnapshot().size)
    }

    @Test
    fun resettingWorkspaceClearsEveryNewTableAndTaskTimestampsAreReal() = runBlocking {
        v3.project(ProjectEntity("p", "math", "پروژه"))
        v3.question(question())
        val id =
            db.studentDao()
                .insertTask(TaskEntity(title = "تمرین", courseId = "math", dueDate = "1405/09/01"))
        repository.markTaskCompleted(id)
        assertNotNull(db.studentDao().getTaskById(id)?.completedAt)
        repository.toggleTaskCompletion(db.studentDao().getTaskById(id)!!)
        assertNull(db.studentDao().getTaskById(id)?.completedAt)
        repository.clearToFreshSlate()
        assertTrue(db.studentDao().v3Snapshot() == StudentV3Snapshot())
    }
}

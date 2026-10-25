package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.LocalDataBackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.*
import com.example.domain.util.JalaliDate
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StudentPlannerPersistenceTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: StudentRepository
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build(); repo = StudentRepository(db.studentDao(), db.curriculumDao(), db) }
    @After fun cleanup() { db.close() }
    @Test fun completingRepeatingTaskPersistsOneNextOccurrenceAndUndoDoesNotDuplicateIt() = runBlocking {
        val today = JalaliDate(1405, 7, 20)
        val original = repo.saveTask(TaskEntity(title = "تمرین هفتگی", dueDate = "1405/07/01", repeatEveryDays = 7, reminderTime = "۰۹:۳۰"))
        repo.toggleTaskCompletion(original, today)
        val next = db.studentDao().getNextOccurrence(original.id)!!
        assertEquals("1405/07/22", next.dueDate)
        assertEquals("09:30", next.reminderTime)
        assertEquals(7, next.repeatEveryDays)
        assertFalse(next.isCompleted)
        // Caller may retain the original snapshot; repository always reads the canonical row.
        repo.toggleTaskCompletion(original, today)
        assertFalse(db.studentDao().getTaskById(original.id)!!.isCompleted)
        repo.toggleTaskCompletion(original, today)
        assertEquals(2, db.studentDao().getAllTasksSync().size)
        assertEquals(next, db.studentDao().getNextOccurrence(original.id))
        repo.toggleTaskCompletion(next, JalaliDate(1405, 7, 22))
        assertEquals("1405/07/29", db.studentDao().getNextOccurrence(next.id)!!.dueDate)
    }
    @Test fun archivedSemesterCannotSpawnNewWorkAndDeletingMissingTaskIsSafe() = runBlocking {
        val dao = db.studentDao()
        dao.insertSemester(SemesterEntity(id = "old", title = "پیشین", isCurrent = false, isArchived = true))
        val original = repo.saveTask(TaskEntity(title = "پیشین", dueDate = "1405/07/01", repeatEveryDays = 1, semesterId = "old"))
        repo.toggleTaskCompletion(original, JalaliDate(1405, 7, 1))
        assertNull(dao.getNextOccurrence(original.id))
        repo.deleteTask(original)
        assertNull(repo.toggleTaskCompletion(original))
        assertTrue(dao.getAllTasksSync().isEmpty())
    }
    @Test fun portableBackupRoundTripsPlanningAndLegacyBackupsDefaultToNoRepeatOrReminder() = runBlocking {
        val original = repo.saveTask(TaskEntity(title = "تمرین", dueDate = "1405/07/20", repeatEveryDays = 14, reminderTime = "12:45"))
        repo.toggleTaskCompletion(original, JalaliDate(1405, 7, 20))
        val before = db.studentDao().getAllTasksSync().sortedBy { it.id }
        val json = LocalDataBackupManager.exportCompleteDatabaseJson(db.studentDao())
        db.studentDao().clearTasks()
        assertTrue(repo.restoreFullBackupJson(json).isSuccess)
        assertEquals(before, db.studentDao().getAllTasksSync().sortedBy { it.id })
        db.studentDao().clearTasks()
        val legacy = """{"tasks":[{"id":42,"title":"قدیمی","dueDate":"1405/07/01"}]}"""
        assertTrue(repo.restoreFullBackupJson(legacy).isSuccess)
        val saved = db.studentDao().getTaskById(42)!!
        assertEquals(0, saved.repeatEveryDays)
        assertEquals("", saved.reminderTime)
        assertNull(saved.recurrenceParentId)
    }
    @Test fun malformedBackupIsRejectedBeforeChangingProfileOrReplacingTasks() = runBlocking {
        db.studentDao().insertProfile(StudentProfileEntity(name = "اصلی"))
        val original = repo.saveTask(TaskEntity(title = "اصلی", dueDate = "1405/07/01"))
        val invalid = """{"profile":{"name":"نباید ذخیره شود"},"tasks":[{"id":99,"title":"ناقص","dueDate":"1405/07/01","repeatEveryDays":3}]}"""
        assertTrue(repo.restoreFullBackupJson(invalid).isFailure)
        assertEquals("اصلی", db.studentDao().getProfileSync()!!.name)
        assertEquals(listOf(original), db.studentDao().getAllTasksSync())
    }
    @Test fun productionVersionTenMigratesWithoutLosingExistingTaskOrCourseRelationships() = runBlocking {
        val name = "planner-upgrade-${java.util.UUID.randomUUID()}.db"
        val schema = javaClass.classLoader!!.getResourceAsStream("com.example.data.local.AppDatabase/10.json")!!.bufferedReader().use { org.json.JSONObject(it.readText()).getJSONObject("database") }
        val helper = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(10) {
                override fun onCreate(raw: androidx.sqlite.db.SupportSQLiteDatabase) {
                    val entities = schema.getJSONArray("entities")
                    for (i in 0 until entities.length()) {
                        val entity = entities.getJSONObject(i)
                        val table = entity.getString("tableName")
                        raw.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                        val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                        for (j in 0 until indices.length()) raw.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                    }
                    val setup = schema.getJSONArray("setupQueries")
                    for (i in 0 until setup.length()) raw.execSQL(setup.getString(i))
                    raw.execSQL("INSERT INTO tasks (id, title, courseName, dueDate, isCompleted, courseId, semesterId) VALUES (42, 'تمرین اصلی', 'ریاضی', '1405/07/01', 1, 'math', 'term')")
                }
                override fun onUpgrade(raw: androidx.sqlite.db.SupportSQLiteDatabase, old: Int, new: Int) {}
            }).build())
        helper.writableDatabase
        helper.close()
        val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_10_11).allowMainThreadQueries().build()
        try {
            val task = upgraded.studentDao().getTaskById(42)!!
            assertEquals("math", task.courseId)
            assertEquals("term", task.semesterId)
            assertTrue(task.isCompleted)
            assertEquals(0, task.repeatEveryDays)
            assertEquals("", task.reminderTime)
            assertNull(task.recurrenceParentId)
        } finally { upgraded.close(); context.deleteDatabase(name) }
    }
    @Test fun accountResetChangesReminderGenerationAndRestartKeepsIt() {
        // Fresh instances share this test's storage; avoid a singleton retained
        // from a different Robolectric Application between unrelated tests.
        val prefs = AppPreferencesRepository(context)
        val old = prefs.taskReminderGeneration.value
        prefs.clearAcademicChoices()
        assertNotEquals(old, prefs.taskReminderGeneration.value)
        assertEquals(prefs.taskReminderGeneration.value, AppPreferencesRepository(context).taskReminderGeneration.value)
    }
}

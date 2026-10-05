package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.StudentRepository
import com.example.data.repository.AppPreferencesRepository
import com.example.data.parser.ParsedCourseDraft
import com.example.domain.model.FocusSession
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AcademicCompletionPersistenceTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: StudentRepository
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build(); repo = StudentRepository(db.studentDao(), db.curriculumDao(), db) }
    @After fun cleanup() { db.close() }
    @Test fun firstGradeCanBeInsertedEditedAndZeroPersistsWithoutDuplicates() = runBlocking {
        repo.saveCourse(CourseEntity(id = "math", name = "ریاضی", units = 3, colorHex = "#59652F"))
        val first = GradeEntity(courseId = "math", courseName = "ریاضی", units = 3)
        repo.updateGrade(first.copy(finalGrade = 18.0))
        repo.updateGrade(first.copy(finalGrade = 0.0))
        val saved = db.studentDao().getAllGradesSync()
        assertEquals(1, saved.size)
        assertTrue(saved.single().isRecorded)
        assertEquals(0.0, saved.single().finalGrade, 0.0)
    }
    @Test fun editingCompletedTaskKeepsIdentityCompletionAndSemester() = runBlocking {
        val task = TaskEntity(id = 42, title = "پیشین", dueDate = "1405/07/10", isCompleted = true, semesterId = "sem_active")
        repo.saveTask(task)
        repo.updateTask(task.copy(title = "اصلاح‌شده", dueDate = "1405/07/20"))
        val saved = db.studentDao().getAllTasksSync().single()
        assertEquals(42L, saved.id); assertTrue(saved.isCompleted); assertEquals("sem_active", saved.semesterId)
    }
    @Test fun removingExamActuallyRemovesPersistedRecordAndKeepsClassSessions() = runBlocking {
        val course = CourseEntity(id = "math", name = "ریاضی", units = 3, colorHex = "#59652F", examDate = "1405/10/20", examTime = "08:00")
        val session = CourseSessionEntity(id = "s1", courseId = course.id, day = 0, start = "08:00", end = "10:00")
        repo.saveCourse(course, listOf(session))
        assertEquals(1, db.studentDao().getAllExamsSync().size)
        repo.saveCourse(course.copy(examDate = "", examTime = ""), listOf(session))
        assertTrue(db.studentDao().getAllExamsSync().isEmpty())
        assertEquals(1, db.studentDao().getSessionsForCourseSync(course.id).size)
    }
    @Test fun importingIntoExistingTermPreservesProfileSemesterHistoryAndTotalUnits() = runBlocking {
        val profile = StudentProfileEntity(name = "دانشجوی اصلی", studentId = "40123", university = "دانشگاه من", major = "رشته من", entryYear = 1401, currentSemester = 5, notes = "جزوه مهم", passedUnits = 60)
        db.studentDao().insertProfile(profile)
        db.studentDao().insertSemester(SemesterEntity(id = "actual_term", title = "عنوان دلخواه", academicYear = 1405, termNumber = 5, isCurrent = true))
        repo.saveCourse(CourseEntity(id = "old", name = "درس قبلی", units = 3, colorHex = "#59652F"))
        repo.importParsedCourses(listOf(ParsedCourseDraft(name = "درس جدید", units = 2)))
        val saved = db.studentDao().getProfileSync()!!
        assertEquals(profile.name, saved.name); assertEquals(profile.currentSemester, saved.currentSemester)
        assertEquals(profile.studentId, saved.studentId); assertEquals(profile.notes, saved.notes); assertEquals(profile.entryYear, saved.entryYear)
        assertEquals(5, saved.activeUnits)
        assertEquals("actual_term", db.studentDao().getCurrentSemesterSync()!!.id)
        assertEquals("عنوان دلخواه", db.studentDao().getCurrentSemesterSync()!!.title)
        assertEquals(2, db.studentDao().getCoursesBySemesterSync("actual_term").size)
    }
    @Test fun focusDeadlineRestoresAndCompletionCanBeClaimedOnlyOnce() {
        val preferences = AppPreferencesRepository(context)
        preferences.saveFocusSession(FocusSession(3600, 3600, endsAtMillis = 1_000_000, label = "فیزیک"))
        val restored = AppPreferencesRepository(context)
        assertEquals("فیزیک", restored.focusSession.value.label)
        assertEquals(3600, restored.focusSession.value.durationSeconds)
        assertFalse(restored.completeFocusSession(999_000))
        assertTrue(restored.completeFocusSession(1_000_001))
        assertFalse(restored.completeFocusSession(1_000_002))
        restored.clearAcademicChoices()
    }
    @Test fun selectedPlanAndReminderChoicesRestoreAndClearAcrossSessions() {
        val preferences = AppPreferencesRepository(context)
        val course = com.example.domain.model.EvaluatedCurriculumCourse("math", "M1", "ریاضی", 3, "اصلی", 1, com.example.domain.model.CourseState.BLOCKED,
            com.example.domain.model.BlockedReason(missingPrerequisiteNames = listOf("پایه"), explanation = "نیاز به پیش‌نیاز"))
        val plan = com.example.domain.model.SemesterPlan("personal", "ترم من", 3, listOf(course), isDraft = false, isActive = true, tradeOffs = listOf("بار سبک"))
        preferences.saveSemesterPlan(plan)
        preferences.setExamReminder("exam-42", true)
        preferences.saveTargetGpaGoal(18.25)
        val restored = AppPreferencesRepository(context)
        assertEquals(plan, restored.selectedSemesterPlan.value)
        assertEquals(setOf("exam-42"), restored.examReminderIds.value)
        assertEquals(18.25, restored.targetGpaGoal.value!!, 0.0)
        assertTrue(runCatching { restored.saveTargetGpaGoal(Double.NaN) }.isFailure)
        restored.clearAcademicChoices()
        val cleared = AppPreferencesRepository(context)
        assertNull(cleared.selectedSemesterPlan.value)
        assertTrue(cleared.examReminderIds.value.isEmpty())
        assertNull(cleared.targetGpaGoal.value)
    }
    @Test fun upgradeAddsRecordedFlagWithoutDeletingGrades() {
        val config = SupportSQLiteOpenHelper.Configuration.builder(context).name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                override fun onCreate(db: SupportSQLiteDatabase) { db.execSQL("CREATE TABLE grades (id INTEGER PRIMARY KEY NOT NULL, midtermGrade REAL NOT NULL, finalGrade REAL NOT NULL)"); db.execSQL("INSERT INTO grades VALUES (1, 4, 14), (2, 0, 0)") }
                override fun onUpgrade(db: SupportSQLiteDatabase, old: Int, new: Int) {}
            }).build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val raw = helper.writableDatabase
        AppDatabase.MIGRATION_9_10.migrate(raw)
        raw.query("SELECT isRecorded FROM grades ORDER BY id").use { cursor -> assertTrue(cursor.moveToFirst()); assertEquals(1, cursor.getInt(0)); assertTrue(cursor.moveToNext()); assertEquals(0, cursor.getInt(0)) }
        helper.close()
    }
    @Test fun productionSchemaNineUpgradesThroughRoomValidationAndRetainsRecords() = runBlocking {
        val name = "upgrade-${java.util.UUID.randomUUID()}.db"
        val schema = javaClass.classLoader!!.getResourceAsStream("com.example.data.local.AppDatabase/9.json")!!.bufferedReader().use { org.json.JSONObject(it.readText()).getJSONObject("database") }
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(9) {
                override fun onCreate(raw: SupportSQLiteDatabase) {
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
                    raw.execSQL("INSERT INTO grades (id, courseName, units, midtermGrade, finalGrade, courseId) VALUES (1, 'ریاضی', 3, 6, 12, 'math'), (2, 'فیزیک', 2, 0, 0, 'physics')")
                }
                override fun onUpgrade(raw: SupportSQLiteDatabase, old: Int, new: Int) {}
            }).build())
        helper.writableDatabase
        helper.close()
        val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_9_10).allowMainThreadQueries().build()
        try {
            val grades = upgraded.studentDao().getAllGradesSync().sortedBy { it.id }
            assertEquals(2, grades.size)
            assertEquals("math", grades.first().courseId)
            assertTrue(grades.first().isRecorded)
            assertFalse(grades.last().isRecorded)
            assertEquals(18.0, grades.first().midtermGrade + grades.first().finalGrade, 0.0)
        } finally { upgraded.close(); context.deleteDatabase(name) }
    }
    @Test fun personalCurriculumAndItsReferenceIdentitySurviveCompleteBackup() = runBlocking {
        db.studentDao().insertProfile(StudentProfileEntity(name = "دانشجوی کامپیوتر", notes = "یادداشت مهم"))
        repo.savePersonalCurriculum("دانشگاه من", "مهندسی کامپیوتر", 1405, 140, "M1 | ریاضی ۱ | ۳ | ۱\nM2 | ریاضی ۲ | ۳ | ۲ | M1")
        val profile = db.studentDao().getProfileSync()!!
        assertTrue(profile.majorId!!.startsWith("USER_MAJOR_"))
        assertEquals("یادداشت مهم", profile.notes)
        assertEquals("ریاضی ۱", db.curriculumDao().getAllCurriculumCoursesSync().find { it.code == "M2" }!!.prerequisites)
        val backup = repo.exportFullBackupJson()
        val restored = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            StudentRepository(restored.studentDao(), restored.curriculumDao(), restored).restoreFullBackupJson(backup).getOrThrow()
            assertEquals(profile.majorId, restored.studentDao().getProfileSync()!!.majorId)
            assertEquals(1, restored.curriculumDao().getAllCurriculumVersionsSync().size)
            assertEquals(2, restored.curriculumDao().getAllCurriculumCoursesSync().size)
        } finally { restored.close() }
    }
    @Test fun failedBackupRestoreRollsBackEarlierWrites() = runBlocking {
        db.studentDao().insertProfile(StudentProfileEntity(name = "نام اصلی"))
        val payload = """{"profile":{"name":"نام خراب"},"sessions":[{"id":"orphan","courseId":"missing","day":0,"start":"08:00","end":"10:00"}]}"""
        assertTrue(repo.restoreFullBackupJson(payload).isFailure)
        assertEquals("نام اصلی", db.studentDao().getProfileSync()!!.name)
    }
    @Test fun archivingUsesWholeRecordedGradeAndIncludesARealZero() = runBlocking {
        db.studentDao().insertSemester(SemesterEntity("active", "ترم اصلی", academicYear = 1405, termNumber = 1, isCurrent = true))
        val a = CourseEntity(id = "a", name = "ریاضی", units = 3, colorHex = "#59652F")
        val b = a.copy(id = "b", name = "فیزیک")
        repo.saveCourse(a); repo.saveCourse(b)
        repo.updateGrade(GradeEntity(courseId = "a", courseName = a.name, units = 3, midtermGrade = 6.0, finalGrade = 12.0))
        repo.updateGrade(GradeEntity(courseId = "b", courseName = b.name, units = 3))
        repo.startNewSemester(com.example.domain.usecase.NewSemesterRequest("ترم بعد", 1405, 2))
        assertEquals(9.0, db.studentDao().getSemesterById("active")!!.gpa!!, 0.001)
    }
}

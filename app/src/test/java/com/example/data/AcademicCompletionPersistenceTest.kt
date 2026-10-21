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
    @Test fun identityChangesCannotKeepAnOldCurriculumOrMatchAnotherUniversity() = runBlocking {
        db.curriculumDao().insertUniversities(listOf(UniversityEntity("old_uni", "دانشگاه قبلی", "قبلی")))
        db.curriculumDao().insertMajors(listOf(MajorEntity("old_major", "old_uni", "old_faculty", "دانشکده قبلی", "ریاضی")))
        val original = StudentProfileEntity(university = "دانشگاه قبلی", major = "ریاضی", universityId = "old_uni",
            majorId = "old_major", facultyId = "old_faculty", currentSemester = 3, notes = "سوابق شخصی")
        repeat(2) { mode ->
            db.studentDao().insertProfile(original)
            if (mode == 0) repo.setOnboardingCompleted(true, university = "دانشگاه جدید", major = "ریاضی")
            else repo.importParsedCourses(listOf(ParsedCourseDraft(name = "جبر")), university = "دانشگاه جدید", major = "ریاضی")
            val saved = db.studentDao().getProfileSync()!!
            assertNull(saved.universityId)
            assertNull(saved.majorId)
            assertNull(saved.facultyId)
            assertEquals("سوابق شخصی", saved.notes)
        }
        db.studentDao().insertProfile(original)
        repo.setOnboardingCompleted(true, university = "دانشگاه قبلی", major = "رشته جدید")
        val saved = db.studentDao().getProfileSync()!!
        assertEquals("old_uni", saved.universityId)
        assertNull(saved.majorId)
        assertNull(saved.facultyId)
    }
    @Test fun revisitingSetupPreservesRecordsAndDoesNotDuplicateCourses() = runBlocking {
        val dao = db.studentDao()
        dao.insertProfile(StudentProfileEntity(name = "دانشجو", notes = "جزوه مهم", declaredGpa = 17.25))
        dao.insertSemester(SemesterEntity(id = "my_term", title = "ترم من", isCurrent = true))
        repo.saveCourse(CourseEntity(id = "math", name = "ریاضی", courseCode = "101", semesterId = "my_term", units = 3))
        repo.updateGrade(GradeEntity(courseId = "math", courseName = "ریاضی", finalGrade = 18.0))
        dao.insertAttendance(AttendanceEntity("math", "ریاضی", 2, 5))
        val selection = listOf(CurriculumCourseEntity("ref", code = "101", name = "ریاضی", units = 3, courseType = "پایه", recommendedSemester = 1))
        repeat(2) {
            repo.completeQuickAcademicSetup(university = "دانشگاه من", major = "رشته من", entryYear = 1403,
                currentSemester = 3, passedCredits = 42, currentGpa = 17.25, selectedCourses = selection, replaceExisting = false)
        }
        assertEquals(listOf("math"), dao.getAllCoursesIncludingArchivedSync().map { it.id })
        assertEquals(18.0, dao.getAllGradesSync().single().finalGrade, 0.0)
        assertEquals(2, dao.getAllAttendanceSync().single().absentCount)
        assertEquals("جزوه مهم", dao.getProfileSync()!!.notes)
        assertEquals(3, dao.getProfileSync()!!.activeUnits)
        assertEquals("my_term", dao.getCurrentSemesterSync()!!.id)
        assertEquals("ترم من", dao.getCurrentSemesterSync()!!.title)
    }
    @Test fun unscheduledImportDoesNotInventSessionsAndReimportKeepsGradesAndAbsences() = runBlocking {
        val draft = ParsedCourseDraft(name = "ریاضی", units = 3, courseCode = "101")
        repo.importParsedCourses(listOf(draft))
        val dao = db.studentDao()
        val course = dao.getAllCoursesIncludingArchivedSync().single()
        assertTrue(dao.getSessionsForCourseSync(course.id).isEmpty())
        repo.updateGrade(dao.getAllGradesSync().single().copy(finalGrade = 17.0))
        dao.insertAttendance(AttendanceEntity(course.id, course.name, 2, 5))
        repo.importParsedCourses(listOf(draft))
        assertEquals(1, dao.getAllCoursesIncludingArchivedSync().size)
        assertEquals(17.0, dao.getAllGradesSync().single().finalGrade, 0.0)
        assertEquals(2, dao.getAllAttendanceSync().single().absentCount)
        assertEquals(3, dao.getProfileSync()!!.activeUnits)
        // A later registration import can fill missing times without losing the grade/limit.
        val scheduled = draft.copy(dayOfWeek = 2, startTime = "09:00", endTime = "11:00", location = "کلاس ۴")
        repeat(2) { repo.importParsedCourses(listOf(scheduled)) }
        assertEquals(course.id, dao.getAllCoursesIncludingArchivedSync().single().id)
        assertEquals(1, dao.getSessionsForCourseSync(course.id).size)
        assertEquals("09:00", dao.getSessionsForCourseSync(course.id).single().start)
        assertEquals(17.0, dao.getAllGradesSync().single().finalGrade, 0.0)
        assertEquals(5, dao.getAllAttendanceSync().single().maxAllowed)
    }
    @Test fun invalidGpaIsRejectedBeforeProfileOrSetupChanges() = runBlocking {
        val original = StudentProfileEntity(name = "اصلی", declaredGpa = 17.0)
        repo.updateProfile(original)
        try { repo.updateProfile(original.copy(declaredGpa = Double.NaN)); fail("Invalid GPA accepted") }
        catch (_: IllegalArgumentException) { }
        assertEquals(17.0, db.studentDao().getProfileSync()!!.declaredGpa!!, 0.0)
    }
    @Test fun attendanceEditsKeepCustomLimitAndReadTheLatestStoredCount() = runBlocking {
        repo.saveCourse(CourseEntity(id = "math", name = "ریاضی", units = 3))
        repo.editAttendance("math", limit = 5)
        repeat(4) { repo.editAttendance("math", delta = 1) }
        val updated = repo.editAttendance("math", count = 2)
        assertEquals(2, updated.absentCount)
        assertEquals(5, updated.maxAllowed)
        try { repo.editAttendance("math", limit = 0); fail("Invalid limit accepted") }
        catch (_: IllegalArgumentException) { }
        assertEquals(5, db.studentDao().getAttendanceByCourseId("math")!!.maxAllowed)
    }
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
        val deadline = System.currentTimeMillis()
        preferences.saveFocusSession(FocusSession(3600, 3600, endsAtMillis = deadline, label = "فیزیک"))
        val restored = AppPreferencesRepository(context)
        assertEquals("فیزیک", restored.focusSession.value.label)
        assertEquals(3600, restored.focusSession.value.durationSeconds)
        val minutesBefore = restored.todayFocusMinutes.value
        assertFalse(restored.completeFocusSession(deadline - 1_000))
        assertTrue(restored.completeFocusSession(deadline + 1))
        assertFalse(restored.completeFocusSession(deadline + 2))
        assertFalse(preferences.completeFocusSession(deadline + 2))
        assertEquals(minutesBefore + 60, restored.todayFocusMinutes.value)
        assertEquals(restored.todayFocusMinutes.value, AppPreferencesRepository(context).todayFocusMinutes.value)
        restored.clearAcademicChoices()
    }
    @Test fun delayedFocusWorkerCreditsTheDayOfCompletionAndRestoresHistory() {
        val preferences = AppPreferencesRepository(context)
        preferences.clearAcademicChoices()
        val deadline = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }.timeInMillis
        preferences.saveFocusSession(FocusSession(1500, 1500, endsAtMillis = deadline, label = "ریاضی"))
        assertTrue(preferences.completeFocusSession(System.currentTimeMillis()))
        assertEquals(0, preferences.todayFocusMinutes.value)
        val history = AppPreferencesRepository(context).studyHistory.value
        assertEquals(1, history.size)
        assertEquals(25, history.single().minutes)
        assertEquals(1, history.single().sessions)
        preferences.clearAcademicChoices()
        assertTrue(AppPreferencesRepository(context).studyHistory.value.isEmpty())
    }
    @Test fun studyPreferencesAndHistoryTravelWithBackupAndInvalidValuesAreRejected() = runBlocking {
        val preferences = AppPreferencesRepository(context)
        preferences.clearAcademicChoices()
        val settings = com.example.domain.model.StudySettingsSnapshot(180, 45, 18.0,
            listOf(com.example.domain.model.StudyDay("2026-10-19", 90, 2)))
        preferences.restoreStudySettings(settings)
        val json = com.example.data.backup.LocalDataBackupManager.withStudySettings(repo.exportFullBackupJson(), preferences.exportStudySettings())
        val loaded = com.example.data.backup.LocalDataBackupManager.readStudySettings(json)!!
        assertEquals(settings, loaded)
        assertNull(com.example.data.backup.LocalDataBackupManager.readStudySettings(repo.exportFullBackupJson()))
        preferences.clearAcademicChoices()
        preferences.restoreStudySettings(loaded)
        val restored = AppPreferencesRepository(context)
        assertEquals(180, restored.dailyStudyGoalMinutes.value)
        assertEquals(45, restored.defaultFocusMinutes.value)
        assertEquals(90, restored.studyHistory.value.single().minutes)
        assertFalse(restored.focusSession.value.isRunning)
        assertEquals(2700, restored.focusSession.value.durationSeconds)
        assertTrue(runCatching { preferences.restoreStudySettings(settings.copy(targetGpa = Double.NaN)) }.isFailure)
        assertTrue(runCatching { preferences.restoreStudySettings(settings.copy(history = listOf(com.example.domain.model.StudyDay("2026-02-30", 90, 2)))) }.isFailure)
        assertEquals(loaded, restored.exportStudySettings())
        preferences.clearAcademicChoices()
    }
    @Test fun selectedPlanAndReminderChoicesRestoreAndClearAcrossSessions() {
        val preferences = AppPreferencesRepository(context)
        val course = com.example.domain.model.EvaluatedCurriculumCourse("math", "M1", "ریاضی", 3, "اصلی", 1, com.example.domain.model.CourseState.BLOCKED,
            com.example.domain.model.BlockedReason(missingPrerequisiteNames = listOf("پایه"), explanation = "نیاز به پیش‌نیاز"))
        val plan = com.example.domain.model.SemesterPlan("personal", "ترم من", 3, listOf(course), isDraft = false, isActive = true, tradeOffs = listOf("بار سبک"))
        preferences.saveSemesterPlan(plan)
        preferences.setExamReminder("exam-42", true)
        preferences.saveTargetGpaGoal(18.25)
        preferences.setDailyStudyGoalMinutes(180)
        preferences.setDefaultFocusMinutes(45)
        val restored = AppPreferencesRepository(context)
        assertEquals(plan, restored.selectedSemesterPlan.value)
        assertEquals(setOf("exam-42"), restored.examReminderIds.value)
        assertEquals(18.25, restored.targetGpaGoal.value!!, 0.0)
        assertEquals(180, restored.dailyStudyGoalMinutes.value)
        assertEquals(45, restored.defaultFocusMinutes.value)
        assertTrue(runCatching { restored.saveTargetGpaGoal(Double.NaN) }.isFailure)
        restored.clearAcademicChoices()
        val cleared = AppPreferencesRepository(context)
        assertNull(cleared.selectedSemesterPlan.value)
        assertTrue(cleared.examReminderIds.value.isEmpty())
        assertNull(cleared.targetGpaGoal.value)
        assertEquals(120, cleared.dailyStudyGoalMinutes.value)
        assertEquals(25, cleared.defaultFocusMinutes.value)
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

package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.LocalDataBackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.SemesterEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.repository.AppPreferencesRepository
import com.example.data.repository.StudentRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DataPersistenceAndBackupTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: StudentRepository
    private lateinit var context: Context
    private lateinit var prefs: AppPreferencesRepository

    @Before
    fun createDb() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = StudentRepository(db.studentDao(), db.curriculumDao(), db)
        prefs = AppPreferencesRepository.getInstance(context)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testCompleteDatabaseExportAndRestoreIntegrity() = runBlocking {
        val dao = db.studentDao()

        // 1. Insert Profile
        val profile = StudentProfileEntity(
            id = 1,
            name = "علی محمدی",
            studentId = "401123456",
            major = "مهندسی کامپیوتر",
            university = "دانشگاه صنعتی امیرکبیر",
            entryYear = 1401,
            currentSemester = 3,
            passedUnits = 40,
            activeUnits = 18,
            isOnboardingCompleted = true
        )
        dao.insertProfile(profile)

        // 2. Insert Semester
        val sem = SemesterEntity(
            id = "sem_1401_3",
            title = "نیمسال اول 1402-1403",
            year = 1402,
            academicYear = 1402,
            semesterNumber = 3,
            termNumber = 3,
            isCurrent = true,
            isArchived = false,
            status = "ACTIVE"
        )
        dao.insertSemester(sem)

        // 3. Insert Course
        val course = CourseEntity(
            id = "course_os",
            name = "سیستم‌های عامل",
            colorHex = "#3B82F6",
            units = 3,
            semesterId = sem.id,
            courseCode = "CS204"
        )
        dao.insertCourse(course)

        // 4. Insert Attendance & Grade
        dao.insertAttendance(
            AttendanceEntity(
                courseId = course.id,
                courseName = course.name,
                absentCount = 1,
                maxAllowed = 3
            )
        )
        dao.insertGrade(
            GradeEntity(
                courseId = course.id,
                courseName = course.name,
                units = course.units,
                midtermGrade = 18.5,
                finalGrade = 19.0
            )
        )

        // 5. Insert Task
        dao.insertTask(
            TaskEntity(
                id = 101,
                title = "پروژه صف‌بندی CPU",
                courseName = course.name,
                dueDate = "1402/08/20",
                isCompleted = false,
                courseId = course.id,
                semesterId = sem.id
            )
        )

        // 6. Export Full JSON Backup
        val jsonBackup = LocalDataBackupManager.exportCompleteDatabaseJson(dao, db.curriculumDao())
        assertNotNull(jsonBackup)
        assertTrue(jsonBackup.contains("علی محمدی"))
        assertTrue(jsonBackup.contains("سیستم‌های عامل"))
        assertTrue(jsonBackup.contains("پروژه صف‌بندی CPU"))

        // 7. Wipe current tables
        dao.clearCourses()
        dao.clearTasks()

        // 8. Restore from backup
        val restoreResult = LocalDataBackupManager.restoreDatabaseFromJson(jsonBackup, dao, db.curriculumDao())
        assertTrue(restoreResult.isSuccess)

        // 9. Verify restored state
        val restoredProfile = dao.getProfileSync()
        assertNotNull(restoredProfile)
        assertEquals("علی محمدی", restoredProfile?.name)
        assertEquals("401123456", restoredProfile?.studentId)

        val restoredCourses = dao.getAllCoursesIncludingArchivedSync()
        assertEquals(1, restoredCourses.size)
        assertEquals("سیستم‌های عامل", restoredCourses.first().name)

        val restoredAttendance = dao.getAttendanceByCourseId(course.id)
        assertNotNull(restoredAttendance)
        assertEquals(1, restoredAttendance?.absentCount)

        val restoredTasks = dao.getAllTasksSync()
        assertEquals(1, restoredTasks.size)
        assertEquals("پروژه صف‌بندی CPU", restoredTasks.first().title)
    }

    @Test
    fun testPreferencesSaveTimestampTracking() {
        val initialTimestamp = prefs.lastSuccessfulSaveTimestamp.value
        prefs.recordSuccessfulSave()
        val updatedTimestamp = prefs.lastSuccessfulSaveTimestamp.value
        assertTrue(updatedTimestamp >= initialTimestamp)
        assertTrue(updatedTimestamp > 0L)
    }

    @Test
    fun testImportCompleteDatabaseJsonRefreshesProfileUpdatedAt() = runBlocking {
        val dao = db.studentDao()
        val staleTimestamp = 1000L // Old timestamp from previous export

        val oldProfile = StudentProfileEntity(
            id = 1,
            name = "سارا احمدی",
            studentId = "400998877",
            university = "دانشگاه صنعتی امیرکبیر",
            major = "مهندسی کامپیوتر",
            updatedAt = staleTimestamp
        )

        val payload = com.example.data.backup.StudentOSBackupPayload(
            profile = oldProfile
        )
        val moshi = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val jsonPayload = moshi.adapter(com.example.data.backup.StudentOSBackupPayload::class.java).toJson(payload)

        val beforeImportTime = System.currentTimeMillis()

        val result = LocalDataBackupManager.importCompleteDatabaseJson(
            jsonString = jsonPayload,
            dao = dao,
            curriculumDao = db.curriculumDao()
        )

        assertTrue(result.isSuccess)

        val persistedProfile = dao.getProfileSync()
        assertNotNull(persistedProfile)
        assertEquals("سارا احمدی", persistedProfile?.name)
        // Assert the persisted profile has a fresh post-import timestamp, strictly greater than the stale timestamp
        assertTrue(
            "Persisted updatedAt (${persistedProfile?.updatedAt}) must be >= beforeImportTime ($beforeImportTime) and not stale ($staleTimestamp)",
            (persistedProfile?.updatedAt ?: 0L) >= beforeImportTime
        )
        assertTrue((persistedProfile?.updatedAt ?: 0L) > staleTimestamp)
    }
    @Test
    fun testUndoActionCarriesRecoverySnapshotAndGeneration() {
        val action = com.example.ui.StudentViewModel.UndoAction(
            message = "واگردانی قابل واگردانی است.",
            snapshot = """{"profile":{"name":"Student"}}""",
            generation = 7L
        )

        assertEquals("واگردانی قابل واگردانی است.", action.message)
        assertTrue(action.snapshot.contains("Student"))
        assertEquals(7L, action.generation)
    }
    @Test
    fun testOnboardingPersistsReferenceIdsForKnownUniversityAndMajor() = runBlocking {
        AppDatabase.populateInitialData(db.studentDao(), db.curriculumDao())
        repository.setOnboardingCompleted(
            completed = true,
            name = "دانشجو",
            university = "دانشگاه صنعتی امیرکبیر",
            major = "مهندسی شیمی",
            entryYear = 1404,
            currentSemester = 1
        )

        val profile = db.studentDao().getProfileSync()
        assertEquals("UNI_AUT", profile?.universityId)
        assertEquals("MAJ_AUT_CHEM_ENG", profile?.majorId)
        assertEquals("FAC_AUT_CHEM_OIL", profile?.facultyId)
    }

    @Test
    fun testFreshSlateDefaultsDoNotCreateSyntheticIdentity() = runBlocking<Unit> {
        repository.clearToFreshSlate()

        val profile = db.studentDao().getProfileSync()
        assertNotNull(profile)
        assertEquals("دانشجو", profile?.name)
        assertEquals("", profile?.studentId)
        assertEquals("", profile?.university)
        assertEquals("", profile?.major)
        assertEquals("", profile?.faculty)
        assertEquals("", profile?.term)
        assertEquals(0, profile?.entryYear)
        assertEquals(0, profile?.currentSemester)
        assertFalse(profile?.isOnboardingCompleted ?: true)
    }

    @Test
    fun testNewProfileDefaultsContainNoPersonalAcademicIdentity() {
        val profile = com.example.data.local.entity.StudentProfileEntity()
        assertEquals("دانشجو", profile.name)
        assertEquals("", profile.studentId)
        assertEquals("", profile.university)
        assertEquals("", profile.major)
        assertEquals("", profile.faculty)
        assertEquals("", profile.term)
        assertEquals(0, profile.entryYear)
        assertEquals(0, profile.currentSemester)
    }

    @Test
    fun testSavingCourseWithoutDetailsDoesNotCreateScheduleOrZeroGrade() = runBlocking {
        val course = CourseEntity(
            id = "course_without_details",
            name = "درس بدون جزئیات",
            units = 3,
            semesterId = "current"
        )

        repository.saveCourse(course)

        assertEquals(1, db.studentDao().getAllCoursesIncludingArchivedSync().size)
        assertEquals(0, db.studentDao().getAllSessionsSync().size)
        assertEquals(0, db.studentDao().getAllGradesSync().size)
        assertNotNull(db.studentDao().getAttendanceByCourseId(course.id))
    }

    @Test
    fun testQuickAcademicSetupDoesNotFabricateScheduleGradesExamsOrHistory() = runBlocking {
        val curriculumCourse = com.example.data.local.entity.CurriculumCourseEntity(
            id = "CURR_TEST_1",
            majorId = "MAJ_TEST",
            code = "TEST101",
            name = "درس آزمایشی",
            units = 3,
            courseType = "تخصصی",
            recommendedSemester = 1
        )

        repository.completeQuickAcademicSetup(
            name = "دانشجو",
            university = "",
            major = "",
            entryYear = 1404,
            currentSemester = 1,
            passedCredits = 0,
            currentGpa = 0.0,
            selectedCourses = listOf(curriculumCourse)
        )

        val courses = db.studentDao().getAllCoursesIncludingArchivedSync()
        assertEquals(1, courses.size)
        assertEquals(0, db.studentDao().getAllSessionsSync().size)
        assertEquals(0, db.studentDao().getAllGradesSync().size)
        assertEquals(0, db.studentDao().getAllExamsSync().size)
        assertEquals(0, db.studentDao().getStudentAttemptsSync(1).size)
    }

    @Test
    fun testClearAllUserDataPurgesPersonalState() = runBlocking {
        val dao = db.studentDao()
        val semester = SemesterEntity(
            id = "sem_user_1",
            title = "ترم کاربر",
            academicYear = 1404,
            termNumber = 1,
            isCurrent = true
        )
        dao.insertSemester(semester)
        dao.insertProfile(
            StudentProfileEntity(
                id = 1,
                name = "کاربر واقعی",
                studentId = "USER-1",
                isOnboardingCompleted = true
            )
        )
        val course = CourseEntity(
            id = "purge_course",
            name = "درس خصوصی",
            units = 3,
            semesterId = semester.id
        )
        dao.insertCourse(course)
        dao.insertTask(
            TaskEntity(
                id = 9001,
                title = "تکلیف خصوصی",
                courseName = course.name,
                courseId = course.id,
                dueDate = "2026-10-01T12:00:00",
                semesterId = semester.id
            )
        )
        dao.upsertSyncMetadata(
            com.example.data.local.entity.SyncMetadataEntity("courses", 1234L)
        )

        repository.clearAllUserData()

        assertEquals(0, dao.getAllCoursesIncludingArchivedSync().size)
        assertEquals(0, dao.getAllTasksSync().size)
        assertEquals(0, dao.getAllSemestersSync().size)
        assertEquals(null, dao.getProfileSync())
        assertEquals(null, dao.getSyncUpdatedAt("courses"))
    }
}

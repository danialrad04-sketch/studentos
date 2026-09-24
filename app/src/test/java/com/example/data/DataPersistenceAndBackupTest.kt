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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
}

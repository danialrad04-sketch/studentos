package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ProfessorEntity
import com.example.data.local.entity.SemesterEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.util.DateTimeNormalizer
import com.example.data.repository.StudentRepository
import com.example.domain.usecase.NewSemesterRequest
import com.example.domain.usecase.SemesterTransitionUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SemesterSystemAndDataIntegrityTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: StudentRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = StudentRepository(db.studentDao(), db.curriculumDao(), db)
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testSemesterEntityCreationAndMultiSemesterCourses() = runBlocking {
        val dao = db.studentDao()

        // 1. Create Semester 1 (1403-1)
        val sem1 = SemesterEntity(
            id = "sem_1403_1",
            title = "ترم ۱ (پاییز ۱۴۰۳)",
            academicYear = 1403,
            termNumber = 1,
            isCurrent = true,
            isArchived = false,
            status = "ACTIVE"
        )
        dao.insertSemester(sem1)

        // 2. Add course to Semester 1
        val c1 = CourseEntity(
            id = "course_math_1",
            name = "ریاضی عمومی ۱",
            colorHex = "#3B82F6",
            units = 3,
            semesterId = "sem_1403_1",
            courseCode = "MATH101",
            examDate = "1403/10/20",
            examTime = "09:00"
        )
        repository.saveCourse(c1)

        // Check course in active semester
        val currentCourses = dao.getCurrentSemesterCourses().first()
        assertEquals(1, currentCourses.size)
        assertEquals("course_math_1", currentCourses[0].id)

        // Verify Attendance auto-created with stable courseId
        val att = dao.getAttendanceByCourseId("course_math_1")
        assertNotNull(att)
        assertEquals("course_math_1", att?.courseId)
        assertEquals(0, att?.absentCount)

        // Verify Grade auto-created with stable courseId
        val grade = dao.getGradeByCourseId("course_math_1")
        assertNotNull(grade)
        assertEquals("course_math_1", grade?.courseId)

        // Verify Exam auto-created with stable courseId
        val exam = dao.getExamByCourseId("course_math_1")
        assertNotNull(exam)
        assertEquals("course_math_1", exam?.courseId)
        assertEquals("1403/10/20", exam?.date)
    }

    @Test
    fun testTransactionSafeStartNewSemester() = runBlocking {
        val dao = db.studentDao()

        // Setup Semester 1 with 2 courses
        val sem1 = SemesterEntity(
            id = "sem_1403_1",
            title = "ترم ۱ (پاییز ۱۴۰۳)",
            academicYear = 1403,
            termNumber = 1,
            isCurrent = true,
            isArchived = false,
            status = "ACTIVE"
        )
        dao.insertSemester(sem1)

        val c1 = CourseEntity(
            id = "c_thermo",
            name = "ترمودینامیک ۱",
            colorHex = "#3B82F6",
            units = 3,
            semesterId = "sem_1403_1"
        )
        val c2 = CourseEntity(
            id = "c_fluids",
            name = "مکانیک سیالات ۱",
            colorHex = "#10B981",
            units = 3,
            semesterId = "sem_1403_1"
        )
        repository.saveCourse(c1)
        repository.saveCourse(c2)

        // Add grades for semester 1
        dao.updateGrade(GradeEntity(courseId = "c_thermo", courseName = "ترمودینامیک ۱", units = 3, midtermGrade = 6.0, finalGrade = 12.0))
        dao.updateGrade(GradeEntity(courseId = "c_fluids", courseName = "مکانیک سیالات ۱", units = 3, midtermGrade = 5.5, finalGrade = 13.5))

        // Trigger New Semester Flow (Archive Sem 1 -> Create Sem 2)
        val newCourses = listOf(
            CourseEntity(
                id = "c_thermo_2",
                name = "ترمودینامیک ۲",
                colorHex = "#F59E0B",
                units = 3,
                courseCode = "CE201"
            )
        )
        val result = repository.startNewSemester(
            NewSemesterRequest(
                title = "ترم ۲ (بهار ۱۴۰۴)",
                academicYear = 1403,
                termNumber = 2,
                newCourses = newCourses
            )
        )

        // 1. Verify previous semester was archived, NOT deleted
        assertNotNull(result.previousSemester)
        val archivedSem1 = dao.getSemesterById("sem_1403_1")
        assertNotNull(archivedSem1)
        assertTrue(archivedSem1?.isArchived == true)
        assertFalse(archivedSem1?.isCurrent == true)
        assertEquals("ARCHIVED", archivedSem1?.status)
        assertEquals(6, archivedSem1?.totalUnits)

        // 2. Verify previous courses are still intact in DB for history/transcripts
        val pastCourses = dao.getCoursesBySemesterSync("sem_1403_1")
        assertEquals(2, pastCourses.size)
        assertTrue(pastCourses.all { it.isArchived })

        // 3. Verify current semester courses only contains the new semester's courses
        val activeCourses = dao.getCurrentSemesterCourses().first()
        assertEquals(1, activeCourses.size)
        assertEquals("ترمودینامیک ۲", activeCourses[0].name)
        assertFalse(activeCourses[0].isArchived)

        // 4. Verify new active semester
        val currentSem = dao.getCurrentSemesterSync()
        assertNotNull(currentSem)
        assertEquals("ترم ۲ (بهار ۱۴۰۴)", currentSem?.title)
        assertTrue(currentSem?.isCurrent == true)
        assertFalse(currentSem?.isArchived == true)
    }

    @Test
    fun testStableIdsPreserveRelationsWhenCourseRenamed() = runBlocking {
        val dao = db.studentDao()

        val c = CourseEntity(
            id = "c_stable_1",
            name = "شیمی فیزیک",
            colorHex = "#EF4444",
            units = 3,
            semesterId = "sem_1"
        )
        repository.saveCourse(c)

        // Update attendance by courseId
        val att = dao.getAttendanceByCourseId("c_stable_1")
        assertNotNull(att)
        dao.updateAttendance(att!!.copy(absentCount = 2))

        // Update task with courseId
        dao.insertTask(TaskEntity(id = 100, title = "حل تمرین شیمی فیزیک", dueDate = "1403/10/25", courseId = "c_stable_1"))

        // Rename course
        val renamed = c.copy(name = "شیمی فیزیک و ترمودینامیک آماری")
        repository.saveCourse(renamed)

        // Relations must remain intact because they are keyed on c_stable_1
        val updatedAtt = dao.getAttendanceByCourseId("c_stable_1")
        assertEquals(2, updatedAtt?.absentCount)
        assertEquals("شیمی فیزیک و ترمودینامیک آماری", updatedAtt?.courseName)

        val tasks = dao.getTasksByCourseId("c_stable_1").first()
        assertEquals(1, tasks.size)
        assertEquals(100L, tasks[0].id)
    }

    @Test
    fun testDuplicateCourseDetection() = runBlocking {
        val dao = db.studentDao()

        val c1 = CourseEntity(
            id = "c_1",
            name = "کنترل فرآیند",
            colorHex = "#8B5CF6",
            units = 3,
            semesterId = "sem_1",
            courseCode = "CE401"
        )
        repository.saveCourse(c1)

        // Same course code in same semester -> Duplicate
        val duplicateByCode = CourseEntity(
            id = "c_other",
            name = "کنترل فرآیندها در مهندسی شیمی",
            colorHex = "#8B5CF6",
            units = 3,
            semesterId = "sem_1",
            courseCode = "CE401"
        )
        assertTrue(repository.isDuplicateCourse(duplicateByCode))

        // Same name in same semester -> Duplicate
        val duplicateByName = CourseEntity(
            id = "c_other_2",
            name = "کنترل فرآیند",
            colorHex = "#8B5CF6",
            units = 3,
            semesterId = "sem_1",
            courseCode = ""
        )
        assertTrue(repository.isDuplicateCourse(duplicateByName))

        // Different course -> Not duplicate
        val different = CourseEntity(
            id = "c_diff",
            name = "طراحی رآکتور",
            colorHex = "#0EA5E9",
            units = 3,
            semesterId = "sem_1",
            courseCode = "CE402"
        )
        assertFalse(repository.isDuplicateCourse(different))
    }

    @Test
    fun testOrphanDataCleanup() = runBlocking {
        val dao = db.studentDao()

        // Insert a real course
        val c = CourseEntity(id = "c_valid", name = "ریاضی ۲", colorHex = "#000", units = 3)
        repository.saveCourse(c)

        // Insert orphan attendance, grade, task, and exam pointing to non-existent course
        dao.insertAttendance(AttendanceEntity(courseId = "c_deleted", courseName = "درس حذف شده", absentCount = 1))
        dao.insertGrade(GradeEntity(courseId = "c_deleted", courseName = "درس حذف شده", units = 3, midtermGrade = 0.0, finalGrade = 0.0))
        dao.insertTask(TaskEntity(title = "تکلیف نامعتبر", dueDate = "1403/10/01", courseId = "c_deleted"))
        dao.insertExam(ExamEntity(id = "exam_del", courseId = "c_deleted", courseName = "درس حذف شده"))

        // Run orphan cleanup
        dao.cleanupOrphans()

        // Valid course relations remain
        assertNotNull(dao.getAttendanceByCourseId("c_valid"))

        // Orphan relations removed
        assertNull(dao.getAttendanceByCourseId("c_deleted"))
        assertNull(dao.getGradeByCourseId("c_deleted"))
        val orphanTasks = dao.getTasksByCourseId("c_deleted").first()
        assertTrue(orphanTasks.isEmpty())
        assertNull(dao.getExamByCourseId("c_deleted"))
    }

    @Test
    fun testDateTimeNormalizerAndConflictDetection() {
        assertEquals("08:00", DateTimeNormalizer.normalizeTime("8:0"))
        assertEquals("14:30", DateTimeNormalizer.normalizeTime("14:30"))
        assertEquals("1403/07/01", DateTimeNormalizer.normalizeSolarDate("1403/7/1"))

        // Overlapping slots on same day
        assertTrue(DateTimeNormalizer.hasTimeConflict(0, "08:00", "10:00", 0, "09:00", "11:00"))
        assertTrue(DateTimeNormalizer.hasTimeConflict(1, "10:00", "12:00", 1, "10:00", "12:00"))

        // Non-overlapping slots
        assertFalse(DateTimeNormalizer.hasTimeConflict(0, "08:00", "10:00", 0, "10:00", "12:00"))
        // Different days
        assertFalse(DateTimeNormalizer.hasTimeConflict(0, "08:00", "10:00", 1, "08:00", "10:00"))
    }

    @Test
    fun testZeroSetupCleanDefaults() = runBlocking {
        val dao = db.studentDao()
        AppDatabase.populateInitialData(dao, null)

        val profile = dao.getProfileSync()
        assertNotNull(profile)
        assertEquals("دانشجو", profile?.name)
        assertEquals("", profile?.studentId)
        assertFalse(profile?.isOnboardingCompleted == true)

        val currentSem = dao.getCurrentSemesterSync()
        assertNotNull(currentSem)
        assertEquals("sem_1", currentSem?.id)
        assertEquals(1, currentSem?.semesterNumber)
        assertTrue(currentSem?.isCurrent == true)
        assertEquals(0, currentSem?.totalUnits)
    }
}

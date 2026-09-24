package com.example.domain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.SemesterEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.repository.StudentRepository
import com.example.domain.usecase.NewSemesterRequest
import kotlinx.coroutines.flow.first
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
@Config(sdk = [34])
class EndToEndStudentOsLifecycleTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: StudentRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = StudentRepository(db.studentDao(), db.curriculumDao(), db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testCompleteSemesterLifecycleAndAcademicHistoryIntegrity() = runBlocking {
        // 1. Initial State: Create Profile & Term 3
        val profile = StudentProfileEntity(
            name = "سارا احمدی",
            major = "مهندسی شیمی",
            currentSemester = 3,
            passedUnits = 36,
            declaredGpa = 17.2
        )
        repository.updateProfile(profile)

        val term3 = SemesterEntity(
            id = "sem_1403_1",
            title = "ترم ۳ (پاییز ۱۴۰۳)",
            academicYear = 1403,
            termNumber = 3,
            status = "ACTIVE",
            isCurrent = true,
            totalUnits = 0,
            gpa = 0.0
        )
        db.studentDao().insertSemester(term3)

        // 2. Add Courses for Term 3
        val course1 = CourseEntity(
            id = "c_math2",
            name = "ریاضی عمومی ۲",
            units = 3,
            colorHex = "#3B82F6",
            semesterId = term3.id
        )
        val course2 = CourseEntity(
            id = "c_physics2",
            name = "فیزیک ۲",
            units = 3,
            colorHex = "#10B981",
            semesterId = term3.id
        )
        repository.saveCourse(course1)
        repository.saveCourse(course2)

        // Verify courses inserted with stable foreign keys
        val term3Courses = repository.getCoursesForSemesterSync(term3.id)
        assertEquals(2, term3Courses.size)

        // 3. Record Attendance & Grades
        db.studentDao().insertAttendance(
            AttendanceEntity(
                courseId = course1.id,
                courseName = course1.name,
                absentCount = 2,
                maxAllowed = 3
            )
        )
        db.studentDao().updateGrade(
            GradeEntity(
                courseId = course1.id,
                courseName = course1.name,
                units = course1.units,
                midtermGrade = 8.5,
                finalGrade = 9.0
            )
        )

        // 4. Perform Safe Semester Transition: Archive Term 3 -> Start Term 4
        val term4Courses = listOf(
            CourseEntity(
                id = "c_diff_eq",
                name = "معادلات دیفرانسیل",
                units = 3,
                colorHex = "#6366F1"
            ),
            CourseEntity(
                id = "c_chem_phys",
                name = "شیمی فیزیک ۱",
                units = 3,
                colorHex = "#F59E0B"
            )
        )

        val transitionResult = repository.startNewSemester(
            NewSemesterRequest(
                title = "ترم ۴ (بهار ۱۴۰۴)",
                academicYear = 1403,
                termNumber = 4,
                newCourses = term4Courses
            )
        )

        assertNotNull(transitionResult)
        assertEquals(2, transitionResult.newCoursesCount)

        // 5. Verify Academic History & Zero Data Loss
        val allSemesters = repository.semesters.first()
        assertEquals("Total semesters should be 2 (Term 3 archived + Term 4 active)", 2, allSemesters.size)

        val archivedTerm3 = allSemesters.find { it.id == term3.id }
        assertNotNull("Term 3 must exist in archive", archivedTerm3)
        assertFalse("Term 3 must not be active", archivedTerm3!!.isCurrent)
        assertEquals("ARCHIVED", archivedTerm3.status)

        val activeTerm4 = allSemesters.find { it.termNumber == 4 }
        assertNotNull("Term 4 must exist", activeTerm4)
        assertTrue("Term 4 must be current", activeTerm4!!.isCurrent)
        assertEquals("ACTIVE", activeTerm4.status)

        // Verify Term 3 courses are intact
        val archivedCourses = repository.getCoursesForSemesterSync(term3.id)
        assertEquals(2, archivedCourses.size)
        assertTrue(archivedCourses.any { it.name == "ریاضی عمومی ۲" })

        // Verify Term 4 courses exist
        val currentCourses = repository.getCoursesForSemesterSync(activeTerm4.id)
        assertEquals(2, currentCourses.size)
        assertTrue(currentCourses.any { it.name == "معادلات دیفرانسیل" })

        // Verify Profile was safely updated
        val updatedProfile = repository.profile.first()
        assertNotNull(updatedProfile)
        assertEquals(4, updatedProfile!!.currentSemester)
    }
}

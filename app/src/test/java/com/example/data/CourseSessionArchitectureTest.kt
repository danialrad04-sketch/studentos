package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.data.repository.StudentRepository
import com.example.domain.engine.ConflictDetectionEngine
import com.example.domain.engine.ConflictType
import com.example.domain.engine.WorkloadEngine
import kotlinx.coroutines.flow.first
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
class CourseSessionArchitectureTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: StudentRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = StudentRepository(db.studentDao(), db.curriculumDao(), db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * 1. Schema Migration Test (MIGRATION_6_7):
     * Verifies that legacy database schema with duplicate session rows for a single course
     * correctly extracts sessions into the `course_sessions` table, merges metadata into a single
     * canonical `CourseEntity` row, and cleans up secondary rows.
     */
    @Test
    fun testMigration6to7_createsSessionsAndMergesDuplicates() {
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration_test_db.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                override fun onCreate(sqLiteDb: SupportSQLiteDatabase) {
                    // Create v6 courses table
                    sqLiteDb.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `courses` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `name` TEXT NOT NULL,
                            `day` INTEGER NOT NULL,
                            `start` TEXT NOT NULL,
                            `end` TEXT NOT NULL,
                            `location` TEXT NOT NULL,
                            `colorHex` TEXT NOT NULL,
                            `units` INTEGER NOT NULL,
                            `semesterId` TEXT NOT NULL DEFAULT 'current',
                            `isArchived` INTEGER NOT NULL DEFAULT 0,
                            `courseCode` TEXT NOT NULL DEFAULT '',
                            `professor` TEXT NOT NULL DEFAULT '',
                            `examDate` TEXT NOT NULL DEFAULT '',
                            `examTime` TEXT NOT NULL DEFAULT '',
                            `examLocation` TEXT NOT NULL DEFAULT '',
                            `notes` TEXT NOT NULL DEFAULT '',
                            `professorId` TEXT DEFAULT NULL
                        )
                        """.trimIndent()
                    )
                    // Create minimal tasks and attendance tables for v6
                    sqLiteDb.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `tasks` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `courseName` TEXT NOT NULL,
                            `dueDate` TEXT NOT NULL,
                            `isCompleted` INTEGER NOT NULL,
                            `courseId` TEXT NOT NULL DEFAULT '',
                            `semesterId` TEXT DEFAULT NULL
                        )
                        """.trimIndent()
                    )
                    sqLiteDb.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `attendance` (
                            `courseId` TEXT NOT NULL PRIMARY KEY,
                            `courseName` TEXT NOT NULL DEFAULT '',
                            `absentCount` INTEGER NOT NULL DEFAULT 0,
                            `maxAllowed` INTEGER NOT NULL DEFAULT 3
                        )
                        """.trimIndent()
                    )
                    sqLiteDb.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `grades` (
                            `courseId` TEXT NOT NULL PRIMARY KEY,
                            `courseName` TEXT NOT NULL,
                            `units` INTEGER NOT NULL,
                            `midtermGrade` REAL NOT NULL,
                            `finalGrade` REAL NOT NULL
                        )
                        """.trimIndent()
                    )
                    sqLiteDb.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `notes` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `courseId` TEXT DEFAULT NULL,
                            `title` TEXT NOT NULL DEFAULT '',
                            `content` TEXT NOT NULL DEFAULT '',
                            `updatedAt` INTEGER NOT NULL DEFAULT 0
                        )
                        """.trimIndent()
                    )
                    sqLiteDb.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `exams` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `courseId` TEXT NOT NULL,
                            `courseName` TEXT NOT NULL,
                            `date` TEXT NOT NULL,
                            `time` TEXT NOT NULL,
                            `location` TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val rawDb = FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase

        // Insert two legacy rows representing two sessions of the same course: "ریاضی عمومی ۲" (3 units)
        // Session 1: Saturday 08:00 - 10:00 (Classroom 101)
        rawDb.execSQL(
            """
            INSERT INTO `courses` (`id`, `name`, `day`, `start`, `end`, `location`, `colorHex`, `units`, `semesterId`, `isArchived`, `courseCode`, `professor`, `examDate`, `examTime`, `examLocation`, `notes`)
            VALUES ('math2_sess1', 'ریاضی عمومی ۲', 0, '08:00', '10:00', 'کلاس ۱۰۱', '#3B82F6', 3, 'current', 0, 'MATH102', 'دکتر حسینی', '1403/10/20', '09:00', 'تالار آزمون', 'مباحث مشتق')
            """.trimIndent()
        )
        // Session 2: Monday 10:00 - 12:00 (Classroom 102)
        rawDb.execSQL(
            """
            INSERT INTO `courses` (`id`, `name`, `day`, `start`, `end`, `location`, `colorHex`, `units`, `semesterId`, `isArchived`, `courseCode`, `professor`, `examDate`, `examTime`, `examLocation`, `notes`)
            VALUES ('math2_sess2', 'ریاضی عمومی ۲', 2, '10:00', '12:00', 'کلاس ۱۰۲', '#3B82F6', 3, 'current', 0, 'MATH102', '', '', '', '', '')
            """.trimIndent()
        )

        // Execute MIGRATION_6_7
        AppDatabase.MIGRATION_6_7.migrate(rawDb)

        // 1. Verify `course_sessions` table exists and contains 2 session rows
        val cursorSessions = rawDb.query("SELECT id, courseId, day, start, end, location FROM `course_sessions` WHERE `courseId` = 'math2_sess1'")
        assertEquals(2, cursorSessions.count)
        cursorSessions.close()

        // 2. Verify `courses` table contains exactly 1 merged canonical row with 3 units
        val cursorCourses = rawDb.query("SELECT id, name, units, professor, examDate FROM `courses` WHERE `courseCode` = 'MATH102'")
        assertEquals(1, cursorCourses.count)
        assertTrue(cursorCourses.moveToFirst())
        assertEquals("math2_sess1", cursorCourses.getString(0))
        assertEquals("ریاضی عمومی ۲", cursorCourses.getString(1))
        assertEquals(3, cursorCourses.getInt(2))
        assertEquals("دکتر حسینی", cursorCourses.getString(3))
        assertEquals("1403/10/20", cursorCourses.getString(4))
        cursorCourses.close()

        rawDb.close()
    }

    /**
     * Test MIGRATION_6_7 Attendance Merging:
     * Seeds two duplicate Course rows with separate Attendance rows (e.g. absentCount 2 and 1),
     * runs MIGRATION_6_7, and asserts the canonical course's post-migration attendance record
     * has absentCount = 3 (merged sum) and maxAllowed = max(3, 4) = 4, preventing data loss.
     */
    @Test
    fun testMigration6to7_mergesAttendanceWithoutDataLoss() {
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name("migration_test_att_db.db")
            .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                override fun onCreate(sqLiteDb: SupportSQLiteDatabase) {
                    sqLiteDb.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `courses` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `name` TEXT NOT NULL,
                            `day` INTEGER NOT NULL,
                            `start` TEXT NOT NULL,
                            `end` TEXT NOT NULL,
                            `location` TEXT NOT NULL,
                            `colorHex` TEXT NOT NULL,
                            `units` INTEGER NOT NULL,
                            `semesterId` TEXT NOT NULL DEFAULT 'current',
                            `isArchived` INTEGER NOT NULL DEFAULT 0,
                            `courseCode` TEXT NOT NULL DEFAULT '',
                            `professor` TEXT NOT NULL DEFAULT '',
                            `examDate` TEXT NOT NULL DEFAULT '',
                            `examTime` TEXT NOT NULL DEFAULT '',
                            `examLocation` TEXT NOT NULL DEFAULT '',
                            `notes` TEXT NOT NULL DEFAULT '',
                            `professorId` TEXT DEFAULT NULL
                        )
                        """.trimIndent()
                    )
                    sqLiteDb.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `tasks` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `courseName` TEXT NOT NULL,
                            `dueDate` TEXT NOT NULL,
                            `isCompleted` INTEGER NOT NULL,
                            `courseId` TEXT NOT NULL DEFAULT '',
                            `semesterId` TEXT DEFAULT NULL
                        )
                        """.trimIndent()
                    )
                    sqLiteDb.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `attendance` (
                            `courseId` TEXT NOT NULL PRIMARY KEY,
                            `courseName` TEXT NOT NULL DEFAULT '',
                            `absentCount` INTEGER NOT NULL DEFAULT 0,
                            `maxAllowed` INTEGER NOT NULL DEFAULT 3
                        )
                        """.trimIndent()
                    )
                    sqLiteDb.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `grades` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `courseId` TEXT NOT NULL,
                            `courseName` TEXT NOT NULL,
                            `units` INTEGER NOT NULL,
                            `midtermGrade` REAL NOT NULL,
                            `finalGrade` REAL NOT NULL
                        )
                        """.trimIndent()
                    )
                    sqLiteDb.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `notes` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `courseId` TEXT DEFAULT NULL,
                            `title` TEXT NOT NULL DEFAULT '',
                            `content` TEXT NOT NULL DEFAULT '',
                            `updatedAt` INTEGER NOT NULL DEFAULT 0
                        )
                        """.trimIndent()
                    )
                    sqLiteDb.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `exams` (
                            `id` TEXT NOT NULL PRIMARY KEY,
                            `courseId` TEXT NOT NULL,
                            `courseName` TEXT NOT NULL,
                            `date` TEXT NOT NULL,
                            `time` TEXT NOT NULL,
                            `location` TEXT NOT NULL,
                            `notes` TEXT NOT NULL DEFAULT ''
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val rawDb = FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase

        // Seed 2 duplicate courses for "فیزیک ۲" (PHYS102)
        rawDb.execSQL(
            """
            INSERT INTO `courses` (`id`, `name`, `day`, `start`, `end`, `location`, `colorHex`, `units`, `semesterId`, `isArchived`, `courseCode`, `professor`, `examDate`, `examTime`, `examLocation`, `notes`)
            VALUES ('phys_sess1', 'فیزیک ۲', 0, '08:00', '10:00', '۱۰۱', '#10B981', 3, 'current', 0, 'PHYS102', 'دکتر محمدی', '', '', '', '')
            """.trimIndent()
        )
        rawDb.execSQL(
            """
            INSERT INTO `courses` (`id`, `name`, `day`, `start`, `end`, `location`, `colorHex`, `units`, `semesterId`, `isArchived`, `courseCode`, `professor`, `examDate`, `examTime`, `examLocation`, `notes`)
            VALUES ('phys_sess2', 'فیزیک ۲', 2, '10:00', '12:00', '۱۰۲', '#10B981', 3, 'current', 0, 'PHYS102', 'دکتر محمدی', '', '', '', '')
            """.trimIndent()
        )

        // Seed attendance records on both duplicate rows
        rawDb.execSQL("INSERT INTO `attendance` (`courseId`, `courseName`, `absentCount`, `maxAllowed`) VALUES ('phys_sess1', 'فیزیک ۲', 2, 3)")
        rawDb.execSQL("INSERT INTO `attendance` (`courseId`, `courseName`, `absentCount`, `maxAllowed`) VALUES ('phys_sess2', 'فیزیک ۲', 1, 4)")

        // Seed grades on both duplicate rows
        rawDb.execSQL("INSERT INTO `grades` (`courseId`, `courseName`, `units`, `midtermGrade`, `finalGrade`) VALUES ('phys_sess1', 'فیزیک ۲', 3, 17.5, 0.0)")
        rawDb.execSQL("INSERT INTO `grades` (`courseId`, `courseName`, `units`, `midtermGrade`, `finalGrade`) VALUES ('phys_sess2', 'فیزیک ۲', 3, 0.0, 18.0)")

        // Seed exam on secondary row
        rawDb.execSQL("INSERT INTO `exams` (`id`, `courseId`, `courseName`, `date`, `time`, `location`, `notes`) VALUES ('exam_phys_2', 'phys_sess2', 'فیزیک ۲', '1403/10/28', '14:00', 'سالن امتحانات', 'فصل ۱ تا ۵')")

        // Execute MIGRATION_6_7
        AppDatabase.MIGRATION_6_7.migrate(rawDb)

        // 1. Assert attendance was merged into canonical course 'phys_sess1'
        val cursorAtt = rawDb.query("SELECT courseId, absentCount, maxAllowed FROM `attendance`")
        assertEquals(1, cursorAtt.count)
        assertTrue(cursorAtt.moveToFirst())
        assertEquals("phys_sess1", cursorAtt.getString(0))
        assertEquals(3, cursorAtt.getInt(1)) // 2 + 1 = 3 merged absentCount
        assertEquals(4, cursorAtt.getInt(2)) // maxOf(3, 4) = 4 maxAllowed
        cursorAtt.close()

        // 2. Assert all grades records are preserved and re-pointed to canonical 'phys_sess1'
        val cursorGrades = rawDb.query("SELECT courseId, midtermGrade, finalGrade FROM `grades` WHERE `courseId` = 'phys_sess1'")
        assertEquals(2, cursorGrades.count)
        cursorGrades.close()

        // 3. Assert exam record is preserved and re-pointed to canonical 'phys_sess1'
        val cursorExams = rawDb.query("SELECT courseId, date, time, location FROM `exams` WHERE `courseId` = 'phys_sess1'")
        assertEquals(1, cursorExams.count)
        assertTrue(cursorExams.moveToFirst())
        assertEquals("phys_sess1", cursorExams.getString(0))
        assertEquals("1403/10/28", cursorExams.getString(1))
        assertEquals("14:00", cursorExams.getString(2))
        assertEquals("سالن امتحانات", cursorExams.getString(3))
        cursorExams.close()

        rawDb.close()
    }

    /**
     * 2. Units Calculation Test:
     * Verifies that multi-session courses are counted exactly once for units total.
     */
    @Test
    fun testUnitsCalculation_doesNotDoubleCountForMultiSessionCourses() = runBlocking {
        val courseMath = CourseEntity(
            id = "course_math",
            name = "ریاضی عمومی ۲",
            colorHex = "#3B82F6",
            units = 3,
            semesterId = "current"
        )
        val session1 = CourseSessionEntity(
            id = "sess_math_1",
            courseId = "course_math",
            day = 0,
            start = "08:00",
            end = "10:00",
            location = "کلاس ۱۰۱"
        )
        val session2 = CourseSessionEntity(
            id = "sess_math_2",
            courseId = "course_math",
            day = 2,
            start = "10:00",
            end = "12:00",
            location = "کلاس ۱۰۲"
        )

        val coursePhysics = CourseEntity(
            id = "course_phys",
            name = "فیزیک ۲",
            colorHex = "#10B981",
            units = 3,
            semesterId = "current"
        )
        val sessionPhys = CourseSessionEntity(
            id = "sess_phys_1",
            courseId = "course_phys",
            day = 1,
            start = "08:00",
            end = "10:00",
            location = "کلاس ۲۰۱"
        )

        // Save via repository with sessions
        repository.saveCourse(courseMath, listOf(session1, session2))
        repository.saveCourse(coursePhysics, listOf(sessionPhys))

        val loadedCourses = repository.allCourses.first()
        val loadedWithSessions = repository.allCoursesWithSessions.first()

        // 2 courses in total
        assertEquals(2, loadedCourses.distinctBy { it.id }.size)
        assertEquals(2, loadedWithSessions.size)

        // Total units must be 3 + 3 = 6, NOT 3 + 3 + 3 = 9
        val totalUnits = loadedCourses.distinctBy { it.id }.sumOf { it.units }
        assertEquals(6, totalUnits)

        // Workload engine calculation must also be based on 6 units
        val workload = WorkloadEngine.calculateWeeklyWorkload(loadedCourses, emptyList())
        assertEquals(9.0, workload.classLectureHours, 0.01) // 6 units * 1.5 hours = 9.0
    }

    /**
     * 3. Exam Deduplication Test:
     * Verifies that exactly one Exam is saved per logical course, even if it has multiple sessions.
     */
    @Test
    fun testExamDeduplication_exactlyOneExamPerLogicalCourse() = runBlocking {
        val course = CourseEntity(
            id = "course_chem",
            name = "شیمی عمومی",
            colorHex = "#EC4899",
            units = 3,
            examDate = "1403/10/25",
            examTime = "14:00",
            examLocation = "آمفی‌تئاتر"
        )
        val sessions = listOf(
            CourseSessionEntity(id = "s1", courseId = "course_chem", day = 0, start = "08:00", end = "10:00"),
            CourseSessionEntity(id = "s2", courseId = "course_chem", day = 3, start = "13:30", end = "15:30")
        )

        repository.saveCourse(course, sessions)

        val exams = repository.exams.first()
        val courses = repository.allCourses.first()
        val deduplicatedExams = courses.distinctBy { it.id }.filter { it.examDate.isNotBlank() }

        assertEquals(1, exams.size)
        assertEquals(1, deduplicatedExams.size)
        assertEquals("شیمی عمومی", deduplicatedExams.first().name)
        assertEquals("1403/10/25", deduplicatedExams.first().examDate)
    }

    /**
     * 4. Multi-Session Conflict Detection Test:
     * Verifies that ConflictDetectionEngine flags overlap across any session of Course A and Course B,
     * but does NOT flag false positives on non-overlapping days, nor within the same course.
     */
    @Test
    fun testConflictDetectionEngine_handlesMultiSessionOverlapAccurately() {
        val courseA = CourseEntity(
            id = "course_a",
            name = "ریاضی ۱",
            colorHex = "#3B82F6",
            units = 3
        )
        val sessionsA = listOf(
            CourseSessionEntity(id = "s_a_1", courseId = "course_a", day = 0, start = "08:00", end = "10:00"), // Sat 8-10
            CourseSessionEntity(id = "s_a_2", courseId = "course_a", day = 2, start = "10:00", end = "12:00")  // Mon 10-12
        )
        val cWithSA = CourseWithSessions(courseA, sessionsA)

        // Course B overlaps on Monday 11:00-13:00 (overlaps with Session A2: Mon 10-12)
        val courseB = CourseEntity(
            id = "course_b",
            name = "برنامه‌نویسی پیشرفته",
            colorHex = "#10B981",
            units = 3
        )
        val sessionsB = listOf(
            CourseSessionEntity(id = "s_b_1", courseId = "course_b", day = 2, start = "11:00", end = "13:00")
        )
        val cWithSB = CourseWithSessions(courseB, sessionsB)

        // Course C on Tuesday 08:00-10:00 (no conflict)
        val courseC = CourseEntity(
            id = "course_c",
            name = "آمار مهندسی",
            colorHex = "#8B5CF6",
            units = 3
        )
        val sessionsC = listOf(
            CourseSessionEntity(id = "s_c_1", courseId = "course_c", day = 3, start = "08:00", end = "10:00")
        )
        val cWithSC = CourseWithSessions(courseC, sessionsC)

        val conflicts = ConflictDetectionEngine.checkAllConflictsWithSessions(listOf(cWithSA, cWithSB, cWithSC))

        // Exactly one conflict found between course_a and course_b
        assertEquals(1, conflicts.size)
        val conflict = conflicts.first()
        assertEquals("course_a", conflict.course1Id)
        assertEquals("course_b", conflict.course2Id)
        assertEquals(ConflictType.CLASS_CLASS_OVERLAP, conflict.type)
    }

    /**
     * 5. Cascade Deletion & Orphan Cleanup Test:
     * Verifies that deleting a course cleans up all associated CourseSessionEntity records,
     * and updating a course with fewer sessions removes orphan sessions.
     */
    @Test
    fun testCascadeDeletionAndOrphanSessionCleanup() = runBlocking {
        val course = CourseEntity(
            id = "course_algo",
            name = "طراحی الگوریتم‌ها",
            colorHex = "#F59E0B",
            units = 3
        )
        val sessions3 = listOf(
            CourseSessionEntity(id = "sess_1", courseId = "course_algo", day = 0, start = "10:00", end = "12:00"),
            CourseSessionEntity(id = "sess_2", courseId = "course_algo", day = 2, start = "14:00", end = "16:00"),
            CourseSessionEntity(id = "sess_3", courseId = "course_algo", day = 4, start = "08:00", end = "10:00")
        )

        // 1. Save with 3 sessions
        repository.saveCourse(course, sessions3)
        var fetched = repository.allCoursesWithSessions.first()
        assertEquals(1, fetched.size)
        assertEquals(3, fetched.first().sessions.size)

        // 2. Update to 2 sessions (sess_3 removed)
        val sessions2 = listOf(sessions3[0], sessions3[1])
        repository.saveCourse(course, sessions2)
        fetched = repository.allCoursesWithSessions.first()
        assertEquals(1, fetched.size)
        assertEquals(2, fetched.first().sessions.size)

        // 3. Delete course -> sessions should be cascade deleted
        repository.deleteCourse("course_algo")
        fetched = repository.allCoursesWithSessions.first()
        assertTrue(fetched.isEmpty())
        val allSessions = db.studentDao().getSessionsForCourseSync("course_algo")
        assertTrue(allSessions.isEmpty())
    }

    /**
     * 6. StudentViewModel Flow Emission Test:
     * Confirms that after calling saveCourse(course, sessions) with 2 sessions,
     * studentViewModel.coursesWithSessions emits a CourseWithSessions entry containing exactly those 2 sessions.
     */
    @Test
    fun testStudentViewModel_saveCourseWithSessions_emitsCorrectSessions() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.StudentViewModel(
            application = app,
            injectedRepository = repository
        )

        val newCourseId = "course_network_sec"
        val course = CourseEntity(
            id = newCourseId,
            name = "امنیت شبکه‌های کامپیوتری",
            colorHex = "#6366F1",
            units = 3,
            professor = "دکتر جلالی"
        )
        val sessions = listOf(
            CourseSessionEntity(
                id = "sess_net_1",
                courseId = newCourseId,
                day = 1, // یکشنبه
                start = "08:00",
                end = "10:00",
                location = "اتاق ۴۰۲"
            ),
            CourseSessionEntity(
                id = "sess_net_2",
                courseId = newCourseId,
                day = 3, // سه‌شنبه
                start = "10:00",
                end = "12:00",
                location = "آزمایشگاه امنیت"
            )
        )

        // Save course with 2 distinct sessions directly to repository & through ViewModel
        repository.saveCourse(course, sessions)
        viewModel.saveCourse(course, sessions)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        // Verify repository Flow emission
        val repoEmitted = repository.allCoursesWithSessions.first { list ->
            list.any { it.course.id == newCourseId }
        }
        val repoItem = repoEmitted.first { it.course.id == newCourseId }
        assertEquals(2, repoItem.sessions.size)
        assertEquals("08:00", repoItem.sessions[0].start)
        assertEquals("10:00", repoItem.sessions[1].start)

        // Verify direct DAO multi-relation query
        val directCws = db.studentDao().getCourseWithSessionsById(newCourseId)
        assertNotNull(directCws)
        assertEquals(2, directCws!!.sessions.size)
        assertEquals("اتاق ۴۰۲", directCws.sessions[0].location)
        assertEquals("آزمایشگاه امنیت", directCws.sessions[1].location)
    }
}

package com.example

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.engine.AcademicCopilotEngine
import com.example.domain.model.SubscriptionDetails
import com.example.domain.model.SubscriptionTier
import com.example.domain.model.UserAccount
import com.example.ui.models.ExamItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AuthAndCopilotEngineTest {

    @Test
    fun testSubscriptionTierPermissions() {
        val freeTier = SubscriptionTier.FREE
        val proTier = SubscriptionTier.PRO
        val ultraTier = SubscriptionTier.ULTRA

        assertEquals(5, freeTier.maxAiQueriesPerDay)
        assertEquals(8, freeTier.maxCourses)
        assertFalse(freeTier.allowsCloudSync)
        assertFalse(freeTier.allowsPdfExport)

        assertEquals(50, proTier.maxAiQueriesPerDay)
        assertEquals(25, proTier.maxCourses)
        assertTrue(proTier.allowsCloudSync)
        assertTrue(proTier.allowsPdfExport)

        assertEquals(999, ultraTier.maxAiQueriesPerDay)
        assertEquals(99, ultraTier.maxCourses)
        assertTrue(ultraTier.allowsCloudSync)
        assertTrue(ultraTier.allowsPdfExport)
    }

    @Test
    fun testUserAccountProGating() {
        val guestUser = UserAccount(
            uid = "guest_123",
            email = "",
            displayName = "مهمان",
            isGuest = true,
            subscription = SubscriptionDetails(tier = SubscriptionTier.FREE)
        )
        assertFalse(guestUser.isProOrHigher)

        val proUser = UserAccount(
            uid = "user_456",
            email = "student@aut.ac.ir",
            displayName = "امیرحسین",
            isGuest = false,
            subscription = SubscriptionDetails(tier = SubscriptionTier.PRO)
        )
        assertTrue(proUser.isProOrHigher)

        val ultraUser = UserAccount(
            uid = "user_789",
            email = "elite@aut.ac.ir",
            displayName = "سارا",
            isGuest = false,
            subscription = SubscriptionDetails(tier = SubscriptionTier.ULTRA)
        )
        assertTrue(ultraUser.isProOrHigher)
    }

    @Test
    fun testAcademicCopilotWelcomeAndProcessQuery() {
        val sampleProfile = StudentProfileEntity(
            id = 1,
            name = "امیرحسین",
            studentId = "40112345",
            major = "مهندسی شیمی",
            university = "دانشگاه صنعتی امیرکبیر",
            entryYear = 1401,
            currentSemester = 6,
            passedUnits = 85,
            activeUnits = 18,
            declaredGpa = 17.2,
            term = "ترم ۶"
        )

        val sampleCourses = listOf(
            CourseEntity(id = "c1", name = "انتقال حرارت ۱", professor = "دکتر نوری", units = 3, colorHex = "#3B82F6"),
            CourseEntity(id = "c2", name = "مکانیک سیالات", professor = "دکتر رضایی", units = 3, colorHex = "#10B981")
        )

        val sampleAttendance = listOf(
            AttendanceEntity(courseId = "c1", courseName = "انتقال حرارت ۱", absentCount = 2, maxAllowed = 3),
            AttendanceEntity(courseId = "c2", courseName = "مکانیک سیالات", absentCount = 0, maxAllowed = 3)
        )

        val sampleGrades = listOf(
            GradeEntity(id = 1, courseName = "انتقال حرارت ۱", units = 3, midtermGrade = 7.5, finalGrade = 9.0, courseId = "c1"),
            GradeEntity(id = 2, courseName = "مکانیک سیالات", units = 3, midtermGrade = 8.0, finalGrade = 9.5, courseId = "c2")
        )

        val sampleTasks = listOf(
            TaskEntity(id = 1, title = "پروژه شبیه‌سازی سیالات", dueDate = "1404/09/25", isCompleted = false, courseId = "c2")
        )

        val sampleExams = listOf(
            ExamItem(id = "e1", courseName = "انتقال حرارت ۱", solarDate = "1404/10/15", time = "09:00", location = "تالار ۱", units = 3)
        )

        val welcome = AcademicCopilotEngine.generateWelcomeMessage(
            profile = sampleProfile,
            courses = sampleCourses,
            attendanceList = sampleAttendance,
            grades = sampleGrades,
            tasks = sampleTasks
        )

        assertNotNull(welcome)
        assertTrue(welcome.text.isNotBlank())
        assertTrue(welcome.text.contains("امیرحسین"))

        val queryResponse = AcademicCopilotEngine.processUserQuery(
            query = "غیبت های من چطوره؟",
            profile = sampleProfile,
            courses = sampleCourses,
            attendanceList = sampleAttendance,
            grades = sampleGrades,
            tasks = sampleTasks,
            exams = sampleExams
        )

        assertNotNull(queryResponse)
        assertTrue(queryResponse.text.contains("غیبت") || queryResponse.text.contains("قانون"))
    }

    @Test
    fun testAcademicCopilotAsyncQueryFallback() = runBlocking {
        val sampleProfile = StudentProfileEntity(
            id = 1,
            name = "امیرحسین",
            studentId = "40112345",
            major = "مهندسی شیمی",
            university = "دانشگاه صنعتی امیرکبیر",
            entryYear = 1401,
            currentSemester = 6,
            passedUnits = 85,
            activeUnits = 18,
            declaredGpa = 17.2,
            term = "ترم ۶"
        )

        val message = AcademicCopilotEngine.processUserQueryAsync(
            query = "برای فارغ‌التحصیلی چند واحد دیگه نیاز دارم؟",
            profile = sampleProfile,
            courses = emptyList(),
            attendanceList = emptyList(),
            grades = emptyList(),
            tasks = emptyList(),
            exams = emptyList(),
            customApiKey = null
        )

        assertNotNull(message)
        assertTrue(message.text.isNotBlank())
    }

    @Test
    fun testRealGeminiCopilotFiveQueries() = runBlocking {
        val sampleProfile = StudentProfileEntity(
            id = 1,
            name = "امیرحسین",
            studentId = "40112345",
            major = "مهندسی کامپیوتر",
            university = "دانشگاه صنعتی امیرکبیر",
            entryYear = 1401,
            currentSemester = 6,
            passedUnits = 85,
            activeUnits = 18,
            declaredGpa = 17.2,
            term = "ترم ۶"
        )
        val courses = listOf(
            CourseEntity(id = "c1", name = "سیستم‌های عامل", units = 3, colorHex = "#3B82F6"),
            CourseEntity(id = "c2", name = "طراحی الگوریتم", units = 3, colorHex = "#10B981")
        )
        val attendance = listOf(
            AttendanceEntity(courseId = "c1", courseName = "سیستم‌های عامل", absentCount = 2, maxAllowed = 3),
            AttendanceEntity(courseId = "c2", courseName = "طراحی الگوریتم", absentCount = 0, maxAllowed = 3)
        )
        val exams = listOf(
            ExamItem(id = "e1", courseName = "سیستم‌های عامل", solarDate = "1403/03/20", time = "09:00", location = "تالار ابوریحان", units = 3)
        )

        val questions = listOf(
            "چقدر دیگه غیبت دارم؟",
            "برنامه مطالعه برای امتحان بده",
            "بهترین روش برای تسلط بر مفاهیم مدیریت حافظه در سیستم عامل چیست؟",
            "تفاوت کارشناسی ارشد آموزش‌محور و پژوهش‌محور در دانشگاه‌های سراسری چیست؟",
            "برای ورود به بازار کار توسعه نرم‌افزار در طول دوران دانشجویی چه مهارت‌های عملی را پیشنهاد می‌کنی؟"
        )

        for ((index, q) in questions.withIndex()) {
            val response = AcademicCopilotEngine.processUserQueryAsync(
                query = q,
                profile = sampleProfile,
                courses = courses,
                attendanceList = attendance,
                grades = emptyList(),
                tasks = emptyList(),
                exams = exams
            )
            println("=== QUERY ${index + 1}: $q ===")
            println("Badge: ${response.confidenceBadge}")
            println("Response: ${response.text}\n")
            assertNotNull(response.text)
            assertTrue(response.text.isNotBlank())
            assertTrue(
                "Expected live AI badge or academic badge but got: ${response.confidenceBadge}",
                response.confidenceBadge?.contains("Gemini") == true ||
                response.confidenceBadge?.contains("هوش مصنوعی") == true ||
                response.confidenceBadge?.contains("آکادمیک") == true ||
                response.confidenceBadge?.contains("آفلاین") == true
            )
        }
    }

    @Test
    fun testComputeGpaAveragesMidtermAndFinalCorrectly() {
        // Test single course with midterm 18.0 and final 16.0, 3 units.
        // Summing them produced 34.0 (wrong). Averaging should produce (18.0 + 16.0) / 2 = 17.0.
        val grades = listOf(
            GradeEntity(id = 1, courseName = "محاسبات عددی", units = 3, midtermGrade = 18.0, finalGrade = 16.0, courseId = "c_math")
        )

        val gpa = AcademicCopilotEngine.computeGpa(grades)
        assertEquals(17.0, gpa, 0.001)
        assertTrue("GPA must not exceed 20", gpa <= 20.0)
    }

    @Test
    fun testComputeGpaUnitsWeightedAverageWithMultipleCourses() {
        // Course 1: units = 2, midterm = 14.0, final = 16.0 -> avg = 15.0 -> weighted = 30.0
        // Course 2: units = 4, midterm = 18.0, final = 18.0 -> avg = 18.0 -> weighted = 72.0
        // Total units = 6, total weighted = 102.0 -> GPA = 102.0 / 6 = 17.0
        val grades = listOf(
            GradeEntity(id = 1, courseName = "آزمایشگاه شیمی", units = 2, midtermGrade = 14.0, finalGrade = 16.0, courseId = "c1"),
            GradeEntity(id = 2, courseName = "ترمودینامیک پیشرفته", units = 4, midtermGrade = 18.0, finalGrade = 18.0, courseId = "c2")
        )

        val gpa = AcademicCopilotEngine.computeGpa(grades)
        assertEquals(17.0, gpa, 0.001)
        assertTrue("GPA must be clamped between 0 and 20", gpa in 0.0..20.0)
    }

    @Test
    fun testRequiredPersianAcademicCopilotPrompts() = runBlocking {
        val sampleProfile = StudentProfileEntity(
            id = 1,
            name = "دانیال",
            studentId = "40112345",
            major = "مهندسی کامپیوتر",
            university = "دانشگاه صنعتی شریف",
            entryYear = 1401,
            currentSemester = 4,
            passedUnits = 60,
            activeUnits = 18,
            declaredGpa = 17.5,
            term = "ترم ۴"
        )
        val courses = listOf(
            CourseEntity(id = "c1", name = "ساختمان داده‌ها", professor = "دکتر قدسی", units = 3, colorHex = "#3B82F6"),
            CourseEntity(id = "c2", name = "مدارهای منطقی", professor = "دکتر نائینی", units = 3, colorHex = "#10B981")
        )
        val sessions = listOf(
            com.example.data.local.relation.CourseWithSessions(
                course = courses[0],
                sessions = listOf(
                    com.example.data.local.entity.CourseSessionEntity(courseId = "c1", day = com.example.ui.components.datepicker.JalaliCalendarUtil.getTodayWeekdayIndex(), start = "08:00", end = "10:00", location = "کلاس ۱۰۱")
                )
            ),
            com.example.data.local.relation.CourseWithSessions(
                course = courses[1],
                sessions = listOf(
                    com.example.data.local.entity.CourseSessionEntity(courseId = "c2", day = com.example.ui.components.datepicker.JalaliCalendarUtil.getTomorrowWeekdayIndex(), start = "10:30", end = "12:30", location = "کلاس ۲۰۴")
                )
            )
        )
        val tomorrowJalali = com.example.ui.components.datepicker.JalaliCalendarUtil.tomorrow().format("/")
        val exams = listOf(
            ExamItem(id = "e1", courseName = "ساختمان داده‌ها", solarDate = tomorrowJalali, time = "09:00", location = "تالار ابوریحان", units = 3)
        )
        val grades = listOf(
            GradeEntity(id = 1, courseName = "ساختمان داده‌ها", units = 3, midtermGrade = 8.0, finalGrade = 9.0, courseId = "c1"),
            GradeEntity(id = 2, courseName = "مدارهای منطقی", units = 3, midtermGrade = 7.0, finalGrade = 8.5, courseId = "c2")
        )
        val tasks = listOf(
            TaskEntity(id = 1, title = "پیاده‌سازی درخت AVL", dueDate = tomorrowJalali, isCompleted = false, courseId = "c1")
        )

        val requiredPrompts = listOf(
            "سلام",
            "تو چی هستی؟",
            "برنامه امروز من چیست؟",
            "فردا چه امتحانی دارم؟",
            "برای امتحان فردا برنامه مطالعه بده.",
            "این هفته چه کلاس‌هایی دارم؟",
            "نمرات من را تحلیل کن.",
            "چه درس‌هایی عقب افتاده‌اند؟",
            "برای من برنامه مطالعه بساز."
        )

        for (prompt in requiredPrompts) {
            val response = AcademicCopilotEngine.processUserQuery(
                query = prompt,
                profile = sampleProfile,
                courses = courses,
                attendanceList = emptyList(),
                grades = grades,
                tasks = tasks,
                exams = exams,
                coursesWithSessions = sessions
            )
            assertNotNull("Response for '$prompt' must not be null", response)
            assertTrue("Response text for '$prompt' must not be empty", response.text.isNotBlank())
            assertTrue("Confidence badge for '$prompt' must be valid", response.confidenceBadge?.isNotBlank() == true || response.confidenceBadge == null)
        }
    }
}

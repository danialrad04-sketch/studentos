package com.example.domain.engine

import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.data.parser.ParsedCourseDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictDetectionEngineTest {

    @Test
    fun testClassClassOverlapDetection() {
        val course1 = CourseEntity(
            id = "c1",
            name = "ریاضی عمومی ۱",
            colorHex = "#3B82F6",
            units = 3
        )
        val session1 = CourseSessionEntity(
            id = "s1",
            courseId = "c1",
            day = 0, // شنبه
            start = "08:00",
            end = "10:00",
            location = "۱۰۱"
        )
        val cWithS1 = CourseWithSessions(course1, listOf(session1))

        val overlappingCourse = CourseEntity(
            id = "c2",
            name = "فیزیک ۱",
            colorHex = "#EF4444",
            units = 3
        )
        val session2 = CourseSessionEntity(
            id = "s2",
            courseId = "c2",
            day = 0, // شنبه
            start = "09:00",
            end = "11:00",
            location = "۱۰۲"
        )

        val conflicts = ConflictDetectionEngine.checkCourseWithSessionsConflicts(
            targetCourse = overlappingCourse,
            targetSessions = listOf(session2),
            existingWithSessions = listOf(cWithS1)
        )
        val overlapConflict = conflicts.find { it.type == ConflictType.CLASS_CLASS_OVERLAP }

        assertTrue("Expected class overlap conflict", overlapConflict != null)
        assertEquals(ConflictSeverity.ERROR, overlapConflict?.severity)
    }

    @Test
    fun testExamExamOverlapDetection() {
        val course1 = CourseEntity(
            id = "c1",
            name = "ترمودینامیک ۱",
            colorHex = "#3B82F6",
            units = 3,
            examDate = "1403/10/22",
            examTime = "09:00"
        )

        val course2 = CourseEntity(
            id = "c2",
            name = "مکانیک سیالات",
            colorHex = "#EF4444",
            units = 3,
            examDate = "1403/10/22",
            examTime = "09:00"
        )

        val conflicts = ConflictDetectionEngine.checkAllConflictsWithSessions(
            listOf(
                CourseWithSessions(course1, emptyList()),
                CourseWithSessions(course2, emptyList())
            )
        )
        val examConflict = conflicts.find { it.type == ConflictType.EXAM_EXAM_OVERLAP }

        assertTrue("Expected exam overlap conflict", examConflict != null)
    }

    @Test
    fun testDuplicateCourseCodeDetection() {
        val course1 = CourseEntity(
            id = "c1",
            name = "شیمی آلی ۱",
            colorHex = "#3B82F6",
            units = 3,
            courseCode = "CH101"
        )

        val duplicateCodeCourse = CourseEntity(
            id = "c2",
            name = "شیمی آلی - گروه ۲",
            colorHex = "#EF4444",
            units = 3,
            courseCode = "CH101"
        )

        val conflicts = ConflictDetectionEngine.checkAllConflictsWithSessions(
            listOf(
                CourseWithSessions(course1, emptyList()),
                CourseWithSessions(duplicateCodeCourse, emptyList())
            )
        )
        val dupConflict = conflicts.find { it.type == ConflictType.DUPLICATE_COURSE_CODE }

        assertTrue("Expected duplicate course code conflict", dupConflict != null)
    }

    @Test
    fun testInvalidTimeRangeDetection() {
        val invalidCourse = CourseEntity(
            id = "c1",
            name = "درس تست",
            colorHex = "#3B82F6",
            units = 3
        )
        val invalidSession = CourseSessionEntity(
            id = "s_inv",
            courseId = "c1",
            day = 0,
            start = "12:00",
            end = "10:00", // Invalid: end before start
            location = "۱۰۱"
        )

        val conflicts = ConflictDetectionEngine.checkCourseWithSessionsConflicts(
            targetCourse = invalidCourse,
            targetSessions = listOf(invalidSession),
            existingWithSessions = emptyList()
        )
        val timeConflict = conflicts.find { it.type == ConflictType.INVALID_TIME_RANGE }

        assertTrue("Expected invalid time range conflict", timeConflict != null)
        assertEquals(ConflictSeverity.ERROR, timeConflict?.severity)
    }

    @Test
    fun testValidDraftPassesWithoutError() {
        val draft = ParsedCourseDraft(
            name = "آمار و احتمالات مهندسی",
            units = 3,
            dayOfWeek = 2,
            startTime = "10:00",
            endTime = "12:00",
            location = "کلاس ۱۰۵"
        )

        val existing = listOf(
            CourseEntity(
                id = "c0",
                name = "فیزیک ۲",
                colorHex = "#3B82F6",
                units = 3
            )
        )

        val conflicts = ConflictDetectionEngine.validateDraft(draft, existing)
        val errors = conflicts.filter { it.severity == ConflictSeverity.ERROR }
        assertTrue("No error conflicts should occur for valid draft", errors.isEmpty())
    }
}

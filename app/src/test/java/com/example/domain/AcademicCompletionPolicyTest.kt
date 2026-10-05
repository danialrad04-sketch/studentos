package com.example.domain

import com.example.domain.model.FocusSession
import com.example.domain.util.AcademicInputValidator
import com.example.domain.util.JalaliCalendarUtil
import com.example.domain.engine.ImportReviewEngine
import com.example.data.parser.ParsedCourseDraft
import com.example.data.local.entity.GradeEntity
import org.junit.Assert.*
import org.junit.Test

class AcademicCompletionPolicyTest {
    @Test fun focusUsesDeadlineAfterProcessRecreationAndPauseResumesRemainingTime() {
        val running = FocusSession(durationSeconds = 2700, pausedSeconds = 2700).start(10_000)
        assertEquals(2640, running.remainingSeconds(70_000))
        val paused = running.pause(70_000)
        assertEquals(2640, paused.remainingSeconds(2_000_000))
        val resumed = paused.start(2_000_000)
        assertEquals(2630, resumed.remainingSeconds(2_010_000))
        assertEquals(0, resumed.remainingSeconds(9_000_000))
    }
    @Test fun restartingCompletedTimerUsesOriginalDuration() {
        assertEquals(3600, FocusSession(3600, 0).start(100).remainingSeconds(100))
    }
    @Test fun datesRejectImpossibleDaysAndTimesNeverClampInvalidInput() {
        assertNull(JalaliCalendarUtil.parse("۱۴۰۵/۰۷/۳۱"))
        assertNull(JalaliCalendarUtil.parse("1405/12/30"))
        assertNull(AcademicInputValidator.time("25:99"))
        assertEquals("08:30", AcademicInputValidator.time("۸:۳۰"))
        assertEquals("08:30", AcademicInputValidator.time("٨:٣٠"))
        assertNotNull(AcademicInputValidator.examTimestamp("۱۴۰۵/۰۷/۲۰", "۰۸:۳۰"))
    }
    @Test fun importRejectsReversedTimesAndPartialExamAndDetectsOverlapsWithoutBlockingAdjacentSessions() {
        val a = ParsedCourseDraft(name = "ریاضی", dayOfWeek = 0, startTime = "08:00", endTime = "10:00")
        assertTrue(AcademicInputValidator.draftIssues(a).isEmpty())
        assertTrue(AcademicInputValidator.draftIssues(a.copy(endTime = "07:00")).isNotEmpty())
        assertTrue(AcademicInputValidator.draftIssues(a.copy(examDate = "1405/10/20")).isNotEmpty())
        val b = a.copy(tempId = "b", name = "فیزیک", startTime = "09:00", endTime = "11:00")
        assertEquals(1, ImportReviewEngine.conflicts(listOf(a, b)).size)
        assertTrue(ImportReviewEngine.conflicts(listOf(a, b.copy(startTime = "10:00"))).isEmpty())
    }
    @Test fun recordedZeroIsDifferentFromAnUnenteredGrade() {
        assertFalse(GradeEntity(courseName = "ریاضی").hasRecordedScore())
        assertTrue(GradeEntity(courseName = "ریاضی", isRecorded = true).hasRecordedScore())
        assertTrue(GradeEntity(courseName = "ریاضی", finalGrade = 18.0).hasRecordedScore())
    }
    @Test fun personalChartRejectsUnknownPrerequisitesAndDuplicateCodes() {
        val parser = com.example.data.parser.ManualCurriculumParser
        val result = parser.parse("M1 | ریاضی ۱ | ۳ | ۱\nM2 | ریاضی ۲ | ۳ | ۲ | M1", "test")
        assertEquals("ریاضی ۱", result[1].prerequisites)
        for (invalid in listOf("M1 | ریاضی | 3 | 1 | missing", "M1 | ریاضی | 3 | 1\nM1 | فیزیک | 3 | 1", "M1 | ریاضی | 0 | 1")) {
            assertTrue(runCatching { parser.parse(invalid, "test") }.isFailure)
        }
        assertTrue(runCatching { parser.parse("A | الف | 3 | 1 | B\nB | ب | 3 | 2 | A", "test") }.isFailure)
    }
}

package com.example

import com.example.data.local.entity.*
import com.example.domain.engine.*
import com.example.domain.model.StudyDay
import com.example.domain.util.JalaliDate
import org.junit.Assert.*
import org.junit.Test

class StudentPlanningEngineTest {
    @Test fun recurrenceCrossesNoruzAndSkipsMissedDatesWithoutChangingWeeklyCadence() {
        assertEquals("1405/01/01", TaskPlanningEngine.nextDate("1404/12/29", 1, JalaliDate(1404, 12, 29)))
        assertEquals("1405/07/22", TaskPlanningEngine.nextDate("1405/07/01", 7, JalaliDate(1405, 7, 20)))
        assertEquals("1405/08/01", TaskPlanningEngine.nextDate("1405/07/01", 30, JalaliDate(1405, 7, 1)))
        assertNull(TaskPlanningEngine.nextDate("1405/07/01", 0, JalaliDate(1405, 7, 1)))
    }
    @Test fun reminderUsesTehranAndAcceptsPersianDigitsButRejectsInvalidPlanning() {
        val task = TaskEntity(title = "تمرین", dueDate = "1405/07/01", reminderTime = "۰۹:۳۰", repeatEveryDays = 7)
        TaskPlanningEngine.validate(task)
        val time = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Tehran")).apply { timeInMillis = TaskPlanningEngine.reminderAt(task)!! }
        assertEquals(9, time.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(30, time.get(java.util.Calendar.MINUTE))
        assertNull(TaskPlanningEngine.reminderAt(task.copy(isCompleted = true)))
        listOf(task.copy(repeatEveryDays = -1), task.copy(reminderTime = "25:00"), task.copy(dueDate = "1405/13/01"), task.copy(id = 1, recurrenceParentId = 1)).forEach {
            try { TaskPlanningEngine.validate(it); fail("Invalid task accepted") } catch (_: IllegalArgumentException) { }
        }
    }
    @Test fun realStudyHistoryCountsStreakAcrossMonthAndYearBoundariesAndExcludesFutureOrInvalidRows() {
        val history = listOf(StudyDay("2025-12-30", 25, 1), StudyDay("2025-12-31", 45, 1), StudyDay("2026-01-01", 60, 2),
            StudyDay("2026-01-20", 100, 4), StudyDay("2026-02-30", 30, 1), StudyDay("2026-01-02", -5, 1))
        val stats = StudyStatisticsEngine.calculate(history, "2026-01-02")
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
        assertEquals(130, stats.totalMinutes)
        assertEquals(4, stats.totalSessions)
        assertEquals(130, stats.lastSevenDaysMinutes)
        assertEquals(0, StudyStatisticsEngine.calculate(history, "2026-01-03").currentStreak)
    }
    @Test fun weeklyWindowsDoNotOverlapAndEmptyHistoryCannotInventProgress() {
        val history = listOf(StudyDay("2026-10-25", 25, 1), StudyDay("2026-10-19", 45, 1), StudyDay("2026-10-18", 60, 2), StudyDay("2026-10-12", 15, 1), StudyDay("2026-10-11", 30, 1))
        val stats = StudyStatisticsEngine.calculate(history, "2026-10-25")
        assertEquals(70, stats.lastSevenDaysMinutes)
        assertEquals(75, stats.previousSevenDaysMinutes)
        val profile = AcademicGamificationEngine.calculateGamificationProfile(emptyList(), emptyList(), emptyList(), 0, studyHistory = emptyList(), todayKey = "2026-10-25")
        assertEquals(0.0, profile.focusHoursTotal, 0.0)
        assertEquals(0, profile.studyStreakDays)
        assertEquals(0f, profile.badges.single { it.id == "att_safe_radar" }.progress)
    }
    @Test fun gamificationUsesRecordedMinutesAndOnlyRecordedGrades() {
        val profile = AcademicGamificationEngine.calculateGamificationProfile(emptyList(), emptyList(), listOf(
            GradeEntity(courseId = "known", finalGrade = 18.0, isRecorded = true), GradeEntity(courseId = "unknown")), 0,
            studyHistory = listOf(StudyDay("2026-10-24", 60, 2), StudyDay("2026-10-25", 60, 2)), todayKey = "2026-10-25")
        assertEquals(2.0, profile.focusHoursTotal, 0.0)
        assertEquals(2, profile.studyStreakDays)
        assertTrue(profile.badges.single { it.id == "focus_flow_beginner" }.isUnlocked)
        assertTrue(profile.badges.single { it.id == "gpa_honors_17" }.isUnlocked)
    }
}

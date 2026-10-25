package com.example.domain.engine

import com.example.data.local.entity.*
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.model.ExamItem
import com.example.domain.model.StudyDay
import com.example.domain.util.AcademicInputValidator
import com.example.domain.util.JalaliCalendarUtil
import com.squareup.moshi.Moshi
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

/** Bounded, factual data for a model; no student ID, private notes or guessed grades. */
object AcademicAssistantContext {
    private val adapter = Moshi.Builder().build().adapter(Any::class.java).serializeNulls()
    fun recordedGrades(grades: List<GradeEntity>) = grades.filter {
        val total = it.midtermGrade + it.finalGrade
        it.hasRecordedScore() && it.units in 1..10 && total.isFinite() && total in 0.0..20.0
    }
    fun build(profile: StudentProfileEntity, courses: List<CourseEntity>, sessions: List<CourseWithSessions>,
        attendance: List<AttendanceEntity>, grades: List<GradeEntity>, tasks: List<TaskEntity>, exams: List<ExamItem>,
        history: List<StudyDay> = emptyList(), dailyGoal: Int = 120, focusMinutes: Int = 25, now: Long = System.currentTimeMillis(), recentLocalAnalysis: List<String> = emptyList()): String {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val today = JalaliCalendarUtil.gregorianToJalali(calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH) + 1, calendar.get(java.util.Calendar.DAY_OF_MONTH))
        val active = courses.filterNot { it.isArchived }.distinctBy { it.id }
        val courseIds = active.map { it.id }.toSet()
        val recorded = recordedGrades(grades).filter { (it.courseId in courseIds || (it.courseId.isBlank() && active.any { c -> c.name == it.courseName })) }
        val study = StudyStatisticsEngine.calculate(history, SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now)))
        fun short(value: String, size: Int = 100) = value.trim().take(size).ifBlank { null }
        val lists = linkedMapOf<String, MutableList<Any?>>()
        lists["courses"] = active.take(24).map { mapOf("id" to it.id, "name" to short(it.name), "credits" to it.units, "instructor" to short(it.professor)) }.toMutableList()
        lists["classSessions"] = sessions.filter { !it.isArchived && it.id in courseIds }.flatMap { c -> c.sessions.mapNotNull { s ->
            val start = AcademicInputValidator.time(s.start); val end = AcademicInputValidator.time(s.end)
            if (s.day !in 0..6 || start == null || end == null || start >= end) null else
                mapOf("courseId" to c.id, "weekdaySaturdayZero" to s.day, "start" to start, "end" to end, "location" to short(s.location))
        } }.take(48).toMutableList()
        lists["attendance"] = attendance.filter { it.courseId in courseIds || active.any { c -> c.name == it.courseName } }.take(24).map {
            mapOf("course" to short(it.courseName), "absencesRecorded" to it.absentCount, "configuredLimit" to it.maxAllowed)
        }.toMutableList()
        lists["grades"] = recorded.take(24).map { mapOf("course" to short(it.courseName), "credits" to it.units, "scoreOutOf20" to it.midtermGrade + it.finalGrade) }.toMutableList()
        lists["tasks"] = tasks.filterNot { it.isCompleted }.sortedWith(compareBy({ JalaliCalendarUtil.parse(it.dueDate)?.format() ?: "9999" }, { it.id })).take(30).map {
            mapOf("id" to it.id, "title" to short(it.title, 180), "course" to short(it.courseName), "dueDate" to JalaliCalendarUtil.parse(it.dueDate)?.format())
        }.toMutableList()
        lists["exams"] = exams.sortedBy { JalaliCalendarUtil.parse(it.solarDate)?.format() ?: "9999" }.take(20).map {
            mapOf("course" to short(it.courseName), "date" to JalaliCalendarUtil.parse(it.solarDate)?.format(), "time" to AcademicInputValidator.time(it.time), "location" to short(it.location))
        }.toMutableList()
        val counts = mapOf(
            "courses" to active.size,
            "classSessions" to sessions.filter { !it.isArchived && it.id in courseIds }.sumOf { c -> c.sessions.count { s ->
                val start = AcademicInputValidator.time(s.start); val end = AcademicInputValidator.time(s.end)
                s.day in 0..6 && start != null && end != null && start < end
            } },
            "attendance" to attendance.count { it.courseId in courseIds || active.any { c -> c.name == it.courseName } },
            "grades" to recorded.size, "tasks" to tasks.count { !it.isCompleted }, "exams" to exams.size)
        val data = linkedMapOf<String, Any?>("schema" to 1, "recentLocalAnalysis" to recentLocalAnalysis.takeLast(2).map { it.take(1500) }, "todayJalali" to today.format(), "weekdaySaturdayZero" to calendar.get(java.util.Calendar.DAY_OF_WEEK) % 7,
            "localTime" to String.format(Locale.US, "%02d:%02d", calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE)),
            "timezone" to java.util.TimeZone.getDefault().id, "university" to short(profile.university), "major" to short(profile.major),
            "semester" to profile.currentSemester.takeIf { it > 0 }, "declaredCumulativeGpa" to profile.declaredGpa?.takeIf { it.isFinite() && it in 0.0..20.0 },
            "gpaOfRecordedGrades" to if (recorded.isEmpty()) null else recorded.sumOf { (it.midtermGrade + it.finalGrade) * it.units } / recorded.sumOf { it.units },
            "declaredPassedCredits" to (profile.declaredPassedCredits ?: profile.passedUnits.takeIf { it > 0 }),
            "dailyStudyGoalMinutes" to dailyGoal, "focusBlockMinutes" to focusMinutes,
            "studyHistory" to mapOf("minutesInRetainedHistory" to study.totalMinutes, "sessions" to study.totalSessions,
                "currentStreakDays" to study.currentStreak, "lastSevenDaysMinutes" to study.lastSevenDaysMinutes, "previousSevenDaysMinutes" to study.previousSevenDaysMinutes),
            "recordCounts" to mapOf("courses" to active.size, "openTasks" to tasks.count { !it.isCompleted }, "recordedGrades" to recorded.size, "exams" to exams.size),
            "missingValuePolicy" to "null means unknown; empty grades are not a GPA of zero; no free-time availability or verified university regulations supplied")
        data.putAll(lists)
        var json = adapter.toJson(data)
        // Remove lower-priority tail rows, never cut JSON mid-value.
        while (json.length > 15800 && lists.values.any { it.isNotEmpty() }) {
            lists.maxBy { it.value.size }.value.let { it.removeAt(it.lastIndex) }
            json = adapter.toJson(data)
        }
        data["omittedRows"] = counts.mapValues { (key, count) -> count - lists.getValue(key).size }
        return adapter.toJson(data).also { check(it.length <= 16000) }
    }
}

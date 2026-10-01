package com.example.domain.engine

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.model.AcademicContextAction
import com.example.domain.model.AcademicContextSnapshot
import com.example.domain.model.AcademicPriorityItem
import com.example.domain.model.ExamItem

object AcademicContextEngine {

    fun buildSnapshot(
        courses: List<CourseEntity>,
        coursesWithSessions: List<CourseWithSessions>,
        attendance: List<AttendanceEntity>,
        tasks: List<TaskEntity>,
        exams: List<ExamItem>,
        todayWeekdayIndex: Int,
        todayDate: String
    ): AcademicContextSnapshot {
        val normalizedToday = normalizeDate(todayDate)

        val todaySessions = coursesWithSessions
            .flatMap { relation ->
                relation.sessions
                    .filter { it.day == todayWeekdayIndex }
                    .map { relation.course to it }
            }
            .sortedBy { it.second.start }

        val openTasks = tasks.count { !it.isCompleted }
        val overdueTasks = tasks.count {
            !it.isCompleted &&
                it.dueDate.isNotBlank() &&
                normalizeDate(it.dueDate) < normalizedToday
        }

        val criticalAttendance = attendance.count {
            it.maxAllowed > 0 && it.absentCount >= it.maxAllowed
        }

        val examCourseNames = exams
            .map { it.courseName.trim() }
            .filter { it.isNotBlank() }
            .toSet()

        val examsWithOpenTasks = tasks.count {
            !it.isCompleted && examCourseNames.contains(it.courseName.trim())
        }

        val next = todaySessions.firstOrNull()
        val classMinutes = todaySessions.sumOf { (_, session) ->
            durationMinutes(session)
        }

        val topPriority: AcademicPriorityItem? = AcademicPriorityEngine.topOrNull(
            courses = courses,
            coursesWithSessions = coursesWithSessions,
            attendance = attendance,
            tasks = tasks,
            exams = exams,
            todayWeekdayIndex = todayWeekdayIndex,
            todayDate = todayDate
        )

        val action = when (topPriority?.kind) {
            com.example.domain.model.AcademicPriorityKind.CRITICAL_ATTENDANCE -> AcademicContextAction.OPEN_ATTENDANCE
            com.example.domain.model.AcademicPriorityKind.OVERDUE_TASK -> AcademicContextAction.OPEN_OVERDUE_TASKS
            com.example.domain.model.AcademicPriorityKind.EXAM_TODAY,
            com.example.domain.model.AcademicPriorityKind.EXAM_LINKED_TASK -> AcademicContextAction.REVIEW_EXAM
            com.example.domain.model.AcademicPriorityKind.NEXT_CLASS -> AcademicContextAction.OPEN_NEXT_CLASS
            com.example.domain.model.AcademicPriorityKind.TASK_TODAY,
            com.example.domain.model.AcademicPriorityKind.OPEN_TASK -> AcademicContextAction.START_FOCUS
            else -> AcademicContextAction.NONE
        }

        return AcademicContextSnapshot(
            todayWeekdayIndex = todayWeekdayIndex,
            todaySessionCount = todaySessions.size,
            todayClassMinutes = classMinutes,
            openTaskCount = openTasks,
            overdueTaskCount = overdueTasks,
            criticalAttendanceCount = criticalAttendance,
            examsWithOpenTasksCount = examsWithOpenTasks,
            totalActiveCourseCount = courses.distinctBy { it.id }.size,
            nextCourseId = next?.first?.id,
            nextCourseName = next?.first?.name,
            nextCourseStart = next?.second?.start,
            nextCourseEnd = next?.second?.end,
            nextCourseLocation = next?.second?.location?.takeIf { it.isNotBlank() },
            primaryAction = action,
            topPriority = topPriority
        )
    }

    private fun durationMinutes(session: CourseSessionEntity): Int {
        fun parse(value: String): Int? {
            val parts = value.trim().split(":")
            if (parts.size != 2) return null
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            return h * 60 + m
        }

        val start = parse(session.start) ?: return 0
        val end = parse(session.end) ?: return 0
        return (end - start).coerceAtLeast(0)
    }

    private fun normalizeDate(value: String): String = value
        .trim()
        .replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3')
        .replace('۴','4').replace('۵','5').replace('۶','6').replace('۷','7')
        .replace('۸','8').replace('۹','9')
}

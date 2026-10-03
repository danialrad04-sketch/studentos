package com.example.domain.engine

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.model.AcademicPriorityItem
import com.example.domain.model.AcademicPriorityKind
import com.example.domain.model.ExamItem
import java.util.Locale

object AcademicPriorityEngine {

    fun rank(
        courses: List<CourseEntity>,
        coursesWithSessions: List<CourseWithSessions>,
        attendance: List<AttendanceEntity>,
        tasks: List<TaskEntity>,
        exams: List<ExamItem>,
        todayWeekdayIndex: Int,
        todayDate: String,
        minuteOfDay: Int = 0
    ): List<AcademicPriorityItem> {
        val today = normalizeDate(todayDate)
        val result = mutableListOf<AcademicPriorityItem>()

        attendance
            .filter { it.maxAllowed > 0 && it.absentCount >= it.maxAllowed }
            .forEach { item ->
                result += AcademicPriorityItem(
                    id = "attendance:" + item.courseId,
                    kind = AcademicPriorityKind.CRITICAL_ATTENDANCE,
                    title = "غیبت بحرانی در " + item.courseName,
                    reason = "به سقف مجاز غیبت رسیده است؛ قبل از جلسه بعدی وضعیت را بررسی کن.",
                    score = 100,
                    courseId = item.courseId,
                    courseName = item.courseName
                )
            }

        tasks
            .filter { !it.isCompleted && it.dueDate.isNotBlank() && normalizeDate(it.dueDate) < today }
            .forEach { item ->
                result += AcademicPriorityItem(
                    id = "overdue-task:" + item.id,
                    kind = AcademicPriorityKind.OVERDUE_TASK,
                    title = "کار عقب‌افتاده: " + item.title,
                    reason = "موعد این کار گذشته است؛ ابتدا آن را تعیین تکلیف کن.",
                    score = 95,
                    courseId = item.courseId,
                    courseName = item.courseName,
                    dueDate = item.dueDate
                )
            }

        exams
            .filter { normalizeDate(it.solarDate) == today }
            .forEach { exam ->
                result += AcademicPriorityItem(
                    id = "exam-today:" + exam.id,
                    kind = AcademicPriorityKind.EXAM_TODAY,
                    title = "امتحان امروز: " + exam.courseName,
                    reason = "امروز برگزار می‌شود؛ زمان و مکان آزمون را بررسی کن.",
                    score = 92,
                    courseName = exam.courseName,
                    dueDate = exam.solarDate
                )
            }

        tasks
            .filter { !it.isCompleted && it.dueDate.isNotBlank() && normalizeDate(it.dueDate) == today }
            .forEach { item ->
                result += AcademicPriorityItem(
                    id = "task-today:" + item.id,
                    kind = AcademicPriorityKind.TASK_TODAY,
                    title = "موعد امروز: " + item.title,
                    reason = "این کار امروز سررسید می‌شود؛ انجامش را به تعویق نینداز.",
                    score = 88,
                    courseId = item.courseId,
                    courseName = item.courseName,
                    dueDate = item.dueDate
                )
            }

        val examCourseNames = exams
            .map { it.courseName.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
            .toSet()

        tasks
            .filter {
                !it.isCompleted &&
                    it.courseName.trim().lowercase(Locale.ROOT) in examCourseNames &&
                    normalizeDate(it.dueDate).isNotBlank()
            }
            .forEach { item ->
                result += AcademicPriorityItem(
                    id = "exam-linked-task:" + item.id,
                    kind = AcademicPriorityKind.EXAM_LINKED_TASK,
                    title = "کار مرتبط با امتحان: " + item.title,
                    reason = "این کار به درسی مرتبط است که برای آن امتحان ثبت شده است.",
                    score = 80,
                    courseId = item.courseId,
                    courseName = item.courseName,
                    dueDate = item.dueDate.takeIf { it.isNotBlank() }
                )
            }

        NextClassEngine.next(coursesWithSessions, todayWeekdayIndex, minuteOfDay)
            ?.takeIf { it.daysAhead == 0 }?.let { next ->
            val course = next.course
            val session = next.session
            result += AcademicPriorityItem(
                id = "next-class:" + session.id,
                kind = AcademicPriorityKind.NEXT_CLASS,
                title = (if (next.isOngoing) "کلاس در حال برگزاری: " else "کلاس بعدی: ") + course.name,
                reason = if (next.isOngoing) "این جلسه اکنون در حال برگزاری است." else "جلسه بعدی امروز در برنامه هفتگی توست.",
                score = 70,
                courseId = course.id,
                courseName = course.name
            )
        }

        tasks
            .filter { !it.isCompleted }
            .take(12)
            .forEach { item ->
                if (result.none {
                    it.courseId == item.courseId &&
                        it.kind in setOf(
                            AcademicPriorityKind.OVERDUE_TASK,
                            AcademicPriorityKind.TASK_TODAY,
                            AcademicPriorityKind.EXAM_LINKED_TASK
                        )
                }) {
                    result += AcademicPriorityItem(
                        id = "open-task:" + item.id,
                        kind = AcademicPriorityKind.OPEN_TASK,
                        title = "کار باز: " + item.title,
                        reason = "یک کار باز داری که می‌تواند وارد برنامه تمرکز شود.",
                        score = 50,
                        courseId = item.courseId,
                        courseName = item.courseName,
                        dueDate = item.dueDate.takeIf { it.isNotBlank() }
                    )
                }
            }

        return result.sortedWith(
            compareByDescending<AcademicPriorityItem> { it.score }
                .thenBy { it.title.length }
        )
    }

    fun topOrNull(
        courses: List<CourseEntity>,
        coursesWithSessions: List<CourseWithSessions>,
        attendance: List<AttendanceEntity>,
        tasks: List<TaskEntity>,
        exams: List<ExamItem>,
        todayWeekdayIndex: Int,
        todayDate: String,
        minuteOfDay: Int = 0
    ): AcademicPriorityItem? = rank(
        courses = courses,
        coursesWithSessions = coursesWithSessions,
        attendance = attendance,
        tasks = tasks,
        exams = exams,
        todayWeekdayIndex = todayWeekdayIndex,
        todayDate = todayDate,
        minuteOfDay = minuteOfDay
    ).firstOrNull()

    private fun normalizeDate(value: String): String {
        val parts = value.trim().replace('-', '/').split('/')
        if (parts.size != 3) return ""
        val year = parts[0].padStart(4, '0')
        val month = parts[1].padStart(2, '0')
        val day = parts[2].padStart(2, '0')
        return year + month + day
    }
}

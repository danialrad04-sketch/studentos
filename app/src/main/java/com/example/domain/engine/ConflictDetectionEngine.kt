package com.example.domain.engine

import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.data.local.util.DateTimeNormalizer
import com.example.data.parser.ParsedCourseDraft

enum class ConflictSeverity {
    ERROR,   // Critical: e.g. Exact time overlap or duplicate course
    WARNING, // Important: e.g. Back-to-back exams on same day or missing location
    INFO     // Notification: e.g. Unspecified exam time
}

enum class ConflictType {
    CLASS_CLASS_OVERLAP,
    EXAM_EXAM_OVERLAP,
    CLASS_EXAM_OVERLAP,
    DUPLICATE_COURSE_CODE,
    DUPLICATE_COURSE_NAME,
    INVALID_TIME_RANGE,
    INVALID_EXAM_DATE,
    INCOMPLETE_RECORD
}

data class CourseConflict(
    val type: ConflictType,
    val severity: ConflictSeverity,
    val course1Id: String,
    val course1Name: String,
    val course2Id: String? = null,
    val course2Name: String? = null,
    val description: String,
    val recommendation: String
)

/**
 * Pure Kotlin conflict detection engine for Student OS.
 * Detects Class/Class conflicts across all sessions, Exam/Exam conflicts, duplicate courses, and invalid time ranges.
 */
object ConflictDetectionEngine {

    /**
     * Checks all conflicts across a list of courses with their weekly sessions.
     */
    fun checkAllConflictsWithSessions(coursesWithSessions: List<CourseWithSessions>): List<CourseConflict> {
        val conflicts = mutableListOf<CourseConflict>()
        val active = coursesWithSessions.filter { !it.course.isArchived }

        // 1. Validate each course and its sessions
        for (item in active) {
            conflicts.addAll(validateSingleCourse(item.course))
            for (s in item.sessions) {
                conflicts.addAll(validateSingleSession(item.course, s))
            }
            // Check self-session overlaps within the same course
            for (i in item.sessions.indices) {
                for (j in i + 1 until item.sessions.size) {
                    val s1 = item.sessions[i]
                    val s2 = item.sessions[j]
                    if (s1.day == s2.day && DateTimeNormalizer.hasTimeConflict(s1.day, s1.start, s1.end, s2.day, s2.start, s2.end)) {
                        conflicts.add(
                            CourseConflict(
                                type = ConflictType.CLASS_CLASS_OVERLAP,
                                severity = ConflictSeverity.ERROR,
                                course1Id = item.course.id,
                                course1Name = item.course.name,
                                description = "تداخل جلسات هفتگی درس «${item.course.name}» با یکدیگر در روز یکسان (${s1.start} تا ${s1.end})",
                                recommendation = "ساعت یا روز یکی از جلسات درس را تغییر دهید."
                            )
                        )
                    }
                }
            }
        }

        // 2. Pairwise checks between different courses
        for (i in active.indices) {
            for (j in i + 1 until active.size) {
                val c1 = active[i]
                val c2 = active[j]

                // Check all session pairs
                val sList1 = c1.sessions
                val sList2 = c2.sessions

                var classOverlapFound = false
                for (s1 in sList1) {
                    for (s2 in sList2) {
                        if (!classOverlapFound && s1.day == s2.day && DateTimeNormalizer.hasTimeConflict(s1.day, s1.start, s1.end, s2.day, s2.start, s2.end)) {
                            conflicts.add(
                                CourseConflict(
                                    type = ConflictType.CLASS_CLASS_OVERLAP,
                                    severity = ConflictSeverity.ERROR,
                                    course1Id = c1.course.id,
                                    course1Name = c1.course.name,
                                    course2Id = c2.course.id,
                                    course2Name = c2.course.name,
                                    description = "تداخل زمانی جلسه کلاس «${c1.course.name}» و «${c2.course.name}» (${s1.start} تا ${s1.end})",
                                    recommendation = "ساعت برگزاری یا گروه درسی یکی از دروس را تغییر دهید."
                                )
                            )
                            classOverlapFound = true
                        }
                    }
                }

                // Exam vs Exam overlap
                if (c1.course.examDate.isNotBlank() && c2.course.examDate.isNotBlank() &&
                    c1.course.examDate.trim() == c2.course.examDate.trim()
                ) {
                    val timeOverlap = if (c1.course.examTime.isNotBlank() && c2.course.examTime.isNotBlank()) {
                        c1.course.examTime.trim() == c2.course.examTime.trim()
                    } else true

                    conflicts.add(
                        CourseConflict(
                            type = ConflictType.EXAM_EXAM_OVERLAP,
                            severity = if (timeOverlap) ConflictSeverity.ERROR else ConflictSeverity.WARNING,
                            course1Id = c1.course.id,
                            course1Name = c1.course.name,
                            course2Id = c2.course.id,
                            course2Name = c2.course.name,
                            description = "تداخل امتحان «${c1.course.name}» و «${c2.course.name}» در تاریخ ${c1.course.examDate}",
                            recommendation = "امکان برگزاری دو امتحان در یک روز یا همزمان وجود ندارد."
                        )
                    )
                }

                // Duplicate course code check for different courses sharing the same code
                if (c1.course.courseCode.isNotBlank() && c2.course.courseCode.isNotBlank() &&
                    c1.course.courseCode.trim().equals(c2.course.courseCode.trim(), ignoreCase = true) &&
                    !c1.course.name.trim().equals(c2.course.name.trim(), ignoreCase = true)
                ) {
                    conflicts.add(
                        CourseConflict(
                            type = ConflictType.DUPLICATE_COURSE_CODE,
                            severity = ConflictSeverity.ERROR,
                            course1Id = c1.course.id,
                            course1Name = c1.course.name,
                            course2Id = c2.course.id,
                            course2Name = c2.course.name,
                            description = "کد درس تکراری: ${c1.course.courseCode} برای هر دو درس «${c1.course.name}» و «${c2.course.name}»",
                            recommendation = "کد درس را بررسی یا درس تکراری را حذف نمایید."
                        )
                    )
                }
            }
        }

        return conflicts
    }

    /**
     * Checks all conflicts across a list of courses (flat legacy support).
     */
    fun checkAllConflicts(courses: List<CourseEntity>): List<CourseConflict> {
        val wrapped = courses.map { CourseWithSessions(course = it, sessions = emptyList()) }
        return checkAllConflictsWithSessions(wrapped)
    }

    /**
     * Checks conflicts for a single incoming or edited course with its sessions against existing courses.
     */
    fun checkCourseWithSessionsConflicts(
        targetCourse: CourseEntity,
        targetSessions: List<CourseSessionEntity>,
        existingWithSessions: List<CourseWithSessions>
    ): List<CourseConflict> {
        val conflicts = mutableListOf<CourseConflict>()
        conflicts.addAll(validateSingleCourse(targetCourse))
        for (s in targetSessions) {
            conflicts.addAll(validateSingleSession(targetCourse, s))
        }

        val otherCourses = existingWithSessions.filter { it.course.id != targetCourse.id && !it.course.isArchived }

        for (c in otherCourses) {
            val sList = c.sessions

            var overlapFound = false
            for (ts in targetSessions) {
                for (es in sList) {
                    if (!overlapFound && ts.day == es.day &&
                        DateTimeNormalizer.hasTimeConflict(ts.day, ts.start, ts.end, es.day, es.start, es.end)
                    ) {
                        conflicts.add(
                            CourseConflict(
                                type = ConflictType.CLASS_CLASS_OVERLAP,
                                severity = ConflictSeverity.ERROR,
                                course1Id = targetCourse.id,
                                course1Name = targetCourse.name,
                                course2Id = c.course.id,
                                course2Name = c.course.name,
                                description = "تداخل زمانی با کلاس «${c.course.name}» (${ts.start} تا ${ts.end})",
                                recommendation = "ساعت برگزاری یکی از دو کلاس را تغییر دهید."
                            )
                        )
                        overlapFound = true
                    }
                }
            }

            // Exam overlap
            if (targetCourse.examDate.isNotBlank() && c.course.examDate.isNotBlank() &&
                targetCourse.examDate.trim() == c.course.examDate.trim()
            ) {
                val timeOverlap = if (targetCourse.examTime.isNotBlank() && c.course.examTime.isNotBlank()) {
                    targetCourse.examTime.trim() == c.course.examTime.trim()
                } else true

                conflicts.add(
                    CourseConflict(
                        type = ConflictType.EXAM_EXAM_OVERLAP,
                        severity = if (timeOverlap) ConflictSeverity.ERROR else ConflictSeverity.WARNING,
                        course1Id = targetCourse.id,
                        course1Name = targetCourse.name,
                        course2Id = c.course.id,
                        course2Name = c.course.name,
                        description = "تداخل تاریخ امتحان با «${c.course.name}» در ${c.course.examDate}",
                        recommendation = "برنامه‌ریزی امتحان‌ها را بازبینی کنید."
                    )
                )
            }

            // Duplicate code for different course names
            if (targetCourse.courseCode.isNotBlank() && c.course.courseCode.isNotBlank() &&
                targetCourse.courseCode.trim().equals(c.course.courseCode.trim(), ignoreCase = true) &&
                !targetCourse.name.trim().equals(c.course.name.trim(), ignoreCase = true)
            ) {
                conflicts.add(
                    CourseConflict(
                        type = ConflictType.DUPLICATE_COURSE_CODE,
                        severity = ConflictSeverity.ERROR,
                        course1Id = targetCourse.id,
                        course1Name = targetCourse.name,
                        course2Id = c.course.id,
                        course2Name = c.course.name,
                        description = "کد درس ${targetCourse.courseCode} قبلاً برای «${c.course.name}» ثبت شده است.",
                        recommendation = "از کد درس منحصر‌به‌فرد استفاده کنید."
                    )
                )
            }
        }

        return conflicts
    }

    /**
     * Checks conflicts for a single incoming or edited course against existing courses (flat legacy).
     */
    fun checkCourseConflicts(targetCourse: CourseEntity, existingCourses: List<CourseEntity>): List<CourseConflict> {
        val existingWrapped = existingCourses.map { CourseWithSessions(course = it, sessions = emptyList()) }
        return checkCourseWithSessionsConflicts(targetCourse, emptyList(), existingWrapped)
    }

    /**
     * Validates a draft course from Bulk Import or AI parser against existing semester courses.
     */
    fun validateDraft(draft: ParsedCourseDraft, existingCourses: List<CourseEntity>): List<CourseConflict> {
        val course = CourseEntity(
            id = draft.tempId,
            name = draft.name,
            colorHex = "#3B82F6",
            units = draft.units,
            courseCode = draft.courseCode,
            examDate = draft.examDate,
            examTime = draft.examTime,
            examLocation = draft.examLocation
        )
        val session = CourseSessionEntity(
            id = "sess_${draft.tempId}",
            courseId = draft.tempId,
            day = draft.dayOfWeek,
            start = draft.startTime,
            end = draft.endTime,
            location = draft.location
        )
        val existingWrapped = existingCourses.map { CourseWithSessions(course = it, sessions = emptyList()) }
        return checkCourseWithSessionsConflicts(course, listOf(session), existingWrapped)
    }

    private fun validateSingleCourse(course: CourseEntity): List<CourseConflict> {
        val list = mutableListOf<CourseConflict>()

        // Check empty course name
        if (course.name.isBlank()) {
            list.add(
                CourseConflict(
                    type = ConflictType.INCOMPLETE_RECORD,
                    severity = ConflictSeverity.ERROR,
                    course1Id = course.id,
                    course1Name = "نامشخص",
                    description = "نام درس نمی‌تواند خالی باشد.",
                    recommendation = "نام کامل درس را وارد کنید."
                )
            )
        }

        // Check units
        if (course.units <= 0 || course.units > 8) {
            list.add(
                CourseConflict(
                    type = ConflictType.INCOMPLETE_RECORD,
                    severity = ConflictSeverity.WARNING,
                    course1Id = course.id,
                    course1Name = course.name,
                    description = "تعداد واحد نامتعارف (${course.units} واحد). واحدهای معتبر بین 1 تا 6 هستند.",
                    recommendation = "تعداد واحد معتبر برای این درس وارد کنید."
                )
            )
        }

        return list
    }

    private fun validateSingleSession(course: CourseEntity, session: CourseSessionEntity): List<CourseConflict> {
        val list = mutableListOf<CourseConflict>()
        val startMin = parseTimeToMinutes(session.start)
        val endMin = parseTimeToMinutes(session.end)
        if (startMin != null && endMin != null) {
            if (endMin <= startMin) {
                list.add(
                    CourseConflict(
                        type = ConflictType.INVALID_TIME_RANGE,
                        severity = ConflictSeverity.ERROR,
                        course1Id = course.id,
                        course1Name = course.name,
                        description = "ساعت پایان (${session.end}) باید پس از ساعت شروع (${session.start}) باشد.",
                        recommendation = "بازه زمانی جلسه را تصحیح نمایید."
                    )
                )
            } else if (endMin - startMin < 30) {
                list.add(
                    CourseConflict(
                        type = ConflictType.INVALID_TIME_RANGE,
                        severity = ConflictSeverity.WARNING,
                        course1Id = course.id,
                        course1Name = course.name,
                        description = "مدت جلسه کلاس کمتر از 30 دقیقه است.",
                        recommendation = "ساعت شروع و پایان جلسه را بررسی نمایید."
                    )
                )
            }
        }
        return list
    }

    private fun parseTimeToMinutes(timeStr: String): Int? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }
}

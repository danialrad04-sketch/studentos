package com.example.domain.engine

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.model.AcademicRisk
import com.example.domain.model.AcademicRiskType
import com.example.domain.model.CourseState
import com.example.domain.model.EvaluatedCurriculumCourse
import com.example.domain.model.GlobalSearchResult
import com.example.domain.model.RiskSeverity
import com.example.domain.model.SemesterPlan
import com.example.domain.model.SemesterPlanComparison
import com.example.domain.model.StudySessionRecommendation
import com.example.domain.model.WeeklyAcademicWorkload
import com.example.domain.model.ExamItem

/**
 * Pure Kotlin Domain Engines for Advanced Student OS Features:
 * - AcademicRiskEngine (Phase 21)
 * - WorkloadEngine (Phase 20)
 * - SemesterPlannerEngine (Phase 12 & 13)
 * - StudyPlannerEngine (Phase 28)
 */

object AcademicRiskEngine {

    /**
     * Evaluates academic risks across attendance records, exams, deadlines, and courses.
     * All messages are formulated constructively without fear-based terminology.
     */
    fun evaluateRisks(
        courses: List<CourseEntity>,
        attendanceList: List<AttendanceEntity>,
        tasks: List<TaskEntity>,
        exams: List<ExamItem>
    ): List<AcademicRisk> {
        val risks = mutableListOf<AcademicRisk>()

        // 1. Attendance Radar Analysis
        val attendanceMap = attendanceList.associateBy { it.courseName }
        courses.forEach { course ->
            val att = attendanceMap[course.name]
            val absences = att?.absentCount ?: 0
            val maxAllowed = att?.maxAllowed ?: 3
            if (absences >= maxAllowed) {
                risks.add(
                    AcademicRisk(
                        id = "att_crit_${course.id}",
                        title = "آستانه غیبت در درس ${course.name}",
                        description = "$absences غیبت از حداکثر $maxAllowed جلسه مجاز ثبت شده است.",
                        severity = RiskSeverity.CRITICAL,
                        riskType = AcademicRiskType.ATTENDANCE_BREACH,
                        recommendedAction = "حضور منظم در جلسات باقی‌مانده و هماهنگی با استاد جهت جلوگیری از حذف آموزشی."
                    )
                )
            } else if (absences >= maxAllowed - 1 && maxAllowed > 1) {
                risks.add(
                    AcademicRisk(
                        id = "att_warn_${course.id}",
                        title = "نزدیک به سقف غیبت: ${course.name}",
                        description = "تنها 1 جلسه غیبت مجاز تا مرز حذف درس باقی مانده است.",
                        severity = RiskSeverity.HIGH,
                        riskType = AcademicRiskType.ATTENDANCE_BREACH,
                        recommendedAction = "اولویت‌دهی به حضور در جلسات آینده این درس."
                    )
                )
            }
        }

        // 2. Exam Density Analysis (e.g. Multiple exams within the same timeframe)
        val examsByDate = exams.groupBy { it.solarDate.trim() }.filter { it.key.isNotBlank() }
        examsByDate.forEach { (date, examsOnDate) ->
            if (examsOnDate.size > 1) {
                risks.add(
                    AcademicRisk(
                        id = "exam_col_$date",
                        title = "تراکم امتحان در تاریخ $date",
                        description = "${examsOnDate.size} آزمون در این روز برنامه‌ریزی شده است: " +
                                examsOnDate.joinToString(" و ") { it.courseName },
                        severity = RiskSeverity.MEDIUM,
                        riskType = AcademicRiskType.EXAM_COLLISION,
                        recommendedAction = "توزیع زودهنگام مباحث مطالعه از روزهای قبل برای جلوگیری از همپوشانی."
                    )
                )
            }
        }

        // 3. Planning Collisions (Task ↔ Exam and same-day task clusters)
        AcademicPlanningCollisionEngine.detect(
            tasks = tasks,
            exams = exams
        ).forEach { collision ->
            risks.add(
                AcademicRisk(
                    id = "planning_" + collision.id,
                    title = collision.title,
                    description = collision.description,
                    severity = if (collision.severity == com.example.domain.model.AcademicPlanningCollisionSeverity.HIGH) {
                        RiskSeverity.HIGH
                    } else {
                        RiskSeverity.MEDIUM
                    },
                    riskType = AcademicRiskType.UPCOMING_DEADLINE,
                    recommendedAction = "یکی از فعالیت‌ها را زودتر انجام بده یا موعدها را جابه‌جا کن تا فشار روز موردنظر کاهش یابد."
                )
            )
        }

        // 3. Pending Overdue Tasks
        val overdueTasks = tasks.filter { !it.isCompleted && it.dueDate.isNotBlank() }
        if (overdueTasks.size >= 3) {
            risks.add(
                AcademicRisk(
                    id = "task_overload",
                    title = "تراکم تکالیف تکمیل‌نشده",
                    description = "${overdueTasks.size} تکلیف یا پروژه معوقه در جریان است.",
                    severity = RiskSeverity.MEDIUM,
                    riskType = AcademicRiskType.UPCOMING_DEADLINE,
                    recommendedAction = "برگزاری جلسه تمرکز (پومودورو) جهت تحویل گام‌به‌گام وظایف دارای اولویت بالاتر."
                )
            )
        }

        return risks
    }
}

object WorkloadEngine {

    /**
     * Estimates weekly academic workload hours (برآورد مطالعه).
     * Clearly deterministic and marked as an estimate.
     */
    fun calculateWeeklyWorkload(
        enrolledCourses: List<CourseEntity>,
        activeTasks: List<TaskEntity>
    ): WeeklyAcademicWorkload {
        val totalUnits = enrolledCourses.distinctBy { it.id }.sumOf { it.units }
        val lectureHours = totalUnits * 1.5 // 1.5 hours per unit standard in universities
        val labCourses = enrolledCourses.count { it.name.contains("آزمایشگاه") || it.name.contains("کارگاه") }
        val labHours = labCourses * 2.5 // Average lab session

        val activeTaskCount = activeTasks.count { !it.isCompleted }
        val assignmentHours = activeTaskCount * 1.5 // Approx 1.5 hours per task

        // 2 hours self-study per credit standard recommendation
        val selfStudyEstimate = (totalUnits * 2.0).coerceAtLeast(4.0)

        val totalEstimated = lectureHours + labHours + assignmentHours + selfStudyEstimate

        return WeeklyAcademicWorkload(
            classLectureHours = lectureHours,
            laboratoryHours = labHours,
            assignmentHours = assignmentHours,
            estimatedSelfStudyHours = selfStudyEstimate,
            totalEstimatedWeeklyHours = totalEstimated,
            isEstimateOnly = true
        )
    }
}

object SemesterPlannerEngine {

    /**
     * Builds candidate semester plans based on available courses and unit constraints.
     */
    fun generateCandidatePlans(
        availableCourses: List<EvaluatedCurriculumCourse>
    ): List<SemesterPlan> {
        if (availableCourses.isEmpty()) return emptyList()

        // Plan A: Balanced Standard Load (~16-18 credits)
        val sortedByRecommendation = availableCourses.sortedBy { it.recommendedSemester }
        var sumUnitsA = 0
        val coursesA = mutableListOf<EvaluatedCurriculumCourse>()
        for (course in sortedByRecommendation) {
            if (sumUnitsA + course.units <= 18) {
                coursesA.add(course)
                sumUnitsA += course.units
            }
        }

        val planA = SemesterPlan(
            id = "plan_balanced",
            name = "برنامه متعادل (استاندارد)",
            totalCredits = sumUnitsA,
            courses = coursesA,
            estimatedWeeklyWorkloadHours = sumUnitsA * 3.5,
            isDraft = false,
            isActive = true,
            tradeOffs = listOf(
                "تراکم زمانی متعادل و فرصت کافی برای تکالیف",
                "ریسک غیبت و خستگی درسی پایین‌تر"
            )
        )

        // Plan B: High Pace Load (~19-21 credits)
        var sumUnitsB = 0
        val coursesB = mutableListOf<EvaluatedCurriculumCourse>()
        for (course in sortedByRecommendation) {
            if (sumUnitsB + course.units <= 21) {
                coursesB.add(course)
                sumUnitsB += course.units
            }
        }

        val planB = SemesterPlan(
            id = "plan_accelerated",
            name = "برنامه پیشتاز (واحدهای بیشتر)",
            totalCredits = sumUnitsB,
            courses = coursesB,
            estimatedWeeklyWorkloadHours = sumUnitsB * 3.8,
            isDraft = true,
            isActive = false,
            tradeOffs = listOf(
                "اتمام سریع‌تر واحدهای درسی چارت",
                "نیاز به ساعات مطالعه هفتگی فشرده‌تر"
            )
        )

        return listOf(planA, planB)
    }

    /**
     * Compares two semester plans objectively.
     */
    fun comparePlans(planA: SemesterPlan, planB: SemesterPlan): SemesterPlanComparison {
        val diffCredits = planB.totalCredits - planA.totalCredits
        val diffHours = planB.estimatedWeeklyWorkloadHours - planA.estimatedWeeklyWorkloadHours
        val summary = if (diffCredits > 0) {
            "برنامه ${planB.name} دارای $diffCredits واحد بیشتر است که برآورد مطالعه هفتگی را حدود ${diffHours.toInt()} ساعت افزایش می‌دهد."
        } else {
            "هر دو برنامه از نظر تعداد واحد برابر هستند اما چینش دروس متفاوتی دارند."
        }

        return SemesterPlanComparison(
            planA = planA,
            planB = planB,
            creditDifference = diffCredits,
            workloadDifferenceHours = diffHours,
            summaryTradeOff = summary
        )
    }
}

object StudyPlannerEngine {

    /**
     * Backward-compatible planner entry point.
     * Uses deterministic date urgency without requiring additional repository data.
     */
    fun generateStudyPlan(
        exams: List<ExamItem>,
        tasks: List<TaskEntity>
    ): List<StudySessionRecommendation> = generateStudyPlan(
        exams = exams,
        tasks = tasks,
        attendanceList = emptyList(),
        todayDate = com.example.domain.util.JalaliCalendarUtil.today().format("/")
    )

    /**
     * v2 planner:
     * - overdue tasks get the highest task urgency
     * - tasks due today outrank ordinary open tasks
     * - tasks linked to an exam are promoted
     * - nearest known exam is promoted
     * - output is stable and deterministic
     */
    fun generateStudyPlan(
        exams: List<ExamItem>,
        tasks: List<TaskEntity>,
        attendanceList: List<AttendanceEntity>,
        todayDate: String
    ): List<StudySessionRecommendation> {
        val today = normalizeDate(todayDate)

        val upcomingExams = exams
            .filter {
                it.courseName.isNotBlank() &&
                    normalizeDate(it.solarDate).isNotBlank() &&
                    normalizeDate(it.solarDate) >= today
            }
            .distinctBy { it.id }
            .sortedWith(
                compareBy<ExamItem>({ normalizeDate(it.solarDate) }, { it.time.trim() }, { it.courseName.trim() })
            )

        val examNames = upcomingExams
            .map { it.courseName.trim().lowercase() }
            .toSet()

        val recommendations = mutableListOf<Pair<Int, StudySessionRecommendation>>()

        upcomingExams.take(4).forEachIndexed { index, exam ->
            val date = normalizeDate(exam.solarDate)
            val score = when {
                date == today -> 100
                index == 0 -> 88
                index == 1 -> 78
                else -> 68
            }
            val minutes = when {
                date == today -> 75
                index == 0 -> 60
                index == 1 -> 50
                else -> 40
            }
            recommendations += score to StudySessionRecommendation(
                id = "study_exam_" + exam.id,
                courseName = exam.courseName.trim(),
                recommendedDurationMinutes = minutes,
                priorityReason = "آمادگی آزمون مورخ " + exam.solarDate.trim(),
                targetType = "آمادگی آزمون"
            )
        }

        tasks
            .filter { !it.isCompleted && it.courseName.isNotBlank() && it.title.isNotBlank() }
            .distinctBy { it.id }
            .forEach { task ->
                val due = normalizeDate(task.dueDate)
                val linkedToExam = examNames.contains(task.courseName.trim().lowercase())
                val overdue = due.isNotBlank() && due < today
                val dueToday = due.isNotBlank() && due == today

                val score = when {
                    overdue && linkedToExam -> 110
                    overdue -> 102
                    dueToday && linkedToExam -> 96
                    dueToday -> 91
                    linkedToExam -> 84
                    else -> 62
                }

                val minutes = when {
                    overdue && linkedToExam -> 60
                    overdue -> 50
                    dueToday -> 45
                    linkedToExam -> 45
                    else -> 30
                }

                val reason = when {
                    overdue && linkedToExam -> "کار عقب‌افتاده و مرتبط با امتحان " + task.courseName.trim()
                    overdue -> "کار عقب‌افتاده: " + task.title.trim()
                    dueToday && linkedToExam -> "موعد امروز و مرتبط با امتحان " + task.courseName.trim()
                    dueToday -> "موعد امروز: " + task.title.trim()
                    linkedToExam -> "کار مرتبط با امتحان درس " + task.courseName.trim()
                    else -> "انجام کار: " + task.title.trim()
                }

                recommendations += score to StudySessionRecommendation(
                    id = "study_task_" + task.id,
                    courseName = task.courseName.trim(),
                    recommendedDurationMinutes = minutes,
                    priorityReason = reason,
                    targetType = "تکمیل تکلیف"
                )
            }

        val criticalAttendanceCourses = attendanceList
            .filter { it.maxAllowed > 0 && it.absentCount >= it.maxAllowed }
            .map { it.courseName.trim().lowercase() }
            .toSet()

        val adjusted = recommendations.map { (score, recommendation) ->
            if (criticalAttendanceCourses.contains(recommendation.courseName.trim().lowercase())) {
                (score + 6).coerceAtMost(120) to recommendation.copy(
                    priorityReason = recommendation.priorityReason + " · این درس در رادار غیبت هم نیازمند توجه است."
                )
            } else {
                score to recommendation
            }
        }

        return adjusted
            .sortedWith(
                compareByDescending<Pair<Int, StudySessionRecommendation>> { it.first }
                    .thenBy { it.second.courseName }
                    .thenBy { it.second.id }
            )
            .map { it.second }
            .distinctBy { it.id }
            .take(8)
    }

    private fun normalizeDate(value: String): String {
        val raw = value
            .trim()
            .replace('-', '/')
            .replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3')
            .replace('۴','4').replace('۵','5').replace('۶','6').replace('۷','7')
            .replace('۸','8').replace('۹','9')
        val parts = raw.split('/')
        if (parts.size != 3) return ""
        return parts[0].padStart(4, '0') +
            parts[1].padStart(2, '0') +
            parts[2].padStart(2, '0')
    }
}

object GlobalSearchEngine {

    /**
     * Phase 24: Deterministic multi-domain global search engine across enrolled courses,
     * official curriculum chart, student tasks, exam schedules, and notes.
     */
    fun search(
        query: String,
        courses: List<CourseEntity>,
        curriculumCourses: List<EvaluatedCurriculumCourse>,
        tasks: List<TaskEntity>,
        exams: List<ExamItem>,
        notes: String
    ): List<GlobalSearchResult> {
        val trimmed = query.trim()
        val results = mutableListOf<GlobalSearchResult>()

        // 1. Enrolled Courses
        courses.distinctBy { it.id }.forEach { course ->
            if (trimmed.isBlank() ||
                course.name.contains(trimmed, ignoreCase = true) ||
                course.courseCode.contains(trimmed, ignoreCase = true) ||
                course.examLocation.contains(trimmed, ignoreCase = true) ||
                course.professor.contains(trimmed, ignoreCase = true)
            ) {
                results.add(
                    GlobalSearchResult.CourseItem(
                        courseId = course.id,
                        name = course.name,
                        code = course.courseCode.ifBlank { "بدون کد" },
                        units = course.units,
                        state = CourseState.CURRENT
                    )
                )
            }
        }

        // 2. Official Curriculum Chart
        curriculumCourses.forEach { currCourse ->
            if (trimmed.isBlank() ||
                currCourse.name.contains(trimmed, ignoreCase = true) ||
                currCourse.courseCode.contains(trimmed, ignoreCase = true)
            ) {
                // Avoid duplication if already present as enrolled CourseItem
                val alreadyAdded = results.any { it is GlobalSearchResult.CourseItem && it.name.trim().equals(currCourse.name.trim(), ignoreCase = true) }
                if (!alreadyAdded) {
                    results.add(
                        GlobalSearchResult.CourseItem(
                            courseId = currCourse.courseId,
                            name = currCourse.name,
                            code = currCourse.courseCode,
                            units = currCourse.units,
                            state = currCourse.state
                        )
                    )
                }
            }
        }

        // 3. Tasks
        tasks.forEach { task ->
            if (trimmed.isBlank() ||
                task.title.contains(trimmed, ignoreCase = true) ||
                task.courseName.contains(trimmed, ignoreCase = true)
            ) {
                results.add(
                    GlobalSearchResult.TaskItem(
                        taskId = task.id,
                        title = task.title,
                        courseName = task.courseName,
                        deadline = task.dueDate,
                        isCompleted = task.isCompleted
                    )
                )
            }
        }

        // 4. Exams
        exams.forEach { exam ->
            if (trimmed.isBlank() ||
                exam.courseName.contains(trimmed, ignoreCase = true) ||
                exam.solarDate.contains(trimmed, ignoreCase = true) ||
                exam.location.contains(trimmed, ignoreCase = true)
            ) {
                results.add(
                    GlobalSearchResult.ExamItem(
                        examId = exam.id.hashCode().toLong(),
                        courseName = exam.courseName,
                        examDate = exam.solarDate,
                        examTime = exam.time
                    )
                )
            }
        }

        // 5. Notes & Formulas
        if (notes.isNotBlank()) {
            notes.lines().filter { it.isNotBlank() }.forEach { line ->
                if (trimmed.isBlank() || line.contains(trimmed, ignoreCase = true)) {
                    val title = if (line.contains(":")) line.substringBefore(":").trim() else "یادداشت فرمول"
                    val content = if (line.contains(":")) line.substringAfter(":").trim() else line.trim()
                    results.add(
                        GlobalSearchResult.NoteFormulaItem(
                            title = title,
                            courseName = "یادداشت‌های تحصیلی",
                            content = content
                        )
                    )
                }
            }
        }

        return results
    }
}

package com.example.domain.engine

import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.model.AcademicBadge
import com.example.domain.model.BadgeCategory
import com.example.domain.model.BadgeTier
import com.example.domain.model.StudentGamificationProfile

/**
 * Pure Kotlin Deterministic Engine for Academic Gamification & Badges (Phase v5).
 * Accurately analyzes student habits, tasks completed, attendance status, and study sessions.
 */
object AcademicGamificationEngine {

    fun calculateGamificationProfile(
        tasks: List<TaskEntity>,
        attendanceList: List<AttendanceEntity>,
        grades: List<GradeEntity>,
        passedUnits: Int,
        pomodoroSecondsTotal: Int = 0
    ): StudentGamificationProfile {
        val completedTasks = tasks.count { it.isCompleted }
        val totalTasks = tasks.size
        val zeroAbsenceCourses = attendanceList.count { it.absentCount == 0 }
        val focusHours = (pomodoroSecondsTotal.toDouble() / 3600.0)

        val totalUnits = grades.sumOf { it.units }
        val totalScore = grades.sumOf { ((it.midtermGrade + it.finalGrade) / 2.0) * it.units }
        val gpa = if (totalUnits > 0) (totalScore / totalUnits.toDouble()).coerceIn(0.0, 20.0) else 0.0

        val badges = mutableListOf<AcademicBadge>()

        // 1. Task Badges
        val taskProgress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks.toFloat() else 0f
        badges.add(
            AcademicBadge(
                id = "task_disciplined_1",
                title = "شروع‌کننده منظم",
                description = "تکمیل حداقل 1 تکلیف درسی",
                iconEmoji = "✍️",
                category = BadgeCategory.TASK_CRUSHER,
                tier = BadgeTier.BRONZE,
                isUnlocked = completedTasks >= 1,
                progress = (completedTasks.toFloat() / 1f).coerceIn(0f, 1f),
                progressText = "$completedTasks از 1 تکلیف",
                xpReward = 50
            )
        )
        badges.add(
            AcademicBadge(
                id = "task_master_5",
                title = "انضباط فولادی",
                description = "تکمیل 5 تکلیف و تحویل به موقع",
                iconEmoji = "📋",
                category = BadgeCategory.TASK_CRUSHER,
                tier = BadgeTier.SILVER,
                isUnlocked = completedTasks >= 5,
                progress = (completedTasks.toFloat() / 5f).coerceIn(0f, 1f),
                progressText = "$completedTasks از 5 تکلیف",
                xpReward = 150
            )
        )

        // 2. Attendance Badges
        val attZeroCount = attendanceList.count { it.absentCount == 0 }
        badges.add(
            AcademicBadge(
                id = "att_zero_hero",
                title = "نگهبان صندلی اول",
                description = "حفظ غیبت صفر در حداقل 3 درس",
                iconEmoji = "🛡️",
                category = BadgeCategory.ATTENDANCE_SHIELD,
                tier = BadgeTier.GOLD,
                isUnlocked = attZeroCount >= 3,
                progress = (attZeroCount.toFloat() / 3f).coerceIn(0f, 1f),
                progressText = "$attZeroCount از 3 درس",
                xpReward = 200
            )
        )
        val noCriticalAttendance = attendanceList.none { it.absentCount >= it.maxAllowed && it.maxAllowed > 0 }
        badges.add(
            AcademicBadge(
                id = "att_safe_radar",
                title = "رادار امن",
                description = "عدم رسیدن به حد نصاب بحرانی غیبت در تمامی دروس",
                iconEmoji = "🚨",
                category = BadgeCategory.ATTENDANCE_SHIELD,
                tier = BadgeTier.SILVER,
                isUnlocked = noCriticalAttendance && attendanceList.isNotEmpty(),
                progress = if (noCriticalAttendance) 1f else 0.5f,
                progressText = if (noCriticalAttendance) "100% امن" else "هشدار غیبت",
                xpReward = 120
            )
        )

        // 3. Focus & Study Badges
        badges.add(
            AcademicBadge(
                id = "focus_flow_beginner",
                title = "موتور تمرکز عمیق",
                description = "ثبت حداقل 2 ساعت تمرکز فعال پومودورو",
                iconEmoji = "⚡",
                category = BadgeCategory.STUDY_FOCUS,
                tier = BadgeTier.BRONZE,
                isUnlocked = focusHours >= 2.0,
                progress = (focusHours.toFloat() / 2f).coerceIn(0f, 1f),
                progressText = "$focusHours از 2 ساعت",
                xpReward = 80
            )
        )
        badges.add(
            AcademicBadge(
                id = "focus_flow_marathon",
                title = "ماراتن مطالعه علمی",
                description = "ثبت 10 ساعت مطالعه پیوسته در طول ترم",
                iconEmoji = "🧠",
                category = BadgeCategory.STUDY_FOCUS,
                tier = BadgeTier.PLATINUM,
                isUnlocked = focusHours >= 10.0,
                progress = (focusHours.toFloat() / 10f).coerceIn(0f, 1f),
                progressText = "$focusHours از 10 ساعت",
                xpReward = 350
            )
        )

        // 4. Curriculum & Progress Badges
        badges.add(
            AcademicBadge(
                id = "curr_pioneer_40",
                title = "فاتح دوره پایه",
                description = "گذراندن بیش از 30 واحد تخصصی و عمومی از چارت مصوب",
                iconEmoji = "🗺️",
                category = BadgeCategory.CURRICULUM_PIONEER,
                tier = BadgeTier.SILVER,
                isUnlocked = passedUnits >= 30,
                progress = (passedUnits.toFloat() / 30f).coerceIn(0f, 1f),
                progressText = "$passedUnits از 30 واحد",
                xpReward = 200
            )
        )
        badges.add(
            AcademicBadge(
                id = "curr_pioneer_80",
                title = "نیمه پر فنجان مهندسی",
                description = "گذراندن بیش از 70 واحد چارت مصوب",
                iconEmoji = "🧪",
                category = BadgeCategory.CURRICULUM_PIONEER,
                tier = BadgeTier.GOLD,
                isUnlocked = passedUnits >= 70,
                progress = (passedUnits.toFloat() / 70f).coerceIn(0f, 1f),
                progressText = "$passedUnits از 70 واحد",
                xpReward = 400
            )
        )

        // 5. Academic Excellence Badges
        badges.add(
            AcademicBadge(
                id = "gpa_honors_17",
                title = "دانشجوی ممتاز (رتبه الف)",
                description = "دستیابی به معدل ترمیک یا کل بالاتر از 17.00",
                iconEmoji = "🏆",
                category = BadgeCategory.ACADEMIC_EXCELLENCE,
                tier = BadgeTier.DIAMOND,
                isUnlocked = gpa >= 17.0,
                progress = (gpa.toFloat() / 17f).coerceIn(0f, 1f),
                progressText = if (gpa > 0) String.format(java.util.Locale.US, "معدل %.2f", gpa) else "در انتظار نمرات",
                xpReward = 500
            )
        )

        // Calculate Level & XP
        val baseTasksXp = completedTasks * 40
        val baseAttendanceXp = zeroAbsenceCourses * 50
        val baseStudyXp = (focusHours * 30).toInt()
        val badgesXp = badges.filter { it.isUnlocked }.sumOf { it.xpReward }
        val totalXp = baseTasksXp + baseAttendanceXp + baseStudyXp + badgesXp + (passedUnits * 5)

        val xpPerLevel = 300
        val level = (totalXp / xpPerLevel) + 1
        val currentLevelProgressXp = totalXp % xpPerLevel
        val nextLevelTargetXp = xpPerLevel

        val levelTitle = when (level) {
            1 -> "دانشجوی کاوشگر"
            2 -> "رهجوی آکادمیک"
            3 -> "پژوهشگر باانگیزه"
            4 -> "استراتژیست درسی"
            5 -> "مهندس ارشد ترم"
            else -> "نخبه علمی دانشکده"
        }

        val achievements = mutableListOf<String>()
        if (completedTasks > 0) achievements.add("انجام $completedTasks تکلیف موفق")
        if (zeroAbsenceCourses > 0) achievements.add("$zeroAbsenceCourses درس بدون حتی یک غیبت")
        if (gpa >= 16.0) achievements.add("معدل عالی $gpa")
        achievements.add("$passedUnits واحد مصوب در پاسپورت")

        return StudentGamificationProfile(
            level = level,
            currentXp = currentLevelProgressXp,
            nextLevelXp = nextLevelTargetXp,
            levelTitle = levelTitle,
            // Daily streak persistence is not modeled yet; do not fabricate a streak.
            studyStreakDays = 0,
            bestStreakDays = 0,
            focusHoursTotal = focusHours,
            completedTasksCount = completedTasks,
            perfectAttendanceCount = zeroAbsenceCourses,
            badges = badges,
            recentAchievements = achievements
        )
    }
}

package com.example.domain.model

/**
 * Phase v5: Student OS Gamification, Streaks & Academic Badges System.
 * Rewarding continuous discipline, focus sessions, attendance vigilance, and curriculum mastery.
 */

enum class BadgeTier(val title: String, val colorHex: Long) {
    BRONZE("برنزی", 0xFFCD7F32),
    SILVER("نقره‌ای", 0xFFC0C0C0),
    GOLD("طلایی", 0xFFFFD700),
    PLATINUM("پلاتینیوم", 0xFF00E5FF),
    DIAMOND("الماس", 0xFF7C4DFF)
}

enum class BadgeCategory(val title: String, val icon: String) {
    STUDY_FOCUS("تمرکز و مطالعه", "⏱️"),
    ATTENDANCE_SHIELD("سپر حضور و غیاب", "🛡️"),
    TASK_CRUSHER("انضباط تکالیف", "📋"),
    CURRICULUM_PIONEER("پیشروی در چارت", "🗺️"),
    EXAM_MASTER("آمادگی آزمون‌ها", "🎯"),
    ACADEMIC_EXCELLENCE("معدل و نمرات", "🏆")
}

data class AcademicBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val category: BadgeCategory,
    val tier: BadgeTier,
    val isUnlocked: Boolean,
    val progress: Float, // 0.0f .. 1.0f
    val progressText: String,
    val xpReward: Int,
    val unlockedAt: String? = null
)

data class StudentGamificationProfile(
    val level: Int,
    val currentXp: Int,
    val nextLevelXp: Int,
    val levelTitle: String,
    val studyStreakDays: Int,
    val bestStreakDays: Int,
    val focusHoursTotal: Double,
    val completedTasksCount: Int,
    val perfectAttendanceCount: Int,
    val badges: List<AcademicBadge>,
    val recentAchievements: List<String>
)

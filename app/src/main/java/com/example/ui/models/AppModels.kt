package com.example.ui.models

typealias ExamItem = com.example.domain.model.ExamItem

data class SemesterCurriculum(
    val title: String,
    val units: Int,
    val courses: List<String>,
    val isCurrent: Boolean = false
)

enum class AppTab(
    val title: String,
    val iconEmoji: String
) {
    DASHBOARD("خانه", "⚡"),
    COPILOT("دستیار تحصیلی", "🤖"),
    ACADEMIC_INTELLIGENCE("پیشرفت و گزارش‌ها", "🧠"),
    PASSPORT("شناسنامه تحصیلی", "📜"),
    AGENDA("برنامهٔ هفته", "📅"),
    NOTES("یادداشت‌ها", "📝"),
    SCHEDULE("کلاس‌ها", "🗓️"),
    ATTENDANCE("حضور و غیاب", "🚨"),
    TASKS("تکلیف‌ها", "📋"),
    EXAMS("امتحان‌ها", "🎯"),
    GRADES("نمره‌ها و معدل", "📈"),
    SEMESTER_PLANNER("برنامه‌ریز ترم", "🧭"),
    CURRICULUM("چارت درسی", "🗺️"),
    POMODORO("تمرکز و مطالعه", "⏱️"),
    GAMIFICATION("دستاوردها", "🏆"),
    HISTORY("ترم‌های قبل", "📚"),
    ACCOUNT("حساب من", "👤")
}

data class SystemNotification(
    val id: String,
    val title: String,
    val description: String,
    val time: String,
    val isDanger: Boolean = false
)

enum class ThemeMode(
    val titleFa: String,
    val descriptionFa: String
) {
    SYSTEM("مطابق سیستم", "تنظیم خودکار بر اساس حالت روز/شب گوشی"),
    LIGHT("روشن", "برای محیط‌های روشن"),
    DARK("تاریک", "برای مطالعه در محیط کم‌نور")
}

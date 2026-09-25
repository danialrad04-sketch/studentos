package com.example.ui.models

data class ExamItem(
    val id: String,
    val courseName: String,
    val solarDate: String,
    val time: String,
    val location: String,
    val units: Int
)

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
    DASHBOARD("داشبورد بنتو", "⚡"),
    COPILOT("دستیار هوشمند", "🤖"),
    ACADEMIC_INTELLIGENCE("هوش تحصیلی", "🧠"),
    PASSPORT("شناسنامه تحصیلی", "📜"),
    SCHEDULE("برنامه کلاسی", "🗓️"),
    ATTENDANCE("رادار غیبت‌ها", "🚨"),
    TASKS("تکالیف و پروژه", "📋"),
    EXAMS("امتحانات پایان‌ترم", "🎯"),
    GRADES("کارنامه و شبیه‌ساز", "📈"),
    SEMESTER_PLANNER("برنامه‌ریز ترم", "🧭"),
    CURRICULUM("چارت مهندسی شیمی", "🗺️"),
    POMODORO("تایمر تمرکز و فرمول", "⏱️"),
    GAMIFICATION("مدال‌ها و دستاوردها", "🏆"),
    HISTORY("سوابق ترم‌ها", "📚")
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
    LIGHT("روشن", "تم بلورین و پرنور با کنتراست شفاف"),
    DARK("تاریک", "تم مخملی عمیق مناسب محیط‌های کم‌نور")
}

package com.example.domain.model

enum class AcademicCommandDestination {
    SCHEDULE,
    TASKS,
    EXAMS,
    GRADES,
    ATTENDANCE,
    FOCUS,
    INTELLIGENCE,
    CURRICULUM,
    SEMESTER_PLANNER
}

enum class AcademicCommand(
    val destination: AcademicCommandDestination,
    val titleFa: String
) {
    TODAY_SCHEDULE(AcademicCommandDestination.SCHEDULE, "برنامه امروز"),
    WEEK_SCHEDULE(AcademicCommandDestination.SCHEDULE, "برنامه این هفته"),
    TASKS(AcademicCommandDestination.TASKS, "تکالیف"),
    EXAMS(AcademicCommandDestination.EXAMS, "امتحانات"),
    GRADES(AcademicCommandDestination.GRADES, "نمرات و کارنامه"),
    ATTENDANCE(AcademicCommandDestination.ATTENDANCE, "حضور و غیاب"),
    FOCUS(AcademicCommandDestination.FOCUS, "تمرکز"),
    INTELLIGENCE(AcademicCommandDestination.INTELLIGENCE, "هوش تحصیلی"),
    CURRICULUM(AcademicCommandDestination.CURRICULUM, "چارت درسی"),
    SEMESTER_PLANNER(AcademicCommandDestination.SEMESTER_PLANNER, "برنامه‌ریز ترم")
}

object AcademicCommandEngine {
    fun resolve(input: String): AcademicCommand? {
        val normalized = input
            .trim()
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('‌', ' ')
            .replace(Regex("\\s+"), " ")

        if (normalized.isBlank()) return null

        val pairs = listOf(
            AcademicCommand.TODAY_SCHEDULE to listOf("برنامه امروز", "کلاس امروز"),
            AcademicCommand.WEEK_SCHEDULE to listOf("برنامه این هفته", "کلاس های این هفته", "کلاس‌های این هفته"),
            AcademicCommand.TASKS to listOf("تکالیف", "وظایف", "کارهای درسی"),
            AcademicCommand.EXAMS to listOf("امتحان", "امتحانات", "آزمون"),
            AcademicCommand.GRADES to listOf("نمرات", "کارنامه", "معدل"),
            AcademicCommand.ATTENDANCE to listOf("حضور و غیاب", "حضورغیاب", "غیبت"),
            AcademicCommand.FOCUS to listOf("تمرکز", "پومودورو", "pomodoro"),
            AcademicCommand.INTELLIGENCE to listOf("هوش تحصیلی", "وضعیت تحصیلی", "تحلیل تحصیلی"),
            AcademicCommand.CURRICULUM to listOf("چارت درسی", "چارت", "سرفصل"),
            AcademicCommand.SEMESTER_PLANNER to listOf("برنامه‌ریز ترم", "برنامه ریز ترم", "برنامه ترم")
        )

        return pairs.firstOrNull { (_, keywords) ->
            keywords.any { keyword ->
                normalized.equals(keyword, ignoreCase = true) ||
                    normalized.contains(keyword, ignoreCase = true)
            }
        }?.first
    }
}

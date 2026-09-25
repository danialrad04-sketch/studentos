package com.example.domain.model

import com.example.ui.models.AppTab

sealed class AcademicCommand(
    val tab: AppTab,
    val titleFa: String
) {
    data object TodaySchedule : AcademicCommand(AppTab.SCHEDULE, "برنامه امروز")
    data object WeekSchedule : AcademicCommand(AppTab.SCHEDULE, "برنامه این هفته")
    data object Tasks : AcademicCommand(AppTab.TASKS, "تکالیف")
    data object Exams : AcademicCommand(AppTab.EXAMS, "امتحانات")
    data object Grades : AcademicCommand(AppTab.GRADES, "نمرات و کارنامه")
    data object Attendance : AcademicCommand(AppTab.ATTENDANCE, "حضور و غیاب")
    data object Focus : AcademicCommand(AppTab.POMODORO, "تمرکز")
    data object Intelligence : AcademicCommand(AppTab.ACADEMIC_INTELLIGENCE, "هوش تحصیلی")
    data object Curriculum : AcademicCommand(AppTab.CURRICULUM, "چارت درسی")
    data object SemesterPlanner : AcademicCommand(AppTab.SEMESTER_PLANNER, "برنامه‌ریز ترم")
}

object AcademicCommandEngine {
    private val commandKeywords: LinkedHashMap<AcademicCommand, List<String>> = linkedMapOf(
        AcademicCommand.TodaySchedule to listOf("برنامه امروز", "کلاس امروز"),
        AcademicCommand.WeekSchedule to listOf("برنامه این هفته", "کلاس های این هفته", "کلاس‌های این هفته"),
        AcademicCommand.Tasks to listOf("تکالیف", "وظایف", "کارهای درسی"),
        AcademicCommand.Exams to listOf("امتحان", "امتحانات", "آزمون"),
        AcademicCommand.Grades to listOf("نمرات", "کارنامه", "معدل"),
        AcademicCommand.Attendance to listOf("حضور و غیاب", "حضورغیاب", "غیبت"),
        AcademicCommand.Focus to listOf("تمرکز", "پومودورو", "pomodoro"),
        AcademicCommand.Intelligence to listOf("هوش تحصیلی", "وضعیت تحصیلی", "تحلیل تحصیلی"),
        AcademicCommand.Curriculum to listOf("چارت درسی", "چارت", "سرفصل"),
        AcademicCommand.SemesterPlanner to listOf("برنامه‌ریز ترم", "برنامه ریز ترم", "برنامه ترم")
    )

    fun resolve(input: String): AcademicCommand? {
        val normalized = input
            .trim()
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('‌', ' ')
            .replace(Regex("\s+"), " ")

        if (normalized.isBlank()) return null

        return commandKeywords.entries
            .firstOrNull { (_, keywords) ->
                keywords.any { keyword ->
                    normalized.equals(keyword, ignoreCase = true) ||
                        normalized.contains(keyword, ignoreCase = true)
                }
            }
            ?.key
    }
}

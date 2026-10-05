package com.example.ui.components

import com.example.ui.models.AppTab

internal val studentFeatureGroups = listOf(
    "برنامهٔ روزمره" to listOf(AppTab.DASHBOARD, AppTab.SCHEDULE, AppTab.TASKS, AppTab.EXAMS, AppTab.ATTENDANCE, AppTab.POMODORO),
    "مطالعه و پیشرفت" to listOf(AppTab.COPILOT, AppTab.ACADEMIC_INTELLIGENCE, AppTab.GRADES, AppTab.GAMIFICATION),
    "مسیر دانشگاه" to listOf(AppTab.PASSPORT, AppTab.SEMESTER_PLANNER, AppTab.CURRICULUM, AppTab.HISTORY)
)

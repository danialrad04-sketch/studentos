package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.models.AppTab

/** A single vector family for structural controls; illustrations stay in content. */
internal fun studentDestinationIcon(tab: AppTab): ImageVector =
    when (tab) {
        AppTab.DASHBOARD -> Icons.Rounded.Home
        AppTab.ACCOUNT -> Icons.Rounded.PersonOutline
        AppTab.SCHEDULE -> Icons.Rounded.CalendarMonth
        AppTab.AGENDA -> Icons.Rounded.DateRange
        AppTab.TASKS -> Icons.Rounded.Checklist
        AppTab.GRADES -> Icons.Rounded.BarChart
        AppTab.COPILOT -> Icons.Rounded.AutoAwesome
        AppTab.ACADEMIC_INTELLIGENCE -> Icons.Rounded.Insights
        AppTab.PASSPORT -> Icons.Rounded.School
        AppTab.NOTES -> Icons.Rounded.EditNote
        AppTab.ATTENDANCE -> Icons.AutoMirrored.Rounded.FactCheck
        AppTab.EXAMS -> Icons.Rounded.EventAvailable
        AppTab.SEMESTER_PLANNER -> Icons.Rounded.Explore
        AppTab.CURRICULUM -> Icons.Rounded.AccountTree
        AppTab.POMODORO -> Icons.Rounded.Timer
        AppTab.GAMIFICATION -> Icons.Rounded.EmojiEvents
        AppTab.HISTORY -> Icons.AutoMirrored.Rounded.MenuBook
    }

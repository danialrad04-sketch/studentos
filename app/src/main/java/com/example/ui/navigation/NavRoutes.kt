package com.example.ui.navigation

/**
 * Type-safe navigation routes for Student OS using Jetpack Navigation Compose.
 */
sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Schedule : Screen("schedule")
    object Tasks : Screen("tasks")
    object Grades : Screen("grades")
    object Attendance : Screen("attendance")
    object Copilot : Screen("copilot")
    object Workload : Screen("workload")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object Gamification : Screen("gamification")
}

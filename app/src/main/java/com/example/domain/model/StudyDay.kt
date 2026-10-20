package com.example.domain.model

/** Completed focus sessions, grouped by the local calendar day of completion. */
data class StudyDay(val date: String, val minutes: Int, val sessions: Int)

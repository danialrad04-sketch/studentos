package com.example.domain.engine

import com.example.domain.model.StudyDay
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class StudyStatistics(
    val totalMinutes: Int = 0,
    val totalSessions: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastSevenDaysMinutes: Int = 0,
    val previousSevenDaysMinutes: Int = 0
)

/** Summarizes recorded history only. The caller owns local-day/time-zone policy. */
object StudyStatisticsEngine {
    fun calculate(history: List<StudyDay>, todayKey: String): StudyStatistics {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }
        fun day(key: String): Long? {
            if (!key.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return null
            val position = ParsePosition(0)
            val parsed = formatter.parse(key, position) ?: return null
            return if (position.index == key.length) parsed.time / 86_400_000L else null
        }
        val today = day(todayKey) ?: return StudyStatistics()
        val days = history.mapNotNull { row ->
            val date = day(row.date) ?: return@mapNotNull null
            if (date > today || row.minutes !in 1..1440 || row.sessions <= 0) null else date to row
        }.groupBy({ it.first }, { it.second }).mapValues { (_, rows) ->
            rows.sumOf { it.minutes.toLong() }.coerceAtMost(1440).toInt() to rows.sumOf { it.sessions.toLong() }.coerceAtMost(1440).toInt()
        }
        var current = 0
        var cursor = if (today in days) today else today - 1
        while (cursor in days) { current++; cursor-- }
        var best = 0
        var run = 0
        var previous: Long? = null
        days.keys.sorted().forEach { date ->
            run = if (previous == date - 1) run + 1 else 1
            best = maxOf(best, run)
            previous = date
        }
        fun sum(from: Long, through: Long) = days.filterKeys { it in from..through }.values.sumOf { it.first }
        return StudyStatistics(days.values.sumOf { it.first }, days.values.sumOf { it.second }, current, best,
            sum(today - 6, today), sum(today - 13, today - 7))
    }
}

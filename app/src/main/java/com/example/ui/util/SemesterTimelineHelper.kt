package com.example.ui.util

import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

data class SemesterProgressInfo(
    val isStarted: Boolean,
    val progressFraction: Float,
    val progressPercent: Int,
    val headline: String,
    val subHeadline: String,
    val currentWeekText: String,
    val daysRemainingToStart: Long,
    val daysPassed: Long,
    val totalDays: Long
)

object SemesterTimelineHelper {

    /**
     * Calculates the semester timeline dynamically for the Fall Semester (نیم‌سال اول).
     * The Iranian Fall semester officially starts on 1 Mehr (approx September 23).
     * Classes conclude around 10-12 Dey (early January),
     * followed by final examinations until ~2-3 Bahman (approx January 22-23).
     */
    fun calculateCurrentSemesterProgress(currentDate: Date = Date()): SemesterProgressInfo {
        val cal = Calendar.getInstance()
        cal.time = currentDate
        val currentYear = cal.get(Calendar.YEAR)

        // Start of Fall Semester: September 23 of current academic year
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, Calendar.SEPTEMBER)
            set(Calendar.DAY_OF_MONTH, 23)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // End of Fall Semester (end of exams): January 23 of the following year
        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear + 1)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 23)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val currentTimeMs = cal.timeInMillis
        val startTimeMs = startCal.timeInMillis
        val endTimeMs = endCal.timeInMillis

        val totalDays = TimeUnit.MILLISECONDS.toDays(endTimeMs - startTimeMs).coerceAtLeast(120)

        if (currentTimeMs < startTimeMs) {
            // Semester has not started yet
            val diffMs = startTimeMs - currentTimeMs
            val daysRemaining = (TimeUnit.MILLISECONDS.toDays(diffMs) + 1).coerceAtLeast(1)

            return SemesterProgressInfo(
                isStarted = false,
                progressFraction = 0f,
                progressPercent = 0,
                headline = "در انتظار آغاز نیم‌سال تحصیلی (1 مهر)",
                subHeadline = "$daysRemaining روز مانده تا شروع کلاس‌ها و بازگشایی دانشگاه",
                currentWeekText = "پیش از شروع ترم",
                daysRemainingToStart = daysRemaining,
                daysPassed = 0,
                totalDays = totalDays
            )
        } else if (currentTimeMs in startTimeMs..endTimeMs) {
            // Semester is currently active
            val diffMs = currentTimeMs - startTimeMs
            val daysPassed = TimeUnit.MILLISECONDS.toDays(diffMs).coerceAtLeast(0)
            val fraction = (daysPassed.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
            val percent = (fraction * 100).toInt()
            val weekNumber = ((daysPassed / 7) + 1).coerceIn(1, 17)

            val phase = when {
                daysPassed < 45 -> "هفته $weekNumber تحصیلی · شروع مباحث پایه و تدریس"
                daysPassed < 80 -> "هفته $weekNumber تحصیلی · امتحانات میان‌ترم و پروژه‌ها"
                daysPassed < 105 -> "هفته $weekNumber تحصیلی · اتمام سرپرفصل‌ها و فرجه"
                else -> "بازه امتحانات پایان‌ترم دی‌ماه"
            }

            return SemesterProgressInfo(
                isStarted = true,
                progressFraction = fraction,
                progressPercent = percent,
                headline = "$percent% از ترم تحصیلی سپری شده",
                subHeadline = phase,
                currentWeekText = "هفته $weekNumber",
                daysRemainingToStart = 0,
                daysPassed = daysPassed,
                totalDays = totalDays
            )
        } else {
            // Past the semester
            return SemesterProgressInfo(
                isStarted = true,
                progressFraction = 1f,
                progressPercent = 100,
                headline = "پایان نیم‌سال تحصیلی",
                subHeadline = "امتحانات به اتمام رسیده‌اند. آماده‌سازی برای انتخاب واحد ترم بعد",
                currentWeekText = "پایان ترم",
                daysRemainingToStart = 0,
                daysPassed = totalDays,
                totalDays = totalDays
            )
        }
    }
}

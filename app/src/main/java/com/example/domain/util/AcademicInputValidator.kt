package com.example.domain.util

import com.example.data.local.util.DateTimeNormalizer
import com.example.data.parser.ParsedCourseDraft
import java.util.Calendar
import java.util.TimeZone

object AcademicInputValidator {
    fun time(raw: String): String? {
        val clean = DateTimeNormalizer.normalizeDigits(raw).trim()
        val parts = clean.split(':')
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return "%02d:%02d".format(java.util.Locale.US, hour, minute)
    }

    fun examTimestamp(date: String, time: String): Long? {
        val day = JalaliCalendarUtil.parse(date) ?: return null
        val clock = time(time) ?: return null
        val (year, month, d) = JalaliCalendarUtil.jalaliToGregorian(day.year, day.month, day.day)
        return Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran")).apply {
            clear()
            set(year, month - 1, d, clock.substringBefore(':').toInt(), clock.substringAfter(':').toInt())
        }.timeInMillis
    }

    fun draftIssues(draft: ParsedCourseDraft): List<String> = buildList {
        if (draft.name.isBlank()) add("نام درس را وارد کنید.")
        if (draft.units !in 1..10) add("تعداد واحد باید بین ۱ تا ۱۰ باشد.")
        if (draft.dayOfWeek !in 0..6) add("روز کلاس را مشخص کنید.")
        val start = time(draft.startTime)
        val end = time(draft.endTime)
        if (start == null || end == null || start >= end) add("ساعت پایان کلاس باید بعد از شروع باشد.")
        if (draft.examDate.isNotBlank() && JalaliCalendarUtil.parse(draft.examDate) == null) add("تاریخ امتحان معتبر نیست.")
        if (draft.examTime.isNotBlank() && time(draft.examTime) == null) add("ساعت امتحان معتبر نیست.")
        if (draft.examDate.isBlank() != draft.examTime.isBlank()) add("تاریخ و ساعت امتحان را با هم وارد کنید یا هر دو را خالی بگذارید.")
    }
}

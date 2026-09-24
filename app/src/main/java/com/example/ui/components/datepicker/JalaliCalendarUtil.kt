package com.example.ui.components.datepicker

import java.util.Calendar

/**
 * Robust, production-grade Persian (Jalali / Solar Hijri) Calendar calculations.
 */
data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int
) : Comparable<JalaliDate> {

    fun format(delimiter: String = "/"): String =
        "%04d%s%02d%s%02d".format(year, delimiter, month, delimiter, day)

    fun toPersianDigits(): String =
        format("/").map { ch ->
            when (ch) {
                '0' -> '۰'
                '1' -> '۱'
                '2' -> '۲'
                '3' -> '۳'
                '4' -> '۴'
                '5' -> '۵'
                '6' -> '۶'
                '7' -> '۷'
                '8' -> '۸'
                '9' -> '۹'
                else -> ch
            }
        }.joinToString("")

    fun getMonthName(): String = JalaliCalendarUtil.PERSIAN_MONTH_NAMES.getOrElse(month - 1) { "" }

    fun getWeekdayName(): String {
        val (gy, gm, gd) = JalaliCalendarUtil.jalaliToGregorian(year, month, day)
        val cal = Calendar.getInstance().apply {
            set(gy, gm - 1, gd)
        }
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        // Calendar.SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
        return when (dow) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یکشنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنج‌شنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> "شنبه"
        }
    }

    override fun compareTo(other: JalaliDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }
}

object JalaliCalendarUtil {

    val PERSIAN_MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val PERSIAN_WEEKDAYS_SHORT = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    fun isLeapYear(year: Int): Boolean {
        val r = (year + 38) % 33
        return r in listOf(1, 5, 9, 13, 17, 22, 26, 30)
    }

    fun getDaysInMonth(year: Int, month: Int): Int {
        return when {
            month in 1..6 -> 31
            month in 7..11 -> 30
            month == 12 -> if (isLeapYear(year)) 30 else 29
            else -> 30
        }
    }

    /**
     * Converts a Gregorian date (1-indexed month) to JalaliDate.
     */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var curGy = gy
        var jy: Int
        if (curGy > 1600) {
            jy = 979
            curGy -= 1600
        } else {
            jy = 0
            curGy -= 621
        }
        val gy2 = if (gm > 2) curGy else curGy - 1
        var days = 365 * curGy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 - 80 + gd + gDaysInMonth[gm - 1]
        jy += 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + (days % 31)
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + ((days - 186) % 30)
        }
        return JalaliDate(jy, jm, jd)
    }

    /**
     * Converts a Jalali date to Gregorian (gy, gm, gd) with 1-indexed month.
     */
    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        var curJy = jy
        var gy: Int
        if (curJy > 979) {
            gy = 1600
            curJy -= 979
        } else {
            gy = 621
        }
        var days = 365 * curJy + (curJy / 33) * 8 + ((curJy % 33 + 3) / 4) + 78 + jd + (if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186)
        gy += 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            days -= 1
            gy += 100 * (days / 36524)
            days %= 36524
            if (days >= 365) {
                days += 1
            }
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val salA = intArrayOf(0, 31, if ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 13 && gd > salA[gm]) {
            gd -= salA[gm]
            gm += 1
        }
        return Triple(gy, gm, gd)
    }

    /**
     * Returns the JalaliDate for today.
     */
    fun today(): JalaliDate {
        val cal = Calendar.getInstance()
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Returns the JalaliDate for tomorrow.
     */
    fun tomorrow(): JalaliDate {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    /**
     * Returns the Persian weekday index for today (0 = Saturday, 1 = Sunday, ..., 6 = Friday).
     */
    fun getTodayWeekdayIndex(): Int {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    /**
     * Returns the Persian weekday index for tomorrow (0 = Saturday, 1 = Sunday, ..., 6 = Friday).
     */
    fun getTomorrowWeekdayIndex(): Int {
        return (getTodayWeekdayIndex() + 1) % 7
    }

    /**
     * Returns the Persian weekday name for the given index.
     */
    fun getWeekdayName(weekdayIndex: Int): String = when (weekdayIndex % 7) {
        0 -> "شنبه"
        1 -> "یکشنبه"
        2 -> "دوشنبه"
        3 -> "سه‌شنبه"
        4 -> "چهارشنبه"
        5 -> "پنج‌شنبه"
        6 -> "جمعه"
        else -> "شنبه"
    }

    /**
     * Returns the Persian weekday index (0 = Saturday, 1 = Sunday, ..., 6 = Friday)
     * for the 1st day of the given Jalali year and month.
     */
    fun getFirstDayOfWeekInMonth(year: Int, month: Int): Int {
        val (gy, gm, gd) = jalaliToGregorian(year, month, 1)
        val cal = Calendar.getInstance().apply {
            set(gy, gm - 1, gd)
        }
        // Calendar: Saturday=7, Sunday=1, Monday=2, Tuesday=3, Wednesday=4, Thursday=5, Friday=6
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    /**
     * Parses a string like "1403/10/23" or "۱۴۰۳/۱۰/۲۳" into a JalaliDate.
     */
    fun parse(raw: String): JalaliDate? {
        val normalized = raw.trim()
            .map { ch ->
                when (ch) {
                    '۰', '٠' -> '0'
                    '۱', '١' -> '1'
                    '۲', '٢' -> '2'
                    '۳', '٣' -> '3'
                    '۴', '٤' -> '4'
                    '۵', '٥' -> '5'
                    '۶', '٦' -> '6'
                    '۷', '٧' -> '7'
                    '۸', '٨' -> '8'
                    '۹', '٩' -> '9'
                    '-' -> '/'
                    else -> ch
                }
            }
            .joinToString("")

        val parts = normalized.split("/")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val d = parts[2].toIntOrNull() ?: return null
            if (y in 1300..1500 && m in 1..12 && d in 1..31) {
                return JalaliDate(y, m, d)
            }
        }
        return null
    }
}

package com.example.ui.components.datepicker

/**
 * UI compatibility facade. Calendar calculations live in the domain layer.
 */
typealias JalaliDate = com.example.domain.util.JalaliDate

object JalaliCalendarUtil {
    val PERSIAN_MONTH_NAMES: List<String>
        get() = com.example.domain.util.JalaliCalendarUtil.PERSIAN_MONTH_NAMES

    val PERSIAN_WEEKDAYS_SHORT: List<String>
        get() = com.example.domain.util.JalaliCalendarUtil.PERSIAN_WEEKDAYS_SHORT

    fun isLeapYear(year: Int): Boolean =
        com.example.domain.util.JalaliCalendarUtil.isLeapYear(year)

    fun getDaysInMonth(year: Int, month: Int): Int =
        com.example.domain.util.JalaliCalendarUtil.getDaysInMonth(year, month)

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate =
        com.example.domain.util.JalaliCalendarUtil.gregorianToJalali(gy, gm, gd)

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> =
        com.example.domain.util.JalaliCalendarUtil.jalaliToGregorian(jy, jm, jd)

    fun today(): JalaliDate =
        com.example.domain.util.JalaliCalendarUtil.today()

    fun tomorrow(): JalaliDate =
        com.example.domain.util.JalaliCalendarUtil.tomorrow()

    fun getTodayWeekdayIndex(): Int =
        com.example.domain.util.JalaliCalendarUtil.getTodayWeekdayIndex()

    fun getTomorrowWeekdayIndex(): Int =
        com.example.domain.util.JalaliCalendarUtil.getTomorrowWeekdayIndex()

    fun getWeekdayName(weekdayIndex: Int): String =
        com.example.domain.util.JalaliCalendarUtil.getWeekdayName(weekdayIndex)

    fun getFirstDayOfWeekInMonth(year: Int, month: Int): Int =
        com.example.domain.util.JalaliCalendarUtil.getFirstDayOfWeekInMonth(year, month)

    fun parse(raw: String): JalaliDate? =
        com.example.domain.util.JalaliCalendarUtil.parse(raw)
}

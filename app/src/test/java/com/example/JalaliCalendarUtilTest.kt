package com.example

import com.example.domain.util.JalaliCalendarUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class JalaliCalendarUtilTest {
    @Test
    fun parses_persian_digits_and_formats_normalized_date() {
        val date = JalaliCalendarUtil.parse("۱۴۰۴/۰۷/۰۳")
        assertNotNull(date)
        assertEquals("1404/07/03", date?.format("/"))
    }

    @Test
    fun rejects_invalid_jalali_date() {
        assertNull(JalaliCalendarUtil.parse("۱۴۰۴/۱۳/۰۱"))
        assertNull(JalaliCalendarUtil.parse("not-a-date"))
    }

    @Test
    fun weekday_index_stays_in_persian_week_range() {
        val today = JalaliCalendarUtil.getTodayWeekdayIndex()
        assertEquals(true, today in 0..6)
        assertEquals((today + 1) % 7, JalaliCalendarUtil.getTomorrowWeekdayIndex())
    }
}

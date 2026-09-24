package com.example.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrationTextParserTest {

    @Test
    fun testParseStructuredPipeFormat() {
        val input = """
            ریاضی عمومی ۲ | ۳ واحد | شنبه ۱۰:۰۰ تا ۱۲:۰۰ | تالار ابوریحان | دکتر احمدی | امتحان ۱۴۰۳/۱۰/۲۰ ساعت ۰۹:۰۰
            فیزیک ۲ | ۳ واحد | یکشنبه ۰۸:۰۰ تا ۱۰:۰۰ | دانشکده فیزیک | دکتر صادقی
        """.trimIndent()

        val parsed = RegistrationTextParser.parse(input)
        assertEquals(2, parsed.size)

        val math = parsed[0]
        assertEquals("ریاضی عمومی ۲", math.name)
        assertEquals(3, math.units)
        assertEquals(0, math.dayOfWeek) // شنبه
        assertEquals("10:00", math.startTime)
        assertEquals("12:00", math.endTime)
        assertEquals("دکتر احمدی", math.instructor)
        assertEquals("1403/10/20", math.examDate)
        assertEquals("09:00", math.examTime)
        assertEquals(DraftValidationState.VALID, math.validationState)

        val physics = parsed[1]
        assertEquals("فیزیک ۲", physics.name)
        assertEquals(3, physics.units)
        assertEquals(1, physics.dayOfWeek) // یکشنبه
        assertEquals("08:00", physics.startTime)
        assertEquals("10:00", physics.endTime)
    }

    @Test
    fun testParseNaturalLanguagePersian() {
        val input = "شیمی فیزیک سه واحد دوشنبه هشت تا ده دکتر کاظمی"
        val parsed = RegistrationTextParser.parse(input)

        assertEquals(1, parsed.size)
        val course = parsed[0]
        assertEquals("شیمی فیزیک", course.name)
        assertEquals(3, course.units)
        assertEquals(2, course.dayOfWeek) // دوشنبه
        assertEquals("08:00", course.startTime)
        assertEquals("10:00", course.endTime)
        assertEquals("دکتر کاظمی", course.instructor)
    }

    @Test
    fun testParseIncompleteDraftFlagsMissingFields() {
        val input = "مقدمات مهندسی شیمی"
        val parsed = RegistrationTextParser.parse(input)

        assertEquals(1, parsed.size)
        val course = parsed[0]
        assertEquals("مقدمات مهندسی شیمی", course.name)
        assertEquals(DraftValidationState.INCOMPLETE, course.validationState)
        assertTrue("Should list missing day or time", course.missingFields.isNotEmpty())
    }

    @Test
    fun testNormalizerHandlesPersianDigits() {
        val input = "کنترل فرایند | ۳ واحد | چهارشنبه ۱۴ تا ۱۶"
        val parsed = RegistrationTextParser.parse(input)

        assertEquals(1, parsed.size)
        val course = parsed[0]
        assertEquals("کنترل فرایند", course.name)
        assertEquals(3, course.units)
        assertEquals(4, course.dayOfWeek) // چهارشنبه
        assertEquals("14:00", course.startTime)
        assertEquals("16:00", course.endTime)
    }

    @Test
    fun testParseNaturalLanguageWithMultiWordInstructorAndRoom() {
        val input = "محاسبات عددی دکتر سیامک علیپور ۲ واحد شنبه ۸-۱۰ کلاس ۱۰۷"
        val parsed = RegistrationTextParser.parse(input)

        assertEquals(1, parsed.size)
        val course = parsed[0]
        assertEquals("محاسبات عددی", course.name)
        assertEquals("دکتر سیامک علیپور", course.instructor)
        assertTrue(course.location.contains("۱۰۷") || course.location.contains("107"))
        assertEquals(0, course.dayOfWeek)
        assertEquals("08:00", course.startTime)
        assertEquals("10:00", course.endTime)
        assertEquals(2, course.units)
    }

    @Test
    fun testParseNaturalLanguageWithTalarLocation() {
        val input = "مکانیک سیالات دکتر حسینی ۳ واحد یکشنبه ۱۰-۱۲ تالار ۵"
        val parsed = RegistrationTextParser.parse(input)

        assertEquals(1, parsed.size)
        val course = parsed[0]
        assertEquals("مکانیک سیالات", course.name)
        assertEquals("دکتر حسینی", course.instructor)
        assertTrue(course.location.contains("تالار"))
        assertEquals(1, course.dayOfWeek)
        assertEquals("10:00", course.startTime)
        assertEquals("12:00", course.endTime)
        assertEquals(3, course.units)
    }

    @Test
    fun testParseNaturalLanguageWithAzmayeshgahAndUnknownLocationFallback() {
        val input = "آزمایشگاه ریزپردازنده مهندس رضایی ۱ واحد سه‌شنبه ۱۴-۱۶ کلاس نامشخص"
        val parsed = RegistrationTextParser.parse(input)

        assertEquals(1, parsed.size)
        val course = parsed[0]
        assertEquals("آزمایشگاه ریزپردازنده", course.name)
        assertEquals("مهندس رضایی", course.instructor)
        assertEquals("دانشکده", course.location) // Graceful fallback
        assertEquals(3, course.dayOfWeek)
        assertEquals("14:00", course.startTime)
        assertEquals("16:00", course.endTime)
        assertEquals(1, course.units)
    }
}

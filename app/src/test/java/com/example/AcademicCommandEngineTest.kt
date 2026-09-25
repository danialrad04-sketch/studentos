package com.example

import com.example.domain.model.AcademicCommand
import com.example.domain.model.AcademicCommandEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AcademicCommandEngineTest {
    @Test
    fun resolves_today_schedule() {
        assertEquals(AcademicCommand.TodaySchedule, AcademicCommandEngine.resolve("برنامه امروز"))
    }

    @Test
    fun normalizes_persian_spacing_and_letters() {
        assertEquals(AcademicCommand.Attendance, AcademicCommandEngine.resolve(" حضور  و غیاب "))
        assertEquals(AcademicCommand.Grades, AcademicCommandEngine.resolve("کارنامه"))
    }

    @Test
    fun resolves_focus_alias() {
        assertEquals(AcademicCommand.Focus, AcademicCommandEngine.resolve("پومودورو"))
    }

    @Test
    fun unknown_text_is_not_a_command() {
        assertNull(AcademicCommandEngine.resolve("فیزیک ۲"))
    }
}

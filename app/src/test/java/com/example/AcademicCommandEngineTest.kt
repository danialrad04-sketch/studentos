package com.example

import com.example.domain.model.AcademicCommand
import com.example.domain.model.AcademicCommandEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AcademicCommandEngineTest {
    @Test
    fun resolves_today_schedule() {
        assertEquals(AcademicCommand.TODAY_SCHEDULE, AcademicCommandEngine.resolve("برنامه امروز"))
    }

    @Test
    fun normalizes_persian_spacing_and_letters() {
        assertEquals(AcademicCommand.ATTENDANCE, AcademicCommandEngine.resolve(" حضور  و غیاب "))
        assertEquals(AcademicCommand.GRADES, AcademicCommandEngine.resolve("کارنامه"))
    }

    @Test
    fun normalizes_tabs_and_newlines_inside_queries() {
        assertEquals(
            AcademicCommand.TODAY_SCHEDULE,
            AcademicCommandEngine.resolve("برنامه\n\tامروز")
        )
    }

    @Test
    fun resolves_focus_alias() {
        assertEquals(AcademicCommand.FOCUS, AcademicCommandEngine.resolve("پومودورو"))
    }

    @Test
    fun unknown_text_is_not_a_command() {
        assertNull(AcademicCommandEngine.resolve("فیزیک ۲"))
    }
}

package com.example.domain.engine

import com.example.domain.util.JalaliDate
import org.junit.Assert.*
import org.junit.Test

class AcademicCalendarWriterTest {
    private val date=JalaliDate(1405,8,2)
    @Test fun exportsUtcClassesAndExclusiveAllDayDatesWithoutInventingExamEnd() {
        val events=listOf(StudentAgendaEngine.Event("c",StudentAgendaEngine.Kind.CLASS,"درس",date,"09:00","10:00"),
            StudentAgendaEngine.Event("t",StudentAgendaEngine.Kind.TASK,"تمرین",date),StudentAgendaEngine.Event("e",StudentAgendaEngine.Kind.EXAM,"آزمون",date,"12:00"))
        val output=AcademicCalendarWriter.write(events,"owner",0)
        assertTrue(output.contains("DTSTART:20261024T053000Z\r\n"))
        assertTrue(output.contains("DTEND:20261024T063000Z\r\n"))
        assertTrue(output.contains("DTSTART;VALUE=DATE:20261024\r\nDTEND;VALUE=DATE:20261025"))
        val exam=output.split("BEGIN:VEVENT").last()
        assertFalse(exam.contains("DTEND"))
        assertEquals(3,Regex("BEGIN:VEVENT").findAll(output).count())
    }
    @Test fun escapesTextAndFoldsLongPersianAndEmojiWithoutSplittingUtf8() {
        val title="تمرین, بخش; اول\nBEGIN:VEVENT " + "یادداشت فارسی 📝".repeat(20)
        val output=AcademicCalendarWriter.write(listOf(StudentAgendaEngine.Event("t",StudentAgendaEngine.Kind.TASK,title,date)),"owner",0)
        assertTrue(output.endsWith("END:VCALENDAR\r\n"))
        output.split("\r\n").forEach { assertTrue(it.toByteArray(Charsets.UTF_8).size<=75) }
        val unfolded=output.replace("\r\n ","")
        assertTrue(unfolded.contains("تمرین\\, بخش\\; اول\\nBEGIN:VEVENT"))
        assertTrue(unfolded.contains("یادداشت فارسی 📝".repeat(20)))
        assertEquals(1,Regex("\r\nBEGIN:VEVENT\r\n").findAll(output).count())
    }
    @Test fun repeatedExportUsesStableIdsWhileSeparateAccountsCannotCollide() {
        val event=StudentAgendaEngine.Event("task:42",StudentAgendaEngine.Kind.TASK,"یک",date)
        fun uid(namespace:String,title:String)=AcademicCalendarWriter.write(listOf(event.copy(title=title)),namespace,0).lineSequence().single{it.startsWith("UID:")}
        assertEquals(uid("owner-a","یک"),uid("owner-a","ویرایش"))
        assertNotEquals(uid("owner-a","یک"),uid("owner-b","یک"))
        assertEquals(1,Regex("BEGIN:VEVENT").findAll(AcademicCalendarWriter.write(listOf(event,event),"owner",0)).count())
    }
}

package com.example.domain.engine

import com.example.data.local.entity.*
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.model.ExamItem
import com.example.domain.util.JalaliDate
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class StudentAgendaEngineTest {
    private val saturday=JalaliDate(1405,8,2)
    private val course=CourseEntity(id="math",name="ریاضی",units=3,colorHex="#FFFF00")
    private fun session(day:Int,start:String="10:00",end:String="12:00",id:String="s")=CourseSessionEntity(id=id,courseId="math",day=day,start=start,end=end)
    @Test fun mapsAllSevenWeekdaysAndPreservesUnknownDatesWithoutInventingEvents() {
        val tasks=listOf(TaskEntity(id=1,title="امروز",dueDate="۱۴۰۵/۸/۲"),TaskEntity(id=2,title="تمام",dueDate="1405/08/02",isCompleted=true),
            TaskEntity(id=3,title="قبلی",dueDate="1405/07/30"),TaskEntity(id=4,title="نامشخص",dueDate="bad"))
        val exams=listOf(ExamItem("e","آزمون","1405/08/08","","",3))
        val result=StudentAgendaEngine.build(saturday,listOf(CourseWithSessions(course,listOf(session(6),session(7,id="bad")))),tasks,exams,saturday)
        assertEquals(7,result.days.size)
        assertEquals(JalaliDate(1405,8,8),result.events.single{it.kind==StudentAgendaEngine.Kind.CLASS}.date)
        assertEquals(listOf(3L),result.overdue.map{it.id});assertEquals(listOf(4L),result.undatedTasks.map{it.id})
        assertEquals(1,result.events.count{it.kind==StudentAgendaEngine.Kind.TASK})
        assertNull(result.events.single{it.kind==StudentAgendaEngine.Kind.EXAM}.start)
        assertEquals(1,result.invalidRecords)
    }
    @Test fun changingAClassTimeKeepsItsExportIdentityAndArchivedCoursesAreExcluded() {
        fun week(from:String)=StudentAgendaEngine.build(saturday,listOf(CourseWithSessions(course,listOf(session(0,from))),CourseWithSessions(course.copy(id="old",isArchived=true),listOf(session(1)))),emptyList(),emptyList(),saturday)
        assertEquals(1,week("10:00").events.size)
        assertEquals(week("10:00").events.single().id,week("11:00").events.single().id)
        val locale=Locale.getDefault()
        try { val before=week("10:00").events.single().id;Locale.setDefault(Locale("fa","IR"));assertEquals(before,week("10:00").events.single().id) }
        finally { Locale.setDefault(locale) }
    }
    @Test fun weekRollsAcrossNoruzAndStartsOnSaturday() {
        assertEquals(JalaliDate(1404,1,1),StudentAgendaEngine.addDays(JalaliDate(1403,12,29),2))
        val friday=JalaliDate(1405,8,8)
        assertEquals(saturday,StudentAgendaEngine.startOfWeek(friday))
    }
    @Test fun conflictsUseActualTimesAndNeverAssignADurationToAnExam() {
        val sessions=listOf(CourseWithSessions(course,listOf(session(0),session(0,"11:00","13:00","second"))))
        val exams=listOf(ExamItem("known","با ساعت","1405/08/02","10:30","",3),ExamItem("unknown","بدون ساعت","1405/08/02","","",3))
        val result=StudentAgendaEngine.build(saturday,sessions,emptyList(),exams,saturday)
        assertEquals(3,result.conflicts.size)
        assertFalse(result.conflicts.contains("exam:unknown"))
        assertTrue(result.events.filter{it.kind==StudentAgendaEngine.Kind.EXAM}.all{it.end==null})
    }
}

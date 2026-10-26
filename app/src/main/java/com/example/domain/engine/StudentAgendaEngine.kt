package com.example.domain.engine

import com.example.data.local.entity.TaskEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.model.ExamItem
import com.example.domain.util.AcademicInputValidator
import com.example.domain.util.JalaliCalendarUtil
import com.example.domain.util.JalaliDate
import java.util.Calendar
import java.util.TimeZone

/** A dated snapshot, never an inferred semester calendar or a guessed exam duration. */
object StudentAgendaEngine {
    enum class Kind(val title: String) { CLASS("کلاس"), TASK("تکلیف"), EXAM("امتحان") }
    data class Event(val id: String, val kind: Kind, val title: String, val date: JalaliDate,
        val start: String? = null, val end: String? = null, val location: String = "", val courseId: String? = null, val taskId: Long? = null)
    data class Week(val start: JalaliDate, val days: List<JalaliDate>, val events: List<Event>,
        val overdue: List<TaskEntity>, val undatedTasks: List<TaskEntity>, val invalidRecords: Int, val conflicts: Set<String> = emptySet())

    fun addDays(date: JalaliDate, days: Int): JalaliDate {
        val (y,m,d) = JalaliCalendarUtil.jalaliToGregorian(date.year,date.month,date.day)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear();set(y,m-1,d);add(Calendar.DAY_OF_MONTH,days) }
        return JalaliCalendarUtil.gregorianToJalali(calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH)+1,calendar.get(Calendar.DAY_OF_MONTH))
    }
    fun weekday(date: JalaliDate): Int {
        val (y,m,d)=JalaliCalendarUtil.jalaliToGregorian(date.year,date.month,date.day)
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear();set(y,m-1,d) }.get(Calendar.DAY_OF_WEEK)%7
    }
    fun startOfWeek(date: JalaliDate) = addDays(date,-weekday(date))
    fun build(start: JalaliDate, sessions: List<CourseWithSessions>, tasks: List<TaskEntity>, exams: List<ExamItem>, today: JalaliDate): Week {
        val days=(0..6).map { addDays(start,it) };val end=days.last();val events=mutableListOf<Event>();var invalid=0
        sessions.filterNot { it.isArchived }.distinctBy { it.id }.forEach { course ->
            course.sessions.distinctBy { listOf(it.day,it.start,it.end,it.location) }.forEach { session ->
                val from=AcademicInputValidator.time(session.start);val to=AcademicInputValidator.time(session.end)
                if(session.day !in 0..6 || from==null || to==null || from>=to) invalid++ else {
                    val date=days.first { weekday(it)==session.day }
                    val dateId=String.format(java.util.Locale.US,"%04d-%02d-%02d",date.year,date.month,date.day)
                    val sessionKey=session.id.ifBlank { "${session.day}:$from:$to:${session.location}" }
                    events+=Event("class:${course.id}:$sessionKey:${dateId}",Kind.CLASS,course.name,date,from,to,session.location,course.id)
                }
            }
        }
        val pending=tasks.filterNot { it.isCompleted }.distinctBy { it.id }
        val undated=mutableListOf<TaskEntity>();val overdue=mutableListOf<TaskEntity>()
        pending.forEach { task ->
            val date=JalaliCalendarUtil.parse(task.dueDate)
            if(date==null) undated+=task else {
                if(date<today) overdue+=task
                if(date>=start && date<=end) events+=Event("task:${task.id}",Kind.TASK,task.title,date,courseId=task.courseId.ifBlank { null },taskId=task.id)
            }
        }
        exams.distinctBy { it.id }.forEach { exam ->
            val date=JalaliCalendarUtil.parse(exam.solarDate)
            if(date==null) invalid++ else if(date>=start && date<=end) {
                val time=AcademicInputValidator.time(exam.time)
                if(exam.time.isNotBlank() && time==null) invalid++
                events+=Event("exam:${exam.id}",Kind.EXAM,exam.courseName,date,time,location=exam.location)
            }
        }
        val conflicts=mutableSetOf<String>()
        events.filter { it.start!=null }.groupBy { it.date }.values.forEach { rows ->
            for(i in rows.indices) for(j in i+1 until rows.size) {
                val a=rows[i];val b=rows[j]
                val overlap=when {
                    a.start==null || b.start==null -> false
                    a.end!=null && b.end!=null -> a.start<b.end && b.start<a.end
                    a.end!=null -> b.start>=a.start && b.start<a.end
                    b.end!=null -> a.start>=b.start && a.start<b.end
                    a.kind==Kind.EXAM && b.kind==Kind.EXAM -> a.start==b.start
                    else -> false
                }
                if(overlap){conflicts+=a.id;conflicts+=b.id}
            }
        }
        return Week(start,days,events.sortedWith(compareBy({it.date},{it.start ?: "99:99"},{it.kind.ordinal},{it.id})),
            overdue.sortedBy { JalaliCalendarUtil.parse(it.dueDate) },undated,invalid,conflicts)
    }
}

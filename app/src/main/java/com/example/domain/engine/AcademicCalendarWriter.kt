package com.example.domain.engine

import com.example.domain.util.JalaliCalendarUtil
import com.example.domain.util.JalaliDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/** RFC 5545: stable identities, UTC timed events, exclusive all-day end and UTF-8-safe folding. */
object AcademicCalendarWriter {
    private fun escape(text: String)=text.filter { it >= ' ' || it == '\n' || it == '\r' || it == '\t' }.replace("\\","\\\\").replace("\r\n","\n").replace('\r','\n').replace("\n","\\n").replace(";","\\;").replace(",","\\,")
    private fun stamp(millis:Long)=SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'",Locale.US).apply { timeZone=TimeZone.getTimeZone("UTC") }.format(Date(millis))
    private fun date(date:JalaliDate):String { val(y,m,d)=JalaliCalendarUtil.jalaliToGregorian(date.year,date.month,date.day);return String.format(Locale.US,"%04d%02d%02d",y,m,d) }
    private fun timed(date:JalaliDate,time:String):String {
        val(y,m,d)=JalaliCalendarUtil.jalaliToGregorian(date.year,date.month,date.day)
        val parts=time.split(':').map(String::toInt)
        return stamp(Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran")).apply { clear();set(y,m-1,d,parts[0],parts[1]) }.timeInMillis)
    }
    private fun fold(line:String):String {
        val output=StringBuilder();var bytes=0
        line.codePoints().forEach { point ->
            val text=String(Character.toChars(point));val size=text.toByteArray(Charsets.UTF_8).size
            if(bytes+size>75){output.append("\r\n ");bytes=1}
            output.append(text);bytes+=size
        }
        return output.toString()
    }
    fun write(events:List<StudentAgendaEngine.Event>,namespace:String, now:Long=System.currentTimeMillis()):String {
        val lines=mutableListOf("BEGIN:VCALENDAR","VERSION:2.0","PRODID:-//Student OS//Academic Agenda//FA","CALSCALE:GREGORIAN","METHOD:PUBLISH")
        events.distinctBy { it.id }.forEach { e ->
            lines+="BEGIN:VEVENT"
            lines+="UID:${UUID.nameUUIDFromBytes((namespace+":"+e.id).toByteArray(Charsets.UTF_8))}@studentos.local"
            lines+="DTSTAMP:${stamp(now)}"
            lines+="SUMMARY:${escape(e.kind.title+": "+e.title)}"
            if(e.start!=null){
                lines+="DTSTART:${timed(e.date,e.start)}"
                if(e.end!=null) lines+="DTEND:${timed(e.date,e.end)}"
            } else {
                lines+="DTSTART;VALUE=DATE:${date(e.date)}"
                lines+="DTEND;VALUE=DATE:${date(StudentAgendaEngine.addDays(e.date,1))}"
            }
            if(e.location.isNotBlank()) lines+="LOCATION:${escape(e.location)}"
            lines+="DESCRIPTION:${escape("اطلاعات ثبت‌شده در Student OS؛ "+if(e.kind==StudentAgendaEngine.Kind.CLASS) "بر اساس الگوی هفتگی ترم جاری؛ برگزاری در این تاریخ، تعطیلی و پایان ترم نیاز به بررسی دارد." else if(e.kind==StudentAgendaEngine.Kind.TASK) "موعد تکلیف؛ ساعت تحویل مشخص نشده است." else "زمان پایان امتحان ثبت نشده است.")}"
            lines+="END:VEVENT"
        }
        lines+="END:VCALENDAR"
        return lines.joinToString("\r\n",postfix="\r\n",transform=::fold)
    }
}

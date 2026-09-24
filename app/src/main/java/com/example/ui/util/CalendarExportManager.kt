package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.ui.models.ExamItem

/**
 * Utility to export class schedules and exam dates directly to Android System / Google Calendar via Intents.
 */
object CalendarExportManager {

    /**
     * Export a course schedule session directly to Android System Calendar / Google Calendar.
     */
    fun exportCourseToCalendar(context: Context, course: CourseEntity, session: CourseSessionEntity? = null) {
        try {
            val location = session?.location?.ifBlank { null } ?: course.examLocation.ifBlank { "دانشگاه" }
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "کلاس: ${course.name}")
                putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                putExtra(CalendarContract.Events.DESCRIPTION, "کلاس دانشگاهی ${course.name} - استاد: ${course.professor.ifBlank { "نامشخص" }}")
                putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            }
            context.startActivity(intent)
            Toast.makeText(context, "ارسال به تقویم دستگاه... 📅", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "برنامه تقویم سوار شده در دستگاه یافت نشد", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Export an exam schedule directly to Android System Calendar / Google Calendar.
     */
    fun exportExamToCalendar(context: Context, exam: ExamItem) {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "امتحان: ${exam.courseName}")
                putExtra(CalendarContract.Events.EVENT_LOCATION, exam.location)
                putExtra(CalendarContract.Events.DESCRIPTION, "امتحان ترم درس ${exam.courseName} - تاریخ: ${exam.solarDate} ساعت: ${exam.time}")
                putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            }
            context.startActivity(intent)
            Toast.makeText(context, "ارسال امتحان به تقویم دستگاه... 📅", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "برنامه تقویم سوار شده در دستگاه یافت نشد", Toast.LENGTH_SHORT).show()
        }
    }
}

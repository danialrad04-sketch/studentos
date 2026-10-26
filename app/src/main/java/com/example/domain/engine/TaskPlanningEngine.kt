package com.example.domain.engine

import com.example.data.local.entity.TaskEntity
import com.example.domain.util.AcademicInputValidator
import com.example.domain.util.JalaliCalendarUtil
import com.example.domain.util.JalaliDate
import java.util.Calendar
import java.util.TimeZone

/** Date-only recurrence follows the original cadence and skips missed occurrences. */
object TaskPlanningEngine {
    val repeatIntervals = setOf(0, 1, 7, 14, 30)

    fun validate(task: TaskEntity) {
        require(task.title.isNotBlank()) { "عنوان تکلیف را وارد کن." }
        require(JalaliCalendarUtil.parse(task.dueDate) != null) { "تاریخ تحویل معتبر نیست." }
        require(task.repeatEveryDays in repeatIntervals) { "بازهٔ تکرار معتبر نیست." }
        require(task.reminderTime.isBlank() || AcademicInputValidator.time(task.reminderTime) != null) { "ساعت یادآور معتبر نیست." }
        require(task.recurrenceParentId == null || (task.recurrenceParentId > 0 && task.recurrenceParentId != task.id)) { "زنجیرهٔ تکرار معتبر نیست." }
    }

    private fun calendar(date: JalaliDate): Calendar {
        val (year, month, day) = JalaliCalendarUtil.jalaliToGregorian(date.year, date.month, date.day)
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(year, month - 1, day) }
    }

    fun nextDate(dueDate: String, interval: Int, today: JalaliDate): String? {
        if (interval == 0) return null
        require(interval in repeatIntervals)
        val due = JalaliCalendarUtil.parse(dueDate) ?: return null
        val day = calendar(due)
        val elapsed = ((calendar(today).timeInMillis - day.timeInMillis) / 86_400_000L).coerceAtLeast(0)
        val steps = elapsed / interval + 1
        day.add(Calendar.DAY_OF_MONTH, (steps * interval).toInt())
        return JalaliCalendarUtil.gregorianToJalali(day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH)).format()
    }

    fun reminderAt(task: TaskEntity): Long? = if (task.isCompleted || task.reminderTime.isBlank()) null
        else AcademicInputValidator.examTimestamp(task.dueDate, task.reminderTime)
}

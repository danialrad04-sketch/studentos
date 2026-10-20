package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.StudyDay
import com.example.domain.util.JalaliCalendarUtil
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Labels and numbers convey the same information as the bars, including with color blindness. */
@Composable
fun StudyActivityJournal(history: List<StudyDay>, dailyGoalMinutes: Int, modifier: Modifier = Modifier) {
    val clock = rememberAcademicClock()
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val week = (0..6).map { daysAgo ->
        val date = (clock.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
        val key = formatter.format(date.time)
        val jalali = JalaliCalendarUtil.gregorianToJalali(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH))
        val label = if (daysAgo == 0) "امروز" else "${jalali.getWeekdayName()} ${jalali.day} ${jalali.getMonthName()}"
        label to (history.firstOrNull { it.date == key } ?: StudyDay(key, 0, 0))
    }
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("گزارش مطالعهٔ هفت‌روزه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${week.sumOf { it.second.minutes }} دقیقه · ${week.sumOf { it.second.sessions }} جلسهٔ کامل", style = MaterialTheme.typography.bodyLarge)
            Text("زمان فقط پس از پایان تایمر ثبت می‌شود. توقف یا بازنشانی تایمر، جلسهٔ کامل محسوب نمی‌شود.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            week.forEach { (label, day) ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("$label · ${day.minutes} دقیقه · ${day.sessions} جلسه", style = MaterialTheme.typography.labelLarge)
                    LinearProgressIndicator(progress = { (day.minutes.toFloat() / dailyGoalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                }
            }
            Text("هدف روزانه: $dailyGoalMinutes دقیقه · قابل تغییر در تنظیمات مطالعه", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

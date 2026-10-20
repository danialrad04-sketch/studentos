package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** A useful first step for both a new semester and an established workspace. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StudentHomeOverview(
    hasCourses: Boolean,
    pendingTasks: Int,
    onOpenSchedule: () -> Unit,
    onAddTask: () -> Unit,
    onOpenSearch: (() -> Unit)?,
    onOpenCopilot: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Rounded.Flag, null, Modifier.padding(8.dp).size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text("قدم بعدی", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (hasCourses) "برای قدم بعدی آماده‌ای؟" else "ترم جدیدت را از اینجا شروع کن",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
            )
            Text(
                if (!hasCourses) "اول درس‌ها و ساعت کلاس‌ها را ثبت کن؛ برنامهٔ روزانه و پیشنهادهای مطالعه با اطلاعات خودت شکل می‌گیرند."
                else if (pendingTasks > 0) "$pendingTasks تکلیف باز داری. یک کار کوچک انتخاب کن و ادامهٔ روزت را بساز."
                else "تکلیف بازی نداری. برنامهٔ کلاس‌ها را مرور کن یا برای مطالعهٔ بعدی آماده شو.",
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalButton(
                    onClick = if (hasCourses) onAddTask else onOpenSchedule,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (hasCourses) "ثبت تکلیف جدید" else "افزودن درس و برنامه")
                }
                TextButton(onClick = onOpenCopilot, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("از دستیار تحصیلی بپرس")
                }
            }
        }
    }
    if (onOpenSearch != null) {
        OutlinedCard(
            onClick = onOpenSearch, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.primary)
                Text("جست‌وجو در درس‌ها، تکالیف و ابزارها", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

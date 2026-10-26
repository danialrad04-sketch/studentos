package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** One useful setup action; established semesters do not get another motivational card. */
@Composable
@Suppress("UNUSED_PARAMETER")
internal fun StudentHomeOverview(hasCourses: Boolean, pendingTasks: Int, onOpenSchedule: () -> Unit,
    onAddTask: () -> Unit, onOpenSearch: (() -> Unit)?, onOpenCopilot: () -> Unit) {
    if (!hasCourses) AcademicCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppEmoji(AppEmojiType.BOOK, size = 52.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("درس‌های این ترم", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("درس‌ها و ساعت کلاس‌ها را اضافه کن.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            FilledTonalButton(onClick = onOpenSchedule, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("افزودن درس و برنامه")
            }
        }
    }
    if (onOpenSearch != null) Surface(onClick = onOpenSearch, modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("جست‌وجوی درس، تکلیف و ابزار", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

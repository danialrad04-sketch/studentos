package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.models.AppTab

/** Short navigation rows, instead of repeating each feature's dashboard on home. */
@Composable
internal fun StudentHomeToolGrid(onNavigate: (AppTab) -> Unit) {
    val tools = listOf(
        Triple(AppTab.COPILOT, "دستیار", AppEmojiType.COPILOT),
        Triple(AppTab.EXAMS, "امتحان‌ها", AppEmojiType.CALENDAR),
        Triple(AppTab.POMODORO, "تمرکز", AppEmojiType.POMODORO),
        Triple(AppTab.ATTENDANCE, "حضور و غیاب", AppEmojiType.CHECK),
        Triple(AppTab.CURRICULUM, "چارت درسی", AppEmojiType.BOOK),
        Triple(AppTab.HISTORY, "ترم‌های قبل", AppEmojiType.GRAD_CAP)
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = when {
            LocalDensity.current.fontScale >= 1.3f || maxWidth < 300.dp -> 1
            maxWidth >= 620.dp -> 3
            else -> 2
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tools.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (tab, label, sticker) ->
                        Surface(onClick = { onNavigate(tab) }, modifier = Modifier.weight(1f).heightIn(min = 64.dp)
                            .testTag("home_tool_${tab.name}"), shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AppEmoji(sticker, size = 32.dp)
                                Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

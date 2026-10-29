package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.*
import com.example.ui.theme.studentStickerColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.models.AppTab

/** Reusable fallback hub; the production app opens the identity/settings drawer from More. */
@Composable
fun StudentModuleHubSheet(currentTab: AppTab, onSelectTab: (AppTab) -> Unit, onDismiss: () -> Unit) {
    StudentGlassModalSheet(onDismiss, title = "ابزارها", subtitle = "درس، برنامه و سوابق", maxWidth = 640.dp) {
        val dismiss = LocalStudentModalDismiss.current ?: onDismiss
        LazyVerticalGrid(
            columns = GridCells.Adaptive(180.dp),
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            studentFeatureGroups.forEach { (section, tabs) ->
            item(span = { GridItemSpan(maxLineSpan) }) { Text(section, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp)) }
            items(tabs, key = { it.name }) { tab ->
                Card(
                    onClick = { onSelectTab(tab); dismiss() },
                    border = if (tab == currentTab) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = CardDefaults.cardColors(containerColor = studentStickerColors(studentDestinationSticker(tab)).container, contentColor = studentStickerColors(studentDestinationSticker(tab)).ink),
                    modifier = Modifier.fillMaxWidth().semantics { selected = tab == currentTab }
                ) {
                    Row(Modifier.heightIn(min = 80.dp).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        androidx.compose.material3.Icon(studentDestinationIcon(tab), null)
                        Text(tab.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            }
        }
    }
}

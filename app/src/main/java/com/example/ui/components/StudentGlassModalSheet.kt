package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

val LocalStudentModalDismiss = staticCompositionLocalOf<(() -> Unit)?> { null }

/** Native sheet: one scrim, real drag/back semantics and IME-aware bounded content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentGlassModalSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 560.dp,
    maxHeightPercent: Float = 0.92f,
    showDragHandle: Boolean = true,
    title: String = "",
    subtitle: String = "",
    showCloseButton: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    val dismiss: () -> Unit = {
        if (!closing) {
            closing = true
            scope.launch {
                sheetState.hide()
                onDismiss()
            }
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = maxWidth,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
        dragHandle = if (showDragHandle) { { BottomSheetDefaults.DragHandle() } } else null
    ) {
        CompositionLocalProvider(LocalStudentModalDismiss provides dismiss) {
            BoxWithConstraints(Modifier.fillMaxWidth().imePadding()) {
                Column(
                    modifier = modifier.fillMaxWidth()
                        .heightIn(max = maxHeight * maxHeightPercent.coerceIn(0.5f, 1f))
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp)
                        .testTag("student_modal")
                        .semantics { paneTitle = "پنجرهٔ Student OS" }
                ) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (title.isNotBlank()) Text(title, style = MaterialTheme.typography.titleLarge)
                            if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (showCloseButton) IconButton(onClick = dismiss, enabled = !closing) {
                            Icon(Icons.Rounded.Close, contentDescription = "بستن پنجره")
                        }
                    }
                    content()
                }
            }
        }
    }
}

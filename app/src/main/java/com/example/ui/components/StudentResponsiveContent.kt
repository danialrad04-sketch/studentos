package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Reflow related controls at their actual content width, including scaled text. */
@Composable
fun StudentAdaptiveRow(
    modifier: Modifier = Modifier,
    minimumRowWidth: Dp = 280.dp,
    gap: Dp = 10.dp,
    content: @Composable (Modifier) -> Unit
) {
    val scale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth < minimumRowWidth * scale) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
                content(Modifier.fillMaxWidth())
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                content(Modifier.weight(1f))
            }
        }
    }
}

/** Keep summaries bounded while preserving a read-only route to the entire text. */
@Composable
fun StudentReadableText(
    text: String,
    modifier: Modifier = Modifier,
    detailTitle: String = "متن کامل",
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    maxLines: Int = 3
) {
    var overflowing by remember(text) { mutableStateOf(false) }
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    Column(modifier.fillMaxWidth().testTag("readable-$detailTitle")) {
        Text(
            text = text,
            style = style,
            maxLines = maxLines.coerceIn(1, 3),
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { overflowing = it.hasVisualOverflow }
        )
        if (overflowing) {
            TextButton(onClick = { expanded = true }) {
                Text("مشاهدهٔ متن کامل", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    if (expanded) {
        StudentTextDetailSheet(detailTitle, text) { expanded = false }
    }
}

@Composable
fun StudentTextDetailSheet(title: String, text: String, onDismiss: () -> Unit) {
    StudentGlassModalSheet(onDismiss = onDismiss, title = title, maxHeightPercent = 0.82f) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                .verticalScroll(rememberScrollState()).padding(bottom = 12.dp)
        )
    }
}

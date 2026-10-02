package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

/**
 * Stable text primitive for user-provided / imported academic content.
 *
 * Long titles, locations, notes and imported strings must never be allowed to
 * determine an unbounded card height. Callers can opt into a larger line budget
 * for prose, while the default remains compact for card surfaces.
 */
@Composable
fun StudentCardText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = style,
        color = color,
        maxLines = maxLines.coerceIn(1, 6),
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun StudentCardMetaText(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines.coerceIn(1, 3),
        overflow = TextOverflow.Ellipsis
    )
}

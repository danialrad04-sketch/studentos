package com.example.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

/**
 * Text primitives for compact information cards.
 *
 * Rule:
 * - card titles stay one line
 * - descriptions stay two lines
 * - metadata stays one line
 *
 * Dynamic/user supplied text must never be allowed to create uncontrolled card height.
 */
@Composable
fun StudentCardTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight? = null
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun StudentCardBody(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    maxLines: Int = 2,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines.coerceIn(1, 4),
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun StudentCardMeta(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

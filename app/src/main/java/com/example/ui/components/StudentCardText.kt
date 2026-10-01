package com.example.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * Text primitives for compact information cards.
 *
 * Rule:
 * - card titles stay one line
 * - descriptions stay two lines
 * - metadata stays one line
 *
 * This prevents dynamic user text from determining unbounded card height.
 */
@Composable
fun StudentCardTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun StudentCardBody(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    maxLines: Int = 2
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun StudentCardMeta(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelSmall
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

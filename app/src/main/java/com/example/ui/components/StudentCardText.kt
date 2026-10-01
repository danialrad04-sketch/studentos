package com.example.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

private fun TextStyle.withOptionalFontSize(fontSize: TextUnit?): TextStyle =
    if (fontSize == null || fontSize == TextUnit.Unspecified) this else copy(fontSize = fontSize)

/**
 * Shared text contract for compact Student OS information surfaces.
 *
 * Title: one line, no uncontrolled growth.
 * Meta: one line, ellipsized.
 * Body: up to three lines with a hard visual height cap.
 */
@Composable
fun StudentCardTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit? = null
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.withOptionalFontSize(fontSize),
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        softWrap = false
    )
}

@Composable
fun StudentCardBody(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    maxLines: Int = 2,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit? = null
) {
    val safeLines = maxLines.coerceIn(1, 3)
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 72.dp),
        style = style.withOptionalFontSize(fontSize),
        color = color,
        maxLines = safeLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun StudentCardMeta(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontWeight: FontWeight? = null,
    fontSize: TextUnit? = null
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = style.withOptionalFontSize(fontSize),
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        softWrap = false
    )
}

package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Static diffuse light behind frosted surfaces; no perpetual animations or bitmap captures. */
@Composable
fun Modifier.studentWorkspaceBackground(): Modifier {
    val colors = MaterialTheme.colorScheme
    return drawWithCache {
        val radius = size.width.coerceAtLeast(1f) * 1.1f
        val upper =
            Brush.radialGradient(
                listOf(colors.secondaryContainer.copy(alpha = 0.48f), Color.Transparent),
                center = Offset(size.width, 0f),
                radius = radius,
            )
        val lower =
            Brush.radialGradient(
                listOf(colors.primaryContainer.copy(alpha = 0.32f), Color.Transparent),
                center = Offset(0f, size.height * 0.7f),
                radius = radius,
            )
        onDrawBehind {
            drawRect(colors.background)
            drawRect(upper)
            drawRect(lower)
        }
    }
}

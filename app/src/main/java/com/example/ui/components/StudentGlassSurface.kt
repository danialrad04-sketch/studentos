package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** Frosted material over the diffuse workspace canvas; text is never blurred. */
@Composable
fun StudentGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    elevated: Boolean = false,
    surfaceAlpha: Float? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.background.luminance() < 0.5f
    Surface(
        modifier = modifier,
        shape = shape,
        color =
            colors.surface.copy(
                alpha = surfaceAlpha?.coerceIn(0f, 1f) ?: if (dark) 0.88f else 0.86f
            ),
        contentColor = colors.onSurface,
        shadowElevation = if (elevated) 8.dp else 0.dp,
        border =
            BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        if (dark) Color.White.copy(alpha = 0.22f) else Color.White,
                        colors.outlineVariant.copy(alpha = 0.65f),
                        colors.secondary.copy(alpha = 0.16f),
                    )
                ),
            ),
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(
                    listOf(
                        colors.secondaryContainer.copy(alpha = if (dark) 0.16f else 0.25f),
                        Color.Transparent,
                        colors.primaryContainer.copy(alpha = 0.16f),
                    )
                )
            ),
            content = content,
        )
    }
}

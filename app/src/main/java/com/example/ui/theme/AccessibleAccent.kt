package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** Preserve a chosen hue while ensuring labels/icons remain visible on the actual surface. */
fun readableAccent(color: Color, surface: Color, minimumContrast: Float = 4.5f): Color {
    fun contrast(candidate: Color): Float {
        val foreground = candidate.luminance()
        val background = surface.luminance()
        return (maxOf(foreground, background) + 0.05f) / (minOf(foreground, background) + 0.05f)
    }
    val opaque = color.copy(alpha = 1f)
    if (contrast(opaque) >= minimumContrast) return opaque
    val target = if (contrast(Color.Black) >= contrast(Color.White)) Color.Black else Color.White
    for (step in 1..32) {
        val adjusted = lerp(opaque, target, step / 32f)
        if (contrast(adjusted) >= minimumContrast) return adjusted
    }
    return target
}

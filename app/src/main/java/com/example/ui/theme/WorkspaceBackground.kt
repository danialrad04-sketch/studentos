package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp

/** Quiet canvas tint; cards and text continue to use opaque semantic surfaces. */
@Composable
fun Modifier.studentWorkspaceBackground(): Modifier {
    val colors=MaterialTheme.colorScheme
    return background(Brush.verticalGradient(listOf(lerp(colors.background,colors.primaryContainer,0.08f),colors.background)))
}

package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * 2026 Tactile Elastic Touch Modifier with Haptic Feedback.
 * Compresses slightly on press (scale = 0.965f) with a spring bounce on release,
 * matching flagship Samsung One UI and modern iOS fluid design trends.
 */
fun Modifier.tactileClickable(
    pressedScale: Float = 0.965f,
    performHaptic: Boolean = false, // Meaningful haptic only: do not vibrate on ordinary clicks
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 500f
        ),
        label = "tactile_scale_spring"
    )

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                onClick()
            }
        )
}

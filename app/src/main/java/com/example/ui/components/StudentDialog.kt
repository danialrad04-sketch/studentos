package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.rememberReducedMotion

/** A shared, interruptible transition for centered pickers and full-screen workspaces. */
@Composable
fun StudentDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    val reduced = rememberReducedMotion()
    val visible = remember { MutableTransitionState(false) }
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true; visible.targetState = true }
    LaunchedEffect(visible.isIdle, visible.currentState, visible.targetState) {
        if (started && visible.isIdle && !visible.currentState && !visible.targetState) onDismissRequest()
    }
    val dismiss = { visible.targetState = false }
    Dialog(onDismissRequest = dismiss, properties = properties) {
        BackHandler(enabled = properties.dismissOnBackPress, onBack = dismiss)
        CompositionLocalProvider(LocalStudentModalDismiss provides dismiss) {
            AnimatedVisibility(
                visibleState = visible,
                enter = if (reduced) fadeIn(snap()) else fadeIn(tween(160)) + scaleIn(initialScale = 0.96f, animationSpec = spring(dampingRatio = 1f, stiffness = 500f)),
                exit = if (reduced) fadeOut(snap()) else fadeOut(tween(120)) + scaleOut(targetScale = 0.98f, animationSpec = tween(120))
            ) { content() }
        }
    }
}

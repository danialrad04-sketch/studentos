package com.example.ui.components

import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/** Only page content is recorded. Navigation never becomes part of its own backdrop. */
@Stable
internal class StudentNavigationBackdrop(val layer: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
}

internal val LocalStudentNavigationBackdrop =
    staticCompositionLocalOf<StudentNavigationBackdrop?> { null }

@Composable
internal fun rememberStudentNavigationBackdrop(): StudentNavigationBackdrop? {
    if (Build.VERSION.SDK_INT < 31 || !LocalView.current.isHardwareAccelerated) return null
    val layer = rememberGraphicsLayer()
    return remember(layer) { StudentNavigationBackdrop(layer) }
}

internal fun Modifier.captureNavigationBackdrop(backdrop: StudentNavigationBackdrop?): Modifier =
    if (backdrop == null) this
    else
        onGloballyPositioned { backdrop.origin = it.positionInRoot() }
            .drawWithContent {
                backdrop.layer.record { this@drawWithContent.drawContent() }
                drawLayer(backdrop.layer)
            }

/** Blur a small, clipped sample of the page; foreground stickers and labels stay sharp. */
@Composable
internal fun Modifier.studentNavigationBackdrop(shape: Shape): Modifier {
    val source = LocalStudentNavigationBackdrop.current ?: return this
    val blurred = rememberGraphicsLayer()
    val radius = with(LocalDensity.current) { 18.dp.toPx() }
    val effect = remember(radius) { BlurEffect(radius, radius, TileMode.Clamp) }
    SideEffect { blurred.renderEffect = effect }
    var origin by remember { mutableStateOf(Offset.Zero) }
    return clip(shape)
        .onGloballyPositioned { origin = it.positionInRoot() }
        .drawWithContent {
            val offset = origin - source.origin
            blurred.record { translate(-offset.x, -offset.y) { drawLayer(source.layer) } }
            drawLayer(blurred)
            drawContent()
        }
}

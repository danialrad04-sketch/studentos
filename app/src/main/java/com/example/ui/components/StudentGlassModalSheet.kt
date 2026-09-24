package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.StudentOsColors
import com.example.ui.theme.StudentOsGlassTokens
import kotlinx.coroutines.delay

/**
 * 2026 Architectural Standard for Minimalist Fluid Glass Modal Bottom Sheet.
 * Adapts seamlessly across:
 * - All screen sizes (Compact smartphones, Foldables, Tablets)
 * - Both Light & Dark Themes (Dark Velvet space glass vs Frost Crisp Paper glass)
 * - Safe Area Insets (Keyboard IME padding + System Navigation Bar)
 */
@Composable
fun StudentGlassModalSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 560.dp,
    maxHeightPercent: Float = 0.92f,
    showDragHandle: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    // Derive theme strictly from MaterialTheme color scheme luminance so pop-ups never break
    // when app theme differs from phone's system theme
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDark = surfaceColor.luminance() < 0.5f

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val sheetShape = RoundedCornerShape(26.dp)

    // Explicit Material3 Theme Tokens for zero-color-leakage consistency across themes
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val dragHandleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    Dialog(
        onDismissRequest = {
            isVisible = false
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        BackHandler(enabled = isVisible) {
            isVisible = false
            onDismiss()
        }

        com.example.ui.theme.MyApplicationTheme(darkTheme = isDark) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Surface(
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isVisible = false
                                onDismiss()
                            }
                            .navigationBarsPadding()
                            .statusBarsPadding()
                            .imePadding()
                            .padding(horizontal = 14.dp, vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val screenHeight = maxHeight
                        val sheetMaxHeight = screenHeight * maxHeightPercent.coerceIn(0.65f, 0.88f)

                        AnimatedVisibility(
                            visible = isVisible,
                            enter = scaleIn(
                                initialScale = 0.90f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + fadeIn(
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                            exit = scaleOut(
                                targetScale = 0.93f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + fadeOut(
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = maxWidth)
                                    .heightIn(max = sheetMaxHeight)
                                    .shadow(
                                        elevation = 16.dp,
                                        shape = sheetShape,
                                        spotColor = Color.Black.copy(alpha = 0.35f),
                                        ambientColor = Color.Black.copy(alpha = 0.20f)
                                    )
                                    .clip(sheetShape)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.surfaceContainer,
                                                MaterialTheme.colorScheme.surfaceContainerLow
                                            )
                                        ),
                                        shape = sheetShape
                                    )
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), sheetShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { /* Consume click inside sheet */ }
                                    .then(modifier)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = sheetMaxHeight)
                                        .padding(horizontal = 18.dp, vertical = 14.dp)
                                ) {
                                    if (showDragHandle) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .padding(bottom = 10.dp)
                                                .size(width = 38.dp, height = 4.5.dp)
                                                .clip(CircleShape)
                                                .background(dragHandleColor)
                                        )
                                    }
                                    content()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

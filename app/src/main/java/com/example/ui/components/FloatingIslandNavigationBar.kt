package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.models.AppTab
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.rememberReducedMotion
import kotlinx.coroutines.launch

private data class UltraNavItem(
    val tab: AppTab,
    val outlineIcon: ImageVector,
    val filledIcon: ImageVector,
    val label: String
)

/**
 * 3D Glassmorphic Responsive Floating Dock Navigation Bar.
 * Designed with Apple / Modern 2026 Mobile Architecture standards:
 * - 3D Specular Highlight Bevel Borders
 * - Responsive Max Width Clamp (Adaptive for phones, foldables, and tablets)
 * - Volumetric Spherical 3D Center FAB with Ambient Glow
 * - Spring Physics Active Pill Morphing & Micro-bounce
 * - Tactile Interactive Pressure Scaling (0.92f press depth)
 */
@Composable
fun FloatingIslandNavigationBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllModulesSheet by remember { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()

    val navItems = remember {
        listOf(
            UltraNavItem(AppTab.DASHBOARD, Icons.Outlined.Home, Icons.Rounded.Home, "خانه"),
            UltraNavItem(AppTab.SCHEDULE, Icons.Outlined.CalendarMonth, Icons.Rounded.CalendarMonth, "برنامه"),
            UltraNavItem(AppTab.TASKS, Icons.Outlined.CheckCircle, Icons.Rounded.CheckCircle, "کارها"),
            UltraNavItem(AppTab.GRADES, Icons.Outlined.BarChart, Icons.Rounded.BarChart, "کارنامه")
        )
    }

    val isOtherTab = navItems.none { it.tab == selectedTab }

    // Outer responsive layout container (centered with widthIn clamp for tablet/desktop)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Ambient Underglow Blur Layer (Soft primary-colored 3D shadow)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 12.dp)
                    .offset(y = 6.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(36.dp)
                    )
            )

            // Main 3D Glassmorphic Floating Dock Shell
            Surface(
                shape = RoundedCornerShape(34.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                border = BorderStroke(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    )
                ),
                shadowElevation = 14.dp,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            ) {
                // Subtle top sheen highlight layer for real glass optical depth
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.14f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. خانه (Home)
                        Modern3DNavItemButton(
                            item = navItems[0],
                            selected = selectedTab == AppTab.DASHBOARD,
                            onClick = { onTabSelected(AppTab.DASHBOARD) },
                            reducedMotion = reducedMotion,
                            modifier = Modifier.weight(1f)
                        )

                        // 2. برنامه (Schedule)
                        Modern3DNavItemButton(
                            item = navItems[1],
                            selected = selectedTab == AppTab.SCHEDULE,
                            onClick = { onTabSelected(AppTab.SCHEDULE) },
                            reducedMotion = reducedMotion,
                            modifier = Modifier.weight(1f)
                        )

                        // 3. Center Spacer / Anchor for Elevated 3D FAB
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Empty anchor to reserve space in dock row
                        }

                        // 4. کارها (Tasks)
                        Modern3DNavItemButton(
                            item = navItems[2],
                            selected = selectedTab == AppTab.TASKS,
                            onClick = { onTabSelected(AppTab.TASKS) },
                            reducedMotion = reducedMotion,
                            modifier = Modifier.weight(1f)
                        )

                        // 5. بیشتر (More / Modules Hub)
                        Modern3DNavItemButton(
                            item = UltraNavItem(
                                tab = AppTab.DASHBOARD,
                                outlineIcon = Icons.Outlined.MoreHoriz,
                                filledIcon = Icons.Outlined.MoreHoriz,
                                label = "بیشتر"
                            ),
                            selected = isOtherTab,
                            onClick = { showAllModulesSheet = true },
                            reducedMotion = reducedMotion,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3D Elevated Center Floating Action Button (Raised above Dock)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-18).dp),
                contentAlignment = Alignment.Center
            ) {
                Modern3DCenterActionButton(
                    onClick = { showAllModulesSheet = true },
                    reducedMotion = reducedMotion
                )
            }
        }
    }

    // Modal Hub Sheet for all modules
    if (showAllModulesSheet) {
        StudentModuleHubSheet(
            currentTab = selectedTab,
            onSelectTab = {
                onTabSelected(it)
                showAllModulesSheet = false
            },
            onDismiss = { showAllModulesSheet = false }
        )
    }
}

/**
 * 3D Tab Item with Spring-Morph Pill Capsule and Micro-glow indicator.
 */
@Composable
private fun Modern3DNavItemButton(
    item: UltraNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile press depth scale
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press_scale_${item.label}"
    )

    // Selection pop scale with bouncy spring physics
    val selectScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1.0f,
        animationSpec = if (reducedMotion) {
            tween(150)
        } else {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        },
        label = "select_scale_${item.label}"
    )

    val currentIcon = if (selected) item.filledIcon else item.outlineIcon

    Column(
        modifier = modifier
            .scale(pressScale * selectScale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Active 3D Pill Capsule surrounding the icon
        Box(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (selected) {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            )
                        )
                    } else {
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                )
                .border(
                    width = if (selected) 1.dp else 0.dp,
                    brush = if (selected) {
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.65f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            )
                        )
                    } else {
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    },
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 14.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = currentIcon,
                contentDescription = item.label,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                },
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(Modifier.height(3.dp))

        // Label with dynamic typography weight
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                letterSpacing = 0.sp
            ),
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f)
            },
            maxLines = 1
        )

        // Micro-glow neon dot under active tab
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(width = if (selected) 12.dp else 0.dp, height = 2.5.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (selected) {
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        )
                    } else {
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                )
        )
    }
}

/**
 * 3D Spherical Elevated Floating Action Button (Center Hero FAB).
 * Engineered with 3D volumetric multi-stop gradients, ambient pulsating aura,
 * and specular highlight reflection bevel.
 */
@Composable
private fun Modern3DCenterActionButton(
    onClick: () -> Unit,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()
    val rotationAnim = remember { Animatable(0f) }

    // Pulsing aura animation in background
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val pulseGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_pulse_alpha"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fab_press_scale"
    )

    Box(
        modifier = modifier
            .size(72.dp)
            .scale(pressScale),
        contentAlignment = Alignment.Center
    ) {
        // 1. Ambient Glow Aura Ring
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = pulseGlowAlpha),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 2. 3D Floating Sphere Body
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 14.dp,
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        scope.launch {
                            rotationAnim.animateTo(
                                targetValue = rotationAnim.value + 90f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                        onClick()
                    }
                )
        ) {
            // Volumetric 3D gradient surface
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.90f)
                            )
                        )
                    )
                    .border(
                        border = BorderStroke(
                            width = 1.6.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.70f),
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Top hemisphere specular 3D light reflection
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.38f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // High-contrast Plus icon with spring rotation
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "امکانات و ابزارهای تحصیلی",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(if (reducedMotion) 0f else rotationAnim.value)
                )
            }
        }
    }
}

@Preview(name = "3D Floating Navigation Light", showBackground = true)
@Composable
private fun FloatingIslandNavPreviewLight() {
    MyApplicationTheme(darkTheme = false) {
        FloatingIslandNavigationBar(
            selectedTab = AppTab.DASHBOARD,
            onTabSelected = {}
        )
    }
}

@Preview(name = "3D Floating Navigation Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun FloatingIslandNavPreviewDark() {
    MyApplicationTheme(darkTheme = true) {
        FloatingIslandNavigationBar(
            selectedTab = AppTab.DASHBOARD,
            onTabSelected = {}
        )
    }
}

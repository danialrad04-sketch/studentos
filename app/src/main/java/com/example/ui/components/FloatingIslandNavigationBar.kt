package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.models.AppTab
import com.example.ui.theme.rememberReducedMotion

private val primaryDestinations =
    listOf(AppTab.DASHBOARD, AppTab.SCHEDULE, AppTab.TASKS, AppTab.GRADES, AppTab.ACCOUNT)
private val primaryLabels = listOf("خانه", "برنامه", "کارها", "کارنامه", "حساب")

/** Five stable targets, one moving indicator and a single system-inset owner. */
@Composable
fun FloatingIslandNavigationBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = primaryDestinations.indexOf(selectedTab)
    val duration = if (rememberReducedMotion()) 0 else 220
    val largeText = LocalDensity.current.fontScale > 1.5f
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = if (largeText) 6.dp else 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        StudentGlassSurface(
            Modifier.widthIn(max = 560.dp)
                .fillMaxWidth()
                .testTag("primary_navigation")
                .studentNavigationBackdrop(RoundedCornerShape(32.dp)),
            RoundedCornerShape(32.dp),
            elevated = false,
            surfaceAlpha = if (LocalStudentNavigationBackdrop.current != null) 0.76f else 0.98f,
        ) {
            BoxWithConstraints(
                Modifier.fillMaxWidth().padding(if (largeText) 4.dp else 6.dp).selectableGroup()
            ) {
                val cellWidth = maxWidth / 5
                val indicatorOffset by
                    animateDpAsState(
                        cellWidth * selectedIndex.coerceAtLeast(0),
                        tween(duration),
                        label = "nav-indicator",
                    )
                if (selectedIndex >= 0)
                    Box(
                        Modifier.offset(x = indicatorOffset)
                            .width(cellWidth)
                            .padding(horizontal = 3.dp, vertical = 2.dp)
                            .height(42.dp)
                            .background(colors.primaryContainer, RoundedCornerShape(22.dp))
                    )
                Row(Modifier.fillMaxWidth()) {
                    repeat(5) { index ->
                        val tab = primaryDestinations[index]
                        val selected = selectedIndex == index
                        val scale by
                            animateFloatAsState(
                                if (selected) 1.05f else 1f,
                                tween(duration),
                                label = "nav-icon",
                            )
                        Column(
                            Modifier.weight(1f)
                                .heightIn(min = 64.dp)
                                .testTag("nav_${tab.name}")
                                .selectable(
                                    selected,
                                    role = Role.Tab,
                                    onClick = { onTabSelected(tab) },
                                )
                                .padding(
                                    horizontal = if (largeText) 0.dp else 2.dp,
                                    vertical = 8.dp,
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            AppEmoji(
                                studentDestinationSticker(tab),
                                Modifier.graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                                size = 28.dp,
                            )
                            Text(
                                if (largeText && index == 3) "نمره" else primaryLabels[index],
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) colors.primary else colors.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

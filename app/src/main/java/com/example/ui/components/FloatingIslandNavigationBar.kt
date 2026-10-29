package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.models.AppTab
import com.example.ui.theme.rememberReducedMotion

private val primaryDestinations =
    listOf(AppTab.DASHBOARD, AppTab.SCHEDULE, AppTab.TASKS, AppTab.GRADES)
private val primaryLabels = listOf("خانه", "برنامه", "کارها", "کارنامه", "بیشتر")

/** Five stable targets, one moving indicator and a single system-inset owner. */
@Composable
fun FloatingIslandNavigationBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    onOpenMore: (() -> Unit)? = null,
) {
    var showAllModulesSheet by remember { mutableStateOf(false) }
    val selectedIndex = primaryDestinations.indexOf(selectedTab).let { if (it < 0) 4 else it }
    val duration = if (rememberReducedMotion()) 0 else 220
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        StudentGlassSurface(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().testTag("primary_navigation"),
            RoundedCornerShape(32.dp),
            elevated = true,
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(6.dp).selectableGroup()) {
                val cellWidth = maxWidth / 5
                val indicatorOffset by
                    animateDpAsState(
                        cellWidth * selectedIndex,
                        tween(duration),
                        label = "nav-indicator",
                    )
                Box(
                    Modifier.offset(x = indicatorOffset)
                        .width(cellWidth)
                        .padding(horizontal = 3.dp, vertical = 2.dp)
                        .height(42.dp)
                        .background(colors.primaryContainer, RoundedCornerShape(22.dp))
                )
                Row(Modifier.fillMaxWidth()) {
                    repeat(5) { index ->
                        val tab = primaryDestinations.getOrNull(index)
                        val selected = selectedIndex == index
                        val tint by
                            animateColorAsState(
                                if (selected) colors.onPrimaryContainer
                                else colors.onSurfaceVariant,
                                tween(duration),
                                label = "nav-tint",
                            )
                        val scale by
                            animateFloatAsState(
                                if (selected) 1.08f else 1f,
                                tween(duration),
                                label = "nav-icon",
                            )
                        Column(
                            Modifier.weight(1f)
                                .heightIn(min = 68.dp)
                                .testTag(tab?.let { "nav_${it.name}" } ?: "nav_more")
                                .selectable(
                                    selected,
                                    role = Role.Tab,
                                    onClick = {
                                        if (tab != null) onTabSelected(tab)
                                        else
                                            onOpenMore?.invoke()
                                                ?: run { showAllModulesSheet = true }
                                    },
                                )
                                .padding(horizontal = 2.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Icon(
                                tab?.let(::studentDestinationIcon) ?: Icons.Rounded.GridView,
                                null,
                                Modifier.size(24.dp).graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                },
                                tint = tint,
                            )
                            Text(
                                primaryLabels[index],
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
    if (showAllModulesSheet)
        StudentModuleHubSheet(
            selectedTab,
            {
                onTabSelected(it)
                showAllModulesSheet = false
            },
            { showAllModulesSheet = false },
        )
}

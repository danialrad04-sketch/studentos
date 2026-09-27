package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.models.AppTab
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.rememberReducedMotion

private data class ModernNavItem(val tab: AppTab, val icon: ImageVector, val label: String)

@Composable
fun FloatingIslandNavigationBar(selectedTab: AppTab, onTabSelected: (AppTab) -> Unit, modifier: Modifier = Modifier) {
    var showAllModulesSheet by remember { mutableStateOf(false) }
    val reducedMotion = rememberReducedMotion()
    val items = listOf(
        ModernNavItem(AppTab.DASHBOARD, Icons.Outlined.Home, "خانه"),
        ModernNavItem(AppTab.SCHEDULE, Icons.Outlined.CalendarMonth, "برنامه"),
        ModernNavItem(AppTab.TASKS, Icons.Outlined.CheckCircle, "کارها"),
        ModernNavItem(AppTab.GRADES, Icons.Outlined.BarChart, "کارنامه")
    )
    val isOtherTab = items.none { it.tab == selectedTab }

    Box(
        modifier = modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            items[0].let { item -> ModernNavItemButton(item, selectedTab == item.tab, { onTabSelected(item.tab) }, reducedMotion, Modifier.weight(1f)) }
            items[1].let { item -> ModernNavItemButton(item, selectedTab == item.tab, { onTabSelected(item.tab) }, reducedMotion, Modifier.weight(1f)) }
            ModernCenterActionButton(onClick = { showAllModulesSheet = true }, reducedMotion = reducedMotion, modifier = Modifier.padding(horizontal = 5.dp))
            items[2].let { item -> ModernNavItemButton(item, selectedTab == item.tab, { onTabSelected(item.tab) }, reducedMotion, Modifier.weight(1f)) }
            items[3].let { item -> ModernNavItemButton(item, selectedTab == item.tab, { onTabSelected(item.tab) }, reducedMotion, Modifier.weight(1f)) }
            ModernNavItemButton(ModernNavItem(AppTab.DASHBOARD, Icons.Outlined.MoreHoriz, "بیشتر"), isOtherTab, { showAllModulesSheet = true }, reducedMotion, Modifier.weight(1f))
        }
    }

    if (showAllModulesSheet) {
        StudentModuleHubSheet(currentTab = selectedTab, onSelectTab = { onTabSelected(it); showAllModulesSheet = false }, onDismiss = { showAllModulesSheet = false })
    }
}

@Composable
private fun ModernNavItemButton(item: ModernNavItem, selected: Boolean, onClick: () -> Unit, reducedMotion: Boolean, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = if (reducedMotion) androidx.compose.animation.core.snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "nav_${item.label}"
    )
    Column(modifier = modifier.scale(scale).minimumInteractiveComponentSize().padding(vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f) else Color.Transparent, contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, tonalElevation = 0.dp, shadowElevation = 0.dp) {
            Box(modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(if (selected) 22.dp else 21.dp))
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(text = item.label, style = MaterialTheme.typography.labelSmall, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun ModernCenterActionButton(onClick: () -> Unit, reducedMotion: Boolean, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(targetValue = 1f, animationSpec = if (reducedMotion) androidx.compose.animation.core.snap() else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "center_action")
    Surface(modifier = modifier.size(50.dp).scale(scale), onClick = onClick, shape = CircleShape, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shadowElevation = 7.dp, tonalElevation = 0.dp) {
        Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, contentDescription = "امکانات بیشتر", modifier = Modifier.size(25.dp)) }
    }
}

@Preview(name = "Modern Floating Navigation", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun FloatingIslandNavPreviewDark() {
    MyApplicationTheme(darkTheme = true) { FloatingIslandNavigationBar(selectedTab = AppTab.DASHBOARD, onTabSelected = {}) }
}
package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.models.AppTab

private data class NavItem(
    val tab: AppTab?,
    val outlineIcon: ImageVector,
    val selectedIcon: ImageVector,
    val label: String
)

/** Primary destinations have one entry each; secondary tools live in the hub. */
@Composable
fun FloatingIslandNavigationBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
    onOpenMore: (() -> Unit)? = null
) {
    var showAllModulesSheet by remember { mutableStateOf(false) }
    val items = remember {
        listOf(
            NavItem(AppTab.DASHBOARD, Icons.Outlined.Home, Icons.Rounded.Home, "خانه"),
            NavItem(AppTab.SCHEDULE, Icons.Outlined.CalendarMonth, Icons.Rounded.CalendarMonth, "برنامه"),
            NavItem(AppTab.TASKS, Icons.Outlined.CheckCircle, Icons.Rounded.CheckCircle, "کارها"),
            NavItem(AppTab.GRADES, Icons.Outlined.BarChart, Icons.Rounded.BarChart, "کارنامه"),
            NavItem(null, Icons.Outlined.GridView, Icons.Rounded.GridView, "بیشتر")
        )
    }
    Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
    Surface(
        modifier = modifier.widthIn(max = 600.dp).fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp
    ) {
    NavigationBar(
        modifier = Modifier.fillMaxWidth().testTag("primary_navigation"),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val selected = item.tab?.let { it == selectedTab }
                ?: items.none { it.tab == selectedTab }
            NavigationBarItem(
                selected = selected,
                onClick = {
                    item.tab?.let(onTabSelected) ?: run { onOpenMore?.invoke() ?: run { showAllModulesSheet = true } }
                },
                icon = {
                    Icon(if (selected) item.selectedIcon else item.outlineIcon, contentDescription = null, modifier = Modifier.size(23.dp))
                },
                label = {
                    Text(item.label, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
    }
    }
    if (showAllModulesSheet) {
        StudentModuleHubSheet(
            currentTab = selectedTab,
            onSelectTab = { onTabSelected(it); showAllModulesSheet = false },
            onDismiss = { showAllModulesSheet = false }
        )
    }
}

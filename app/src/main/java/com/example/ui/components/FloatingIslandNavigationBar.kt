package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.models.AppTab

private data class NavItem(
    val tab: AppTab?,
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
            NavItem(AppTab.DASHBOARD, "خانه"),
            NavItem(AppTab.SCHEDULE, "برنامه"),
            NavItem(AppTab.TASKS, "کارها"),
            NavItem(AppTab.GRADES, "کارنامه"),
            NavItem(null, "بیشتر")
        )
    }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    NavigationBar(
        modifier = modifier.widthIn(max = 600.dp).fillMaxWidth().testTag("primary_navigation"),
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
                    AppEmoji(item.tab?.let(::studentDestinationSticker) ?: AppEmojiType.MORE, size = 32.dp)
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

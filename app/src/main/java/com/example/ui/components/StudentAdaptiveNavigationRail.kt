package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.models.AppTab
import com.example.ui.theme.StudentSpacing

/**
 * Expanded navigation shell. The same module hub used on phones is exposed
 * here as well, preventing secondary destinations from becoming unreachable
 * on tablets and large screens.
 */
@Composable
fun StudentAdaptiveNavigationRail(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllModulesSheet by remember { mutableStateOf(false) }

    val destinations = listOf(
        AppTab.DASHBOARD to Icons.Default.Home,
        AppTab.SCHEDULE to Icons.Default.CalendarMonth,
        AppTab.TASKS to Icons.Default.CheckBox,
        AppTab.GRADES to Icons.Default.Analytics,
        AppTab.COPILOT to Icons.Default.SmartToy
    )
    val isSecondaryTab = destinations.none { it.first == selectedTab }

    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surface,
        header = {
            Text(
                text = "Student OS",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(vertical = StudentSpacing.Md)
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(StudentSpacing.Xs)) {
            destinations.forEach { (tab, icon) ->
                NavigationRailItem(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = { Icon(icon, contentDescription = tab.title) },
                    label = { Text(tab.title.split(" ").firstOrNull() ?: tab.title) }
                )
            }

            NavigationRailItem(
                selected = isSecondaryTab,
                onClick = { showAllModulesSheet = true },
                icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "بیشتر") },
                label = { Text("بیشتر") }
            )
        }
    }

    if (showAllModulesSheet) {
        StudentModuleHubSheet(
            currentTab = selectedTab,
            onSelectTab = onTabSelected,
            onDismiss = { showAllModulesSheet = false }
        )
    }
}

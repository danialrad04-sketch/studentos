package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.StudentProfileEntity
import com.example.domain.model.UserAccount
import com.example.ui.models.AppTab
import kotlinx.coroutines.launch

@Composable
fun StudentNavigationDrawer(
    open: Boolean,
    onOpenChanged: (Boolean) -> Unit,
    selectedTab: AppTab,
    profile: StudentProfileEntity,
    user: UserAccount,
    onSelectTab: (AppTab) -> Unit,
    onOpenAccount: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSupport: () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var wasOpened by remember { mutableStateOf(false) }
    LaunchedEffect(open) { if (open) drawerState.open() else drawerState.close() }
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Open) wasOpened = true
        else if (wasOpened) { wasOpened = false; onOpenChanged(false) }
    }
    fun select(action: () -> Unit) {
        scope.launch { drawerState.close(); onOpenChanged(false); action() }
    }
    BackHandler(enabled = open) { onOpenChanged(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val drawerWidth = (maxWidth * 0.88f).coerceAtMost(360.dp)
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(drawerWidth).testTag("app_drawer"), drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.School, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(profile.name.ifBlank { user.displayName }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(if (user.isGuest) "حالت مهمان · ذخیره روی دستگاه" else user.email ?: "حساب متصل", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        NavigationDrawerItem(label = { Text("حساب کاربری") }, icon = { Icon(Icons.Rounded.AccountCircle, null) }, selected = false, onClick = { select(onOpenAccount) })
                        NavigationDrawerItem(label = { Text("تنظیمات") }, icon = { Icon(Icons.Rounded.Settings, null) }, selected = false, onClick = { select(onOpenSettings) })
                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                        Text("فضای تحصیلی", Modifier.padding(16.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        AppTab.entries.forEach { tab ->
                            NavigationDrawerItem(
                                label = { Text(tab.title, style = MaterialTheme.typography.bodyLarge) },
                                icon = { Icon(studentDestinationIcon(tab), null) },
                                selected = selectedTab == tab,
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                onClick = { select { onSelectTab(tab) } }
                            )
                        }
                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                        NavigationDrawerItem(label = { Text("پشتیبانی") }, icon = { Icon(Icons.AutoMirrored.Rounded.HelpOutline, null) }, selected = false, onClick = { select(onOpenSupport) })
                        Spacer(Modifier.height(16.dp))
                    }
                }
            },
            content = content
        )
    }
}

internal fun studentDestinationIcon(tab: AppTab) = when (tab) {
    AppTab.DASHBOARD -> Icons.Rounded.Home
    AppTab.SCHEDULE -> Icons.Rounded.CalendarMonth
    AppTab.TASKS -> Icons.Rounded.CheckCircle
    AppTab.GRADES -> Icons.Rounded.BarChart
    AppTab.EXAMS -> Icons.Rounded.Event
    AppTab.ATTENDANCE -> Icons.Rounded.FactCheck
    AppTab.COPILOT -> Icons.Rounded.AutoAwesome
    AppTab.ACADEMIC_INTELLIGENCE -> Icons.Rounded.Insights
    AppTab.POMODORO -> Icons.Rounded.Timer
    AppTab.GAMIFICATION -> Icons.Rounded.EmojiEvents
    AppTab.HISTORY -> Icons.Rounded.History
    else -> Icons.Rounded.MenuBook
}

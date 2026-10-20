package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(open) { if (open) drawerState.open() else drawerState.close() }
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Open) wasOpened = true
        else if (wasOpened) { wasOpened = false; onOpenChanged(false) }
    }
    fun select(action: () -> Unit) {
        scope.launch { drawerState.close(); onOpenChanged(false); query = ""; action() }
    }
    BackHandler(enabled = open) { onOpenChanged(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val drawerWidth = (maxWidth * 0.9f).coerceAtMost(380.dp)
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(drawerWidth).testTag("app_drawer"),
                    drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Icon(Icons.Rounded.School, null, Modifier.padding(10.dp).size(24.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text("Student OS", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("فضای تحصیلی من", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onOpenChanged(false) }) { Icon(Icons.Rounded.Close, "بستن منوی برنامه") }
                        }
                        // Keep navigation scrollable even on a short window or with large text.
                        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Card(
                                onClick = { select(onOpenAccount) },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(profile.name.ifBlank { user.displayName.ifBlank { "دانشجوی عزیز" } }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(if (user.isGuest) "حالت مهمان · ذخیره روی دستگاه" else user.email ?: "حساب متصل", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("حساب کاربری", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            NavigationDrawerItem(label = { Text("تنظیمات") }, icon = { Icon(Icons.Rounded.Settings, null) }, selected = false, onClick = { select(onOpenSettings) })
                            OutlinedTextField(
                                value = query, onValueChange = { query = it },
                                label = { Text("پیدا کردن صفحه") },
                                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "پاک کردن جست‌وجوی منو") } },
                                singleLine = true, shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().testTag("drawer_search")
                            )
                            val groups = studentFeatureGroups.map { (section, tabs) -> section to tabs.filter { it.title.contains(query.trim(), ignoreCase = true) } }.filter { it.second.isNotEmpty() }
                            groups.forEach { (section, tabs) ->
                                Text(section, Modifier.padding(start = 12.dp, top = 18.dp, bottom = 4.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                tabs.forEach { tab ->
                                    NavigationDrawerItem(
                                        modifier = Modifier.testTag("drawer_${tab.name}"),
                                        label = { Text(tab.title, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal) },
                                        icon = { Icon(studentDestinationIcon(tab), null, Modifier.size(22.dp)) },
                                        selected = selectedTab == tab,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = NavigationDrawerItemDefaults.colors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        onClick = { select { onSelectTab(tab) } }
                                    )
                                }
                            }
                            if (groups.isEmpty()) Text("صفحه‌ای پیدا نشد؛ نام دیگری جست‌وجو کنید.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                            HorizontalDivider(Modifier.padding(vertical = 12.dp))
                            NavigationDrawerItem(label = { Text("پشتیبانی") }, icon = { Icon(Icons.AutoMirrored.Rounded.HelpOutline, null) }, selected = false, onClick = { select(onOpenSupport) })
                            Spacer(Modifier.height(16.dp))
                        }
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
    AppTab.PASSPORT -> Icons.Rounded.Badge
    AppTab.SEMESTER_PLANNER -> Icons.Rounded.Route
    AppTab.CURRICULUM -> Icons.Rounded.AccountTree
}

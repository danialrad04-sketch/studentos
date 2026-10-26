package com.example.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalDensity
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
        val drawerWidth = (maxWidth * 0.84f).coerceAtMost(340.dp)
        val shortWindow = maxHeight < 480.dp
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(drawerWidth).testTag("app_drawer"),
                    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Column(if (shortWindow) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                Icon(Icons.Rounded.School, null, Modifier.padding(9.dp).size(22.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text("Student OS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                                Text("منوی دانشجو", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onOpenChanged(false) }) { Icon(Icons.Rounded.Close, "بستن منوی برنامه") }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        OutlinedTextField(
                            value = query, onValueChange = { query = it },
                            placeholder = { Text("جست‌وجوی بخش‌ها") },
                            leadingIcon = { Icon(Icons.Rounded.Search, null) },
                            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "پاک کردن جست‌وجوی منو") } },
                            singleLine = true, shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).testTag("drawer_search")
                        )
                        // Only destinations scroll. Identity and core actions stay reachable at every font size.
                        Column((if (shortWindow) Modifier else Modifier.weight(1f).verticalScroll(rememberScrollState())).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val groups = studentFeatureGroups.map { (section, tabs) -> section to tabs.filter { it.title.contains(query.trim(), ignoreCase = true) } }.filter { it.second.isNotEmpty() }
                            groups.forEach { (section, tabs) ->
                                Text(section, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                tabs.forEach { tab ->
                                    NavigationDrawerItem(
                                        modifier = Modifier.testTag("drawer_${tab.name}"),
                                        label = { Text(tab.title, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal) },
                                        icon = { Icon(studentDestinationIcon(tab), null, Modifier.size(22.dp)) },
                                        selected = selectedTab == tab,
                                        shape = RoundedCornerShape(12.dp),
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
                            Spacer(Modifier.height(8.dp))
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            NavigationDrawerItem(
                                label = { Column { Text(profile.name.ifBlank { user.displayName.ifBlank { "دانشجو" } }, fontWeight = FontWeight.Bold); Text("حساب کاربری", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                                icon = { Icon(Icons.Rounded.AccountCircle, null) }, selected = false,
                                badge = { if (user.isGuest) Text("مهمان", style = MaterialTheme.typography.labelSmall) },
                                onClick = { select(onOpenAccount) }
                            )
                            val settingsItem: @Composable (Modifier) -> Unit = { itemModifier ->
                                NavigationDrawerItem(modifier = itemModifier, label = { Text("تنظیمات", style = MaterialTheme.typography.labelLarge) }, icon = { Icon(Icons.Rounded.Settings, null, Modifier.size(20.dp)) }, selected = false, onClick = { select(onOpenSettings) })
                            }
                            val supportItem: @Composable (Modifier) -> Unit = { itemModifier ->
                                NavigationDrawerItem(modifier = itemModifier, label = { Text("پشتیبانی", style = MaterialTheme.typography.labelLarge) }, icon = { Icon(Icons.AutoMirrored.Rounded.HelpOutline, null, Modifier.size(20.dp)) }, selected = false, onClick = { select(onOpenSupport) })
                            }
                            if (LocalDensity.current.fontScale >= 1.3f) {
                                settingsItem(Modifier.fillMaxWidth())
                                supportItem(Modifier.fillMaxWidth())
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    settingsItem(Modifier.weight(1f))
                                    supportItem(Modifier.weight(1f))
                                }
                            }
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
    AppTab.AGENDA -> Icons.Rounded.DateRange
    AppTab.NOTES -> Icons.Rounded.Description
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

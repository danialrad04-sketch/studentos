package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.models.AppTab
import com.example.ui.models.ThemeMode
import com.example.ui.theme.MyApplicationTheme

@Composable
fun SubScreenHeaderSection(
    currentTab: AppTab,
    onBackToDashboard: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit = {},
    notifCount: Int,
    isDarkTheme: Boolean,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Return to dashboard button (guaranteed 48x48dp touch target)
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(44.dp)
                        .tactileClickable { onBackToDashboard() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت به داشبورد",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "${currentTab.iconEmoji} ${currentTab.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (currentTab) {
                            AppTab.COPILOT -> "دستیار هوشمند و مشاور تحصیلی"
                            AppTab.SCHEDULE -> "تقویم کلاس‌ها و مدیریت تداخل"
                            AppTab.ATTENDANCE -> "رادار سقف غیبت قانون ۳/۱۶"
                            AppTab.TASKS -> "اسپرینت و تحویل تکالیف"
                            AppTab.EXAMS -> "برنامه امتحانات و کارت آزمون"
                            AppTab.GRADES -> "کارنامه و معدل کل"
                            AppTab.POMODORO -> "تمرکز عمیق و یادداشت سریع"
                            AppTab.CURRICULUM -> "چارت دروس مصوب وزارت علوم"
                            AppTab.PASSPORT -> "گواهینامه صلاحیت آکادمیک"
                            AppTab.GAMIFICATION -> "تالار افتخارات و استریک"
                            AppTab.HISTORY -> "دفترچه سوابق و آرشیو ترم‌ها"
                            else -> "ماژول مدیریت آموزشی"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Quick Theme Toggle (48x48dp touch target)
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    val themeIcon = when (themeMode) {
                        ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                        ThemeMode.LIGHT -> Icons.Default.LightMode
                        ThemeMode.DARK -> Icons.Default.DarkMode
                    }
                    Icon(
                        imageVector = themeIcon,
                        contentDescription = "تغییر تم",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Settings (48x48dp touch target)
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "تنظیمات",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Search (48x48dp touch target)
                IconButton(
                    onClick = onOpenSearch,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "جستجوی سریع",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Notifications (48x48dp touch target)
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    BadgedBox(
                        badge = {
                            if (notifCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ) {
                                    Text(
                                        text = "$notifCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "اعلان‌ها",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "SubScreenHeader Light", showBackground = true)
@Composable
private fun SubScreenHeaderPreviewLight() {
    MyApplicationTheme(darkTheme = false) {
        SubScreenHeaderSection(
            currentTab = AppTab.SCHEDULE,
            onBackToDashboard = {},
            onOpenSearch = {},
            onOpenNotifications = {},
            onOpenSettings = {},
            notifCount = 3,
            isDarkTheme = false,
            onToggleTheme = {}
        )
    }
}

@Preview(name = "SubScreenHeader Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun SubScreenHeaderPreviewDark() {
    MyApplicationTheme(darkTheme = true) {
        SubScreenHeaderSection(
            currentTab = AppTab.SCHEDULE,
            onBackToDashboard = {},
            onOpenSearch = {},
            onOpenNotifications = {},
            onOpenSettings = {},
            notifCount = 3,
            isDarkTheme = true,
            onToggleTheme = {}
        )
    }
}


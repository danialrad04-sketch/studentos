package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.models.AppTab
import com.example.ui.models.ThemeMode

/** Keep the title readable on compact phones; secondary actions live in overflow. */
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
    onOpenMenu: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    StudentGlassSurface(modifier.fillMaxWidth()) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackToDashboard) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت به داشبورد")
        }
        Column(Modifier.weight(1f).padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                currentTab.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

        }
        if (onOpenMenu == null) {
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Default.Search, "جستجوی سریع", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onOpenMenu != null) {
            IconButton(onClick = onOpenMenu) {
                Icon(Icons.Rounded.Menu, "باز کردن منوی برنامه")
            }
        }
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, "گزینه‌های صفحه", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (onOpenMenu != null) {
                    DropdownMenuItem(text = { Text("جستجوی سریع") }, onClick = { expanded = false; onOpenSearch() })
                }
                DropdownMenuItem(
                    text = { Text(if (notifCount > 0) "اعلان‌ها ($notifCount)" else "اعلان‌ها") },
                    onClick = { expanded = false; onOpenNotifications() }
                )
                DropdownMenuItem(text = { Text("تنظیمات") }, onClick = { expanded = false; onOpenSettings() })
                DropdownMenuItem(text = { Text("تغییر تم") }, onClick = { expanded = false; onToggleTheme() })
            }
        }
    }
    }
}

/** Shared page context used throughout the academic workspace. */
internal fun studentDestinationDescription(tab: AppTab): String = when (tab) {
    AppTab.DASHBOARD -> "نمای امروز و قدم بعدی"
    AppTab.AGENDA -> "کلاس، امتحان و تکلیف در یک هفته"
    AppTab.LIBRARY -> "جزوه‌ها، نشانک‌ها و ادامهٔ مطالعه"
    AppTab.NOTES -> "نکته‌ها و خلاصه‌های درس، همیشه همراهت"
    AppTab.SCHEDULE -> "زمان و مکان کلاس‌ها"
    AppTab.TASKS -> "از برنامه تا انجام"
    AppTab.EXAMS -> "موعدها و یادآورهای امتحان"
    AppTab.GRADES -> "نمرات واقعی و پیش‌بینی معدل"
    AppTab.ATTENDANCE -> "پیگیری حضور و هشدار غیبت"
    AppTab.COPILOT -> "راهنمای مطالعه با اطلاعات خودت"
    AppTab.ACADEMIC_INTELLIGENCE -> "اولویت‌ها، ریسک‌ها و پیشرفت"
    AppTab.POMODORO -> "زمانی برای تمرکز عمیق"
    AppTab.PASSPORT -> "تصویری از مسیر تحصیلی تو"
    AppTab.SEMESTER_PLANNER -> "انتخاب آگاهانهٔ درس‌های بعدی"
    AppTab.CURRICULUM -> "واحدها و پیش‌نیازهای رشته"
    AppTab.GAMIFICATION -> "قدم‌های کوچک، پیشرفت ماندگار"
    AppTab.HISTORY -> "مرور ترم‌های گذشته"
    AppTab.ACCOUNT -> "پروفایل، ظاهر و تنظیمات شخصی تو"
}

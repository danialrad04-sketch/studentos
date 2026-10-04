package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
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
    Row(
        modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackToDashboard) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت به داشبورد")
        }
        Text(
            currentTab.title,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onOpenSearch) {
            Icon(Icons.Default.Search, "جستجوی سریع", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, "گزینه‌های صفحه", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (onOpenMenu != null) DropdownMenuItem(text = { Text("منوی برنامه") }, onClick = { expanded = false; onOpenMenu() })
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

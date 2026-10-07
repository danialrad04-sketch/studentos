package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.StudentProfileEntity

/** One visible menu, one account entry; flexible brand text on compact/large-font windows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderSection(
    profile: StudentProfileEntity,
    notifCount: Int,
    onOpenNotifications: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onOpenMenu) { Icon(Icons.Rounded.Menu, "باز کردن منوی برنامه") }
        Column(Modifier.weight(1f).padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("سلام ${profile.name.trim().split(" ").firstOrNull().orEmpty().ifBlank { "دانشجو" }}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("یک روز آرام‌تر، یک قدم جلوتر", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
        IconButton(onClick = { expanded = true }) {
            BadgedBox(badge = { if (notifCount > 0) Badge { Text(if (notifCount > 99) "۹۹+" else notifCount.toString()) } }) {
                Icon(Icons.Rounded.MoreVert, "جست‌وجو و اعلان‌ها")
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("جست‌وجوی سریع") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, onClick = { expanded = false; onOpenSearch() })
            DropdownMenuItem(text = { Text(if (notifCount > 0) "اعلان‌ها ($notifCount)" else "اعلان‌ها") }, leadingIcon = { Icon(Icons.Rounded.NotificationsNone, null) }, onClick = { expanded = false; onOpenNotifications() })
        }
        }
        FilledTonalIconButton(onClick = onOpenAccount, shape = CircleShape, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer), modifier = Modifier.semantics { contentDescription = "حساب کاربری" }) {
            Text(profile.name.trim().firstOrNull()?.toString() ?: "د", style = MaterialTheme.typography.titleMedium)
        }
    }
}

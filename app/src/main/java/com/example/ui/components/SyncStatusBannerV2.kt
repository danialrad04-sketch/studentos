package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.SyncState
import com.example.domain.model.SyncStatusSnapshot

@Composable
fun SyncStatusBannerV2(
    status: SyncStatusSnapshot,
    isGuest: Boolean,
    online: Boolean,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isGuest) return

    val context = LocalContext.current
    val online = rememberValidatedNetworkV2(context)
    val (icon, title) = when {
        !online -> Icons.Outlined.CloudOff to "آفلاین؛ داده‌های محلی در دسترس است"
        status.state == SyncState.SYNCING -> Icons.Outlined.CloudSync to "در حال همگام‌سازی"
        status.state == SyncState.NEEDS_ATTENTION -> Icons.Outlined.ErrorOutline to "همگام‌سازی نیاز به بررسی دارد"
        status.state == SyncState.SYNCED -> Icons.Outlined.CloudDone to "همگام‌سازی انجام شد"
        else -> Icons.Outlined.CloudSync to "آماده همگام‌سازی"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
        color = when {
            !online -> MaterialTheme.colorScheme.secondaryContainer
            status.state == SyncState.NEEDS_ATTENTION -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        }
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, contentDescription = null)
            StudentCardMeta(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (online && status.state != SyncState.SYNCING) {
                TextButton(onClick = onSyncNow) {
                    Text("همگام‌سازی")
                }
            }
        }
    }

@Composable
private fun rememberValidatedNetworkV2(context: android.content.Context): Boolean {
    fun current(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    val state = remember { mutableStateOf(current()) }

    DisposableEffect(context) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { state.value = current() }
            override fun onLost(network: Network) { state.value = current() }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                state.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose { manager.unregisterNetworkCallback(callback) }
    }

    return state.value
}

}

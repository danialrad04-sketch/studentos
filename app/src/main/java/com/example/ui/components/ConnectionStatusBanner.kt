package com.example.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.example.ui.theme.StudentSpacing
import com.example.ui.theme.StudentShapeTokens

private fun Context.hasValidatedNetwork(): Boolean {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

@Composable
fun OfflineStatusBanner(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val online = rememberConnectivityState(context)

    if (!online) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = StudentShapeTokens.Compact,
            color = MaterialTheme.colorScheme.secondaryContainer,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = StudentSpacing.Md, vertical = StudentSpacing.Sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(StudentSpacing.Sm)
            ) {
                Icon(
                    Icons.Outlined.CloudOff,
                    contentDescription = "بدون اتصال اینترنت",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "آفلاین هستید؛ داده‌های ذخیره‌شده روی دستگاه همچنان در دسترس است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun rememberConnectivityState(context: Context): Boolean {
    val state = androidx.compose.runtime.remember { mutableStateOf(context.hasValidatedNetwork()) }

    DisposableEffect(context) {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                state.value = context.hasValidatedNetwork()
            }
            override fun onLost(network: Network) {
                state.value = context.hasValidatedNetwork()
            }
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                state.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose {
            manager.unregisterNetworkCallback(callback)
        }
    }

    return state.value
}

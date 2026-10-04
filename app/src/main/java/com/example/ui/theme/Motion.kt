package com.example.ui.theme

import android.provider.Settings
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Android system animation preference used by Student OS motion primitives.
 *
 * When the user disables system animations, Student OS avoids decorative
 * spring/bounce motion and uses the final visual state immediately.
 */
fun shouldReduceMotion(animatorDurationScale: Float): Boolean =
    animatorDurationScale <= 0f

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    fun readScale(): Float = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        }.getOrDefault(1f)
    var scale by remember(context) { mutableFloatStateOf(readScale()) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { scale = readScale() }
        }
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return shouldReduceMotion(scale)
}

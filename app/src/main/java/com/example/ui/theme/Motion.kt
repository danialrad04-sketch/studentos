package com.example.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    return remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        }.getOrDefault(1f)
    }.let(::shouldReduceMotion)
}

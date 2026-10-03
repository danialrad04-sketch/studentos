package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import java.util.Calendar
import kotlinx.coroutines.delay

/** Refresh time-sensitive surfaces even while the screen remains open. */
@Composable
internal fun rememberAcademicClock(): Calendar {
    val time by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000L)
        }
    }
    return Calendar.getInstance().apply { timeInMillis = time }
}

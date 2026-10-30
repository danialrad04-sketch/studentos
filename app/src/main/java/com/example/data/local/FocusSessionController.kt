package com.example.data.local

import android.content.Context
import com.example.data.repository.AppPreferencesRepository
import com.example.ui.util.NotificationHelper

/** App and widget use the same persisted deadline and completion lock. */
object FocusSessionController {
    fun toggle(context: Context, expectedOwner: String? = null) {
        val preferences = AppPreferencesRepository.getInstance(context)
        val next =
            preferences.toggleFocusSession(System.currentTimeMillis(), expectedOwner) ?: return
        if (next.isRunning) AcademicReminderWorker.scheduleFocus(context, next.endsAtMillis)
        else AcademicReminderWorker.cancelFocus(context)
    }

    fun complete(context: Context): Boolean {
        val preferences = AppPreferencesRepository.getInstance(context)
        val completed = preferences.completeFocusSession(System.currentTimeMillis())
        if (completed && preferences.notificationsEnabled.value)
            NotificationHelper.showSystemNotification(
                context,
                "جلسه تمرکز تمام شد",
                "چند دقیقه استراحت کنید و سپس سراغ کار بعدی بروید.",
                notificationId = 41001,
            )
        return completed
    }
}

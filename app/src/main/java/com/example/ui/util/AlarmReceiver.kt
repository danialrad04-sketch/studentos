package com.example.ui.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver triggered by AlarmManager to post exact exam and task reminder notifications.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "یادآور تحصیلی Student OS 🎓"
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "زمان بررسی برنامه‌های تحصیلی فرارسیده است."
        val isDanger = intent.getBooleanExtra("EXTRA_IS_DANGER", false)
        val id = intent.getIntExtra("EXTRA_ID", System.currentTimeMillis().toInt())

        NotificationHelper.showSystemNotification(
            context = context,
            title = title,
            message = message,
            isDanger = isDanger,
            notificationId = id
        )
    }
}

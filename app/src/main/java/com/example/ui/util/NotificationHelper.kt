package com.example.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_ID = "student_os_alerts_channel"
    private const val CHANNEL_NAME = "اعلان‌ها و هشدارهای تحصیلی Student OS"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "هشدارهای غیبت بیش‌ازحد، یادآور تکالیف و امتحانات ترم"
                enableLights(true)
                enableVibration(false)
                vibrationPattern = null
                setSound(soundUri, audioAttributes)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showSystemNotification(
        context: Context,
        title: String,
        message: String,
        isDanger: Boolean = false,
        notificationId: Int = System.currentTimeMillis().toInt()
    ): Boolean {
        initNotificationChannel(context)

        // Check permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not yet granted
                triggerHaptic(context, isDanger)
                playToneSafely(isDanger)
                return false
            }
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(if (isDanger) android.R.drawable.stat_notify_error else android.R.drawable.stat_notify_more)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(if (isDanger) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            triggerHaptic(context, isDanger)
            playToneSafely(isDanger)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            triggerHaptic(context, isDanger)
            playToneSafely(isDanger)
            return false
        }
    }

    fun triggerHaptic(context: Context, isStrong: Boolean) {
        // Disabled per user request: heavy vibrations are removed to keep experience quiet and comfortable
    }

    fun playToneSafely(isDanger: Boolean) {
        var toneGen: ToneGenerator? = null
        try {
            val toneType = if (isDanger) ToneGenerator.TONE_CDMA_HIGH_L else ToneGenerator.TONE_PROP_BEEP
            toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(toneType, if (isDanger) 350 else 180)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    toneGen?.release()
                } catch (_: Throwable) {}
            }, 500)
        } catch (_: Throwable) {
            try {
                toneGen?.release()
            } catch (_: Throwable) {}
        }
    }
}

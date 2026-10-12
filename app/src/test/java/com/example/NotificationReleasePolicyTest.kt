package com.example

import android.app.Application
import com.example.data.repository.AppPreferencesRepository
import com.example.ui.util.NotificationHelper
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class NotificationReleasePolicyTest {
    @Test fun inAppNotificationDisablePreventsSystemDelivery() {
        val context = RuntimeEnvironment.getApplication()
        val preferences = AppPreferencesRepository.getInstance(context)
        preferences.setNotificationsEnabled(false)
        try {
            assertFalse(NotificationHelper.showSystemNotification(context, "امتحان", "یادآور", notificationId = 42))
        } finally {
            preferences.setNotificationsEnabled(true)
        }
    }

    @Test fun refusingAndroidPermissionDoesNotPreventLocalUseOrForceDelivery() {
        val context = RuntimeEnvironment.getApplication()
        AppPreferencesRepository.getInstance(context).setNotificationsEnabled(true)
        org.robolectric.Shadows.shadowOf(context).denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(NotificationHelper.showSystemNotification(context, "امتحان", "یادآور", notificationId = 43))
    }
}

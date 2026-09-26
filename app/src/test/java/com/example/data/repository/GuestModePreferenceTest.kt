package com.example.data.repository

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuestModePreferenceTest {

    @Test
    fun guest_mode_persists_across_repository_instances() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = context.getSharedPreferences("student_os_preferences", 0)
        prefs.edit().clear().commit()

        val first = AppPreferencesRepository(context)
        assertFalse(first.guestModeEnabled.value)

        first.setGuestModeEnabled(true)
        assertTrue(first.guestModeEnabled.value)

        val second = AppPreferencesRepository(context)
        assertTrue(second.guestModeEnabled.value)

        second.setGuestModeEnabled(false)
        assertFalse(second.guestModeEnabled.value)
    }
}

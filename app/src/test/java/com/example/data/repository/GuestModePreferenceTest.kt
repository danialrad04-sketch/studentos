package com.example.data.repository

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GuestModePreferenceTest {

    @Test
    fun fresh_install_has_guest_access_without_network_authentication() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = AppPreferencesRepository(context)
        assertTrue(repository.guestModeEnabled.value)
    }

    @Test
    fun guest_mode_persists_across_repository_instances() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val first = AppPreferencesRepository(context)
        first.setGuestModeEnabled(false)
        assertFalse(first.guestModeEnabled.value)

        first.setGuestModeEnabled(true)
        assertTrue(first.guestModeEnabled.value)

        val second = AppPreferencesRepository(context)
        assertTrue(second.guestModeEnabled.value)

        second.setGuestModeEnabled(false)
        assertFalse(second.guestModeEnabled.value)
    }
}

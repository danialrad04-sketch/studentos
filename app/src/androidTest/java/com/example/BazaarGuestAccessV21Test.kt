package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.AppPreferencesRepository
import org.junit.Assert.assertTrue
import org.junit.Test

class BazaarGuestAccessV21Test {

    @Test
    fun freshInstallCanEnterWithoutAuthentication() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        clearPreferences(context)

        val repository = AppPreferencesRepository(context)

        assertTrue(repository.guestModeEnabled.value)
    }

    private fun clearPreferences(context: Context) {
        context.getSharedPreferences("student_os_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        context.getSharedPreferences("student_os_preferences_secure", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}

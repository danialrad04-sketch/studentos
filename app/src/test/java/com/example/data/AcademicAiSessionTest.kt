package com.example.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.AcademicAiSession
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AcademicAiSessionTest {
    @Test fun assistantUsesAnIsolatedIdentityInTheExistingProjectAndReusesIt() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val primary = FirebaseApp.getApps(context).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
            ?: FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
                .setApplicationId("1:123456789:android:abcdef")
                .setApiKey("AIza" + "x".repeat(35)).setProjectId("demo-studentos-assistant").build())
        val primaryAuth = FirebaseAuth.getInstance(primary)
        val originalUser = primaryAuth.currentUser
        val assistant = AcademicAiSession.app()
        try {
            assertNotSame(primary, assistant)
            assertEquals(primary.options, assistant.options)
            assertSame(assistant, AcademicAiSession.app())
            assertNotSame(primaryAuth, FirebaseAuth.getInstance(assistant))
            assertNotSame(FirebaseFunctions.getInstance(primary), FirebaseFunctions.getInstance(assistant))
            assertSame(primary, FirebaseApp.getInstance())
            assertSame(originalUser, primaryAuth.currentUser)
            assertNull(FirebaseAuth.getInstance(assistant).currentUser)
        } finally { assistant.delete() }
    }
}

package com.example

import android.app.Application
import android.util.Log
import com.example.di.AppContainer
import com.example.ui.util.NotificationHelper
import com.example.util.CrashLogger
import com.example.data.api.GeminiApiClient
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class StudentApplication : Application() {

    var container: AppContainer? = null
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Global Uncaught Exception Handler
        val previousDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                CrashLogger.recordException(throwable)
            } catch (_: Throwable) {
            }
            previousDefaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            NotificationHelper.initNotificationChannel(this)
        } catch (e: Throwable) {
            Log.e("StudentApplication", "Failed to initialize notification channel", e)
            CrashLogger.recordException(e)
        }

        // Initialize Firebase & Firebase App Check (Play Integrity for Release, Debug for Debug)
        try {
            FirebaseApp.initializeApp(this)
            val appCheck = FirebaseAppCheck.getInstance()
            if (BuildConfig.DEBUG) {
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d("StudentApplication", "Firebase App Check initialized with Debug provider")
            } else {
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                Log.d("StudentApplication", "Firebase App Check initialized with Play Integrity provider")
            }
        } catch (e: Throwable) {
            Log.w("StudentApplication", "Firebase App Check initialization skipped/failed: ${e.message}")
        }

        // Initialize Remote Config for dynamic model configuration
        try {
            GeminiApiClient.initRemoteConfig()
        } catch (e: Throwable) {
            Log.w("StudentApplication", "GeminiApiClient remote config initialization failed: ${e.message}")
        }
    }
}



package com.example

import android.app.Application
import android.util.Log
import com.example.di.AppContainer
import com.example.ui.util.NotificationHelper
import com.example.util.CrashLogger
import com.example.data.cloud.BackendSyncScheduler
import com.example.data.local.AppDatabase
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
        BackendSyncScheduler.install(this, AppDatabase.getDatabase(this))

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

        // Initialize Firebase & Firebase App Check safely for production and debug
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            try {
                val appCheck = FirebaseAppCheck.getInstance()
                if (BuildConfig.DEBUG) {
                    appCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                    Log.d("StudentApplication", "Firebase App Check initialized with Debug provider")
                } else {
                    try {
                        appCheck.installAppCheckProviderFactory(
                            PlayIntegrityAppCheckProviderFactory.getInstance()
                        )
                        Log.d("StudentApplication", "Firebase App Check initialized with Play Integrity provider")
                    } catch (e: Throwable) {
                        Log.w("StudentApplication", "PlayIntegrity provider registration skipped: ${e.message}")
                    }
                }
            } catch (e: Throwable) {
                Log.w("StudentApplication", "Firebase App Check initialization skipped: ${e.message}")
            }
        } catch (e: Throwable) {
            Log.w("StudentApplication", "Firebase initialization skipped/failed: ${e.message}")
        }


    }
}



package com.example.data.api

import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/** Transport-only identity: it never signs into, replaces, or signs out the student's account. */
internal object AcademicAiSession {
    private const val APP_NAME = "studentos-academic-assistant"

    @Synchronized
    fun app(): FirebaseApp {
        val primary = FirebaseApp.getInstance()
        FirebaseApp.getApps(primary.applicationContext).firstOrNull { it.name == APP_NAME }?.let { return it }
        return FirebaseApp.initializeApp(primary.applicationContext, primary.options, APP_NAME).also { assistant ->
            val check = FirebaseAppCheck.getInstance(assistant)
            if (BuildConfig.DEBUG) {
                check.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            } else {
                check.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
            }
        }
    }
}

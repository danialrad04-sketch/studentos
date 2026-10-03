package com.example.data.api.backend

import com.example.BuildConfig

/**
 * Single source of truth for the self-hosted Node.js backend base URL.
 * Configure STUDENTOS_API_BASE_URL in the local .env file.
 */
object BackendConfig {
    private const val FALLBACK_URL = "https://localhost.invalid/"

    val isConfigured: Boolean
        get() {
            val configured = BuildConfig.STUDENTOS_API_BASE_URL.trim()
            return configured.isNotBlank() &&
                !configured.equals("https://api.example.com/", ignoreCase = true) &&
                !configured.equals("https://api.example.com", ignoreCase = true)
        }

    val BASE_URL: String
        get() {
            val configured = BuildConfig.STUDENTOS_API_BASE_URL.trim()
            val value = if (isConfigured) configured else FALLBACK_URL
            return if (value.endsWith("/")) value else value + "/"
        }
}

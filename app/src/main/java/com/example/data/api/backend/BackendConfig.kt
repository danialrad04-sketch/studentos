package com.example.data.api.backend

import com.example.BuildConfig

/**
 * Single source of truth for the self-hosted Node.js backend base URL.
 * Configure STUDENTOS_API_BASE_URL in the local .env file.
 */
object BackendConfig {
    private const val FALLBACK_URL = "https://api.example.com/"

    val BASE_URL: String
        get() {
            val configured = BuildConfig.STUDENTOS_API_BASE_URL.trim()
            return if (configured.isBlank()) FALLBACK_URL
            else if (configured.endsWith("/")) configured else "$configured/"
        }

    val isConfigured: Boolean
        get() {
            val configured = BuildConfig.STUDENTOS_API_BASE_URL.trim()
            return configured.isNotBlank() && !configured.contains("api.example.com", ignoreCase = true)
        }
}

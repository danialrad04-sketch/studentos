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
                .ifBlank { FALLBACK_URL }
            return if (configured.endsWith("/")) configured else configured + "/"
        }
}

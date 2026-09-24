package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Hardware-backed encrypted storage for sensitive values: the custom backend's
 * access/refresh tokens, and any user-supplied AI API key. Backed by
 * Jetpack Security's EncryptedSharedPreferences (AES-256-GCM for values,
 * AES-256-SIV for keys), so the file is unreadable even on a rooted device
 * without the Android Keystore-protected master key.
 *
 * Non-sensitive app settings (theme, notification toggles, onboarding flags)
 * should keep using the existing plain AppPreferencesRepository — encrypting
 * everything indiscriminately adds overhead for no security benefit on data
 * that isn't sensitive.
 */
class SecureTokenStore(context: Context) {

    private val prefs: SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // If the Keystore is unavailable/corrupted (rare, e.g. after a
        // factory-reset-protection edge case on some OEMs), fail closed:
        // return null and force the caller to treat the user as logged out
        // rather than silently falling back to plaintext storage.
        Log.e(TAG, "Failed to initialize encrypted storage", e)
        null
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs?.edit()
            ?.putString(KEY_ACCESS_TOKEN, accessToken)
            ?.putString(KEY_REFRESH_TOKEN, refreshToken)
            ?.apply()
    }

    fun getAccessToken(): String? = prefs?.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs?.getString(KEY_REFRESH_TOKEN, null)

    fun updateAccessToken(accessToken: String) {
        prefs?.edit()?.putString(KEY_ACCESS_TOKEN, accessToken)?.apply()
    }

    fun saveCustomAiApiKey(apiKey: String) {
        prefs?.edit()?.putString(KEY_AI_API_KEY, apiKey)?.apply()
    }

    fun getCustomAiApiKey(): String? = prefs?.getString(KEY_AI_API_KEY, null)

    fun clearAll() {
        prefs?.edit()?.clear()?.apply()
    }

    fun saveUserInfo(userId: String, email: String, displayName: String) {
        prefs?.edit()
            ?.putString(KEY_USER_ID, userId)
            ?.putString(KEY_USER_EMAIL, email)
            ?.putString(KEY_USER_DISPLAY_NAME, displayName)
            ?.apply()
    }

    fun getUserId(): String? = prefs?.getString(KEY_USER_ID, null)
    fun getUserEmail(): String? = prefs?.getString(KEY_USER_EMAIL, null)
    fun getUserDisplayName(): String? = prefs?.getString(KEY_USER_DISPLAY_NAME, null)

    fun isAvailable(): Boolean = prefs != null

    companion object {
        private const val TAG = "SecureTokenStore"
        private const val FILE_NAME = "studentos_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_AI_API_KEY = "custom_ai_api_key"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_DISPLAY_NAME = "user_display_name"
    }
}

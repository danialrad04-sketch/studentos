package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.ui.models.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AppPreferencesRepository manages persistent key-value configuration,
 * launch flags, biometric security, and last-save timestamps for crash recovery.
 */
class AppPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME + "_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        android.util.Log.e("AppPreferencesRepository", "Failed to initialize EncryptedSharedPreferences, falling back to standard prefs", e)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _themeMode = MutableStateFlow(readStoredThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(readStoredNotificationsEnabled())
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _isFirstLaunchCompleted = MutableStateFlow(readFirstLaunchCompleted())
    val isFirstLaunchCompleted: StateFlow<Boolean> = _isFirstLaunchCompleted.asStateFlow()

    private val _isOnboardingCompleted = MutableStateFlow(readOnboardingCompleted())
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _lastSuccessfulSaveTimestamp = MutableStateFlow(readLastSuccessfulSave())
    val lastSuccessfulSaveTimestamp: StateFlow<Long> = _lastSuccessfulSaveTimestamp.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(readBiometricEnabled())
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _isAutoBackupEnabled = MutableStateFlow(readAutoBackupEnabled())
    val isAutoBackupEnabled: StateFlow<Boolean> = _isAutoBackupEnabled.asStateFlow()

    private val _dataLossWarningDismissed = MutableStateFlow(readDataLossWarningDismissed())
    val dataLossWarningDismissed: StateFlow<Boolean> = _dataLossWarningDismissed.asStateFlow()

    private val _customGeminiApiKey = MutableStateFlow(readStoredCustomGeminiApiKey())
    val customGeminiApiKey: StateFlow<String> = _customGeminiApiKey.asStateFlow()

    private fun readStoredThemeMode(): ThemeMode {
        val stored = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(stored)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    private fun readStoredNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    private fun readFirstLaunchCompleted(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH_COMPLETED, false)
    }

    private fun readOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    private fun readLastSuccessfulSave(): Long {
        return prefs.getLong(KEY_LAST_SUCCESSFUL_SAVE, 0L)
    }

    private fun readBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    private fun readAutoBackupEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, true)
    }

    private fun readDataLossWarningDismissed(): Boolean {
        return prefs.getBoolean(KEY_DATA_LOSS_WARNING_DISMISSED, false)
    }

    private fun readStoredCustomGeminiApiKey(): String {
        return prefs.getString(KEY_CUSTOM_GEMINI_API_KEY, "") ?: ""
    }

    fun setCustomGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM_GEMINI_API_KEY, key.trim()).apply()
        _customGeminiApiKey.value = key.trim()
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setFirstLaunchCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH_COMPLETED, completed).commit()
        _isFirstLaunchCompleted.value = completed
    }

    fun setOnboardingCompleted(completed: Boolean) {
        // Use commit() to guarantee immediate synchronous disk write (essential for Samsung One UI process lifecycles)
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).commit()
        _isOnboardingCompleted.value = completed
        setFirstLaunchCompleted(true)
    }

    fun isIdentityMigratedToRoom(): Boolean {
        return prefs.getBoolean(KEY_IDENTITY_MIGRATED_TO_ROOM, false)
    }

    fun markIdentityMigratedToRoom() {
        prefs.edit().putBoolean(KEY_IDENTITY_MIGRATED_TO_ROOM, true).commit()
    }

    /**
     * Reads legacy identity fields from SharedPreferences for one-time Room migration,
     * wipes the legacy keys from SharedPreferences, and sets KEY_IDENTITY_MIGRATED_TO_ROOM = true.
     * Guaranteed to execute at most once.
     */
    fun readAndClearLegacyIdentity(): Map<String, Any?>? {
        if (isIdentityMigratedToRoom()) return null

        val legacyKeys = listOf(
            KEY_LEGACY_STUDENT_NAME,
            KEY_LEGACY_STUDENT_ID,
            KEY_LEGACY_UNIVERSITY,
            KEY_LEGACY_MAJOR,
            KEY_LEGACY_ENTRY_YEAR,
            KEY_LEGACY_CURRENT_SEMESTER,
            KEY_LEGACY_PASSED_CREDITS,
            KEY_LEGACY_DECLARED_GPA
        )
        val hasLegacy = legacyKeys.any { prefs.contains(it) }
        if (!hasLegacy) {
            markIdentityMigratedToRoom()
            return null
        }

        val legacyData = mapOf(
            "name" to prefs.getString(KEY_LEGACY_STUDENT_NAME, null),
            "studentId" to prefs.getString(KEY_LEGACY_STUDENT_ID, null),
            "university" to prefs.getString(KEY_LEGACY_UNIVERSITY, null),
            "major" to prefs.getString(KEY_LEGACY_MAJOR, null),
            "entryYear" to if (prefs.contains(KEY_LEGACY_ENTRY_YEAR)) prefs.getInt(KEY_LEGACY_ENTRY_YEAR, 0) else null,
            "currentSemester" to if (prefs.contains(KEY_LEGACY_CURRENT_SEMESTER)) prefs.getInt(KEY_LEGACY_CURRENT_SEMESTER, 0) else null,
            "passedCredits" to if (prefs.contains(KEY_LEGACY_PASSED_CREDITS)) prefs.getInt(KEY_LEGACY_PASSED_CREDITS, 0) else null,
            "declaredGpa" to prefs.getString(KEY_LEGACY_DECLARED_GPA, null)?.toDoubleOrNull()
        )

        val editor = prefs.edit()
        legacyKeys.forEach { editor.remove(it) }
        editor.putBoolean(KEY_IDENTITY_MIGRATED_TO_ROOM, true)
        editor.commit()

        return legacyData
    }

    fun recordSuccessfulSave() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_SUCCESSFUL_SAVE, now).commit()
        _lastSuccessfulSaveTimestamp.value = now
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).commit()
        _isBiometricEnabled.value = enabled
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).commit()
        _isAutoBackupEnabled.value = enabled
    }

    fun setDataLossWarningDismissed(dismissed: Boolean) {
        prefs.edit().putBoolean(KEY_DATA_LOSS_WARNING_DISMISSED, dismissed).commit()
        _dataLossWarningDismissed.value = dismissed
    }

    companion object {
        private const val PREFS_NAME = "student_os_preferences"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_NOTIFICATIONS_ENABLED = "key_notifications_enabled"
        private const val KEY_FIRST_LAUNCH_COMPLETED = "key_first_launch_completed"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_LAST_SUCCESSFUL_SAVE = "key_last_successful_save"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"
        private const val KEY_AUTO_BACKUP_ENABLED = "key_auto_backup_enabled"
        private const val KEY_DATA_LOSS_WARNING_DISMISSED = "key_data_loss_warning_dismissed"
        private const val KEY_CUSTOM_GEMINI_API_KEY = "key_custom_gemini_api_key"

        private const val KEY_IDENTITY_MIGRATED_TO_ROOM = "key_identity_migrated_to_room_v1"
        private const val KEY_LEGACY_STUDENT_NAME = "key_student_name"
        private const val KEY_LEGACY_STUDENT_ID = "key_student_id"
        private const val KEY_LEGACY_UNIVERSITY = "key_university"
        private const val KEY_LEGACY_MAJOR = "key_major"
        private const val KEY_LEGACY_ENTRY_YEAR = "key_entry_year"
        private const val KEY_LEGACY_CURRENT_SEMESTER = "key_current_semester"
        private const val KEY_LEGACY_PASSED_CREDITS = "key_passed_credits"
        private const val KEY_LEGACY_DECLARED_GPA = "key_declared_gpa"

        @Volatile
        private var INSTANCE: AppPreferencesRepository? = null

        fun getInstance(context: Context): AppPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppPreferencesRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

package com.example.ui.components

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Google identity picker for Firebase Authentication.
 *
 * Google issues the ID token here; StudentAuthManager is responsible for
 * exchanging that token for a Firebase credential.
 */
object GoogleSignInManager {

    suspend fun getIdToken(activity: Activity): Result<String> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(activity.getString(R.string.default_web_client_id))
                .setFilterByAuthorizedAccounts(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val credentialManager = CredentialManager.create(activity)
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                Result.success(
                    GoogleIdTokenCredential.createFrom(credential.data).idToken
                )
            } else {
                Result.failure(
                    IllegalStateException("Google ID token credential was not returned.")
                )
            }
        } catch (e: Exception) {
            val raw = e.localizedMessage.orEmpty()
            val text = raw.lowercase()
            val userMessage = when {
                text.contains("12500") || text.contains("developer_error") || text.contains("10:") ->
                    "ورود گوگل برای نسخه Release تنظیم نشده است؛ SHA-1 نسخه Release و OAuth Client را در Firebase بررسی کنید."
                text.contains("403") || text.contains("forbidden") || text.contains("<!doctype html") || text.contains("<html") ->
                    "گوگل درخواست ورود را رد کرد؛ SHA-1 نسخه Release یا تنظیمات OAuth/Firebase نیاز به اصلاح دارد."
                text.contains("cancel") || text.contains("canceled") -> "ورود گوگل لغو شد."
                text.contains("network") -> "اتصال اینترنت را بررسی کنید و دوباره تلاش کنید."
                else -> "ورود با گوگل انجام نشد. تنظیمات حساب گوگل و Firebase را بررسی کنید."
            }
            Result.failure(IllegalStateException(userMessage, e))
        }
    }
}

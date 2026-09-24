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
            Result.failure(e)
        }
    }
}

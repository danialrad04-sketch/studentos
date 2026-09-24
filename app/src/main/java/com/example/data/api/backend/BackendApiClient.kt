package com.example.data.api.backend

import android.content.Context
import android.util.Log
import com.example.data.repository.SecureTokenStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds a Retrofit client for the self-hosted backend with:
 *  - the current access token attached to every request,
 *  - automatic silent refresh-and-retry on a 401 (expired access token),
 *  - the app-wide SSL pin declared in network_security_config.xml applying
 *    automatically since this uses the platform's default TLS stack.
 *
 * Only ONE refresh happens even if several requests 401 at the same instant
 * (e.g. a screen firing 3 parallel sync calls right as the token expires) —
 * the `synchronized` block below makes every caller after the first one
 * simply reuse the token the first caller just fetched, instead of each
 * independently racing to refresh (which would revoke each other's refresh
 * token under the rotation scheme the backend uses).
 */
class BackendApiClient(context: Context) {

    private val tokenStore = SecureTokenStore(context.applicationContext)

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = tokenStore.getAccessToken()
        val request = if (token != null) {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            original
        }
        chain.proceed(request)
    }

    private val refreshAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            // Don't try to refresh more than once for the same request chain
            // (avoids an infinite loop if the refresh itself is rejected).
            if (responseCount(response) >= 2) return null

            val refreshToken = tokenStore.getRefreshToken() ?: return null

            synchronized(this) {
                // Another thread may have already refreshed while we were
                // waiting on the lock — if the stored access token changed
                // since this response was generated, just reuse it.
                val currentAccessToken = tokenStore.getAccessToken()
                val requestToken = response.request.header("Authorization")
                    ?.removePrefix("Bearer ")
                if (currentAccessToken != null && currentAccessToken != requestToken) {
                    return response.request.newBuilder()
                        .header("Authorization", "Bearer $currentAccessToken")
                        .build()
                }

                val newTokens = runBlocking {
                    try {
                        val plainClient = OkHttpClient.Builder().build()
                        val retrofit = Retrofit.Builder()
                            .baseUrl(BackendConfig.BASE_URL)
                            .client(plainClient)
                            .addConverterFactory(MoshiConverterFactory.create(moshi))
                            .build()
                        val api = retrofit.create(BackendApi::class.java)
                        val resp = api.refresh(RefreshRequest(refreshToken))
                        if (resp.isSuccessful) resp.body() else null
                    } catch (e: Exception) {
                        Log.w(TAG, "Token refresh failed", e)
                        null
                    }
                }

                if (newTokens == null) {
                    // Refresh token is dead too — the user needs to log in
                    // again. Clear stored tokens so the app's auth-state
                    // observer can route them back to the login screen.
                    tokenStore.clearAll()
                    return null
                }

                tokenStore.saveTokens(newTokens.accessToken, newTokens.refreshToken)
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${newTokens.accessToken}")
                    .build()
            }
        }

        private fun responseCount(response: Response): Int {
            var result = 1
            var prior = response.priorResponse
            while (prior != null) {
                result++
                prior = prior.priorResponse
            }
            return result
        }
    }

    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .authenticator(refreshAuthenticator)
        .build()

    val api: BackendApi = Retrofit.Builder()
        .baseUrl(BackendConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(BackendApi::class.java)

    fun tokenStore(): SecureTokenStore = tokenStore

    companion object {
        private const val TAG = "BackendApiClient"

        @Volatile
        private var INSTANCE: BackendApiClient? = null

        fun getInstance(context: Context): BackendApiClient =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BackendApiClient(context.applicationContext).also { INSTANCE = it }
            }
    }
}

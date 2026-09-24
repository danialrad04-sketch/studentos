package com.example.data.api.backend

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.POST

data class SignUpRequest(
    val email: String,
    val password: String,
    val displayName: String = "",
    val deviceLabel: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String,
    val deviceLabel: String? = null
)

data class RefreshRequest(val refreshToken: String)
data class LogoutRequest(val refreshToken: String)

data class BackendUser(val id: String, val email: String, val displayName: String)

data class AuthResponse(
    val user: BackendUser,
    val accessToken: String,
    val refreshToken: String
)

data class TokenPairResponse(val accessToken: String, val refreshToken: String)

data class SyncPushRequest(val payload: Any, val updatedAt: Long)
data class SyncPullResponse(val payload: Any?, val updatedAt: Long)
data class SyncPushResult(val ok: Boolean, val updatedAt: Long)

/**
 * Retrofit interface for the self-hosted Student OS backend.
 * Base URL comes from BackendConfig.BASE_URL (see that file for how to set
 * your own VPS domain after running setup.sh).
 */
interface BackendApi {

    @POST("api/auth/signup")
    suspend fun signUp(@Body body: SignUpRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): Response<TokenPairResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Body body: LogoutRequest): Response<Unit>

    @POST("api/auth/logout-all")
    suspend fun logoutAllDevices(): Response<Unit>

    @GET("api/sync/{dataType}")
    suspend fun pullDataType(@Path("dataType") dataType: String): Response<SyncPullResponse>

    @PUT("api/sync/{dataType}")
    suspend fun pushDataType(
        @Path("dataType") dataType: String,
        @Body body: SyncPushRequest
    ): Response<SyncPushResult>
}

package com.example.data.api

import com.example.data.auth.awaitResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.UUID
import java.util.concurrent.TimeUnit

data class AcademicAiStatus(val enabled: Boolean, val visionEnabled: Boolean, val model: String?,
    val used: Int, val limit: Int, val scansUsed: Int, val scanLimit: Int) {
    val remaining: Int get() = (limit - used).coerceAtLeast(0)
}

/** Free assistant gateway. A private provider key never enters the Android APK. */
object AcademicAiApiClient {
    private val sessionLock = Mutex()
    private suspend fun call(name: String, data: Map<String, Any> = emptyMap()): Map<*, *> = withTimeout(60_000) {
        val auth = FirebaseAuth.getInstance()
        sessionLock.withLock {
            // Reuse any existing account. Never replace a signed-in student with a guest.
            if (auth.currentUser == null) {
                val context = com.google.firebase.FirebaseApp.getInstance().applicationContext
                val backend = com.example.data.api.backend.BackendApiClient.getInstance(context).tokenStore()
                // A backend-only account must never be replaced by the Firebase guest listener.
                check(backend.getAccessToken() == null || backend.getUserId() == null) {
                    "حساب فعلی از بک‌اند وارد شده است؛ اتصال مدل به این نوع حساب هنوز آماده نیست. تحلیل روی دستگاه در دسترس است."
                }
                withTimeout(12_000) { auth.signInAnonymously().awaitResult() }
            }
        }
        check(auth.currentUser != null) { "نشست امن دستیار برقرار نشد؛ دوباره تلاش کن." }
        FirebaseFunctions.getInstance(com.example.BuildConfig.PREMIUM_FUNCTIONS_REGION).getHttpsCallable(name)
            .withTimeout(50, TimeUnit.SECONDS).call(data).awaitResult().data as? Map<*, *>
            ?: error("پاسخ سرویس معتبر نیست؛ دوباره تلاش کن.")
    }
    /** Opening the local assistant does not create an account or require a network connection. */
    suspend fun status(): AcademicAiStatus? {
        if (FirebaseAuth.getInstance().currentUser == null) return null
        val data = call("getAcademicAiStatus")
        return AcademicAiStatus(data["enabled"] == true, data["visionEnabled"] == true, data["model"] as? String,
            (data["dailyAiQuotaUsed"] as? Number)?.toInt() ?: 0, (data["maxDailyAiQuota"] as? Number)?.toInt() ?: 0,
            (data["dailyScanQuotaUsed"] as? Number)?.toInt() ?: 0, (data["maxDailyScanQuota"] as? Number)?.toInt() ?: 0)
    }
    suspend fun advice(prompt: String, context: String, history: List<Pair<String, String>>, kind: String = "chat"): Result<String> = result {
        require(prompt.trim().length in 1..4000) { "پرسش باید بین ۱ تا ۴۰۰۰ نویسه باشد." }
        require(context.length <= 16000) { "اطلاعات این درخواست بیش از اندازه است." }
        val data = call("generateAcademicAdvice", mapOf("prompt" to prompt.trim(), "studentContext" to context, "kind" to kind,
            "requestId" to UUID.randomUUID().toString(), "history" to history.takeLast(8).map { mapOf("role" to it.first, "text" to it.second.take(3000)) }))
        (data["text"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: error("پاسخی از مدل دریافت نشد.")
    }
    suspend fun schedule(bytes: ByteArray, mimeType: String): String {
        require(bytes.isNotEmpty() && bytes.size <= 2_000_000) { "تصویر باید غیرخالی و کوچک‌تر از ۲ مگابایت باشد." }
        val data = call("generateAcademicAdvice", mapOf("kind" to "schedule", "prompt" to "Read the supplied timetable", "requestId" to UUID.randomUUID().toString(),
            "imageBase64" to android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP), "mimeType" to mimeType))
        return data["text"] as? String ?: error("جلسهٔ قابل خواندنی دریافت نشد.")
    }
    fun userMessage(error: Throwable): String = when {
        error is TimeoutCancellationException -> "پاسخ سرویس طول کشید؛ دوباره تلاش کن یا تحلیل روی دستگاه را انتخاب کن."
        error is FirebaseFunctionsException -> when (error.code) {
            FirebaseFunctionsException.Code.NOT_FOUND -> "سرویس پاسخ‌گویی آنلاین هنوز فعال نشده است؛ تحلیل روی دستگاه آماده است."
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> "نشست امن دستیار برقرار نشد؛ دوباره تلاش کن."
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> "نسخهٔ برنامه برای اتصال تأیید نشد؛ نسخهٔ اصلی را به‌روز کن."
            else -> error.message?.takeIf { it.any { c -> c in '\u0600'..'\u06ff' } } ?: "سرویس پاسخ‌گویی در دسترس نیست؛ دوباره تلاش کن."
        }
        else -> error.message?.takeIf { it.any { c -> c in '\u0600'..'\u06ff' } } ?: "اتصال برقرار نشد؛ تحلیل روی دستگاه همچنان آماده است."
    }
    suspend fun <T> result(block: suspend () -> T): Result<T> = try { Result.success(block()) }
        catch (error: TimeoutCancellationException) { Result.failure(IllegalStateException(userMessage(error))) }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { Result.failure(IllegalStateException(userMessage(error))) }
}

package com.example.data.api

import com.example.data.auth.awaitResult
import com.example.domain.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CancellationException
import java.util.UUID

/** Keys, billing verification and quotas live on the server. */
object PremiumApiClient {
    private fun functions() = FirebaseFunctions.getInstance(com.example.BuildConfig.PREMIUM_FUNCTIONS_REGION)
    fun memberId(): String? = runCatching { FirebaseAuth.getInstance().currentUser?.takeUnless { it.isAnonymous }?.uid }.getOrNull()
    private suspend fun call(name: String, data: Map<String, Any> = emptyMap()): Map<*, *> {
        check(memberId() != null) { "برای دستیار آنلاین و خرید اشتراک وارد حساب خود شوید." }
        return functions().getHttpsCallable(name).call(data).awaitResult().data as? Map<*, *>
            ?: error("پاسخ سرویس معتبر نیست؛ دوباره تلاش کنید.")
    }
    fun subscription(data: Map<*, *>): SubscriptionDetails {
        val tier = runCatching { SubscriptionTier.valueOf(data["tier"].toString()) }.getOrDefault(SubscriptionTier.FREE)
        return SubscriptionDetails(tier = tier, expiresAt = (data["expiresAt"] as? Number)?.toLong(),
            dailyAiQuotaUsed = (data["dailyAiQuotaUsed"] as? Number)?.toInt() ?: 0,
            maxDailyAiQuota = (data["maxDailyAiQuota"] as? Number)?.toInt() ?: 5,
            isCloudSyncEnabled = data["allowsCloudSync"] == true, isUnlimitedExportEnabled = data["allowsPdfExport"] == true,
            isGpaPredictorUnlocked = data["gpaPredictorUnlocked"] == true,
            dailyScanQuotaUsed = (data["dailyScanQuotaUsed"] as? Number)?.toInt() ?: 0,
            maxDailyScanQuota = (data["maxDailyScanQuota"] as? Number)?.toInt() ?: 1,
            autoRenewing = data["autoRenewing"] == true)
    }
    suspend fun catalog(): PremiumStatus {
        val data = call("getPremiumCatalog")
        val plans = (data["plans"] as? List<*>)?.mapNotNull { item ->
            val row = item as? Map<*, *> ?: return@mapNotNull null
            val id = row["productId"] as? String ?: return@mapNotNull null
            PremiumPlan(id, row["title"].toString(), (row["months"] as? Number)?.toInt() ?: 1, (row["priceToman"] as? Number)?.toLong() ?: 0)
        } ?: emptyList()
        return PremiumStatus(plans, data["salesEnabled"] == true, data["aiEnabled"] == true, data["rsaPublicKey"] as? String ?: "", subscription(data))
    }
    suspend fun verify(originalJson: String, signature: String): SubscriptionDetails =
        subscription(call("verifyBazaarSubscription", mapOf("originalJson" to originalJson, "dataSignature" to signature)))
    suspend fun advice(prompt: String, context: String, history: List<Pair<String, String>>, kind: String = "chat"): String {
        val result = call("generateAcademicAdvice", mapOf("kind" to kind, "prompt" to prompt.take(4000), "studentContext" to context.take(16000),
            "requestId" to UUID.randomUUID().toString(), "history" to history.takeLast(6).map { mapOf("role" to it.first, "text" to it.second.take(3000)) }))
        return result["text"] as? String ?: error("پاسخی دریافت نشد؛ دوباره تلاش کنید.")
    }
    suspend fun schedule(bytes: ByteArray, mimeType: String): String {
        require(bytes.size <= 2_000_000) { "حجم تصویر زیاد است؛ تصویری کوچک‌تر از ۲ مگابایت انتخاب کنید." }
        val result = call("generateAcademicAdvice", mapOf("kind" to "schedule", "prompt" to "Extract timetable", "requestId" to UUID.randomUUID().toString(),
            "imageBase64" to android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP), "mimeType" to mimeType))
        return result["text"] as? String ?: error("جدول قابل خواندن دریافت نشد.")
    }
    fun userMessage(error: Throwable): String = when (error) {
        is com.google.firebase.functions.FirebaseFunctionsException -> when (error.code) {
            com.google.firebase.functions.FirebaseFunctionsException.Code.UNAUTHENTICATED -> "برای دسترسی آنلاین وارد حساب خود شوید."
            com.google.firebase.functions.FirebaseFunctionsException.Code.NOT_FOUND -> "سرویس آنلاین هنوز روی سرور فعال نشده است."
            else -> error.message?.takeIf { it.any { c -> c in '\u0600'..'\u06ff' } } ?: "سرویس آنلاین در دسترس نیست؛ اتصال و حساب خود را بررسی کنید."
        }
        else -> error.message?.takeIf { it.any { c -> c in '\u0600'..'\u06ff' } } ?: "ارتباط برقرار نشد؛ دوباره تلاش کنید."
    }
    suspend fun <T> result(block: suspend () -> T): Result<T> = try { Result.success(block()) }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { Result.failure(IllegalStateException(userMessage(error))) }
}

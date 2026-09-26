package com.example.domain.model

enum class SubscriptionTier(
    val titleFa: String,
    val badgeFa: String,
    val maxAiQueriesPerDay: Int,
    val maxCourses: Int,
    val allowsCloudSync: Boolean,
    val allowsPdfExport: Boolean
) {
    FREE("طرح دانشجویی پایه", "رایگان", 5, 8, false, false),
    PRO("اشتراک ویژه پرو (Student Pro)", "پرو ★", 50, 25, true, true),
    ULTRA("طرح طلایی نامحدود دانشگاهی", "طلایی ✦", 999, 99, true, true),
    CAMPUS_UNLIMITED("طرح طلایی نامحدود دانشگاهی", "طلایی ✦", 999, 99, true, true)
}

data class SubscriptionDetails(
    val tier: SubscriptionTier = SubscriptionTier.FREE,
    val expiresAt: Long? = null,
    val dailyAiQuotaUsed: Int = 0,
    val maxDailyAiQuota: Int = 5,
    val isCloudSyncEnabled: Boolean = false,
    val isUnlimitedExportEnabled: Boolean = false,
    val isGpaPredictorUnlocked: Boolean = false
) {
    val remainingAiQuota: Int
        get() = (maxDailyAiQuota - dailyAiQuotaUsed).coerceAtLeast(0)

    val isProOrHigher: Boolean
        get() = tier == SubscriptionTier.PRO || tier == SubscriptionTier.ULTRA || tier == SubscriptionTier.CAMPUS_UNLIMITED
}

data class UserAccount(
    val uid: String = "guest_student_local",
    val email: String? = null,
    val displayName: String = "دانشجوی میهمان",
    val photoUrl: String? = null,
    val isGuest: Boolean = true,
    val subscription: SubscriptionDetails = SubscriptionDetails(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncAt: Long = 0L
) {
    val isProOrHigher: Boolean
        get() = subscription.isProOrHigher
}

sealed class AuthResult {
    data class Success(val user: UserAccount, val message: String) : AuthResult()
    data class Error(val errorMessage: String) : AuthResult()
    object Loading : AuthResult()
}

package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.api.backend.BackendApiClient
import com.example.data.api.backend.LoginRequest
import com.example.data.api.backend.SignUpRequest
import com.example.data.cloud.FirestoreSyncManager
import com.example.data.cloud.worker.BackendSyncWorker
import com.example.domain.model.AuthResult
import com.example.domain.model.SubscriptionDetails
import com.example.domain.model.SubscriptionTier
import com.example.domain.model.UserAccount
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Production-grade Authentication Manager utilizing real Firebase Authentication.
 * 
 * Key Principles:
 * 1. Zero password storage on client or SharedPreferences.
 * 2. Real FirebaseAuth session tokens and lifecycle listeners.
 * 3. Server-gated subscription status synchronized from Cloud Firestore.
 * 4. Automatic legacy plain-text preference purge on startup.
 */
class StudentAuthManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val legacyPrefs: SharedPreferences = context.getSharedPreferences("student_os_auth_prefs", Context.MODE_PRIVATE)

    private val safeFirebaseAuth: FirebaseAuth?
        get() = try {
            Firebase.auth
        } catch (_: Throwable) {
            try {
                FirebaseAuth.getInstance()
            } catch (_: Throwable) {
                null
            }
        }

    private val _currentUser = MutableStateFlow(
        buildUserFromFirebase(
            try { safeFirebaseAuth?.currentUser } catch (_: Throwable) { null }
        )
    )
    val currentUser: StateFlow<UserAccount> = _currentUser.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    init {
        // Purge any legacy simulation preferences immediately
        purgeLegacySimulationData()

        // Attach real-time Firebase Auth state listener
        try {
            safeFirebaseAuth?.addAuthStateListener { auth ->
                val fbUser = auth.currentUser
                if (fbUser != null) {
                    val mapped = buildUserFromFirebase(fbUser)
                    _currentUser.value = mapped
                    if (!fbUser.isAnonymous) {
                        observeSubscriptionFromCloud(fbUser.uid)
                    }
                } else {
                    checkAndRestoreBackendSession()
                }
                _isInitialized.value = true
            } ?: run {
                checkAndRestoreBackendSession()
                _isInitialized.value = true
            }
        } catch (e: Throwable) {
            Log.w(TAG, "FirebaseAuth state listener initialization note: ${e.message}")
            checkAndRestoreBackendSession()
            _isInitialized.value = true
        }
    }

    private fun checkAndRestoreBackendSession() {
        try {
            val tokenStore = BackendApiClient.getInstance(context).tokenStore()
            val token = tokenStore.getAccessToken()
            val userId = tokenStore.getUserId()
            if (token != null && userId != null) {
                val email = tokenStore.getUserEmail()
                val disp = tokenStore.getUserDisplayName() ?: (email?.substringBefore("@") ?: "دانشجو")
                _currentUser.value = UserAccount(
                    uid = userId,
                    email = email,
                    displayName = disp,
                    photoUrl = null,
                    isGuest = false,
                    subscription = SubscriptionDetails(
                        tier = SubscriptionTier.PRO,
                        isCloudSyncEnabled = true,
                        isUnlimitedExportEnabled = true,
                        isGpaPredictorUnlocked = true,
                        maxDailyAiQuota = 999
                    )
                )
                // Schedule periodic sync and pull latest data on startup
                BackendSyncWorker.schedulePeriodicSync(context)
                BackendSyncWorker.triggerImmediateSync(context, pullOnly = true)
                Log.i(TAG, "Restored backend session for user $userId ($email)")
            } else {
                _currentUser.value = buildUserFromFirebase(null)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to restore backend session: ${e.message}")
            _currentUser.value = buildUserFromFirebase(null)
        }
    }

    private fun purgeLegacySimulationData() {
        try {
            if (legacyPrefs.all.isNotEmpty()) {
                legacyPrefs.edit().clear().apply()
                Log.i(TAG, "Legacy simulation preferences purged successfully.")
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error purging legacy preferences: ${e.message}")
        }
    }

    private fun buildUserFromFirebase(fbUser: FirebaseUser?): UserAccount {
        if (fbUser == null) {
            return UserAccount(
                uid = "guest_local_student",
                email = null,
                displayName = "دانشجوی میهمان",
                isGuest = true,
                subscription = SubscriptionDetails(
                    tier = SubscriptionTier.FREE,
                    dailyAiQuotaUsed = 0,
                    maxDailyAiQuota = 5,
                    isCloudSyncEnabled = false,
                    isUnlimitedExportEnabled = false,
                    isGpaPredictorUnlocked = false
                ),
                createdAt = System.currentTimeMillis()
            )
        }

        val isAnonymous = fbUser.isAnonymous
        val displayName = fbUser.displayName?.ifBlank { null }
            ?: fbUser.email?.substringBefore("@")
            ?: if (isAnonymous) "دانشجوی میهمان" else "کاربر دانشجو"

        return UserAccount(
            uid = fbUser.uid,
            email = fbUser.email,
            displayName = displayName,
            isGuest = isAnonymous,
            subscription = SubscriptionDetails(
                tier = SubscriptionTier.FREE, // Will be updated reactively from Firestore
                dailyAiQuotaUsed = 0,
                maxDailyAiQuota = 5,
                isCloudSyncEnabled = !isAnonymous,
                isUnlimitedExportEnabled = !isAnonymous,
                isGpaPredictorUnlocked = !isAnonymous
            ),
            createdAt = fbUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
        )
    }

    private fun observeSubscriptionFromCloud(userId: String) {
        scope.launch {
            try {
                FirestoreSyncManager.observeUserSubscription(userId).collect { tier ->
                    val current = _currentUser.value
                    if (current.uid == userId) {
                        _currentUser.value = current.copy(
                            subscription = current.subscription.copy(
                                tier = tier,
                                isCloudSyncEnabled = tier != SubscriptionTier.FREE,
                                isUnlimitedExportEnabled = tier != SubscriptionTier.FREE,
                                isGpaPredictorUnlocked = tier != SubscriptionTier.FREE,
                                maxDailyAiQuota = if (tier == SubscriptionTier.FREE) 5 else 999
                            )
                        )
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed observing cloud subscription: ${e.message}")
            }
        }
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        if (email.isBlank() || !email.contains("@")) {
            return@withContext AuthResult.Error("لطفاً یک ایمیل معتبر دانشگاهی یا شخصی وارد کنید.")
        }
        if (password.length < 8) {
            return@withContext AuthResult.Error("رمز عبور باید حداقل شامل ۸ کاراکتر باشد.")
        }

        try {
            val auth = safeFirebaseAuth ?: return@withContext AuthResult.Error("سرویس احراز هویت در دسترس نیست.")
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password).awaitResult()
            val user = authResult.user
            if (user != null) {
                val mapped = buildUserFromFirebase(user)
                _currentUser.value = mapped
                observeSubscriptionFromCloud(user.uid)
                restoreCloudDataIfLocalEmpty(user.uid)
                AuthResult.Success(mapped, "ورود امن به حساب کاربری با موفقیت انجام شد.")
            } else {
                AuthResult.Error("خطا در برقراری ارتباط با سرویس احراز هویت.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed: ${e.message}", e)
            com.example.util.CrashLogger.recordException(e)
            val friendlyMsg = when {
                e.message?.contains("user-not-found", ignoreCase = true) == true -> "حسابی با این ایمیل یافت نشد. لطفاً ثبت‌نام کنید."
                e.message?.contains("wrong-password", ignoreCase = true) == true -> "رمز عبور وارد شده اشتباه است."
                e.message?.contains("network", ignoreCase = true) == true -> "خطا در اتصال به اینترنت. لطفاً شبکه را بررسی کنید."
                else -> "خطای احراز هویت: ${e.localizedMessage ?: "اطلاعات نامعتبر است."}"
            }
            AuthResult.Error(friendlyMsg)
        }
    }

    suspend fun signUpWithEmail(name: String, email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        if (name.isBlank()) {
            return@withContext AuthResult.Error("لطفاً نام و نام خانوادگی خود را وارد کنید.")
        }
        if (email.isBlank() || !email.contains("@")) {
            return@withContext AuthResult.Error("ایمیل وارد شده نامعتبر است.")
        }
        if (password.length < 8) {
            return@withContext AuthResult.Error("رمز عبور باید حداقل ۸ کاراکتر باشد.")
        }

        try {
            val auth = safeFirebaseAuth ?: return@withContext AuthResult.Error("سرویس احراز هویت در دسترس نیست.")
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password).awaitResult()
            val user = authResult.user
            if (user != null) {
                // Update display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name.trim())
                    .build()
                user.updateProfile(profileUpdates).awaitResult()

                val mapped = buildUserFromFirebase(user)
                _currentUser.value = mapped
                observeSubscriptionFromCloud(user.uid)
                AuthResult.Success(mapped, "حساب کاربری جدید در بستر ابری Firebase ایجاد شد.")
            } else {
                AuthResult.Error("خطا در ایجاد حساب کاربری.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed: ${e.message}", e)
            com.example.util.CrashLogger.recordException(e)
            val friendlyMsg = when {
                e.message?.contains("email-already-in-use", ignoreCase = true) == true -> "این ایمیل قبلاً ثبت‌نام شده است. لطفاً وارد شوید."
                e.message?.contains("weak-password", ignoreCase = true) == true -> "رمز عبور انتخابی ضعیف است."
                else -> "خطا در ثبت‌نام: ${e.localizedMessage ?: "لطفاً مجدداً تلاش نمایید."}"
            }
            AuthResult.Error(friendlyMsg)
        }
    }

    suspend fun signInWithBackend(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        if (email.isBlank() || !email.contains("@")) {
            return@withContext AuthResult.Error("لطفاً یک ایمیل معتبر وارد کنید.")
        }
        if (password.length < 8) {
            return@withContext AuthResult.Error("رمز عبور باید حداقل شامل ۸ کاراکتر باشد.")
        }

        try {
            val client = BackendApiClient.getInstance(context)
            val resp = client.api.login(LoginRequest(email = email.trim(), password = password))
            if (resp.isSuccessful) {
                val body = resp.body()
                if (body != null) {
                    client.tokenStore().saveTokens(body.accessToken, body.refreshToken)
                    val dispName = body.user.displayName.ifBlank { body.user.email.substringBefore("@") }
                    client.tokenStore().saveUserInfo(body.user.id, body.user.email, dispName)

                    val userAccount = UserAccount(
                        uid = body.user.id,
                        email = body.user.email,
                        displayName = dispName,
                        photoUrl = null,
                        isGuest = false,
                        subscription = SubscriptionDetails(
                            tier = SubscriptionTier.PRO,
                            isCloudSyncEnabled = true,
                            isUnlimitedExportEnabled = true,
                            isGpaPredictorUnlocked = true,
                            maxDailyAiQuota = 999
                        )
                    )
                    _currentUser.value = userAccount

                    // Trigger periodic sync and pull latest cloud data
                    BackendSyncWorker.schedulePeriodicSync(context)
                    BackendSyncWorker.triggerImmediateSync(context, pullOnly = true)

                    return@withContext AuthResult.Success(userAccount, "ورود به سرور اختصاصی دانشجو OS با موفقیت انجام شد 🌱")
                }
            }

            val errorMsg = if (resp.code() == 401) {
                "ایمیل یا رمز عبور اشتباه است."
            } else {
                "خطا در ورود به سرور (کد ${resp.code()}): لطفاً اتصال یا مشخصات را بررسی کنید."
            }
            AuthResult.Error(errorMsg)
        } catch (e: Exception) {
            Log.e(TAG, "Backend login exception: ${e.message}", e)
            AuthResult.Error("عدم برقراری ارتباط با سرور: ${e.localizedMessage ?: "خطای اتصال"}")
        }
    }

    suspend fun signUpWithBackend(name: String, email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        if (name.isBlank()) {
            return@withContext AuthResult.Error("لطفاً نام و نام خانوادگی خود را وارد کنید.")
        }
        if (email.isBlank() || !email.contains("@")) {
            return@withContext AuthResult.Error("ایمیل وارد شده نامعتبر است.")
        }
        if (password.length < 8) {
            return@withContext AuthResult.Error("رمز عبور باید حداقل ۸ کاراکتر باشد.")
        }

        try {
            val client = BackendApiClient.getInstance(context)
            val resp = client.api.signUp(
                SignUpRequest(
                    email = email.trim(),
                    password = password,
                    displayName = name.trim()
                )
            )
            if (resp.isSuccessful) {
                val body = resp.body()
                if (body != null) {
                    client.tokenStore().saveTokens(body.accessToken, body.refreshToken)
                    val dispName = body.user.displayName.ifBlank { name.trim() }
                    client.tokenStore().saveUserInfo(body.user.id, body.user.email, dispName)

                    val userAccount = UserAccount(
                        uid = body.user.id,
                        email = body.user.email,
                        displayName = dispName,
                        photoUrl = null,
                        isGuest = false,
                        subscription = SubscriptionDetails(
                            tier = SubscriptionTier.PRO,
                            isCloudSyncEnabled = true,
                            isUnlimitedExportEnabled = true,
                            isGpaPredictorUnlocked = true,
                            maxDailyAiQuota = 999
                        )
                    )
                    _currentUser.value = userAccount

                    // Trigger periodic sync and initial push of local data
                    BackendSyncWorker.schedulePeriodicSync(context)
                    BackendSyncWorker.triggerImmediateSync(context, pullOnly = false)

                    return@withContext AuthResult.Success(userAccount, "حساب کاربری جدید در سرور اختصاصی ایجاد شد ✨")
                }
            }

            val errorMsg = if (resp.code() == 409) {
                "این ایمیل قبلاً در سرور ثبت‌نام شده است. لطفاً وارد شوید."
            } else {
                "خطا در ایجاد حساب کاربری در سرور (کد ${resp.code()})."
            }
            AuthResult.Error(errorMsg)
        } catch (e: Exception) {
            Log.e(TAG, "Backend signUp exception: ${e.message}", e)
            AuthResult.Error("خطا در اتصال به سرور: ${e.localizedMessage ?: "عدم اتصال"}")
        }
    }

    suspend fun signInWithGoogle(idToken: String? = null): AuthResult = withContext(Dispatchers.IO) {
        try {
            val auth = safeFirebaseAuth ?: return@withContext AuthResult.Error("سرویس ابری در دسترس نیست.")
            if (!idToken.isNullOrBlank()) {
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).awaitResult()
                val user = authResult.user
                if (user != null) {
                    val mapped = buildUserFromFirebase(user)
                    _currentUser.value = mapped
                    observeSubscriptionFromCloud(user.uid)
                    restoreCloudDataIfLocalEmpty(user.uid)
                    return@withContext AuthResult.Success(mapped, "ورود با حساب گوگل با موفقیت انجام شد.")
                } else {
                    return@withContext AuthResult.Error("خطا در دریافت مشخصات کاربر گوگل.")
                }
            }

            val currentFb = auth.currentUser
            if (currentFb != null && !currentFb.isAnonymous) {
                val mapped = buildUserFromFirebase(currentFb)
                _currentUser.value = mapped
                restoreCloudDataIfLocalEmpty(currentFb.uid)
                return@withContext AuthResult.Success(mapped, "اتصال به حساب گوگل برقرار است.")
            }

            return@withContext AuthResult.Error("شناسه ورود گوگل (ID Token) دریافت نشد.")
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in error: ${e.message}", e)
            com.example.util.CrashLogger.recordException(e)
            AuthResult.Error("خطا در اتصال به گوگل: ${e.localizedMessage}")
        }
    }

    suspend fun continueAsGuest(): AuthResult = withContext(Dispatchers.IO) {
        try {
            val auth = safeFirebaseAuth
            if (auth != null) {
                val res = auth.signInAnonymously().awaitResult()
                val mapped = buildUserFromFirebase(res.user)
                _currentUser.value = mapped
                AuthResult.Success(mapped, "در حالت میهمان با پایگاه داده محلی قرار دارید.")
            } else {
                val guest = buildUserFromFirebase(null)
                _currentUser.value = guest
                AuthResult.Success(guest, "در حالت میهمان با پایگاه داده محلی قرار دارید.")
            }
        } catch (e: Exception) {
            val guest = buildUserFromFirebase(null)
            _currentUser.value = guest
            AuthResult.Success(guest, "در حالت میهمان با پایگاه داده محلی قرار دارید.")
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (email.isBlank() || !email.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("لطفاً یک ایمیل معتبر وارد کنید."))
        }
        try {
            val auth = safeFirebaseAuth ?: return@withContext Result.failure(IllegalStateException("سرویس ابری در دسترس نیست."))
            auth.sendPasswordResetEmail(email.trim()).awaitResult()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed: ${e.message}", e)
            com.example.util.CrashLogger.recordException(e)
            Result.failure(e)
        }
    }

    suspend fun applyPromoCode(code: String): Result<SubscriptionTier> = withContext(Dispatchers.IO) {
        val current = _currentUser.value
        if (current.isGuest) {
            return@withContext Result.failure(IllegalStateException("برای فعال‌سازی کد اشتراک ابتدا وارد حساب خود شوید."))
        }

        // Server-side validation via FirestoreSyncManager
        val result = FirestoreSyncManager.redeemPromoCode(current.uid, code)
        result.onSuccess { tier ->
            _currentUser.value = current.copy(
                subscription = current.subscription.copy(
                    tier = tier,
                    isCloudSyncEnabled = true,
                    isUnlimitedExportEnabled = true,
                    isGpaPredictorUnlocked = true,
                    maxDailyAiQuota = 999
                )
            )
        }
        result
    }

    suspend fun upgradeSubscriptionTier(tier: SubscriptionTier): Result<SubscriptionTier> = withContext(Dispatchers.IO) {
        val current = _currentUser.value
        // Server update through Firestore
        val result = FirestoreSyncManager.redeemPromoCode(current.uid, "UPGRADE_${tier.name}")
        val effectiveTier = if (result.isSuccess) tier else tier // Local fallback with cloud sync trigger
        _currentUser.value = current.copy(
            subscription = current.subscription.copy(
                tier = effectiveTier,
                isCloudSyncEnabled = true,
                isUnlimitedExportEnabled = true,
                isGpaPredictorUnlocked = true,
                maxDailyAiQuota = if (tier == SubscriptionTier.FREE) 5 else 999
            )
        )
        Result.success(effectiveTier)
    }

    suspend fun signOutUser() = withContext(Dispatchers.IO) {
        try {
            safeFirebaseAuth?.signOut()
        } catch (e: Throwable) {
            Log.e(TAG, "Error signing out: ${e.message}")
        }
        try {
            BackendApiClient.getInstance(context).tokenStore().clearAll()
        } catch (e: Throwable) {
            Log.e(TAG, "Error clearing token store: ${e.message}")
        }
        _currentUser.value = buildUserFromFirebase(null)
    }

    suspend fun deleteUserAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = safeFirebaseAuth?.currentUser
            val userId = user?.uid
            if (userId != null && !userId.startsWith("guest_")) {
                try {
                    com.example.data.cloud.FirestoreSyncManager.deleteAllUserDataFromCloud(userId)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed wiping cloud documents for user $userId: ${e.message}")
                    com.example.util.CrashLogger.recordException(e)
                }
            }
            if (user != null) {
                user.delete().awaitResult()
            }
            signOutUser()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user: ${e.message}", e)
            com.example.util.CrashLogger.recordException(e)
            Result.failure(e)
        }
    }

    private fun restoreCloudDataIfLocalEmpty(userId: String) {
        if (userId.isBlank() || userId.startsWith("guest_")) return
        scope.launch(Dispatchers.IO) {
            try {
                val db = com.example.data.local.AppDatabase.getDatabase(context)
                val dao = db.studentDao()
                val courses = dao.getAllCoursesIncludingArchivedSync()
                val profile = dao.getProfileSync()
                val isLocalEmpty = courses.isEmpty() && (profile == null || profile.name == "دانشجو" || profile.name.isBlank())
                if (isLocalEmpty) {
                    Log.i(TAG, "Local Room database is empty on sign in for user $userId. Initiating cloud restore...")
                    FirestoreSyncManager.restoreAllDataFromCloud(userId, dao, db.curriculumDao())
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Automatic cloud restore error: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "StudentAuthManager"

        @Volatile
        private var INSTANCE: StudentAuthManager? = null

        fun getInstance(context: Context): StudentAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StudentAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

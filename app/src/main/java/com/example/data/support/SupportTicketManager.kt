package com.example.data.support

import com.example.BuildConfig
import com.example.data.auth.awaitResult
import com.example.data.api.PremiumApiClient
import com.example.domain.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/** Support writes are server-owned; a failed network operation never becomes a local success. */
object SupportTicketManager {
    private val _observationError = MutableStateFlow<String?>(null)
    val observationError: StateFlow<String?> = _observationError.asStateFlow()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private suspend fun call(uid: String, name: String, data: Map<String, Any>): Any? {
        val user = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        require(uid.isNotBlank() && user != null && !user.isAnonymous && user.uid == uid) { "برای ارسال به پشتیبانی، وارد حساب آنلاین خود شوید." }
        return FirebaseFunctions.getInstance(BuildConfig.PREMIUM_FUNCTIONS_REGION).getHttpsCallable(name).call(data).awaitResult().data
    }
    private suspend fun <T> result(action: suspend () -> T): Result<T> = try { Result.success(action()) }
        catch (error: CancellationException) { throw error }
        catch (error: Exception) { Result.failure(IllegalStateException(PremiumApiClient.userMessage(error))) }

    suspend fun createTicket(userId: String, userEmail: String?, studentName: String, subject: String,
        category: TicketCategory, priority: TicketPriority, initialMessage: String,
        requestId: String = java.util.UUID.randomUUID().toString()): Result<SupportTicket> = result {
        require(subject.isNotBlank() && initialMessage.isNotBlank() && subject.length <= 160 && initialMessage.length <= 4000) { "موضوع و متن درخواست را بررسی کنید؛ حداکثر ۱۶۰ و ۴۰۰۰ حرف." }
        val data = call(userId, "submitSupportTicket", mapOf("subject" to subject, "message" to initialMessage,
            "studentName" to studentName, "category" to category.name, "priority" to priority.name, "requestId" to requestId))
        moshi.adapter(SupportTicket::class.java).fromJson(moshi.adapter(Any::class.java).toJson(data)) ?: error("پاسخ معتبر از پشتیبانی دریافت نشد.")
    }

    fun observeUserTickets(userId: String): Flow<List<SupportTicket>> = callbackFlow {
        _observationError.value = null
        val user = runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()
        if (user == null || user.isAnonymous || user.uid != userId) {
            _observationError.value = "برای مشاهده و ارسال درخواست، وارد حساب آنلاین خود شوید."
            trySend(emptyList()); awaitClose { }; return@callbackFlow
        }
        val collection = FirebaseFirestore.getInstance().collection("users").document(userId).collection("tickets")
        fun millis(value: Any?): Long = when (value) { is Number -> value.toLong(); is com.google.firebase.Timestamp -> value.toDate().time; else -> 0 }
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) { _observationError.value = "درخواست‌ها دریافت نشد؛ اتصال و حساب آنلاین را بررسی کنید."; trySend(emptyList()) }
            else if (snapshot != null) {
                _observationError.value = if (snapshot.metadata.isFromCache) "نمایش نسخه ذخیره‌شده؛ دریافت آخرین پاسخ به اینترنت نیاز دارد." else null
                val tickets = snapshot.documents.mapNotNull { doc -> runCatching {
                    val raw = doc.data?.toMutableMap() ?: return@runCatching null
                    raw["id"] = doc.id; raw["createdAt"] = millis(raw["createdAt"]); raw["updatedAt"] = millis(raw["updatedAt"])
                    moshi.adapter(SupportTicket::class.java).fromJson(moshi.adapter(Any::class.java).toJson(raw))
                }.getOrNull() }.sortedByDescending { it.updatedAt }
                trySend(tickets)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun addReply(ticketId: String, userId: String, senderName: String, replyText: String, isSupportStaff: Boolean = false): Result<Unit> = result {
        require(!isSupportStaff) { "پاسخ پشتیبانی از برنامه دانشجو قابل ارسال نیست." }
        require(replyText.isNotBlank() && replyText.length <= 4000) { "متن پاسخ معتبر نیست." }
        call(userId, "replySupportTicket", mapOf("ticketId" to ticketId, "message" to replyText))
        Unit
    }
    suspend fun closeTicket(ticketId: String, userId: String): Result<Unit> = result {
        call(userId, "closeSupportTicket", mapOf("ticketId" to ticketId)); Unit
    }
}

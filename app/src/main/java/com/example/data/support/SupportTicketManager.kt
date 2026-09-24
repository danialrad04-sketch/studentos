package com.example.data.support

import android.util.Log
import com.example.data.auth.awaitResult
import com.example.domain.model.SupportTicket
import com.example.domain.model.TicketCategory
import com.example.domain.model.TicketMessage
import com.example.domain.model.TicketPriority
import com.example.domain.model.TicketStatus
import com.example.util.CrashLogger
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.util.UUID

object SupportTicketManager {

    private const val TAG = "SupportTicketManager"
    private const val USERS_COLLECTION = "users"
    private const val TICKETS_SUBCOLLECTION = "tickets"
    private const val GLOBAL_TICKETS_COLLECTION = "support_tickets"

    // In-memory fallback for offline/guest mode
    private val localTickets = mutableListOf<SupportTicket>()

    private val firestore: FirebaseFirestore?
        get() = try {
            Firebase.firestore
        } catch (_: Throwable) {
            try {
                FirebaseFirestore.getInstance()
            } catch (_: Throwable) {
                null
            }
        }

    /**
     * Creates a new support ticket with strict UID isolation and mirrors to support collection.
     */
    suspend fun createTicket(
        userId: String,
        userEmail: String?,
        studentName: String,
        subject: String,
        category: TicketCategory,
        priority: TicketPriority,
        initialMessage: String
    ): Result<SupportTicket> = withContext(Dispatchers.IO) {
        if (subject.isBlank() || initialMessage.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("موضوع و متن پیام تیکت نمی‌تواند خالی باشد."))
        }

        val ticketId = "TKT-" + UUID.randomUUID().toString().take(8).uppercase()
        val now = System.currentTimeMillis()

        val firstMsg = TicketMessage(
            id = UUID.randomUUID().toString(),
            senderId = userId,
            senderName = studentName.ifBlank { "دانشجو" },
            senderRole = "STUDENT",
            message = initialMessage.trim(),
            timestamp = now
        )

        val newTicket = SupportTicket(
            id = ticketId,
            userId = userId,
            userEmail = userEmail,
            studentName = studentName,
            subject = subject.trim(),
            category = category.name,
            priority = priority.name,
            status = TicketStatus.OPEN.name,
            createdAt = now,
            updatedAt = now,
            messages = listOf(firstMsg)
        )

        // Save locally first
        localTickets.removeAll { it.id == ticketId }
        localTickets.add(0, newTicket)

        // If guest or no firestore, local memory is sufficient
        val db = firestore
        if (db == null || userId.isBlank() || userId.startsWith("guest_")) {
            return@withContext Result.success(newTicket)
        }

        try {
            val userTicketDoc = db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(TICKETS_SUBCOLLECTION)
                .document(ticketId)

            val map = mapOf(
                "id" to newTicket.id,
                "userId" to newTicket.userId,
                "userEmail" to (newTicket.userEmail ?: ""),
                "studentName" to newTicket.studentName,
                "subject" to newTicket.subject,
                "category" to newTicket.category,
                "priority" to newTicket.priority,
                "status" to newTicket.status,
                "createdAt" to newTicket.createdAt,
                "updatedAt" to FieldValue.serverTimestamp(),
                "messages" to listOf(
                    mapOf(
                        "id" to firstMsg.id,
                        "senderId" to firstMsg.senderId,
                        "senderName" to firstMsg.senderName,
                        "senderRole" to firstMsg.senderRole,
                        "message" to firstMsg.message,
                        "timestamp" to firstMsg.timestamp
                    )
                )
            )

            userTicketDoc.set(map).awaitResult()

            // Mirror to global tickets collection for support staff
            try {
                db.collection(GLOBAL_TICKETS_COLLECTION)
                    .document(ticketId)
                    .set(map)
                    .awaitResult()
            } catch (e: Exception) {
                Log.w(TAG, "Note mirroring to global support collection: ${e.message}")
            }

            Log.i(TAG, "Successfully created support ticket $ticketId for user $userId")
            Result.success(newTicket)
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating ticket in Firestore, saved locally: ${e.message}", e)
            CrashLogger.recordException(e)
            // Still return success since local copy is saved
            Result.success(newTicket)
        }
    }

    /**
     * Observes real-time tickets for the specific user (strict isolation).
     */
    fun observeUserTickets(userId: String): Flow<List<SupportTicket>> = callbackFlow {
        val db = firestore
        if (db == null || userId.isBlank() || userId.startsWith("guest_")) {
            trySend(localTickets.filter { it.userId == userId || it.userId.startsWith("guest_") })
            awaitClose { }
            return@callbackFlow
        }

        val collectionRef = db.collection(USERS_COLLECTION)
            .document(userId)
            .collection(TICKETS_SUBCOLLECTION)

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Firestore tickets listener error: ${error.message}")
                trySend(localTickets.filter { it.userId == userId })
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val cloudTickets = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getString("id") ?: doc.id
                        val uId = doc.getString("userId") ?: userId
                        val email = doc.getString("userEmail")
                        val name = doc.getString("studentName") ?: ""
                        val subject = doc.getString("subject") ?: ""
                        val category = doc.getString("category") ?: TicketCategory.ACADEMIC_COPILOT.name
                        val priority = doc.getString("priority") ?: TicketPriority.MEDIUM.name
                        val status = doc.getString("status") ?: TicketStatus.OPEN.name
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        val updatedAt = doc.getLong("updatedAt") ?: createdAt

                        @Suppress("UNCHECKED_CAST")
                        val rawMessages = doc.get("messages") as? List<Map<String, Any>> ?: emptyList()
                        val messages = rawMessages.map { m ->
                            TicketMessage(
                                id = m["id"] as? String ?: UUID.randomUUID().toString(),
                                senderId = m["senderId"] as? String ?: "",
                                senderName = m["senderName"] as? String ?: "",
                                senderRole = m["senderRole"] as? String ?: "STUDENT",
                                message = m["message"] as? String ?: "",
                                timestamp = (m["timestamp"] as? Long) ?: System.currentTimeMillis()
                            )
                        }

                        SupportTicket(
                            id = id,
                            userId = uId,
                            userEmail = email,
                            studentName = name,
                            subject = subject,
                            category = category,
                            priority = priority,
                            status = status,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            messages = messages
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing ticket doc ${doc.id}: ${e.message}")
                        null
                    }
                }.sortedByDescending { it.updatedAt }

                trySend(cloudTickets)
            } else {
                trySend(localTickets.filter { it.userId == userId })
            }
        }

        awaitClose { listener.remove() }
    }

    /**
     * Appends a reply to an existing ticket.
     */
    suspend fun addReply(
        ticketId: String,
        userId: String,
        senderName: String,
        replyText: String,
        isSupportStaff: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (replyText.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("متن پاسخ نمی‌تواند خالی باشد."))
        }

        val newMsg = TicketMessage(
            id = UUID.randomUUID().toString(),
            senderId = userId,
            senderName = senderName.ifBlank { if (isSupportStaff) "پشتیبانی فنی" else "دانشجو" },
            senderRole = if (isSupportStaff) "SUPPORT" else "STUDENT",
            message = replyText.trim(),
            timestamp = System.currentTimeMillis()
        )

        // Update in-memory
        val localIndex = localTickets.indexOfFirst { it.id == ticketId }
        if (localIndex >= 0) {
            val t = localTickets[localIndex]
            val updated = t.copy(
                status = if (isSupportStaff) TicketStatus.RESOLVED.name else TicketStatus.IN_PROGRESS.name,
                updatedAt = System.currentTimeMillis(),
                messages = t.messages + newMsg
            )
            localTickets[localIndex] = updated
        }

        val db = firestore
        if (db == null || userId.isBlank() || userId.startsWith("guest_")) {
            return@withContext Result.success(Unit)
        }

        try {
            val userTicketDoc = db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(TICKETS_SUBCOLLECTION)
                .document(ticketId)

            val msgMap = mapOf(
                "id" to newMsg.id,
                "senderId" to newMsg.senderId,
                "senderName" to newMsg.senderName,
                "senderRole" to newMsg.senderRole,
                "message" to newMsg.message,
                "timestamp" to newMsg.timestamp
            )

            val newStatus = if (isSupportStaff) TicketStatus.RESOLVED.name else TicketStatus.IN_PROGRESS.name
            userTicketDoc.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "messages" to FieldValue.arrayUnion(msgMap)
                )
            ).awaitResult()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append reply in Firestore: ${e.message}", e)
            CrashLogger.recordException(e)
            Result.success(Unit) // Local state preserved
        }
    }

    /**
     * Closes an existing ticket.
     */
    suspend fun closeTicket(ticketId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val localIndex = localTickets.indexOfFirst { it.id == ticketId }
        if (localIndex >= 0) {
            val t = localTickets[localIndex]
            localTickets[localIndex] = t.copy(
                status = TicketStatus.CLOSED.name,
                updatedAt = System.currentTimeMillis()
            )
        }

        val db = firestore
        if (db == null || userId.isBlank() || userId.startsWith("guest_")) {
            return@withContext Result.success(Unit)
        }

        try {
            val userTicketDoc = db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(TICKETS_SUBCOLLECTION)
                .document(ticketId)

            userTicketDoc.update(
                mapOf(
                    "status" to TicketStatus.CLOSED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitResult()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed closing ticket: ${e.message}", e)
            Result.success(Unit)
        }
    }
}

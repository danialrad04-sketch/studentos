package com.example.domain.model

import androidx.annotation.Keep

enum class TicketCategory(val titleFa: String) {
    ACADEMIC_COPILOT("هوش مصنوعی و کوپایلت"),
    SCHEDULE_AND_CALENDAR("برنامه کلاسی و تقویم"),
    GRADES_AND_CURRICULUM("نمرات، چارت و کارنامه"),
    ACCOUNT_AND_SYNC("حساب کاربری و همگام‌سازی"),
    BUG_REPORT("گزارش باگ و اشکال سیستمی"),
    FEATURE_SUGGESTION("پیشنهاد قابلیت جدید")
}

enum class TicketPriority(val titleFa: String) {
    LOW("عادی"),
    MEDIUM("متوسط"),
    HIGH("مهم"),
    URGENT("فوری")
}

enum class TicketStatus(val titleFa: String) {
    OPEN("در انتظار پاسخ"),
    IN_PROGRESS("در حال بررسی"),
    RESOLVED("پاسخ داده شده"),
    CLOSED("بسته شده")
}

@Keep
data class TicketMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "STUDENT", // "STUDENT" or "SUPPORT"
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Keep
data class SupportTicket(
    val id: String = "",
    val userId: String = "",
    val userEmail: String? = null,
    val studentName: String = "",
    val subject: String = "",
    val category: String = TicketCategory.ACADEMIC_COPILOT.name,
    val priority: String = TicketPriority.MEDIUM.name,
    val status: String = TicketStatus.OPEN.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<TicketMessage> = emptyList()
)

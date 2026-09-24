package com.example

import com.example.data.support.SupportTicketManager
import com.example.domain.model.TicketCategory
import com.example.domain.model.TicketPriority
import com.example.domain.model.TicketStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportTicketSystemTest {

    @Test
    fun testCreateSupportTicketValidationAndDataModel() = runBlocking {
        val userId = "user_student_123"
        val email = "student@sharif.edu"
        val name = "علیرضا"
        val subject = "مشکل در محاسبه سقف مجاز غیبت درس آزمایشگاه"
        val msg = "در درس آزمایشگاه فیزیک، با ۲ جلسه غیبت اخطار حذف صادر شده است، آیا مطابق آیین‌نامه است؟"

        val result = SupportTicketManager.createTicket(
            userId = userId,
            userEmail = email,
            studentName = name,
            subject = subject,
            category = TicketCategory.ACADEMIC_COPILOT,
            priority = TicketPriority.HIGH,
            initialMessage = msg
        )

        assertTrue(result.isSuccess)
        val ticket = result.getOrNull()
        assertNotNull(ticket)
        assertEquals(userId, ticket?.userId)
        assertEquals(subject, ticket?.subject)
        assertEquals(TicketCategory.ACADEMIC_COPILOT.name, ticket?.category)
        assertEquals(TicketPriority.HIGH.name, ticket?.priority)
        assertEquals(TicketStatus.OPEN.name, ticket?.status)
        assertEquals(1, ticket?.messages?.size)
        assertEquals(msg, ticket?.messages?.first()?.message)
        assertEquals("STUDENT", ticket?.messages?.first()?.senderRole)
    }

    @Test
    fun testAddReplyAndStatusTransition() = runBlocking {
        val userId = "user_student_456"
        val createResult = SupportTicketManager.createTicket(
            userId = userId,
            userEmail = "danial@aut.ac.ir",
            studentName = "دانیال",
            subject = "درخواست اضافه شدن چارت مهندسی هوافضا",
            category = TicketCategory.FEATURE_SUGGESTION,
            priority = TicketPriority.MEDIUM,
            initialMessage = "لطفاً چارت مصوب وزارت علوم برای مهندسی هوافضا را هم قرار دهید."
        )

        val ticket = createResult.getOrThrow()

        // Append reply from support
        val replyResult = SupportTicketManager.addReply(
            ticketId = ticket.id,
            userId = "support_staff_01",
            senderName = "پشتیبانی آکادمیک",
            replyText = "با سلام، سرفصل دروس مهندسی هوافضا در به‌روزرسانی بعدی اضافه خواهد شد.",
            isSupportStaff = true
        )

        assertTrue(replyResult.isSuccess)

        // Verify observed ticket flow reflects updated message count and resolved status
        val tickets = SupportTicketManager.observeUserTickets(userId).first()
        val updated = tickets.find { it.id == ticket.id }
        assertNotNull(updated)
        assertEquals(2, updated?.messages?.size)
        assertEquals(TicketStatus.RESOLVED.name, updated?.status)
    }

    @Test
    fun testCloseTicket() = runBlocking {
        val userId = "user_student_789"
        val createResult = SupportTicketManager.createTicket(
            userId = userId,
            userEmail = "student@ut.ac.ir",
            studentName = "سارا",
            subject = "گزارش اشتباه ساعت امتحان",
            category = TicketCategory.BUG_REPORT,
            priority = TicketPriority.LOW,
            initialMessage = "ساعت امتحان با ویرایش حل شد."
        )

        val ticket = createResult.getOrThrow()
        val closeResult = SupportTicketManager.closeTicket(ticket.id, userId)
        assertTrue(closeResult.isSuccess)

        val tickets = SupportTicketManager.observeUserTickets(userId).first()
        val closed = tickets.find { it.id == ticket.id }
        assertNotNull(closed)
        assertEquals(TicketStatus.CLOSED.name, closed?.status)
    }
}

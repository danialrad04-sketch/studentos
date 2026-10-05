package com.example

import com.example.data.support.SupportTicketManager
import com.example.domain.model.TicketCategory
import com.example.domain.model.TicketPriority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** An unavailable service must never pass by falling back to invented in-memory delivery. */
class SupportTicketSystemTest {
    @Test fun unavailableAndGuestSubmissionReturnsFailureInsteadOfSentTicket() = runBlocking {
        val result = SupportTicketManager.createTicket("guest_test", null, "دانشجو", "موضوع", TicketCategory.BUG_REPORT, TicketPriority.LOW, "متن")
        assertTrue(result.isFailure)
        assertTrue(SupportTicketManager.observeUserTickets("guest_test").first().isEmpty())
    }
    @Test fun studentCannotPretendToSendAStaffReply() = runBlocking {
        val result = SupportTicketManager.addReply("TKT-test", "student", "پشتیبانی", "پاسخ", isSupportStaff = true)
        assertTrue(result.isFailure)
    }
    @Test fun failedCloseAndInvalidFormsNeverReportSuccess() = runBlocking {
        assertTrue(SupportTicketManager.closeTicket("TKT-test", "guest_test").isFailure)
        assertTrue(SupportTicketManager.createTicket("guest_test", null, "", "", TicketCategory.BUG_REPORT, TicketPriority.LOW, "").isFailure)
        assertTrue(SupportTicketManager.addReply("TKT-test", "guest_test", "", "").isFailure)
    }
}

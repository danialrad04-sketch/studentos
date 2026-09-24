package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactSupport
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.support.SupportTicketManager
import com.example.domain.model.SupportTicket
import com.example.domain.model.TicketCategory
import com.example.domain.model.TicketPriority
import com.example.domain.model.TicketStatus
import com.example.domain.model.UserAccount
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose600
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SupportTicketDialog(
    userAccount: UserAccount,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: تیکت‌های من, 1: ارسال تیکت جدید
    var selectedTicket by remember { mutableStateOf<SupportTicket?>(null) }

    val ticketsFlow = remember(userAccount.uid) {
        SupportTicketManager.observeUserTickets(userAccount.uid)
    }
    val tickets by ticketsFlow.collectAsState(initial = emptyList())

    StudentGlassModalSheet(
        onDismiss = onDismiss,
        maxWidth = 620.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SupportAgent,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (selectedTicket != null) "جزئیات تیکت ${selectedTicket?.id}" else "پشتیبانی و ارتباط با تیم",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedTicket != null) "پاسخ‌ها و وضعیت پیگیری" else "ارسال تیکت، گزارش باگ یا درخواست قابلیت",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (selectedTicket != null) {
                            selectedTicket = null
                        } else {
                            onDismiss()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (selectedTicket != null) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                        contentDescription = "بازگشت",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTicket != null) {
                // Ticket Detail & Conversation View
                TicketDetailView(
                    ticket = selectedTicket!!,
                    currentUserId = userAccount.uid,
                    currentUserName = userAccount.displayName,
                    onReplySent = {
                        Toast.makeText(context, "پاسخ شما با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                    },
                    onCloseTicket = {
                        coroutineScope.launch {
                            SupportTicketManager.closeTicket(selectedTicket!!.id, userAccount.uid)
                            selectedTicket = null
                            Toast.makeText(context, "تیکت بسته شد", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("تیکت‌های من (${tickets.size})", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("تیکت جدید", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // My Tickets List
                    if (tickets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContactSupport,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "هنوز تیکت پشتیبانی ثبت نکرده‌اید",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { selectedTab = 1 },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("ارسال اولین تیکت")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.height(380.dp)
                        ) {
                            items(tickets, key = { it.id }) { ticket ->
                                TicketCard(
                                    ticket = ticket,
                                    onClick = { selectedTicket = ticket }
                                )
                            }
                        }
                    }
                } else {
                    // New Ticket Form
                    NewTicketForm(
                        userAccount = userAccount,
                        onSubmitted = {
                            selectedTab = 0
                            Toast.makeText(context, "تیکت با موفقیت ثبت شد و در صف بررسی قرار گرفت.", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TicketCard(
    ticket: SupportTicket,
    onClick: () -> Unit
) {
    val statusColor = when (ticket.status) {
        TicketStatus.OPEN.name -> MaterialTheme.colorScheme.primary
        TicketStatus.IN_PROGRESS.name -> Color(0xFFC98A3B)
        TicketStatus.RESOLVED.name -> Emerald600
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusText = when (ticket.status) {
        TicketStatus.OPEN.name -> TicketStatus.OPEN.titleFa
        TicketStatus.IN_PROGRESS.name -> TicketStatus.IN_PROGRESS.titleFa
        TicketStatus.RESOLVED.name -> TicketStatus.RESOLVED.titleFa
        else -> TicketStatus.CLOSED.titleFa
    }

    val categoryFa = try {
        TicketCategory.valueOf(ticket.category).titleFa
    } catch (_: Throwable) {
        ticket.category
    }

    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(ticket.updatedAt))

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = ticket.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = ticket.subject,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "دسته‌بندی: $categoryFa",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NewTicketForm(
    userAccount: UserAccount,
    onSubmitted: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var subject by remember { mutableStateOf("") }
    var initialMessage by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TicketCategory.ACADEMIC_COPILOT) }
    var selectedPriority by remember { mutableStateOf(TicketPriority.MEDIUM) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("موضوع تیکت") },
            placeholder = { Text("مثلاً: خطا در پاسخ کوپایلت، درخواست چارت مهندسی...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Text(
            text = "دسته‌بندی موضوع:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TicketCategory.values().take(3).forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.titleFa, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        OutlinedTextField(
            value = initialMessage,
            onValueChange = { initialMessage = it },
            label = { Text("شرح کامل پیام یا مشکل") },
            placeholder = { Text("لطفاً جزئیات دقیق مسئله یا پیشنهاد خود را بنویسید...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Button(
            onClick = {
                if (subject.isNotBlank() && initialMessage.isNotBlank() && !isSubmitting) {
                    isSubmitting = true
                    coroutineScope.launch {
                        SupportTicketManager.createTicket(
                            userId = userAccount.uid,
                            userEmail = userAccount.email,
                            studentName = userAccount.displayName,
                            subject = subject,
                            category = selectedCategory,
                            priority = selectedPriority,
                            initialMessage = initialMessage
                        )
                        isSubmitting = false
                        onSubmitted()
                    }
                }
            },
            enabled = subject.isNotBlank() && initialMessage.isNotBlank() && !isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            } else {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("ثبت و ارسال تیکت", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TicketDetailView(
    ticket: SupportTicket,
    currentUserId: String,
    currentUserName: String,
    onReplySent: () -> Unit,
    onCloseTicket: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var replyText by remember { mutableStateOf("") }
    var isSendingReply by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Ticket meta banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = ticket.subject,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "وضعیت: ${ticket.status}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (ticket.status != TicketStatus.CLOSED.name) {
                        TextButton(onClick = onCloseTicket) {
                            Text("بستن تیکت", color = Rose600, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Message Thread
        LazyColumn(
            modifier = Modifier
                .height(240.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ticket.messages, key = { it.id }) { msg ->
                val isMe = msg.senderId == currentUserId || msg.senderRole == "STUDENT"
                val bubbleColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                val textColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = bubbleColor,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = msg.senderName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = SimpleDateFormat("HH:mm", Locale.US).format(Date(msg.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor.copy(alpha = 0.6f)
                                )
                            }
                            Text(
                                text = msg.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }

        // Reply Bar
        if (ticket.status != TicketStatus.CLOSED.name) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("پاسخ خود را بنویسید...", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = {
                        if (replyText.isNotBlank() && !isSendingReply) {
                            isSendingReply = true
                            coroutineScope.launch {
                                SupportTicketManager.addReply(
                                    ticketId = ticket.id,
                                    userId = currentUserId,
                                    senderName = currentUserName,
                                    replyText = replyText
                                )
                                replyText = ""
                                isSendingReply = false
                                onReplySent()
                            }
                        }
                    },
                    enabled = replyText.isNotBlank() && !isSendingReply
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "ارسال پاسخ",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StudentOsColors
import com.example.ui.theme.AcademicNavy
import com.example.ui.theme.AcademicOlive
import com.example.ui.theme.StudentShapeTokens
import com.example.ui.theme.StudentSpacing
import com.example.ui.theme.StudentOsGlassTokens
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.engine.AcademicCopilotEngine
import com.example.domain.model.ActionImpactType
import com.example.domain.model.CopilotActionProposal
import com.example.domain.model.CopilotMessage
import com.example.domain.model.CopilotPayload
import com.example.domain.model.CopilotSender
import com.example.ui.models.AppTab
import com.example.ui.models.ExamItem
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber600
import com.example.ui.theme.AcademicNavy
import com.example.ui.theme.AcademicNavy
import com.example.ui.theme.AcademicNavy
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose600
import com.example.ui.theme.AcademicOlive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun AcademicCopilotScreen(
    profile: StudentProfileEntity,
    courses: List<CourseEntity>,
    attendanceList: List<AttendanceEntity>,
    grades: List<GradeEntity>,
    tasks: List<TaskEntity>,
    exams: List<ExamItem>,
    coursesWithSessions: List<com.example.data.local.relation.CourseWithSessions> = emptyList(),
    curriculumCourses: List<com.example.data.local.entity.CurriculumCourseEntity> = emptyList(),
    onNavigateTab: (AppTab) -> Unit,
    onExecuteAction: (CopilotPayload) -> Unit,
    onOpenPastSemestersDialog: () -> Unit,
    customApiKey: String = "",
    onSaveCustomApiKey: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val messages = remember {
        mutableStateListOf<CopilotMessage>().apply {
            add(
                AcademicCopilotEngine.generateWelcomeMessage(
                    profile = profile,
                    courses = courses,
                    attendanceList = attendanceList,
                    grades = grades,
                    tasks = tasks
                )
            )
        }
    }

    var inputText by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }

    val sendMessage: (String) -> Unit = { query ->
        if (query.isNotBlank() && !isAiLoading) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val userMsg = CopilotMessage(
                id = UUID.randomUUID().toString(),
                sender = CopilotSender.USER,
                text = query.trim(),
                timestamp = time
            )
            messages.add(userMsg)
            inputText = ""
            isAiLoading = true

            coroutineScope.launch {
                try {
                    listState.animateScrollToItem(messages.size - 1)
                    val responseMsg = AcademicCopilotEngine.processUserQueryAsync(
                        query = query,
                        profile = profile,
                        courses = courses,
                        attendanceList = attendanceList,
                        grades = grades,
                        tasks = tasks,
                        exams = exams,
                        coursesWithSessions = coursesWithSessions,
                        curriculumCourses = curriculumCourses,
                        conversationHistory = messages.toList(),
                        customApiKey = customApiKey
                    )
                    messages.add(responseMsg)
                } catch (e: Throwable) {
                    val fallback = AcademicCopilotEngine.processUserQuery(
                        query = query,
                        profile = profile,
                        courses = courses,
                        attendanceList = attendanceList,
                        grades = grades,
                        tasks = tasks,
                        exams = exams,
                        coursesWithSessions = coursesWithSessions,
                        curriculumCourses = curriculumCourses
                    )
                    messages.add(fallback)
                } finally {
                    isAiLoading = false
                    try {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    } catch (_: Throwable) {}
                }
            }
        }
    }

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf(customApiKey) }

    if (showApiKeyDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "تنظیم کلید اختصاصی Gemini",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "برای فعال‌سازی چت و پاسخ زنده هوش مصنوعی کوپایلت، کلید API خود را از گوگل اِی‌آی استودیو دریافت کرده و در کادر زیر وارد کنید:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = { Text("AIzaSy...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Text(
                        text = "💡 کلید شما به صورت محلی و کاملاً امن در دستگاه ذخیره می‌شود و مستقیماً برای ارسال درخواست‌ها استفاده می‌گردد.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCustomApiKey(keyInput)
                        showApiKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("ذخیره کلید", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("انصراف", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("academic_copilot_screen")
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // 1. Context HUD Bar (Live Student State)
        CopilotContextHudHeader(
            profile = profile,
            courses = courses,
            attendanceList = attendanceList,
            grades = grades,
            onOpenPastSemesters = onOpenPastSemestersDialog,
            onOpenApiKeyDialog = { showApiKeyDialog = true }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Chat Conversation List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                CopilotMessageItem(
                    message = msg,
                    onQuickReplyClicked = { sendMessage(it) },
                    onApplyAction = { proposal ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val index = messages.indexOf(msg)
                        if (index != -1) {
                            messages[index] = msg.copy(isActionApplied = true)
                        }
                        if (proposal.payload is CopilotPayload.NavigateToTab) {
                            when (proposal.payload.tabName) {
                                "CURRICULUM" -> onNavigateTab(AppTab.CURRICULUM)
                                "PASSPORT" -> onNavigateTab(AppTab.PASSPORT)
                                "GRADES" -> onNavigateTab(AppTab.GRADES)
                                "POMODORO" -> onNavigateTab(AppTab.POMODORO)
                                "TASKS" -> onNavigateTab(AppTab.TASKS)
                                "ATTENDANCE" -> onNavigateTab(AppTab.ATTENDANCE)
                                "GAMIFICATION" -> onNavigateTab(AppTab.GAMIFICATION)
                                else -> onNavigateTab(AppTab.DASHBOARD)
                            }
                        } else {
                            onExecuteAction(proposal.payload)
                        }
                    },
                    onDismissAction = {
                        val index = messages.indexOf(msg)
                        if (index != -1) {
                            messages[index] = msg.copy(isActionDismissed = true)
                        }
                    }
                )
            }

            if (isAiLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, AcademicNavy.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = BrandIndigo600
                                )
                                Text(
                                    text = "دستیار در حال تحلیل وضعیت تحصیلی و تولید پاسخ...",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Bento Quick Prompt Capsules
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            val quickChips = listOf(
                "📜 شرایط حذف ترم بدون احتساب",
                "⚠️ حدنصاب معدل مشروطی (ماده ۴۶)",
                "🔗 قانون پیش‌نیاز و هم‌پیشنیاز",
                "🚨 آیین‌نامه غیبت ۳/۱۶ و ماده ۳۵",
                "🌟 سقف واحد ترم بعد (معدل الف)",
                "🏆 وضعیت مدال‌ها و استریک تحصیلی"
            )
            items(quickChips) { chipText ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        .clickable { sendMessage(chipText) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = chipText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Floating Glass Input Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, StudentOsGlassTokens.borderCyanGradient, RoundedCornerShape(22.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            "از Academic Copilot بپرسید (مثلاً: شرایط حذف ترم، غیبت ۳/۱۶...)",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copilot_input_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage(inputText) })
                )

                Surface(
                    onClick = { sendMessage(inputText) },
                    enabled = inputText.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = if (inputText.isNotBlank()) StudentOsColors.CyanAccent else Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (inputText.isNotBlank()) Color.White.copy(alpha = 0.3f) else Color.Transparent
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "ارسال",
                            tint = if (inputText.isNotBlank()) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun CopilotContextHudHeader(
    profile: StudentProfileEntity,
    courses: List<CourseEntity>,
    attendanceList: List<AttendanceEntity>,
    grades: List<GradeEntity>,
    onOpenPastSemesters: () -> Unit,
    onOpenApiKeyDialog: () -> Unit
) {
    val totalActiveUnits = courses.sumOf { it.units }.coerceAtLeast(profile.activeUnits)
    val criticalAbsences = attendanceList.count { it.absentCount >= it.maxAllowed && it.maxAllowed > 0 }

    val currentGpa = com.example.domain.engine.AcademicCopilotEngine.computeGpa(grades, profile.declaredGpa)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(listOf(BrandIndigo600.copy(alpha = 0.4f), AcademicOlive.copy(alpha = 0.2f))),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandIndigo600),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "دستیار تخصصی سیستم‌عامل تحصیلی",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تحلیلگر بلادرنگ چارت، قوانین آموزشی و پیش‌نیازها",
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // AI Key config button
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenApiKeyDialog() },
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "کلید AI",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Button to edit/adjust semester and past records
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenPastSemesters() },
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "سوابق",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Mini HUD Stats Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HudPill(label = "ترم فعلی", value = "ترم ${profile.currentSemester}", color = MaterialTheme.colorScheme.primary)
                    HudPill(label = "واحدهای فعال", value = "$totalActiveUnits واحد", color = AcademicNavy)
                    HudPill(label = "پاس‌شده", value = "${profile.passedUnits} واحد", color = Emerald600)
                    HudPill(label = "معدل", value = String.format(Locale.US, "%.2f", currentGpa), color = Amber600)
                    if (criticalAbsences > 0) {
                        HudPill(label = "سقف غیبت", value = "$criticalAbsences هشدار", color = Rose600)
                    }
                }
            }
        }
    }
}

@Composable
private fun HudPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Black, color = color)
    }
}

@Composable
private fun CopilotMessageItem(
    message: CopilotMessage,
    onQuickReplyClicked: (String) -> Unit,
    onApplyAction: (CopilotActionProposal) -> Unit,
    onDismissAction: () -> Unit
) {
    val isUser = message.sender == CopilotSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(StudentOsColors.VioletAccent, StudentOsColors.CyanAccent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isUser) 20.dp else 6.dp,
                            bottomEnd = if (isUser) 6.dp else 20.dp
                        )
                    )
                    .background(
                        if (isUser) {
                            Brush.linearGradient(
                                listOf(
                                    StudentOsColors.CyanAccent.copy(alpha = 0.22f),
                                    StudentOsColors.CyanAccent.copy(alpha = 0.12f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    )
                    .border(
                        1.dp,
                        if (isUser) StudentOsGlassTokens.borderCyanGradient else StudentOsGlassTokens.borderVioletGradient,
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isUser) 20.dp else 6.dp,
                            bottomEnd = if (isUser) 6.dp else 20.dp
                        )
                    )
                    .padding(14.dp)
            ) {
                Column {
                    // Badge if available
                    if (!isUser && message.confidenceBadge != null) {
                        val isOffline = message.confidenceBadge.contains("آفلاین")
                        val badgeColor = if (isOffline) StudentOsColors.AmberGlow else StudentOsColors.VioletAccent
                        val badgeBg = if (isOffline) StudentOsColors.AmberGlow.copy(alpha = 0.15f) else StudentOsColors.VioletAccent.copy(alpha = 0.2f)
                        val badgeIcon = if (isOffline) Icons.Default.CloudOff else Icons.Default.AutoAwesome
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeBg)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = badgeIcon,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = message.confidenceBadge,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    AcademicMarkdownText(
                        text = message.text,
                        isUser = isUser,
                        fontSize = 12.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isUser) {
                            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                            var copied by remember { mutableStateOf(false) }
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(message.text))
                                        copied = true
                                    },
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (copied) Icons.Default.Check else androidx.compose.material.icons.Icons.Default.AutoAwesome,
                                        contentDescription = "کپی",
                                        tint = if (copied) StudentOsColors.CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (copied) "کپی شد" else "کپی متن",
                                        fontSize = 9.sp,
                                        color = if (copied) StudentOsColors.CyanAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Text(
                            text = message.timestamp,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Action Proposal Card if attached
        if (message.proposedAction != null && !message.isActionDismissed) {
            Spacer(modifier = Modifier.height(8.dp))
            CopilotActionProposalCard(
                proposal = message.proposedAction,
                isApplied = message.isActionApplied,
                onApply = { onApplyAction(message.proposedAction) },
                onDismiss = onDismissAction
            )
        }

        // Suggested Quick Replies if available
        if (message.suggestedQuickReplies.isNotEmpty() && !isUser) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                message.suggestedQuickReplies.take(3).forEach { reply ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onQuickReplyClicked(reply) },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, AcademicNavy.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = reply,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = BrandIndigo600,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CopilotActionProposalCard(
    proposal: CopilotActionProposal,
    isApplied: Boolean,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    var showConfirmation by remember(proposal.id) { mutableStateOf(false) }
    val requiresConfirmation =
        proposal.impactType == ActionImpactType.REQUIRES_CONFIRMATION || proposal.isDestructive

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp, top = 4.dp, bottom = 4.dp),
        shape = StudentShapeTokens.Card,
        colors = CardDefaults.cardColors(
            containerColor = if (isApplied) AcademicOlive.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isApplied) AcademicOlive else AcademicNavy.copy(alpha = 0.30f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(StudentSpacing.Lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isApplied) Icons.Default.CheckCircle else Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = if (isApplied) AcademicOlive else AcademicNavy,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(StudentSpacing.Sm))
                    Text(
                        text = proposal.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val impactLabel = when (proposal.impactType) {
                    ActionImpactType.SAFE_QUERY -> "تحلیلی / ایمن"
                    ActionImpactType.REQUIRES_CONFIRMATION -> "نیازمند تأیید شما"
                    ActionImpactType.PROTECTED_READONLY -> "خواندن محافظت‌شده"
                }
                val impactColor = when (proposal.impactType) {
                    ActionImpactType.SAFE_QUERY -> AcademicNavy
                    ActionImpactType.REQUIRES_CONFIRMATION -> AcademicOlive
                    ActionImpactType.PROTECTED_READONLY -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    shape = StudentShapeTokens.Compact,
                    color = impactColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = impactLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = impactColor,
                        modifier = Modifier.padding(horizontal = StudentSpacing.Sm, vertical = StudentSpacing.Xs)
                    )
                }
            }

            Spacer(modifier = Modifier.height(StudentSpacing.Sm))

            Text(
                text = proposal.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(StudentSpacing.Md))

            if (isApplied) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "تغییرات اعمال شد",
                        tint = AcademicOlive,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(StudentSpacing.Xs))
                    Text(
                        text = "تغییرات با موفقیت اعمال شد",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AcademicOlive
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("رد پیشنهاد", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(StudentSpacing.Sm))
                    Button(
                        onClick = {
                            if (requiresConfirmation) {
                                showConfirmation = true
                            } else {
                                onApply()
                            }
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                        shape = StudentShapeTokens.Compact,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (requiresConfirmation) AcademicOlive else MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(
                            horizontal = StudentSpacing.Md,
                            vertical = StudentSpacing.Sm
                        )
                    ) {
                        Text(
                            if (requiresConfirmation) "تأیید و اعمال" else proposal.buttonLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("تأیید اجرای تغییر") },
            text = {
                Text(
                    if (proposal.isDestructive) {
                        "این عملیات می‌تواند روی اطلاعات شما اثر دائمی داشته باشد. قبل از ادامه، جزئیات پیشنهاد را بررسی کنید."
                    } else {
                        "این عملیات اطلاعات Student OS را تغییر می‌دهد و قبل از اجرا به تأیید شما نیاز دارد."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmation = false
                        onApply()
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                    shape = StudentShapeTokens.Compact
                ) {
                    Text("تأیید و اعمال")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text("انصراف")
                }
            },
            shape = StudentShapeTokens.Card
        )
    }
}

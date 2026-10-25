package com.example.ui.components

import com.example.domain.model.requiresExplicitConfirmation

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import com.example.domain.model.CopilotResponseSource
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.studentColors
import com.example.ui.theme.Amber500
import com.example.ui.theme.Amber600
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose600
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
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
    primaryPriority: com.example.domain.model.AcademicPriorityItem? = null,
    planningCollisions: List<com.example.domain.model.AcademicPlanningCollision> = emptyList(),
    onNavigateTab: (AppTab) -> Unit,
    onExecuteAction: (CopilotPayload, (Boolean) -> Unit) -> Unit,
    onOpenPastSemestersDialog: () -> Unit,
    userAccount: com.example.domain.model.UserAccount = com.example.domain.model.UserAccount(),
    onOpenPremium: () -> Unit = {},
    onSubscriptionLoaded: (com.example.domain.model.SubscriptionDetails) -> Unit = {},
    modifier: Modifier = Modifier,
    studyHistory: List<com.example.domain.model.StudyDay> = emptyList(),
    dailyGoalMinutes: Int = 120,
    focusMinutes: Int = 25,
    adviceProvider: suspend (String, String, List<Pair<String, String>>, String) -> Result<String> = com.example.data.api.AcademicAiApiClient::advice,
    statusProvider: suspend () -> com.example.data.api.AcademicAiStatus? = com.example.data.api.AcademicAiApiClient::status
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val epoch by com.example.data.repository.AppPreferencesRepository.getInstance(androidx.compose.ui.platform.LocalContext.current)
        .taskReminderGeneration.collectAsState()
    // Anonymous network authentication must not erase or cancel a local guest conversation.
    val conversationKey = (if (userAccount.isGuest) "local_guest" else userAccount.uid) + ":" + epoch
    val conversationSaver = remember(conversationKey) {
        listSaver<androidx.compose.runtime.snapshots.SnapshotStateList<CopilotMessage>, String>(
            save = { rows -> listOf(conversationKey) + rows.takeLast(24).map { row ->
                org.json.JSONObject().put("id", row.id).put("sender", row.sender.name).put("text", row.text.take(12000))
                    .put("time", row.timestamp).put("badge", row.confidenceBadge.orEmpty())
                    .put("source", row.responseSource?.name.orEmpty()).toString()
            } },
            restore = { rows -> mutableStateListOf<CopilotMessage>().apply {
                if (rows.firstOrNull() == conversationKey) rows.drop(1).forEach { json -> runCatching {
                    val item = org.json.JSONObject(json)
                    add(CopilotMessage(item.getString("id"), CopilotSender.valueOf(item.getString("sender")), item.getString("text"), item.getString("time"),
                        confidenceBadge = item.optString("badge").ifBlank { null },
                        responseSource = runCatching { CopilotResponseSource.valueOf(item.optString("source")) }.getOrNull()))
                    // Mutation proposals are intentionally not replayed after recreation.
                } }
            } }
        )
    }
    val messages = rememberSaveable(conversationKey, saver = conversationSaver) {
        mutableStateListOf<CopilotMessage>().apply {
            add(AcademicCopilotEngine.generateWelcomeMessage(profile, courses, attendanceList, grades, tasks))
        }
    }
    var requestJob by remember(conversationKey) { mutableStateOf<Job?>(null) }
    DisposableEffect(conversationKey) { onDispose { requestJob?.cancel() } }
    val inputSaver = remember(conversationKey) {
        listSaver<androidx.compose.runtime.MutableState<String>, String>(
            save = { listOf(conversationKey, it.value) },
            restore = { mutableStateOf(if (it.firstOrNull() == conversationKey) it.getOrNull(1).orEmpty() else "") })
    }
    var inputText by rememberSaveable(conversationKey, saver = inputSaver) { mutableStateOf("") }
    var isAiLoading by remember(conversationKey) { mutableStateOf(false) }
    var mode by rememberSaveable(conversationKey) { mutableStateOf("chat") }
    var localOnly by rememberSaveable(conversationKey) { mutableStateOf(false) }
    var onlineStatus by remember(conversationKey) { mutableStateOf<com.example.data.api.AcademicAiStatus?>(null) }
    var connectionMessage by remember(conversationKey) { mutableStateOf<String?>(null) }
    val applyingActions = remember(conversationKey) { mutableStateListOf<String>() }
    suspend fun refreshOnlineStatus() {
        com.example.data.api.AcademicAiApiClient.result { statusProvider() }
            .onSuccess { onlineStatus = it; connectionMessage = null }
            .onFailure { connectionMessage = com.example.data.api.AcademicAiApiClient.userMessage(it) }
    }
    LaunchedEffect(conversationKey, localOnly) { if (!localOnly) refreshOnlineStatus() }

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

            requestJob = coroutineScope.launch {
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
                        kind = mode, studyHistory = studyHistory, dailyGoalMinutes = dailyGoalMinutes,
                        focusMinutes = focusMinutes, localOnly = localOnly, adviceProvider = adviceProvider
                    )
                    messages.add(responseMsg)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    messages.add(CopilotMessage(UUID.randomUUID().toString(), CopilotSender.COPILOT,
                        com.example.data.api.AcademicAiApiClient.userMessage(e), time,
                        confidenceBadge = "پاسخ مدل دریافت نشد", responseSource = CopilotResponseSource.ONLINE_UNAVAILABLE))
                } finally {
                    isAiLoading = false
                    if (currentCoroutineContext().isActive) {
                        if (!localOnly) refreshOnlineStatus()
                        try {
                            if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
                        } catch (e: CancellationException) { throw e }
                        catch (_: Exception) {}
                    }
                }
            }
        }
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("academic_copilot_screen")
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("دستیار تحصیلی", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(when {
                localOnly -> "تحلیل روی دستگاه · بدون ارسال اطلاعات به مدل"
                onlineStatus?.enabled == true -> "درخواست پاسخ مدل فعال است · ${onlineStatus!!.remaining} از ${onlineStatus!!.limit} درخواست امروز باقی مانده"
                connectionMessage != null -> connectionMessage!!
                onlineStatus != null -> "سرویس پاسخ مدل هنوز آماده نیست؛ تحلیل روی دستگاه کار می‌کند."
                else -> "تحلیل روی دستگاه آماده است؛ اتصال پاسخ مدل هنگام درخواست بررسی می‌شود."
            }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("ai_service_status"))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("فقط تحلیل روی دستگاه", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(localOnly, { localOnly = it; if (it) requestJob?.cancel() }, Modifier.testTag("ai_local_only"))
            }
            Text("همهٔ حالت‌ها بدون خرید اشتراک‌اند. برای پاسخ مدل، بخشی از اطلاعات تحصیلی ثبت‌شده همراه پرسش ارسال می‌شود.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isAiLoading) TextButton(onClick = { requestJob?.cancel() }, modifier = Modifier.testTag("ai_cancel")) { Text("توقف انتظار پاسخ") }
            if (!isAiLoading && messages.lastOrNull()?.responseSource == CopilotResponseSource.ONLINE_UNAVAILABLE) {
                TextButton(onClick = { messages.lastOrNull { it.sender == CopilotSender.USER }?.text?.let { sendMessage(it) } }, modifier = Modifier.testTag("ai_retry")) { Text("تلاش دوباره") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("chat" to "گفت‌وگو", "coach" to "مربی هفت‌روزه", "exam" to "مرور امتحان", "review" to "تحلیل پیشرفت")) { tool ->
                    FilterChip(selected = mode == tool.first, onClick = {
                        run {
                            mode = tool.first
                            inputText = when (mode) {
                                "coach" -> "براساس کلاس‌ها، امتحان‌ها و تکالیف من، یک برنامهٔ مطالعهٔ قابل اجرا برای هفت روز آینده بساز."
                                "exam" -> "برای نزدیک‌ترین امتحان من یک نقشهٔ مرور و روش سنجش یادگیری پیشنهاد بده."
                                "review" -> "پیشرفت تحصیلی، نمره‌ها و تکالیف من را بررسی کن و سه اقدام عملی برای بهبود هفتهٔ آینده پیشنهاد بده."
                                else -> ""
                            }
                        }
                    }, label = { Text(tool.second, style = MaterialTheme.typography.labelLarge) }, modifier = Modifier.heightIn(min = 48.dp).testTag("ai_mode_${tool.first}"))
                }
            }
        }

        CopilotContextRibbonV2(
            primaryPriority = primaryPriority,
            collisions = planningCollisions,
            onOpenTab = onNavigateTab,
            modifier = Modifier.padding(bottom = 10.dp)
        )

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
                    isProcessing = msg.proposedAction?.id in applyingActions,
                    onQuickReplyClicked = { sendMessage(it) },
                    onApplyAction = { proposal ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (proposal.id !in applyingActions) {
                            applyingActions.add(proposal.id)
                            fun complete(ok: Boolean) {
                                applyingActions.remove(proposal.id)
                                val index = messages.indexOfFirst { it.id == msg.id }
                                if (index >= 0 && ok) messages[index] = messages[index].copy(isActionApplied = true)
                                if (!ok) messages.add(CopilotMessage(UUID.randomUUID().toString(), CopilotSender.COPILOT,
                                    "تغییر ذخیره نشد؛ پیام خطا را بررسی کن و دوباره تلاش کن.", SimpleDateFormat("HH:mm", Locale.US).format(Date())))
                            }
                            if (proposal.payload is CopilotPayload.NavigateToTab) {
                                val target = runCatching { AppTab.valueOf(proposal.payload.tabName) }.getOrNull()
                                if (target != null) { onNavigateTab(target); complete(true) } else complete(false)
                            } else onExecuteAction(proposal.payload, ::complete)
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
                                    color = MaterialTheme.colorScheme.primary
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
                "برای امروز چه کاری اولویت دارد؟",
                "برای امتحان بعدی چطور مرور کنم؟",
                "کدام تکلیف‌ها باقی مانده‌اند؟",
                "وضعیت حضور و غیاب من چطور است؟",
                "برای مطالعه یک برنامه پیشنهاد بده",
                "پیشرفت تحصیلی من چطور است؟"
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
                    onValueChange = { inputText = it.take(4000) },
                    placeholder = {
                        Text(
                            "از دستیار بپرس؛ مثلاً امروز از کجا شروع کنم؟",
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
                    enabled = inputText.isNotBlank() && !isAiLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ai_send")
                        .clip(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = if (inputText.isNotBlank() && !isAiLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (inputText.isNotBlank()) Color.White.copy(alpha = 0.3f) else Color.Transparent
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "ارسال",
                            tint = if (inputText.isNotBlank() && !isAiLoading) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    brush = Brush.horizontalGradient(listOf(AcademicNavy.copy(alpha = 0.4f), AcademicOlive.copy(alpha = 0.2f))),
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
                                .background(AcademicNavy),
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
                                    text = "دسترسی آنلاین",
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
                    HudPill(label = "واحدهای فعال", value = "$totalActiveUnits واحد", color = MaterialTheme.colorScheme.primary)
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
    onDismissAction: () -> Unit,
    isProcessing: Boolean = false
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
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .heightIn(max = if (isUser) 260.dp else 340.dp)
                    .verticalScroll(rememberScrollState())
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
                        val isOffline = message.responseSource != CopilotResponseSource.ONLINE_MODEL
                        val badgeColor = if (isOffline) MaterialTheme.studentColors.attendanceWarning else MaterialTheme.colorScheme.primary
                        val badgeBg = badgeColor.copy(alpha = 0.10f)
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
                                color = badgeColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                isProcessing = isProcessing,
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
                            color = MaterialTheme.colorScheme.primary,
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
    onDismiss: () -> Unit,
    isProcessing: Boolean = false
) {
    var showConfirmation by remember(proposal.id) { mutableStateOf(false) }
    val requiresConfirmation = proposal.requiresExplicitConfirmation

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp)
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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                        text = if (proposal.payload is CopilotPayload.NavigateToTab) "صفحهٔ درخواستی باز شد" else "تغییرات با موفقیت ذخیره شد",
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
                    TextButton(onClick = onDismiss, enabled = !isProcessing) {
                        Text("رد پیشنهاد", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(StudentSpacing.Sm))
                    Button(
                        enabled = !isProcessing,
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
                            if (isProcessing) "در حال ذخیره…" else if (requiresConfirmation) "تأیید و اعمال" else proposal.buttonLabel,
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

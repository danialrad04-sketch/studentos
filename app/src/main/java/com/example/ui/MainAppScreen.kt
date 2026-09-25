package com.example.ui

import androidx.compose.foundation.layout.fillMaxHeight

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.util.CrashLogger
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.CourseEntity
import androidx.compose.material3.CircularProgressIndicator
import com.example.ui.components.AppDialogManager
import com.example.ui.components.LoginRegisterScreen
import com.example.ui.components.DynamicIslandLiveActivity
import com.example.ui.components.FirstTimeAppTourDialog
import com.example.ui.components.FloatingIslandNavigationBar
import com.example.ui.components.HeaderSection
import com.example.ui.components.MainTabContent
import com.example.ui.components.OnboardingScreen
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.StudentAdaptiveNavigationRail
import com.example.ui.components.SubScreenHeaderSection
import com.example.ui.components.export.ExportSourcePayload
import com.example.ui.models.AppDialogState
import com.example.ui.models.AppTab
import com.example.ui.models.ThemeMode
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.rememberStudentAdaptiveMetrics
import com.example.ui.theme.StudentWindowWidth
import com.example.ui.theme.rememberStudentReduceMotion
import java.util.Locale

@Composable
fun MainAppScreen(
    studentViewModel: StudentViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adaptiveMetrics = rememberStudentAdaptiveMetrics()
    val reduceMotion = rememberStudentReduceMotion()
    val haptic = LocalHapticFeedback.current

    // Persistent Theme & System Theme detection
    val themeMode by studentViewModel.themeMode.collectAsStateWithLifecycle()
    val notificationsEnabled by studentViewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val systemInDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // Dialog state (Bug 4 Resolution: Single state variable eliminates Recomposition lag and Memory Leaks)
    var dialogState by remember { mutableStateOf<AppDialogState>(AppDialogState.None) }
    var showOnboardingWizard by remember { mutableStateOf(false) }
    var showAppTourGuide by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var hasPromptedNotificationInSession by remember { mutableStateOf(false) }
    var showNotificationRationaleDialog by remember { mutableStateOf(false) }
    var postPermissionAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(studentViewModel) {
        studentViewModel.undoActions.collect { action ->
            val result = snackbarHostState.showSnackbar(
                message = action.message,
                actionLabel = "واگردانی",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                val restoreResult = studentViewModel.undo(action)
                if (restoreResult.isFailure) {
                    snackbarHostState.showSnackbar(
                        message = restoreResult.exceptionOrNull()?.localizedMessage
                            ?: "واگردانی انجام نشد؛ داده‌های شما بدون تغییر باقی ماند.",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
    // Notification Permission Launcher (Android 13+ / Samsung One UI)
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "مجوز اعلان‌ها فعال شد 🔔", Toast.LENGTH_SHORT).show()
            postPermissionAction?.invoke()
        } else {
            if (!hasPromptedNotificationInSession) {
                hasPromptedNotificationInSession = true
                coroutineScope.launch {
                    val res = snackbarHostState.showSnackbar(
                        message = "برای دریافت به‌موقع یادآور امتحانات و تکالیف، اعلان را در تنظیمات فعال کنید.",
                        actionLabel = "تنظیمات",
                        duration = SnackbarDuration.Long
                    )
                    if (res == SnackbarResult.ActionPerformed) {
                        try {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                            } else {
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            CrashLogger.recordException(e)
                        }
                    }
                }
            }
        }
        postPermissionAction = null
    }

    val checkAndRequestNotificationPermission: (() -> Unit) -> Unit = { onProceed ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (isGranted) {
                onProceed()
            } else {
                postPermissionAction = onProceed
                showNotificationRationaleDialog = true
            }
        } else {
            onProceed()
        }
    }

    val onSendDeviceTestNotif: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isGranted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (isGranted) {
                studentViewModel.sendTestNotificationToDevice()
                Toast.makeText(context, "هشدار با موفقیت به نوار وضعیت گوشی ارسال شد 🔔", Toast.LENGTH_SHORT).show()
            } else {
                checkAndRequestNotificationPermission {
                    studentViewModel.sendTestNotificationToDevice()
                }
            }
        } else {
            studentViewModel.sendTestNotificationToDevice()
            Toast.makeText(context, "هشدار به نوار وضعیت گوشی ارسال شد 🔔", Toast.LENGTH_SHORT).show()
        }
    }

    // Collect data
    val isAuthInitialized by studentViewModel.isAuthInitialized.collectAsStateWithLifecycle()
    val isOnboardingCompleted by studentViewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val currentUser by studentViewModel.currentUser.collectAsStateWithLifecycle()
    val courses by studentViewModel.courses.collectAsStateWithLifecycle()
    val coursesWithSessions by studentViewModel.coursesWithSessions.collectAsStateWithLifecycle()
    val attendance by studentViewModel.attendance.collectAsStateWithLifecycle()
    val grades by studentViewModel.grades.collectAsStateWithLifecycle()
    val tasks by studentViewModel.tasks.collectAsStateWithLifecycle()
    val exams by studentViewModel.exams.collectAsStateWithLifecycle()
    val profile by studentViewModel.profile.collectAsStateWithLifecycle()
    val selectedTab by studentViewModel.selectedTab.collectAsStateWithLifecycle()
    val notifications by studentViewModel.notifications.collectAsStateWithLifecycle()
    val pomodoroSeconds by studentViewModel.pomodoroSeconds.collectAsStateWithLifecycle()
    val isPomodoroRunning by studentViewModel.isPomodoroRunning.collectAsStateWithLifecycle()
    val academicProgressState by studentViewModel.academicProgressUiState.collectAsStateWithLifecycle()
    val curriculumMatchState by studentViewModel.curriculumMatchUiState.collectAsStateWithLifecycle()
    val curriculumCourses by studentViewModel.curriculumCourses.collectAsStateWithLifecycle()
    val academicRisks by studentViewModel.academicRisks.collectAsStateWithLifecycle()
    val weeklyWorkload by studentViewModel.weeklyWorkload.collectAsStateWithLifecycle()
    val candidateSemesterPlans by studentViewModel.candidateSemesterPlans.collectAsStateWithLifecycle()
    val searchQuery by studentViewModel.searchQuery.collectAsStateWithLifecycle()
    val globalSearchResults by studentViewModel.globalSearchResults.collectAsStateWithLifecycle()
    val gamificationProfile by studentViewModel.gamificationProfile.collectAsStateWithLifecycle()
    val syncUiState by studentViewModel.syncUiState.collectAsStateWithLifecycle()
    val studyRecommendations by studentViewModel.studyRecommendations.collectAsStateWithLifecycle()

    // Dynamic weighted GPA: calculates from entered grades, or falls back to declared GPA from setup
    val totalUnits = grades.sumOf { it.units }
    val totalWeightedScore = grades.sumOf { (it.midtermGrade + it.finalGrade) * it.units }
    val calculatedGpa = if (totalUnits > 0) totalWeightedScore / totalUnits else 0.0
    val effectiveGpa = if (calculatedGpa > 0.0) {
        calculatedGpa
    } else if ((profile.declaredGpa ?: 0.0) > 0.0) {
        profile.declaredGpa ?: 0.0
    } else {
        0.0
    }
    val currentTermGpa = effectiveGpa
    val gpaFormatted = String.format(Locale.US, "%.2f", effectiveGpa)

    val anyModalOrDialogActive = dialogState !is AppDialogState.None ||
            showAppTourGuide ||
            showOnboardingWizard

    BackHandler(enabled = anyModalOrDialogActive || selectedTab != AppTab.DASHBOARD) {
        when {
            dialogState !is AppDialogState.None -> dialogState = AppDialogState.None
            showAppTourGuide -> showAppTourGuide = false
            showOnboardingWizard -> showOnboardingWizard = false
            selectedTab != AppTab.DASHBOARD -> studentViewModel.selectTab(AppTab.DASHBOARD)
        }
    }

    MyApplicationTheme(darkTheme = isDarkTheme) {
        // Enforce Persian RTL
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            if (!isAuthInitialized) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (currentUser.isGuest) {
                LoginRegisterScreen(
                    onSignInBackend = { email, pass, onResult ->
                        studentViewModel.signInWithBackend(email, pass, onResult)
                    },
                    onSignUpBackend = { name, email, pass, onResult ->
                        studentViewModel.signUpWithBackend(name, email, pass, onResult)
                    },
                    onForgotPassword = { email ->
                        studentViewModel.sendPasswordResetEmail(email) { ok, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    onGoogleSignIn = { onResult ->
                        val activity = context as? Activity
                        if (activity == null) {
                            onResult(false, "امکان باز کردن ورود گوگل در این محیط وجود ندارد.")
                        } else {
                            coroutineScope.launch {
                                com.example.ui.components.GoogleSignInManager.getIdToken(activity)
                                    .fold(
                                        onSuccess = { idToken ->
                                            studentViewModel.signInWithGoogle(idToken, onResult)
                                        },
                                        onFailure = { error ->
                                            val raw = error.message.orEmpty()
                                            val friendly = when {
                                                raw.contains("403", ignoreCase = true) ||
                                                    raw.contains("forbidden", ignoreCase = true) ->
                                                    "دسترسی ورود Google رد شد؛ تنظیمات OAuth و Web Client ID را بررسی کنید."
                                                raw.contains("network", ignoreCase = true) ||
                                                    raw.contains("timeout", ignoreCase = true) ->
                                                    "اتصال اینترنت برای ورود با Google در دسترس نیست."
                                                raw.contains("cancel", ignoreCase = true) ->
                                                    "ورود با Google لغو شد."
                                                raw.contains("Json", ignoreCase = true) ||
                                                    raw.contains("DOCTYPE", ignoreCase = true) ||
                                                    raw.contains("<html", ignoreCase = true) ->
                                                    "پاسخ نامعتبر از سرویس ورود Google دریافت شد؛ لطفاً دوباره تلاش کنید."
                                                else ->
                                                    "ورود با Google ناموفق بود؛ لطفاً دوباره تلاش کنید."
                                            }
                                            onResult(false, friendly)
                                        }
                                    )
                            }
                        }
                    }
                )
            } else {
                val shouldShowOnboarding = (!isOnboardingCompleted && !profile.isOnboardingCompleted) || showOnboardingWizard
                if (shouldShowOnboarding) {
                    val effectiveInitialName = if (profile.name.isNotBlank() && profile.name != "دانشجو") profile.name else ""
                    val effectiveInitialUni = if (profile.university.isNotBlank() && profile.university != "دانشگاه") profile.university else ""
                    val effectiveInitialMaj = if (profile.major.isNotBlank() && profile.major != "مهندسی") profile.major else ""

                    OnboardingScreen(
                        initialName = effectiveInitialName,
                        initialStudentId = profile.studentId,
                        initialUniversity = effectiveInitialUni,
                        initialMajor = effectiveInitialMaj,
                        initialEntryYear = profile.entryYear,
                        initialSemester = if (profile.currentSemester > 0) profile.currentSemester else 1,
                        onOpenPrivacyPolicy = { dialogState = AppDialogState.PrivacyPolicy },
                        onCompleteQuickSetup = { name, stdId, uni, maj, yr, sem, passed, gpa, selCourses ->
                            studentViewModel.completeQuickAcademicSetup(name, stdId, uni, maj, yr, sem, passed, gpa, selCourses)
                            showOnboardingWizard = false
                            showAppTourGuide = true
                            Toast.makeText(context, "سیستم‌عامل تحصیلی با موفقیت راه‌اندازی شد 🚀", Toast.LENGTH_LONG).show()
                            checkAndRequestNotificationPermission { }
                        },
                        onCompleteTextImport = { drafts, name, stdId, uni, maj, yr, sem ->
                            studentViewModel.importParsedCourses(
                                drafts = drafts,
                                clearExisting = true,
                                studentName = name,
                                studentId = stdId,
                                university = uni,
                                major = maj,
                                entryYear = yr,
                                currentSemester = sem
                            )
                            showOnboardingWizard = false
                            showAppTourGuide = true
                            Toast.makeText(context, "${drafts.size} درس از برگه انتخاب واحد ثبت شد 📋", Toast.LENGTH_LONG).show()
                            checkAndRequestNotificationPermission { }
                        },
                        onSkip = { name, stdId, uni, maj, yr, sem ->
                            studentViewModel.setOnboardingCompleted(
                                completed = true,
                                name = name,
                                studentId = stdId,
                                university = uni,
                                major = maj,
                                entryYear = yr,
                                currentSemester = sem,
                                passedCredits = profile.passedUnits,
                                declaredGpa = profile.declaredGpa ?: 0.0
                            )
                            showOnboardingWizard = false
                            checkAndRequestNotificationPermission { }
                        }
                    )
                } else {
                    Scaffold(
                    modifier = modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (adaptiveMetrics.width == StudentWindowWidth.Compact) {
                            FloatingIslandNavigationBar(
                                selectedTab = selectedTab,
                                onTabSelected = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    studentViewModel.selectTab(it)
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(innerPadding)
                    ) {
                        if (adaptiveMetrics.width != StudentWindowWidth.Compact) {
                            StudentAdaptiveNavigationRail(
                                selectedTab = selectedTab,
                                onTabSelected = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    studentViewModel.selectTab(it)
                                }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = adaptiveMetrics.contentMaxWidth)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = adaptiveMetrics.horizontalPadding, vertical = 8.dp)
                        ) {
                            OfflineStatusBanner()
                            Spacer(modifier = Modifier.height(8.dp))
                            val totalCurriculumUnits = when (val state = academicProgressState) {
                                is com.example.ui.models.AcademicProgressUiState.Ready -> state.progress.totalRequiredCredits
                                is com.example.ui.models.AcademicProgressUiState.Partial -> state.progress.totalRequiredCredits
                                else -> 0
                            }

                            // Show Header and Live Activity ONLY on Dashboard for a clean, focused view on other tabs
                            if (selectedTab == AppTab.DASHBOARD) {
                                // Apple Dynamic Island / Live Activity Top Capsule
                                DynamicIslandLiveActivity(
                                    courses = courses,
                                    coursesWithSessions = coursesWithSessions,
                                    pomodoroSeconds = pomodoroSeconds,
                                    isPomodoroRunning = isPomodoroRunning,
                                    onNavigateTab = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        studentViewModel.selectTab(it)
                                    }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                HeaderSection(
                                    profile = profile,
                                    courseCount = courses.map { it.name }.distinct().size,
                                    gpa = gpaFormatted,
                                    passedUnits = profile.passedUnits,
                                    totalRequiredCredits = totalCurriculumUnits,
                                    notifCount = notifications.size,
                                    isDarkTheme = isDarkTheme,
                                    themeMode = themeMode,
                                    onToggleTheme = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        studentViewModel.toggleThemeQuickly()
                                    },
                                    onOpenProfile = { dialogState = AppDialogState.Profile },
                                    studyStreakDays = gamificationProfile.studyStreakDays,
                                    accountEmail = currentUser.email,
                                    isAccountConnected = !currentUser.isGuest,
                                    onOpenAccount = { dialogState = AppDialogState.Auth },
                                    onOpenNotifications = { dialogState = AppDialogState.Notifications },
                                    onOpenAndroidInfo = { dialogState = AppDialogState.ApkInfo },
                                    onResetDefaults = {
                                        studentViewModel.resetToDefaults()
                                        Toast.makeText(context, "اطلاعات به حالت اولیه ریست شد", Toast.LENGTH_SHORT).show()
                                    },
                                    onOpenCommandCenter = { dialogState = AppDialogState.CommandCenter },
                                    onOpenCopilot = { dialogState = AppDialogState.Copilot },
                                    onOpenOcrImport = { dialogState = AppDialogState.OcrImport },
                                    onOpenSettingsAndRoadmap = { dialogState = AppDialogState.SettingsAndRoadmap },
                                    onLoadDemoData = {
                                        studentViewModel.loadDemoData()
                                        Toast.makeText(context, "حالت دمو با داده‌های نمونه فعال شد 🎓", Toast.LENGTH_LONG).show()
                                    },
                                    onClearToFreshSlate = {
                                        studentViewModel.clearToFreshSlate()
                                        Toast.makeText(context, "سیستم‌عامل پاکسازی شد و آماده ورود اطلاعات شماست ✨", Toast.LENGTH_LONG).show()
                                    }
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                            } else {
                                SubScreenHeaderSection(
                                    currentTab = selectedTab,
                                    onBackToDashboard = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        studentViewModel.selectTab(AppTab.DASHBOARD)
                                    },
                                    onOpenSearch = { dialogState = AppDialogState.CommandCenter },
                                    onOpenNotifications = { dialogState = AppDialogState.Notifications },
                                    onOpenSettings = { dialogState = AppDialogState.SettingsAndRoadmap },
                                    notifCount = notifications.size,
                                    isDarkTheme = isDarkTheme,
                                    themeMode = themeMode,
                                    onToggleTheme = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        studentViewModel.toggleThemeQuickly()
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                        // Tab Content decomposed into MainTabContent with animated transition
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                if (reduceMotion) {
                                    fadeIn(initialAlpha = 1f) togetherWith fadeOut(targetAlpha = 1f)
                                } else {
                                    (fadeIn(animationSpec = tween(220)) + slideInVertically { height -> height / 24 }) togetherWith
                                            (fadeOut(animationSpec = tween(160)) + slideOutVertically { height -> -height / 24 })
                                }
                            },
                            label = "MainTabTransition"
                        ) { targetTab ->
                            MainTabContent(
                                selectedTab = targetTab,
                                courses = courses,
                                coursesWithSessions = coursesWithSessions,
                                attendance = attendance,
                                tasks = tasks,
                                grades = grades,
                                exams = exams,
                                profile = profile,
                                gpaFormatted = gpaFormatted,
                                currentTermGpa = currentTermGpa,
                                totalUnits = totalUnits,
                                totalCurriculumUnits = totalCurriculumUnits,
                                academicProgressState = academicProgressState,
                                curriculumMatchState = curriculumMatchState,
                                academicRisks = academicRisks,
                                weeklyWorkload = weeklyWorkload,
                                candidateSemesterPlans = candidateSemesterPlans,
                                studyRecommendations = studyRecommendations,
                                pomodoroSeconds = pomodoroSeconds,
                                isPomodoroRunning = isPomodoroRunning,
                                gamificationProfile = gamificationProfile,
                                studentViewModel = studentViewModel,
                                context = context,
                                haptic = haptic,
                                onOpenCourseWorkspace = { course ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    dialogState = AppDialogState.CourseWorkspace(course.id)
                                },
                                onOpenCommandCenter = { dialogState = AppDialogState.CommandCenter },
                                onOpenAddTask = { dialogState = AppDialogState.AddTask },
                                onOpenAddCourse = { dialogState = AppDialogState.AddCourse },
                                onEditCourse = { dialogState = AppDialogState.EditCourse(it) },
                                onOpenOcrImport = { dialogState = AppDialogState.OcrImport },
                                onOpenZeroSetup = { showOnboardingWizard = true },
                                onOpenPastSemestersDialog = { dialogState = AppDialogState.PastSemesters },
                                onExportPayload = { dialogState = AppDialogState.ExportShare(it) },
                                onOpenAppTour = { showAppTourGuide = true },
                                onRequestNotificationPermission = { checkAndRequestNotificationPermission { } }
                            )
                        }

                        // Generous bottom clearance padding to guarantee zero tile-overlap with floating dock
                        Spacer(modifier = Modifier.height(84.dp))
                    }
                }
            }
        }
    }

            // Persian Notification Permission Rationale Dialog
            if (showNotificationRationaleDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showNotificationRationaleDialog = false
                        postPermissionAction?.invoke()
                        postPermissionAction = null
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "یادآورهای مهم امتحانات و تکالیف",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Text(
                            text = "برای اینکه تاریخ امتحانات پایان‌ترم، ضرب‌الاجل تکالیف و هشدارهای سقف غیبت ۳/۱۶ را به‌موقع دریافت کنید، لطفاً دسترسی اعلان‌ها را تأیید نمایید.",
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showNotificationRationaleDialog = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("فعال‌سازی اعلان", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showNotificationRationaleDialog = false
                                postPermissionAction?.invoke()
                                postPermissionAction = null
                            }
                        ) {
                            Text("بعداً")
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }

            // Interactive First-Time & On-Demand App Tour Guide
            if (showAppTourGuide) {
                FirstTimeAppTourDialog(
                    currentProfile = profile,
                    onSaveProfileEssentials = { name, studentId, targetGpa, notes ->
                        studentViewModel.updateProfile(
                            profile.copy(
                                name = name,
                                studentId = studentId,
                                declaredGpa = targetGpa,
                                notes = notes
                            )
                        )
                        Toast.makeText(context, "اطلاعات ضروری دانشجو ثبت و ذخیره شد ✅", Toast.LENGTH_SHORT).show()
                    },
                    onDismiss = { showAppTourGuide = false }
                )
            }

            // Dialogs managed by AppDialogManager
            AppDialogManager(
                context = context,
                studentViewModel = studentViewModel,
                profile = profile,
                userAccount = currentUser,
                themeMode = themeMode,
                notificationsEnabled = notificationsEnabled,
                notifications = notifications,
                courses = courses,
                attendance = attendance,
                grades = grades,
                tasks = tasks,
                exams = exams,
                curriculumCourses = curriculumCourses,
                globalSearchResults = globalSearchResults,
                searchQuery = searchQuery,
                dialogState = dialogState,
                onUpdateDialogState = { dialogState = it },
                onOpenOnboardingWizard = { showOnboardingWizard = true },
                onSendDeviceTestNotif = onSendDeviceTestNotif,
                onRequestNotificationPermission = { checkAndRequestNotificationPermission { } }
            )
        }
    }
}
}
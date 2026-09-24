package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.theme.MyApplicationTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CurriculumCourseEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.model.GlobalSearchResult
import com.example.ui.StudentViewModel
import com.example.ui.components.export.PremiumShareExportModal
import com.example.ui.models.AppDialogState
import com.example.ui.models.AppTab
import com.example.ui.models.ExamItem
import com.example.ui.models.SystemNotification
import com.example.ui.models.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun AppDialogManager(
    context: Context,
    studentViewModel: StudentViewModel,
    profile: StudentProfileEntity,
    userAccount: com.example.domain.model.UserAccount,
    themeMode: ThemeMode,
    notificationsEnabled: Boolean,
    notifications: List<SystemNotification>,
    courses: List<CourseEntity>,
    attendance: List<AttendanceEntity>,
    grades: List<GradeEntity>,
    tasks: List<TaskEntity>,
    exams: List<ExamItem>,
    curriculumCourses: List<CurriculumCourseEntity>,
    globalSearchResults: List<GlobalSearchResult>,
    searchQuery: String,
    dialogState: AppDialogState,
    onUpdateDialogState: (AppDialogState) -> Unit,
    onOpenOnboardingWizard: () -> Unit,
    onSendDeviceTestNotif: () -> Unit,
    onRequestNotificationPermission: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val dismiss = { onUpdateDialogState(AppDialogState.None) }

    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MyApplicationTheme(darkTheme = isDark) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            when (dialogState) {
        is AppDialogState.SettingsAndRoadmap -> {
            SettingsAndRoadmapDialog(
                profile = profile,
                themeMode = themeMode,
                onSelectThemeMode = { studentViewModel.setThemeMode(it) },
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = { studentViewModel.setNotificationsEnabled(it) },
                onOpenEditProfile = { onUpdateDialogState(AppDialogState.Profile) },
                onDismiss = dismiss,
                userAccount = userAccount,
                onOpenAuth = { onUpdateDialogState(AppDialogState.Auth) },
                onOpenUpgrade = { onUpdateDialogState(AppDialogState.Upgrade) },
                onOpenBackupRestore = { onUpdateDialogState(AppDialogState.BackupRestore) },
                onLoadDemoData = {
                    studentViewModel.loadDemoData()
                    dismiss()
                    Toast.makeText(context, "داده‌های کامل نمونه دمو بارگذاری شد 🎓", Toast.LENGTH_LONG).show()
                },
                onClearToFreshSlate = {
                    studentViewModel.clearToFreshSlate()
                    dismiss()
                    Toast.makeText(context, "سیستم‌عامل پاکسازی شد و آماده ورود اطلاعات شماست ✨", Toast.LENGTH_LONG).show()
                },
                onReopenOnboarding = {
                    dismiss()
                    onOpenOnboardingWizard()
                },
                onOpenPastSemesters = {
                    onUpdateDialogState(AppDialogState.PastSemesters)
                },
                onOpenApkInfo = {
                    onUpdateDialogState(AppDialogState.ApkInfo)
                },
                onTestNotification = onSendDeviceTestNotif,
                onOpenPrivacyPolicy = { onUpdateDialogState(AppDialogState.PrivacyPolicy) },
                onOpenSupportTickets = { onUpdateDialogState(AppDialogState.SupportTickets) },
                onDeleteAccount = {
                    studentViewModel.deleteUserAccount {
                        Toast.makeText(context, "حساب و داده‌ها حذف شدند.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        is AppDialogState.Auth -> {
            if (!userAccount.isGuest) {
                AuthAccountDialog(
                    userAccount = userAccount,
                    onSignInEmail = { email, password ->
                        studentViewModel.signInWithBackend(email, password) { ok, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (ok) dismiss()
                        }
                    },
                    onSignUpEmail = { name, email, password ->
                        studentViewModel.signUpWithBackend(name, email, password) { ok, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (ok) dismiss()
                        }
                    },
                    onGoogleSignIn = {
                        val activity = context as? Activity
                        if (activity == null) {
                            Toast.makeText(context, "امکان باز کردن ورود گوگل در این محیط وجود ندارد.", Toast.LENGTH_LONG).show()
                        } else {
                            coroutineScope.launch {
                                GoogleSignInManager.getIdToken(activity)
                                    .fold(
                                        onSuccess = { idToken ->
                                            studentViewModel.signInWithGoogle(idToken) { ok, msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                if (ok) dismiss()
                                            }
                                        },
                                        onFailure = {
                                            Toast.makeText(context, "ورود با گوگل ناموفق بود.", Toast.LENGTH_LONG).show()
                                        }
                                    )
                            }
                        }
                    },
                    onForgotPassword = { email ->
                        studentViewModel.sendPasswordResetEmail(email) { ok, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    onSignOut = {
                        studentViewModel.signOutUser()
                        Toast.makeText(context, "از حساب خارج شدید.", Toast.LENGTH_SHORT).show()
                        dismiss()
                    },
                    onDeleteAccount = {
                        studentViewModel.deleteUserAccount {
                            Toast.makeText(context, "حساب و داده‌ها حذف شدند.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenUpgrade = { onUpdateDialogState(AppDialogState.Upgrade) },
                    onSyncNow = { studentViewModel.syncWithBackendNow { _, _ -> } },
                    onDismiss = dismiss
                )
            }
        }

        is AppDialogState.Upgrade -> {
            SubscriptionUpgradeDialog(
                userAccount = userAccount,
                onUpgradeTier = { tier ->
                    studentViewModel.upgradeSubscriptionTier(tier) { success, msg ->
                        if (success) {
                            Toast.makeText(
                                context,
                                "اشتراک شما به ${tier.titleFa} ارتقا یافت! ★",
                                Toast.LENGTH_LONG
                            ).show()
                            dismiss()
                        } else {
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onApplyPromoCode = { code ->
                    studentViewModel.applyPromoCode(code) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        if (success) dismiss()
                    }
                },
                onDismiss = dismiss
            )
        }

        is AppDialogState.BackupRestore -> {
            val lastTimestamp by studentViewModel.lastSavedTimestamp.collectAsStateWithLifecycle()
            BackupRestoreDialog(
                lastSavedTimestamp = lastTimestamp,
                onExportJson = {
                    studentViewModel.exportDatabaseBackup()
                },
                onImportJson = { json, callback ->
                    studentViewModel.importDatabaseBackup(json, callback)
                },
                onRestoreFromCloud = { callback ->
                    studentViewModel.restoreAllDataFromCloud(callback)
                },
                onDismiss = dismiss
            )
        }

        is AppDialogState.Profile -> {
            EditProfileDialog(
                profile = profile,
                onDismiss = dismiss,
                onSave = { name, stdId, uni, maj, year, sem, passed, active ->
                    studentViewModel.updateFullProfile(name, stdId, uni, maj, year, sem, passed, active)
                    dismiss()
                    Toast.makeText(context, "مشخصات دانشجویی با موفقیت ذخیره شد ✨", Toast.LENGTH_SHORT).show()
                },
                onReopenOnboarding = {
                    dismiss()
                    onOpenOnboardingWizard()
                },
                onOpenPastSemesters = {
                    onUpdateDialogState(AppDialogState.PastSemesters)
                }
            )
        }

        is AppDialogState.PastSemesters -> {
            PastSemestersHistoryDialog(
                profile = profile,
                curriculumCourses = curriculumCourses,
                onDismiss = dismiss,
                onSaveSummaryHistory = { selectedSemester, totalPassedCredits, overallGpa, summaries ->
                    studentViewModel.savePastSemesterHistory(selectedSemester, totalPassedCredits, overallGpa, summaries)
                    dismiss()
                    Toast.makeText(context, "سوابق تحصیلی و ترم جدید با موفقیت همگام‌سازی شد 🚀", Toast.LENGTH_LONG).show()
                }
            )
        }

        is AppDialogState.Notifications -> {
            NotificationDialog(
                notifications = notifications,
                onDismiss = dismiss,
                onTestAlarm = {
                    studentViewModel.playNotificationTone(true)
                    Toast.makeText(context, "آلارم صوتی تست شد 🔔", Toast.LENGTH_SHORT).show()
                },
                onSendDeviceTestNotif = onSendDeviceTestNotif,
                onClearAll = {
                    studentViewModel.clearNotifications()
                }
            )
        }

        is AppDialogState.ApkInfo -> {
            AndroidApkDialog(
                onDismiss = dismiss
            )
        }

        is AppDialogState.AddCourse -> {
            AddEditCourseDialog(
                initialCourse = null,
                existingCourses = courses,
                onDismiss = dismiss,
                onSave = { newCourse ->
                    studentViewModel.saveCourse(newCourse)
                    dismiss()
                },
                onSaveWithSessions = { course, sessions ->
                    studentViewModel.saveCourse(course, sessions)
                    dismiss()
                }
            )
        }

        is AppDialogState.EditCourse -> {
            val allCoursesWithSessions by studentViewModel.coursesWithSessions.collectAsStateWithLifecycle()
            val courseSessions = allCoursesWithSessions.firstOrNull { it.course.id == dialogState.course.id }?.sessions ?: emptyList()
            AddEditCourseDialog(
                initialCourse = dialogState.course,
                initialSessions = courseSessions,
                existingCourses = courses,
                onDismiss = dismiss,
                onSave = { updated ->
                    studentViewModel.saveCourse(updated)
                    dismiss()
                },
                onSaveWithSessions = { course, sessions ->
                    studentViewModel.saveCourse(course, sessions)
                    dismiss()
                },
                onDelete = { id ->
                    studentViewModel.deleteCourse(id)
                    dismiss()
                }
            )
        }

        is AppDialogState.AddTask -> {
            val availableCourseNames = courses.map { it.name }.distinct().ifEmpty { listOf("عمومی") }
            AddTaskDialog(
                courseNames = availableCourseNames,
                onDismiss = dismiss,
                onSave = { title, cName, date ->
                    onRequestNotificationPermission()
                    studentViewModel.saveTask(title, cName, date)
                    dismiss()
                }
            )
        }

        is AppDialogState.CommandCenter -> {
            SpotlightSearchDialog(
                searchResults = globalSearchResults,
                searchQuery = searchQuery,
                onSearchQueryChange = { studentViewModel.setSearchQuery(it) },
                onDismiss = {
                    studentViewModel.setSearchQuery("")
                    dismiss()
                },
                onSelectCourse = { courseId ->
                    val selected = courses.firstOrNull { it.id == courseId }
                    if (selected != null) {
                        onUpdateDialogState(AppDialogState.CourseWorkspace(selected.id))
                    } else {
                        studentViewModel.selectTab(AppTab.SCHEDULE)
                        dismiss()
                    }
                },
                onSelectCurriculumCourse = {
                    studentViewModel.selectTab(AppTab.CURRICULUM)
                    dismiss()
                },
                onNavigateToTasks = {
                    studentViewModel.selectTab(AppTab.TASKS)
                    dismiss()
                },
                onNavigateToExams = {
                    studentViewModel.selectTab(AppTab.EXAMS)
                    dismiss()
                },
                onNavigateToNotes = {
                    studentViewModel.selectTab(AppTab.POMODORO)
                    dismiss()
                }
            )
        }

        is AppDialogState.Copilot -> {
            AcademicCopilotDialog(
                profile = profile,
                courses = courses,
                attendanceList = attendance,
                grades = grades,
                tasks = tasks,
                exams = exams,
                onDismiss = dismiss
            )
        }

        is AppDialogState.OcrImport -> {
            OcrScheduleImportDialog(
                onDismiss = dismiss,
                onConfirmImport = { drafts ->
                    studentViewModel.importParsedCourses(drafts, clearExisting = false)
                    dismiss()
                    Toast.makeText(context, "${drafts.size} درس جدید با موفقیت به برنامه هفتگی اضافه شد 🚀", Toast.LENGTH_LONG).show()
                }
            )
        }

        is AppDialogState.CourseWorkspace -> {
            val courseId = dialogState.courseId
            val activeCourse = courses.find { it.id == courseId }
            if (activeCourse != null) {
                val allCoursesWithSessions by studentViewModel.coursesWithSessions.collectAsStateWithLifecycle()
                val courseSessions = allCoursesWithSessions.firstOrNull { it.course.id == activeCourse.id }?.sessions ?: emptyList()
                val courseAttendance = attendance.find { it.courseName == activeCourse.name }
                val courseGrade = grades.find { it.courseName == activeCourse.name }
                val courseTasks = tasks.filter { it.courseName == activeCourse.name }
                val courseExam = exams.find { it.courseName == activeCourse.name }

                CourseWorkspaceDialog(
                    course = activeCourse,
                    sessions = courseSessions,
                    attendance = courseAttendance,
                    grade = courseGrade,
                    tasks = courseTasks,
                    exam = courseExam,
                    onDismiss = dismiss,
                    onEditCourse = { updated ->
                        studentViewModel.saveCourse(updated)
                    },
                    onDeleteCourse = { id ->
                        studentViewModel.deleteCourse(id)
                        dismiss()
                    },
                    onChangeAttendance = { delta ->
                        studentViewModel.changeAttendance(activeCourse.name, delta)
                    },
                    onToggleTask = { task ->
                        studentViewModel.toggleTask(task)
                    },
                    onAddTask = { title, date ->
                        onRequestNotificationPermission()
                        studentViewModel.saveTask(title, activeCourse.name, date)
                    },
                    onDeleteTask = { task ->
                        studentViewModel.deleteTask(task)
                    },
                    onStartFocus = {
                        studentViewModel.selectTab(AppTab.POMODORO)
                        dismiss()
                    }
                )
            }
        }

        is AppDialogState.ExportShare -> {
            PremiumShareExportModal(
                payload = dialogState.payload,
                onDismiss = dismiss
            )
        }

        is AppDialogState.PrivacyPolicy -> {
            PrivacyPolicyDialog(
                onDismiss = dismiss
            )
        }

        is AppDialogState.SupportTickets -> {
            SupportTicketDialog(
                userAccount = userAccount,
                onDismiss = dismiss
            )
        }

        is AppDialogState.None -> { /* No active dialog */ }
    }
}
}
}


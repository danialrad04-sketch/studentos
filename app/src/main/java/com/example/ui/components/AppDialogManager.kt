package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    var parents by remember { mutableStateOf<List<AppDialogState>>(emptyList()) }
    var settingsSection by rememberSaveable { mutableStateOf("home") }
    LaunchedEffect(dialogState) {
        if (dialogState == AppDialogState.None) { parents = emptyList(); settingsSection = "home" }
    }
    val isSaving by studentViewModel.isSavingRecord.collectAsStateWithLifecycle()
    val dismiss = {
        val parent = parents.lastOrNull() ?: AppDialogState.None
        parents = parents.dropLast(1)
        onUpdateDialogState(parent)
    }
    fun openChild(state: AppDialogState) {
        parents = parents + dialogState
        onUpdateDialogState(state)
    }
    fun closeAll() { parents = emptyList(); onUpdateDialogState(AppDialogState.None) }


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
                initialSection = settingsSection,
                onSectionChanged = { settingsSection = it },
                themeMode = themeMode,
                onSelectThemeMode = { studentViewModel.setThemeMode(it) },
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = { studentViewModel.setNotificationsEnabled(it); if (it) onRequestNotificationPermission() },
                onOpenEditProfile = { openChild(AppDialogState.Profile) },
                onDismiss = dismiss,
                userAccount = userAccount,
                onOpenAuth = { openChild(AppDialogState.Auth) },
                onOpenBackupRestore = { openChild(AppDialogState.BackupRestore) },
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
                    openChild(AppDialogState.PastSemesters)
                },
                onOpenApkInfo = {
                    openChild(AppDialogState.ApkInfo)
                },
                onTestNotification = onSendDeviceTestNotif,
                onOpenPrivacyPolicy = { openChild(AppDialogState.PrivacyPolicy) },
                onOpenSupportTickets = { openChild(AppDialogState.SupportTickets) }
            )
        }

        is AppDialogState.Auth -> {
            AuthAccountDialog(
                userAccount = userAccount,
                onSignInEmail = { email, password, result -> studentViewModel.signInWithEmail(email, password, result) },
                onSignUpEmail = { name, email, password, result -> studentViewModel.signUpWithEmail(name, email, password, result) },
                onGoogleSignIn = { result -> studentViewModel.signInWithGoogle(activityContext = context, onResult = result) },
                onForgotPassword = { email ->
                    studentViewModel.sendPasswordResetEmail(email) { _, message -> Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
                },
                onSignOut = { studentViewModel.signOutUser(); closeAll() },
                onDeleteAccount = { studentViewModel.deleteUserAccount { closeAll() } },
                onOpenUpgrade = { openChild(AppDialogState.Upgrade) },
                onOpenProfile = { openChild(AppDialogState.Profile) },
                onOpenBackupRestore = { openChild(AppDialogState.BackupRestore) },
                onOpenPrivacyPolicy = { openChild(AppDialogState.PrivacyPolicy) },
                onOpenSupport = { openChild(AppDialogState.SupportTickets) },
                onSyncNow = { result -> studentViewModel.triggerManualCloudSync(result) },
                onDismiss = dismiss
            )
        }

        is AppDialogState.Upgrade -> {
            SubscriptionUpgradeRoute(
                userAccount = userAccount,
                onVerified = studentViewModel::applyVerifiedSubscription,
                onSignIn = { openChild(AppDialogState.Auth) },
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
                    studentViewModel.saveCourse(newCourse, onSaved = dismiss)
                },
                onSaveWithSessions = { course, sessions ->
                    studentViewModel.saveCourse(course, sessions, onSaved = dismiss)
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
                    studentViewModel.saveCourse(updated, onSaved = dismiss)
                },
                onSaveWithSessions = { course, sessions ->
                    studentViewModel.saveCourse(course, sessions, onSaved = dismiss)
                },
                onDelete = { id ->
                    studentViewModel.deleteCourse(id)
                    dismiss()
                }
            )
        }

        is AppDialogState.AddTask -> {
            val availableCourseNames = (listOf("عمومی") + courses.map { it.name }).distinct()
            AddTaskDialog(
                courseNames = availableCourseNames,
                isSaving = isSaving,
                onDismiss = dismiss,
                onSave = { title, cName, date ->
                    onRequestNotificationPermission()
                    studentViewModel.saveTask(title, cName, date, onSaved = dismiss)
                }
            )
        }

        is AppDialogState.EditTask -> {
            AddTaskDialog(courseNames = (listOf("عمومی") + courses.map { it.name } + dialogState.task.courseName).distinct(),
                initialTask = dialogState.task, isSaving = isSaving, onDismiss = dismiss,
                onSave = { title, name, date -> studentViewModel.editTask(dialogState.task, title, name, date, dismiss) })
        }
        is AppDialogState.ExamEditor -> {
            ExamEditorDialog(courses, dialogState.courseId, dismiss,
                { course, date, time, location -> studentViewModel.saveExamDetails(course, date, time, location, dismiss) }, isSaving)
        }
        is AppDialogState.GradeEditor -> {
            val course = courses.find { it.id == dialogState.courseId }
            if (course != null) RecordGradeDialog(course, grades.find { it.courseId == course.id }, dismiss,
                { grade, mid, fin -> studentViewModel.updateGrade(grade, mid, fin, dismiss) }, isSaving)
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
            val scheduledCourses by studentViewModel.coursesWithSessions.collectAsStateWithLifecycle()
            OcrScheduleImportDialog(
                existingCourses = scheduledCourses,
                isSaving = isSaving,
                dismissOnConfirm = false,
                onDismiss = dismiss,
                onConfirmImport = { drafts ->
                    studentViewModel.importParsedCourses(drafts, clearExisting = false, onSaved = dismiss)
                }
            )
        }

        is AppDialogState.CourseWorkspace -> {
            val courseId = dialogState.courseId
            val activeCourse = courses.find { it.id == courseId }
            if (activeCourse != null) {
                val allCoursesWithSessions by studentViewModel.coursesWithSessions.collectAsStateWithLifecycle()
                val courseSessions = allCoursesWithSessions.firstOrNull { it.course.id == activeCourse.id }?.sessions ?: emptyList()
                val courseAttendance = attendance.find { it.courseId == activeCourse.id || (it.courseId.isBlank() && it.courseName == activeCourse.name) }
                val courseGrade = grades.find { it.courseId == activeCourse.id || (it.courseId.isBlank() && it.courseName == activeCourse.name) }
                val courseTasks = tasks.filter { it.courseId == activeCourse.id || (it.courseId.isBlank() && it.courseName == activeCourse.name) }
                val courseExam = exams.find { it.courseName == activeCourse.name }

                CourseWorkspaceDialogV2(
                    course = activeCourse,
                    sessions = courseSessions,
                    attendance = courseAttendance,
                    grade = courseGrade,
                    tasks = courseTasks,
                    exam = courseExam,
                    onDismiss = dismiss,
                    onEditCourse = { updated ->
                        openChild(AppDialogState.EditCourse(updated))
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
                    onRecordGrade = { openChild(AppDialogState.GradeEditor(activeCourse.id)) },
                    onEditExam = { openChild(AppDialogState.ExamEditor(activeCourse.id)) },
                    onStartFocus = {
                        if (!studentViewModel.focusSession.value.isRunning) {
                            studentViewModel.setFocusDuration(25, activeCourse.name)
                            studentViewModel.togglePomodoro()
                        }
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

package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.domain.model.AcademicRisk
import com.example.domain.model.SemesterPlan
import com.example.domain.model.StudySessionRecommendation
import com.example.domain.model.StudentGamificationProfile
import com.example.domain.model.WeeklyAcademicWorkload
import com.example.ui.StudentViewModel
import com.example.ui.components.export.ExportSourcePayload
import com.example.ui.models.AcademicProgressUiState
import com.example.ui.models.AppTab
import com.example.ui.models.CurriculumMatchUiState
import com.example.ui.models.ExamItem

@Composable
fun MainTabContent(
    selectedTab: AppTab,
    courses: List<CourseEntity>,
    coursesWithSessions: List<CourseWithSessions> = emptyList(),
    attendance: List<AttendanceEntity>,
    tasks: List<TaskEntity>,
    grades: List<GradeEntity>,
    exams: List<ExamItem>,
    profile: StudentProfileEntity,
    gpaFormatted: String,
    currentTermGpa: Double,
    totalUnits: Int,
    totalCurriculumUnits: Int,
    academicProgressState: AcademicProgressUiState,
    curriculumMatchState: CurriculumMatchUiState,
    academicRisks: List<AcademicRisk>,
    weeklyWorkload: WeeklyAcademicWorkload,
    candidateSemesterPlans: List<SemesterPlan>,
    studyRecommendations: List<StudySessionRecommendation> = emptyList(),
    primaryPriority: com.example.domain.model.AcademicPriorityItem? = null,
    planningCollisions: List<com.example.domain.model.AcademicPlanningCollision> = emptyList(),
    acceptedStudyPlanIds: Set<String> = emptySet(),
    onAcceptStudyPlan: (StudySessionRecommendation) -> Unit = {},
    pomodoroSeconds: Int,
    isPomodoroRunning: Boolean,
    gamificationProfile: StudentGamificationProfile,
    studentViewModel: StudentViewModel,
    context: Context,
    haptic: HapticFeedback,
    onOpenCourseWorkspace: (CourseEntity) -> Unit,
    onOpenCommandCenter: () -> Unit,
    onOpenAddTask: () -> Unit,
    onEditTask: ((TaskEntity) -> Unit)? = null,
    onOpenExamEditor: ((String?) -> Unit)? = null,
    onOpenGradeEditor: ((String) -> Unit)? = null,
    onScheduleExamReminder: ((ExamItem, Int) -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    onImportPersonalCurriculum: (() -> Unit)? = null,
    onOpenAddCourse: () -> Unit,
    onEditCourse: (CourseEntity) -> Unit,
    onOpenOcrImport: () -> Unit,
    onOpenZeroSetup: () -> Unit,
    onOpenPastSemestersDialog: () -> Unit,
    onExportPayload: (ExportSourcePayload) -> Unit,
    onOpenAppTour: (() -> Unit)? = null,
    onRequestNotificationPermission: () -> Unit = {},
    onOpenPremium: () -> Unit = {}
) {
    AnimatedContent(
        targetState = selectedTab,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "tab_content_transition"
    ) { currentTab ->
        when (currentTab) {
            AppTab.DASHBOARD -> {
                ModernBentoDashboard(
                    courses = courses,
                    coursesWithSessions = coursesWithSessions,
                    attendanceList = attendance,
                    tasks = tasks,
                    exams = exams,
                    grades = grades,
                    passedUnits = profile.passedUnits,
                    gpa = gpaFormatted,
                    targetGpa = profile.declaredGpa,
                    totalRequiredCredits = totalCurriculumUnits,
                    academicProgressState = academicProgressState,
                    academicRisks = academicRisks,
                    weeklyWorkload = weeklyWorkload,
                    studyRecommendations = studyRecommendations,
                    primaryPriority = primaryPriority,
                    acceptedStudyPlanIds = acceptedStudyPlanIds,
                    onAcceptStudyPlan = onAcceptStudyPlan,
                    onStartStudyPlan = studentViewModel::startStudyRecommendation,
                    pomodoroSeconds = pomodoroSeconds,
                    isPomodoroRunning = isPomodoroRunning,
                    onTogglePomodoro = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        studentViewModel.togglePomodoro()
                    },
                    onNavigateTab = { targetTab ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        studentViewModel.selectTab(targetTab)
                    },
                    gamificationProfile = gamificationProfile,
                    onOpenCourseWorkspace = onOpenCourseWorkspace,
                    onOpenCommandCenter = onOpenCommandCenter,
                    onOpenCopilot = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        studentViewModel.selectTab(AppTab.COPILOT)
                    },
                    onQuickAddTask = onOpenAddTask,
                    onOpenOcrImport = onOpenOcrImport,
                    onOpenAppTour = onOpenAppTour
                )
            }
            AppTab.COPILOT -> {
                val currCourses by studentViewModel.curriculumCourses.collectAsStateWithLifecycle()
                val userAccount by studentViewModel.currentUser.collectAsStateWithLifecycle()
                AcademicCopilotScreen(
                    profile = profile,
                    courses = courses,
                    attendanceList = attendance,
                    grades = grades,
                    tasks = tasks,
                    exams = exams,
                    coursesWithSessions = coursesWithSessions,
                    curriculumCourses = currCourses,
                    primaryPriority = primaryPriority,
                    planningCollisions = planningCollisions,
                    onNavigateTab = { targetTab ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        studentViewModel.selectTab(targetTab)
                    },
                    onExecuteAction = { payload ->
                        studentViewModel.executeCopilotPayload(payload)
                    },
                    onOpenPastSemestersDialog = onOpenPastSemestersDialog,
                    userAccount = userAccount,
                    onOpenPremium = onOpenPremium,
                    onSubscriptionLoaded = studentViewModel::applyVerifiedSubscription
                )
            }
            AppTab.ACADEMIC_INTELLIGENCE -> {
                val history by studentViewModel.allSemesters.collectAsStateWithLifecycle()
                AcademicIntelligenceScreen(
                    semesterHistory = history,
                    gpa = currentTermGpa,
                    passedUnits = profile.passedUnits,
                    totalRequiredCredits = totalCurriculumUnits,
                    courses = courses,
                    attendance = attendance,
                    tasks = tasks,
                    grades = grades,
                    risks = academicRisks,
                    onOpenTab = { targetTab ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        studentViewModel.selectTab(targetTab)
                    }
                )
            }
            AppTab.PASSPORT -> {
                AcademicPassportScreen(
                    profile = profile,
                    progressState = academicProgressState,
                    matchState = curriculumMatchState,
                    onNavigateToCurriculum = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        studentViewModel.selectTab(AppTab.CURRICULUM)
                    },
                    onOpenExport = {
                        val progress = when (val s = academicProgressState) {
                            is AcademicProgressUiState.Ready -> s.progress
                            is AcademicProgressUiState.Partial -> s.progress
                            else -> null
                        }
                        val currTitle = when (val cState = curriculumMatchState) {
                            is CurriculumMatchUiState.Ready -> cState.output.version.title
                            else -> "چارت مصوب مهندسی و علوم پایه"
                        }
                        onExportPayload(
                            ExportSourcePayload.Passport(
                                profile = profile,
                                progress = progress,
                                curriculumTitle = currTitle
                            )
                        )
                    }
                )
            }
            AppTab.SCHEDULE -> {
                WeeklyScheduleScreen(
                    courses = courses,
                    coursesWithSessions = coursesWithSessions,
                    onAddCourse = onOpenAddCourse,
                    onEditCourse = onEditCourse,
                    onOpenCourseWorkspace = onOpenCourseWorkspace,
                    onDeleteCourse = { id ->
                        studentViewModel.deleteCourse(id)
                    },
                    onOpenZeroSetup = onOpenZeroSetup
                )
            }
            AppTab.ATTENDANCE -> {
                AttendanceRadarScreen(
                    attendanceList = attendance,
                    courses = courses,
                    onChangeAttendance = { name, delta ->
                        studentViewModel.changeAttendance(name, delta)
                    },
                    onSetAttendance = { name, count ->
                        studentViewModel.setAttendanceCount(name, count)
                    },
                    onEvaluateAlerts = {
                        val dangerCount = attendance.count { it.absentCount >= it.maxAllowed && it.maxAllowed > 0 }
                        if (dangerCount > 0) {
                            studentViewModel.addNotification(
                                "🚨 هشدار بحرانی سقف غیبت",
                                "شما در $dangerCount درس در آستانه حذف آموزشی قرار دارید!",
                                isDanger = true
                            )
                            Toast.makeText(context, "هشدار سقف غیبت صادر شد!", Toast.LENGTH_SHORT).show()
                        } else {
                            studentViewModel.addNotification(
                                "✅ وضعیت غیبت‌ها مجاز است",
                                "غیبت‌های ثبت‌شده در کلیه دروس کمتر از سقف قانونی ۳/۱۶ می‌باشد."
                            )
                            Toast.makeText(context, "وضعیت غیبت‌ها مجاز است", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenCourseWorkspace = onOpenCourseWorkspace
                )
            }
            AppTab.TASKS -> {
                TasksScreen(
                    tasks = tasks,
                    courses = courses,
                    exams = exams,
                    planningCollisions = planningCollisions,
                    onAddTask = onOpenAddTask,
                    onEditTask = onEditTask,
                    onToggleTask = {
                        studentViewModel.toggleTask(it)
                    },
                    onDeleteTask = { studentViewModel.deleteTask(it) },
                    onOpenCourseWorkspace = onOpenCourseWorkspace
                )
            }
            AppTab.EXAMS -> {
                val reminders by studentViewModel.examReminderIds.collectAsStateWithLifecycle()
                ExamsScreen(exams = exams, tasks = tasks, onSetReminder = { studentViewModel.scheduleExamReminder(it) },
                    onAddExam = { if (courses.isEmpty()) onOpenAddCourse() else onOpenExamEditor?.invoke(null) },
                    onEditExam = { onOpenExamEditor?.invoke(it.id.removePrefix("exam_")) },
                    onScheduleReminder = onScheduleExamReminder, reminderIds = reminders,
                    onCancelReminder = studentViewModel::cancelExamReminder)
            }
            AppTab.GRADES -> {
                GradesScreen(
                    grades = grades,
                    courses = courses,
                    onRecordGrade = onOpenGradeEditor,
                    onAddCourse = onOpenAddCourse,
                    onUpdateGrade = { target, mid, fin ->
                        studentViewModel.updateGrade(target, mid, fin)
                    },
                    onOpenExport = {
                        onExportPayload(
                            ExportSourcePayload.Grades(
                                profile = profile,
                                grades = grades,
                                termGpa = currentTermGpa,
                                totalUnits = totalUnits
                            )
                        )
                    }
                )
            }
            AppTab.SEMESTER_PLANNER -> {
                val savedPlan by studentViewModel.selectedSemesterPlan.collectAsStateWithLifecycle()
                SemesterPlannerScreen(
                    matchState = curriculumMatchState,
                    candidatePlans = candidateSemesterPlans,
                    savedPlan = savedPlan,
                    onOpenProfile = onOpenProfile,
                    onPlanSelected = studentViewModel::selectSemesterPlan
                )
            }
            AppTab.CURRICULUM -> {
                CurriculumScreen(
                    matchState = curriculumMatchState,
                    onOpenProfile = onOpenProfile,
                    onImportPersonalCurriculum = onImportPersonalCurriculum
                )
            }
            AppTab.POMODORO -> {
                val session by studentViewModel.focusSession.collectAsStateWithLifecycle()
                PomodoroAndNotesScreen(
                    secondsRemaining = pomodoroSeconds,
                    durationSeconds = session.durationSeconds,
                    focusLabel = session.label,
                    onDurationSelected = { studentViewModel.setFocusDuration(it) },
                    studyRecommendations = studyRecommendations,
                    isRunning = isPomodoroRunning,
                    onTogglePomodoro = {
                        studentViewModel.togglePomodoro()
                    },
                    onResetPomodoro = {
                        studentViewModel.resetPomodoro()
                    },
                    notes = profile.notes,
                    onSaveNotes = { newNotes ->
                        studentViewModel.saveNotes(newNotes)
                    }
                )
            }
            AppTab.GAMIFICATION -> {
                StudentGamificationHub(
                    profile = gamificationProfile
                )
            }
            AppTab.HISTORY -> {
                SemesterHistoryScreen(
                    studentViewModel = studentViewModel
                )
            }
        }
    }
}

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
    pomodoroSeconds: Int,
    isPomodoroRunning: Boolean,
    gamificationProfile: StudentGamificationProfile,
    studentViewModel: StudentViewModel,
    context: Context,
    haptic: HapticFeedback,
    onOpenCourseWorkspace: (CourseEntity) -> Unit,
    onOpenCommandCenter: () -> Unit,
    onOpenAddTask: () -> Unit,
    onOpenAddCourse: () -> Unit,
    onEditCourse: (CourseEntity) -> Unit,
    onOpenOcrImport: () -> Unit,
    onOpenZeroSetup: () -> Unit,
    onOpenPastSemestersDialog: () -> Unit,
    onExportPayload: (ExportSourcePayload) -> Unit,
    onOpenAppTour: (() -> Unit)? = null,
    onRequestNotificationPermission: () -> Unit = {}
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
                    grades = grades,
                    passedUnits = profile.passedUnits,
                    gpa = gpaFormatted,
                    totalRequiredCredits = totalCurriculumUnits,
                    academicProgressState = academicProgressState,
                    academicRisks = academicRisks,
                    weeklyWorkload = weeklyWorkload,
                    studyRecommendations = studyRecommendations,
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
                val customApiKey by studentViewModel.customGeminiApiKey.collectAsStateWithLifecycle()
                AcademicCopilotScreen(
                    profile = profile,
                    courses = courses,
                    attendanceList = attendance,
                    grades = grades,
                    tasks = tasks,
                    exams = exams,
                    coursesWithSessions = coursesWithSessions,
                    curriculumCourses = currCourses,
                    onNavigateTab = { targetTab ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        studentViewModel.selectTab(targetTab)
                    },
                    onExecuteAction = { payload ->
                        studentViewModel.executeCopilotPayload(payload)
                        Toast.makeText(context, "تغییرات با موفقیت اعمال شد ✨", Toast.LENGTH_SHORT).show()
                    },
                    onOpenPastSemestersDialog = onOpenPastSemestersDialog,
                    customApiKey = customApiKey,
                    onSaveCustomApiKey = { studentViewModel.setCustomGeminiApiKey(it) }
                )
            }
            AppTab.ACADEMIC_INTELLIGENCE -> {
                AcademicIntelligenceScreen(
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
                    onAddTask = onOpenAddTask,
                    onToggleTask = {
                        studentViewModel.toggleTask(it)
                    },
                    onDeleteTask = { studentViewModel.deleteTask(it) },
                    onOpenCourseWorkspace = onOpenCourseWorkspace
                )
            }
            AppTab.EXAMS -> {
                ExamsScreen(
                    exams = exams,
                    onSetReminder = { exam ->
                        onRequestNotificationPermission()
                        studentViewModel.addNotification(
                            "🎯 یادآور امتحان ${exam.courseName}",
                            "آزمون در تاریخ ${exam.solarDate} ساعت ${exam.time} در ${exam.location} برگزار خواهد شد."
                        )
                        studentViewModel.playNotificationTone(false)
                        Toast.makeText(context, "یادآور آزمون ${exam.courseName} کوک شد", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            AppTab.GRADES -> {
                GradesScreen(
                    grades = grades,
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
                SemesterPlannerScreen(
                    matchState = curriculumMatchState,
                    candidatePlans = candidateSemesterPlans,
                    onPlanSelected = { plan ->
                        Toast.makeText(context, "سناریوی ${plan.name} انتخاب شد", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            AppTab.CURRICULUM -> {
                CurriculumScreen(
                    matchState = curriculumMatchState,
                    curriculumList = studentViewModel.curriculumList
                )
            }
            AppTab.POMODORO -> {
                PomodoroAndNotesScreen(
                    secondsRemaining = pomodoroSeconds,
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
                        Toast.makeText(context, "یادداشت‌ها و فرمول‌ها ذخیره شدند", Toast.LENGTH_SHORT).show()
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

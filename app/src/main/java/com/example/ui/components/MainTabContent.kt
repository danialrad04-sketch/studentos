package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalLayoutApi::class)
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
    dailyStudyGoalMinutes: Int = 120,
    studiedTodayMinutes: Int = 0,
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
    val learning by studentViewModel.learningWorkspace.collectAsStateWithLifecycle()
    val adaptivePlan by studentViewModel.adaptiveLearningPlan.collectAsStateWithLifecycle()
    val learningOwner by studentViewModel.workspaceGeneration.collectAsStateWithLifecycle()
    val calendarLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/calendar"),
        studentViewModel::writePendingCalendar
    )
    AnimatedContent(
        targetState = selectedTab,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "tab_content_transition"
    ) { currentTab ->
        when (currentTab) {
            AppTab.DASHBOARD -> {
                val targetGpa by studentViewModel.targetGpaGoal.collectAsStateWithLifecycle()
                ModernBentoDashboard(
                    learningSummary = {
                        LearningHomeSummary(learning, adaptivePlan, courses,
                            studentViewModel::openStudyPlanner, onOpenCourseWorkspace)
                    },
                    courses = courses,
                    coursesWithSessions = coursesWithSessions,
                    attendanceList = attendance,
                    tasks = tasks,
                    exams = exams,
                    grades = grades,
                    passedUnits = profile.passedUnits,
                    gpa = gpaFormatted,
                    declaredGpa = profile.declaredGpa,
                    targetGpa = targetGpa,
                    totalRequiredCredits = totalCurriculumUnits,
                    academicProgressState = academicProgressState,
                    academicRisks = academicRisks,
                    weeklyWorkload = weeklyWorkload,
                    studyRecommendations = studyRecommendations,
                    primaryPriority = primaryPriority,
                    acceptedStudyPlanIds = acceptedStudyPlanIds,
                    dailyStudyGoalMinutes = dailyStudyGoalMinutes,
                    studiedTodayMinutes = studiedTodayMinutes,
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
            AppTab.AGENDA -> {
                val busy by studentViewModel.calendarExportBusy.collectAsStateWithLifecycle()
                StudentAgendaScreen(coursesWithSessions,tasks,exams,
                    onOpenTask={task->onEditTask?.invoke(task) ?: studentViewModel.selectTab(AppTab.TASKS)},
                    onOpenCourse={id->courses.find{it.id==id}?.let(onOpenCourseWorkspace)},
                    onOpenExams={studentViewModel.selectTab(AppTab.EXAMS)},onAddTask=onOpenAddTask,
                    onExport={week->studentViewModel.prepareCalendarExport(week){calendarLauncher.launch(it)}},exportBusy=busy)
            }
            AppTab.NOTES -> {
                val notes by studentViewModel.studyNotes.collectAsStateWithLifecycle()
                val noteCourses by studentViewModel.notebookCourses.collectAsStateWithLifecycle()
                val generation by studentViewModel.workspaceGeneration.collectAsStateWithLifecycle()
                val user by studentViewModel.currentUser.collectAsStateWithLifecycle()
                StudyNotesScreen(notes,noteCourses,user.uid+":"+generation,
                    studentViewModel::saveStudyNote,studentViewModel::archiveStudyNote)
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
                    onExecuteAction = { payload, completed ->
                        studentViewModel.executeCopilotPayload(payload, completed)
                    },
                    onOpenPastSemestersDialog = onOpenPastSemestersDialog,
                    userAccount = userAccount,
                    onOpenPremium = onOpenPremium,
                    onSubscriptionLoaded = studentViewModel::applyVerifiedSubscription,
                    studyHistory = studentViewModel.studyHistory.collectAsStateWithLifecycle().value,
                    dailyGoalMinutes = dailyStudyGoalMinutes,
                    focusMinutes = studentViewModel.defaultFocusMinutes.collectAsStateWithLifecycle().value
                )
            }
            AppTab.ACADEMIC_INTELLIGENCE -> {
                val history by studentViewModel.allSemesters.collectAsStateWithLifecycle()
                val recordedHistory by studentViewModel.studyHistory.collectAsStateWithLifecycle()
                val recordedDay by studentViewModel.preferencesRepository.studyDate.collectAsStateWithLifecycle()
                AcademicIntelligenceScreen(
                    weeklyReport = {
                        WeeklyLearningReport(learning, recordedHistory, recordedDay, courses.map { it.id }.toSet(), tasks, studentViewModel::openStudyPlanner)
                    },
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
                    onSetLimit = studentViewModel::setAttendanceLimit,
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
                                "غیبت‌های ثبت‌شده از سقف تنظیم‌شدهٔ هر درس کمتر است."
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
                val studyHistory by studentViewModel.studyHistory.collectAsStateWithLifecycle()
                val planner by studentViewModel.learningPanel.collectAsStateWithLifecycle()
                androidx.compose.runtime.key(learningOwner) {
                Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        FilterChip(!planner,{studentViewModel.learningPanel.value=false},label={Text("تمرکز")})
                        FilterChip(planner,{studentViewModel.learningPanel.value=true},label={Text("برنامهٔ مطالعه")})
                    }
                    if(planner) LearningPlannerScreen(learning,adaptivePlan,courses,
                        onRetry=studentViewModel::retryLearning,
                        onSaveGoal={row,done->studentViewModel.learningAction({it.saveGoal(row)},done)},
                        onSaveAvailability={row,done->studentViewModel.learningAction({it.saveAvailability(row)},done)},
                        onComplete={block,minutes,done->studentViewModel.learningAction({it.complete(block,minutes)}){ok->if(ok)studentViewModel.releaseLearningBlock(block);done(ok)}},
                        onUndo={row,done->studentViewModel.learningAction({it.undo(row)},done)},
                        onFocus={block->
                            if(!studentViewModel.focusSession.value.isRunning){
                                studentViewModel.startLearningBlock(block)
                            } else android.widget.Toast.makeText(context,"ابتدا جلسهٔ تمرکز فعلی را تمام یا متوقف کن.",android.widget.Toast.LENGTH_SHORT).show()
                        },activeBlock=session.learningBlock,timerRunning=session.isRunning,
                        onReleaseBlock=studentViewModel::releaseLearningBlock,
                        onReturnToFocus={studentViewModel.learningPanel.value=false})
                    else {
                    if(session.learningBlock!=null) FilledTonalButton(onClick={studentViewModel.learningPanel.value=true},modifier=Modifier.fillMaxWidth()) { Text("ثبت جلسه در برنامهٔ مطالعه") }
                    PomodoroAndNotesScreen(
                    secondsRemaining = pomodoroSeconds,
                    durationSeconds = session.durationSeconds,
                    focusLabel = session.label,
                    onDurationSelected = { studentViewModel.setFocusDuration(it) },
                    studyRecommendations = studyRecommendations,
                    studyHistory = studyHistory,
                    dailyStudyGoalMinutes = dailyStudyGoalMinutes,
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
                }
                }
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

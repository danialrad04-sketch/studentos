package com.example.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.LocalDataBackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.SemesterEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.repository.AppPreferencesRepository
import com.example.data.repository.StudentRepository
import com.example.ui.models.AppTab
import com.example.ui.models.ExamItem
import com.example.ui.models.SemesterCurriculum
import com.example.ui.models.SystemNotification
import com.example.ui.models.ThemeMode
import com.example.ui.models.SyncUiState
import com.example.ui.util.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.example.domain.engine.AcademicGamificationEngine
import com.example.domain.engine.AcademicRiskEngine
import com.example.domain.engine.GlobalSearchEngine
import com.example.domain.engine.SemesterPlannerEngine
import com.example.domain.engine.StudyPlannerEngine
import com.example.domain.engine.WorkloadEngine
import com.example.domain.model.AcademicBadge
import com.example.domain.model.AcademicRisk
import com.example.domain.model.GlobalSearchResult
import com.example.domain.model.SemesterPlan
import com.example.domain.model.SemesterPlanComparison
import com.example.domain.model.StudentGamificationProfile
import com.example.domain.model.StudySessionRecommendation
import com.example.domain.model.WeeklyAcademicWorkload
import com.example.domain.engine.CourseIdentityNormalizer
import com.example.domain.engine.CurriculumMatchOutput
import com.example.domain.engine.CurriculumMatcher
import com.example.domain.engine.CurriculumResolutionResult
import com.example.domain.engine.CurriculumResolver
import com.example.domain.engine.ResolutionFailureReason
import com.example.domain.engine.ResolvableCurriculumCourse
import com.example.domain.engine.ResolvedCurriculumVersion
import com.example.domain.engine.ResolvedMajor
import com.example.domain.engine.ResolvedUniversity
import com.example.domain.engine.StudentCourseAttempt
import com.example.domain.model.AcademicProgress
import com.example.domain.model.CoursePrerequisiteRule
import com.example.domain.model.DataConfidence
import com.example.domain.model.PrerequisiteCondition
import com.example.ui.models.AcademicProgressUiState
import com.example.ui.models.CurriculumMatchUiState
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentViewModel @JvmOverloads constructor(
    private val application: Application,
    injectedRepository: StudentRepository? = null,
    injectedPreferencesRepository: AppPreferencesRepository? = null,
    injectedAuthManager: com.example.data.auth.StudentAuthManager? = null
) : AndroidViewModel(application) {

    private val container = (application as? com.example.StudentApplication)?.container

    private val repository: StudentRepository = injectedRepository
        ?: container?.studentRepository
        ?: run {
            val db = AppDatabase.getDatabase(application, viewModelScope)
            StudentRepository(db.studentDao(), db.curriculumDao(), db)
        }

    val preferencesRepository: AppPreferencesRepository = injectedPreferencesRepository
        ?: container?.preferencesRepository
        ?: AppPreferencesRepository.getInstance(application)

    private val authManager: com.example.data.auth.StudentAuthManager = injectedAuthManager
        ?: container?.authManager
        ?: com.example.data.auth.StudentAuthManager.getInstance(application)

    private var pomodoroJob: Job? = null

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode
    val notificationsEnabled: StateFlow<Boolean> = preferencesRepository.notificationsEnabled
    val customGeminiApiKey: StateFlow<String> = preferencesRepository.customGeminiApiKey
    val lastSavedTimestamp: StateFlow<Long> = preferencesRepository.lastSuccessfulSaveTimestamp
    val currentUser: StateFlow<com.example.domain.model.UserAccount> = authManager.currentUser
    val isAuthInitialized: StateFlow<Boolean> = authManager.isInitialized

    private val _databaseRecoveryWarning = MutableStateFlow(false)
    val databaseRecoveryWarning: StateFlow<Boolean> = _databaseRecoveryWarning.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    init {
        try {
            NotificationHelper.initNotificationChannel(application)
        } catch (_: Throwable) {
        }

        // Safe Profile & Initial Seed handling (Phase 1 Data Persistence & Stability)
        // With one-time legacy SharedPreferences identity migration to Room (Single Source of Truth)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val db = repository.database ?: AppDatabase.getDatabase(application, viewModelScope)
                var p = repository.getProfileSync()

                // Execute one-time legacy identity migration if legacy SharedPreferences keys exist
                val legacyData = preferencesRepository.readAndClearLegacyIdentity()
                if (legacyData != null) {
                    val legacyName = legacyData["name"] as? String ?: ""
                    val legacyStudentId = legacyData["studentId"] as? String ?: ""
                    val legacyUniversity = legacyData["university"] as? String ?: ""
                    val legacyMajor = legacyData["major"] as? String ?: ""
                    val legacyEntryYear = legacyData["entryYear"] as? Int ?: 0
                    val legacySemester = legacyData["currentSemester"] as? Int ?: 0
                    val legacyPassed = legacyData["passedCredits"] as? Int ?: 0
                    val legacyGpa = legacyData["declaredGpa"] as? Double ?: 0.0

                    val target = p ?: StudentProfileEntity(id = 1)
                    val merged = target.copy(
                        name = if (target.name.isBlank() || target.name == "دانشجو") legacyName.ifBlank { target.name } else target.name,
                        studentId = if (target.studentId.isBlank()) legacyStudentId else target.studentId,
                        university = if (target.university.isBlank() || target.university == "دانشگاه") legacyUniversity.ifBlank { target.university } else target.university,
                        major = if (target.major.isBlank() || target.major == "مهندسی") legacyMajor.ifBlank { target.major } else target.major,
                        entryYear = if (target.entryYear <= 0 || target.entryYear == 1403) (if (legacyEntryYear > 0) legacyEntryYear else target.entryYear) else target.entryYear,
                        currentSemester = if (target.currentSemester <= 0 || target.currentSemester == 1) (if (legacySemester > 0) legacySemester else target.currentSemester) else target.currentSemester,
                        passedUnits = if (target.passedUnits <= 0) (if (legacyPassed > 0) legacyPassed else target.passedUnits) else target.passedUnits,
                        declaredPassedCredits = if ((target.declaredPassedCredits ?: 0) <= 0) (if (legacyPassed > 0) legacyPassed else target.declaredPassedCredits) else target.declaredPassedCredits,
                        declaredGpa = if (target.declaredGpa == null || target.declaredGpa == 0.0) (if (legacyGpa > 0.0) legacyGpa else target.declaredGpa) else target.declaredGpa,
                        isOnboardingCompleted = target.isOnboardingCompleted || preferencesRepository.isOnboardingCompleted.value || legacyName.isNotBlank(),
                        updatedAt = System.currentTimeMillis()
                    )
                    db.studentDao().insertProfile(merged)
                    p = merged
                    preferencesRepository.recordSuccessfulSave()
                }

                val isFirstLaunch = !preferencesRepository.isFirstLaunchCompleted.value
                if (p == null) {
                    if (isFirstLaunch) {
                        // Truly new user: seed initial clean state
                        AppDatabase.populateInitialData(db.studentDao(), db.curriculumDao())
                        preferencesRepository.setFirstLaunchCompleted(true)
                        preferencesRepository.recordSuccessfulSave()
                    } else {
                        // Existing student with unexpected missing profile - attempt snapshot recovery
                        val snapshot = LocalDataBackupManager.loadLatestLocalSnapshot(application)
                        if (snapshot != null) {
                            val res = repository.restoreFullBackupJson(snapshot)
                            if (res.isSuccess) {
                                _userMessage.emit("اطلاعات شما با موفقیت از نسخه پشتیبان خودکار بازیابی شد.")
                            } else {
                                _databaseRecoveryWarning.value = true
                                AppDatabase.populateInitialData(db.studentDao(), db.curriculumDao())
                            }
                        } else {
                            _databaseRecoveryWarning.value = true
                            AppDatabase.populateInitialData(db.studentDao(), db.curriculumDao())
                        }
                    }
                } else {
                    preferencesRepository.setFirstLaunchCompleted(true)
                    preferencesRepository.recordSuccessfulSave()
                    // Only backup if onboarding is actually completed and student profile is valid
                    if (preferencesRepository.isAutoBackupEnabled.value && p.isOnboardingCompleted) {
                        val snapshot = repository.exportFullBackupJson()
                        LocalDataBackupManager.saveLocalSnapshot(application, snapshot)
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                _databaseRecoveryWarning.value = true
            }

            // Offline-first sync: Pull latest changes from custom backend on app startup
            try {
                com.example.data.cloud.worker.BackendSyncWorker.triggerImmediateSync(application, pullOnly = true)
            } catch (_: Throwable) {}
        }
    }

    val courses: StateFlow<List<CourseEntity>> = repository.courses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coursesWithSessions: StateFlow<List<com.example.data.local.relation.CourseWithSessions>> = repository.coursesWithSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Phase 1 (BUG-01): Dynamic Exams StateFlow derived from active courses table (one exam per canonical course)
    val exams: StateFlow<List<ExamItem>> = courses.map { courseList ->
        courseList
            .filter { !it.isArchived }
            .distinctBy { it.id }
            .mapNotNull { c ->
                val date = c.examDate.trim()
                val time = c.examTime.trim()
                val loc = c.examLocation.trim().ifBlank { "سالن امتحانات" }
                if (date.isNotBlank() || time.isNotBlank()) {
                    ExamItem(
                        id = "exam_${c.id}",
                        courseName = c.name,
                        solarDate = date.ifBlank { "نامشخص" },
                        time = time.ifBlank { "نامشخص" },
                        location = loc,
                        units = c.units
                    )
                } else null
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Backward compatibility accessor for legacy callers
    val examsList: List<ExamItem>
        get() = exams.value

    val curriculumCourses: StateFlow<List<com.example.data.local.entity.CurriculumCourseEntity>> = (repository.curriculumCourses ?: kotlinx.coroutines.flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.seed.CurriculumSeedData.chemicalEngineeringCourses)

    val attendance: StateFlow<List<AttendanceEntity>> = repository.attendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val grades: StateFlow<List<GradeEntity>> = repository.grades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = repository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isOnboardingCompleted = MutableStateFlow(preferencesRepository.isOnboardingCompleted.value)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _optimisticProfile = MutableStateFlow<StudentProfileEntity?>(null)

    val profile: StateFlow<StudentProfileEntity> = combine(repository.profile, _optimisticProfile) { dbProfile, optProfile ->
        val resolved = optProfile ?: dbProfile
        val effectiveProfile = resolved ?: StudentProfileEntity(isOnboardingCompleted = preferencesRepository.isOnboardingCompleted.value)

        // Keep onboarding flag in sync
        if (effectiveProfile.isOnboardingCompleted && !_isOnboardingCompleted.value) {
            _isOnboardingCompleted.value = true
            preferencesRepository.setOnboardingCompleted(true)
        }
        effectiveProfile
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StudentProfileEntity(isOnboardingCompleted = preferencesRepository.isOnboardingCompleted.value)
    )

    val currentSemester: StateFlow<SemesterEntity?> = repository.currentSemester
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val archivedSemesters: StateFlow<List<SemesterEntity>> = repository.archivedSemesters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSemesters: StateFlow<List<SemesterEntity>> = repository.semesters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Phase 4 Academic Foundations Engine Flows
    val curriculumMatchUiState: StateFlow<CurriculumMatchUiState> = combine(
        profile,
        courses,
        curriculumCourses,
        repository.studentAttempts
    ) { p, currentCoursesList, currCoursesList, attemptsList ->
        val universityId = p.universityId.orEmpty()
        val majorId = p.majorId.orEmpty()
        val entryYear = p.entryYear

        if (universityId.isBlank() || majorId.isBlank() || entryYear <= 0) {
            return@combine CurriculumMatchUiState.Empty
        }

        val universities = listOf(
            ResolvedUniversity("UNI_AUT", "دانشگاه صنعتی امیرکبیر", "پلی‌تکنیک تهران")
        )
        val majors = listOf(
            ResolvedMajor("MAJ_AUT_CHEM_ENG", "UNI_AUT", "FAC_AUT_CHEM_OIL", "مهندسی شیمی")
        )
        val versions = listOf(
            ResolvedCurriculumVersion(
                id = "CURR_AUT_CE_1401",
                majorId = "MAJ_AUT_CHEM_ENG",
                title = "چارت کارشناسی مهندسی شیمی ورودی‌های 1401 تا 1404",
                entryYearMin = 1401,
                entryYearMax = 1404,
                totalCreditsRequired = 140
            )
        )

        val resolver = CurriculumResolver(universities, majors, versions)
        when (val res = resolver.resolve(universityId, majorId, entryYear)) {
            is CurriculumResolutionResult.NotFound -> {
                val reasonMsg = when (res.reason) {
                    ResolutionFailureReason.PROFILE_DATA_INCOMPLETE -> "اطلاعات شناسنامه یا سال ورود ناقص است"
                    ResolutionFailureReason.UNIVERSITY_NOT_SUPPORTED -> "دانشگاه انتخابی در پایگاه داده مرجع ثبت نشده است"
                    ResolutionFailureReason.MAJOR_NOT_SUPPORTED -> "رشته انتخابی در سیستم ثبت نشده است"
                    ResolutionFailureReason.MAJOR_UNIVERSITY_MISMATCH -> "رشته انتخابی متعلق به دانشگاه انتخابی نیست"
                    ResolutionFailureReason.ENTRY_YEAR_OUT_OF_RANGE -> "چارت مصوبی برای این سال ورود یافت نشد"
                }
                CurriculumMatchUiState.NotFound(res.reason, reasonMsg)
            }
            is CurriculumResolutionResult.ExactMatch,
            is CurriculumResolutionResult.ExplicitRangeMatch -> {
                val resolvedVersion = if (res is CurriculumResolutionResult.ExactMatch) res.version else (res as CurriculumResolutionResult.ExplicitRangeMatch).version

                val resolvableCourses = currCoursesList.map { cc ->
                    val prereqList = cc.prerequisites.split("،", ",").map { it.trim() }.filter { it.isNotEmpty() }
                    val conditions = prereqList.map { pName ->
                        val matchingCourse = currCoursesList.find { CourseIdentityNormalizer.normalize(it.name) == CourseIdentityNormalizer.normalize(pName) }
                        val pId = matchingCourse?.id ?: pName
                        PrerequisiteCondition.CoursePassed(pId, pName)
                    }
                    val rule = if (conditions.isNotEmpty()) {
                        CoursePrerequisiteRule(rootCondition = if (conditions.size == 1) conditions.first() else PrerequisiteCondition.AndGroup(conditions))
                    } else if (cc.name.contains("کارآموزی")) {
                        CoursePrerequisiteRule(rootCondition = PrerequisiteCondition.MinimumTotalPassedCredits(80))
                    } else {
                        CoursePrerequisiteRule()
                    }

                    ResolvableCurriculumCourse(
                        id = cc.id,
                        curriculumVersionId = resolvedVersion.id,
                        code = cc.code,
                        canonicalName = cc.name,
                        units = cc.units,
                        courseType = cc.courseType,
                        recommendedSemester = cc.recommendedSemester,
                        rule = rule
                    )
                }

                val enrolledIds = currentCoursesList.map { it.id }.toSet()
                val enrolledNames = currentCoursesList.map { it.name }.toSet()
                val attempts = attemptsList.map {
                    StudentCourseAttempt(
                        courseId = it.courseId,
                        courseName = it.courseName,
                        units = it.units,
                        status = it.status,
                        grade = it.grade
                    )
                }

                val match = CurriculumMatcher.match(
                    version = resolvedVersion,
                    curriculumCourses = resolvableCourses,
                    enrolledCurrentCourseIds = enrolledIds,
                    enrolledCurrentCourseNames = enrolledNames,
                    studentAttempts = attempts,
                    declaredPassedCredits = p.declaredPassedCredits ?: p.passedUnits
                )

                CurriculumMatchUiState.Ready(
                    output = match,
                    groupedBySemester = match.semesterMatrix
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CurriculumMatchUiState.Loading)

    val academicProgressUiState: StateFlow<AcademicProgressUiState> = combine(
        profile,
        courses,
        curriculumMatchUiState,
        repository.studentAttempts
    ) { p, currentCoursesList, matchState, attemptsList ->
        when (matchState) {
            is CurriculumMatchUiState.Loading -> AcademicProgressUiState.Loading
            is CurriculumMatchUiState.Empty -> AcademicProgressUiState.Empty
            is CurriculumMatchUiState.NotFound -> AcademicProgressUiState.Error(matchState.explanation)
            is CurriculumMatchUiState.Error -> AcademicProgressUiState.Error(matchState.message)
            is CurriculumMatchUiState.Ready -> {
                val attempts = attemptsList.map {
                    StudentCourseAttempt(
                        courseId = it.courseId,
                        courseName = it.courseName,
                        units = it.units,
                        status = it.status,
                        grade = it.grade
                    )
                }
                val currentUnits = currentCoursesList.sumOf { it.units }
                val progress = CurriculumMatcher.computeProgress(
                    version = matchState.output.version,
                    declaredPassedCredits = p.declaredPassedCredits ?: p.passedUnits,
                    declaredGpa = p.declaredGpa,
                    studentAttempts = attempts,
                    currentEnrolledUnits = currentUnits,
                    matchOutput = matchState.output
                )

                if (progress.confidence == DataConfidence.CREDIT_ONLY) {
                    AcademicProgressUiState.Partial(
                        progress = progress,
                        note = "پیشرفت بر اساس مجموع واحدهای اعلامی کاربر است؛ ریز نمرات ترم‌های پیشین ثبت قطعی نشده است."
                    )
                } else {
                    AcademicProgressUiState.Ready(progress)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AcademicProgressUiState.Loading)

    // Academic Risk Engine (Phase 21)
    val academicRisks: StateFlow<List<AcademicRisk>> = combine(
        courses,
        attendance,
        tasks,
        exams
    ) { coursesList, attendanceList, tasksList, examsList ->
        AcademicRiskEngine.evaluateRisks(coursesList, attendanceList, tasksList, examsList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weekly Academic Workload (Phase 20)
    val weeklyWorkload: StateFlow<WeeklyAcademicWorkload> = combine(
        courses,
        tasks
    ) { coursesList, tasksList ->
        WorkloadEngine.calculateWeeklyWorkload(coursesList, tasksList)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        WeeklyAcademicWorkload(0.0, 0.0, 0.0, 0.0, 0.0)
    )

    // Semester Planner Candidate Plans (Phase 12 & 13)
    val candidateSemesterPlans: StateFlow<List<SemesterPlan>> = curriculumMatchUiState.map { matchState ->
        if (matchState is CurriculumMatchUiState.Ready) {
            SemesterPlannerEngine.generateCandidatePlans(matchState.output.availableCourses)
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Study Planner Recommendations (Phase 28)
    val studyRecommendations: StateFlow<List<StudySessionRecommendation>> = combine(
        exams,
        tasks
    ) { currentExams, tasksList ->
        StudyPlannerEngine.generateStudyPlan(currentExams, tasksList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Student Gamification & Academic Badges Profile (Phase v5)
    val gamificationProfile: StateFlow<StudentGamificationProfile> = combine(
        tasks,
        attendance,
        grades,
        profile
    ) { tasksList, attendanceList, gradesList, studentProfile ->
        val passed = studentProfile.declaredPassedCredits ?: studentProfile.passedUnits
        AcademicGamificationEngine.calculateGamificationProfile(
            tasks = tasksList,
            attendanceList = attendanceList,
            grades = gradesList,
            passedUnits = passed
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AcademicGamificationEngine.calculateGamificationProfile(emptyList(), emptyList(), emptyList(), 0)
    )

    private val _syncUiState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncUiState: StateFlow<SyncUiState> = _syncUiState.asStateFlow()

    // Global Search Query and Results (Phase 24)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val globalSearchResults: StateFlow<List<GlobalSearchResult>> = combine(
        combine(_searchQuery, courses, curriculumMatchUiState) { query, coursesList, matchState ->
            Triple(query, coursesList, matchState)
        },
        combine(tasks, profile, exams) { tasksList, studentProfile, currentExams ->
            Triple(tasksList, studentProfile, currentExams)
        }
    ) { (query, coursesList, matchState), (tasksList, studentProfile, currentExams) ->
        val curriculumCourses = if (matchState is CurriculumMatchUiState.Ready) {
            matchState.output.evaluatedCourses
        } else {
            emptyList()
        }
        GlobalSearchEngine.search(
            query = query,
            courses = coursesList,
            curriculumCourses = curriculumCourses,
            tasks = tasksList,
            exams = currentExams,
            notes = studentProfile.notes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Tab
    private val _selectedTab = MutableStateFlow(AppTab.DASHBOARD)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    // Notifications
    private val _notifications = MutableStateFlow<List<SystemNotification>>(
        listOf(
            SystemNotification(
                id = "1",
                title = "خوش‌آمدید",
                description = "سامانه دانشجویی آماده استفاده است.",
                time = "08:00"
            )
        )
    )
    val notifications: StateFlow<List<SystemNotification>> = _notifications.asStateFlow()

    fun addNotification(title: String, description: String, isDanger: Boolean = false) {
        val time = SimpleDateFormat("HH:mm", Locale.US).format(Date())
        val notif = SystemNotification(
            id = System.currentTimeMillis().toString(),
            title = title,
            description = description,
            time = time,
            isDanger = isDanger
        )
        _notifications.value = listOf(notif) + _notifications.value
        playNotificationTone(isDanger)
        NotificationHelper.showSystemNotification(application, title, description, isDanger)
    }

    fun sendTestNotificationToDevice() {
        addNotification("🔔 هشدار تستی سامانه دانشجویی", "اتصال سیستم اعلان‌ها به نوار اعلان گوشی موفقیت‌آمیز است.", false)
    }

    fun clearNotifications() {
        _notifications.value = emptyList<SystemNotification>()
    }

    fun playNotificationTone(isDanger: Boolean) {
        NotificationHelper.playToneSafely(isDanger)
    }

    private fun triggerCloudSync(dataType: String? = null) {
        try {
            // Node.js/PostgreSQL is the single data-sync backend.
            com.example.data.cloud.worker.BackendSyncWorker.triggerImmediateSync(application, dataType)
        } catch (e: Throwable) {
            android.util.Log.w("StudentViewModel", "Failed to trigger backend sync: ${e.message}")
        }
    }

    // Pomodoro
    private val _pomodoroSeconds = MutableStateFlow(25 * 60)
    val pomodoroSeconds: StateFlow<Int> = _pomodoroSeconds.asStateFlow()

    private val _isPomodoroRunning = MutableStateFlow(false)
    val isPomodoroRunning: StateFlow<Boolean> = _isPomodoroRunning.asStateFlow()

    fun togglePomodoro() {
        if (_isPomodoroRunning.value) {
            pomodoroJob?.cancel()
            _isPomodoroRunning.value = false
        } else {
            _isPomodoroRunning.value = true
            pomodoroJob = viewModelScope.launch {
                while (_pomodoroSeconds.value > 0 && _isPomodoroRunning.value) {
                    delay(1000)
                    _pomodoroSeconds.value -= 1
                }
                if (_pomodoroSeconds.value == 0) {
                    _isPomodoroRunning.value = false
                    addNotification(
                        "پایان تایم مطالعه پومودورو",
                        "25 دقیقه تمرکز به پایان رسید! 5 دقیقه استراحت چشمی داشته باشید.",
                        isDanger = false
                    )
                }
            }
        }
    }

    fun resetPomodoro() {
        pomodoroJob?.cancel()
        _isPomodoroRunning.value = false
        _pomodoroSeconds.value = 25 * 60
    }

    // Actions
    fun saveCourse(course: CourseEntity, sessions: List<com.example.data.local.entity.CourseSessionEntity> = emptyList()) {
        viewModelScope.launch {
            repository.saveCourse(course, sessions)
            addNotification("ثبت درس", "درس ${course.name} در برنامه هفتگی ذخیره شد.")
            triggerCloudSync()
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            repository.deleteCourse(courseId)
            addNotification("حذف درس", "درس با موفقیت از جدول کلاسی حذف شد.", isDanger = true)
            triggerCloudSync()
        }
    }

    fun changeAttendance(courseName: String, delta: Int) {
        viewModelScope.launch {
            val currentList = attendance.value
            val existing = currentList.find { it.courseName == courseName || it.courseId == courseName }
            val course = courses.value.find { it.name == courseName || it.id == courseName }
            val courseId = course?.id ?: existing?.courseId ?: "att_${courseName.hashCode()}"
            val effectiveCourseName = course?.name ?: existing?.courseName ?: courseName
            val maxAllowed = if (effectiveCourseName.contains("آزمایشگاه") || effectiveCourseName.contains("کارگاه")) 2 else 3
            val newCount = ((existing?.absentCount ?: 0) + delta).coerceIn(0, 50)

            val updated = AttendanceEntity(
                courseId = courseId,
                courseName = effectiveCourseName,
                absentCount = newCount,
                maxAllowed = maxAllowed
            )
            repository.updateAttendance(updated)

            if (newCount >= maxAllowed) {
                addNotification(
                    "⚠️ اخطار سقف غیبت!",
                    "تعداد غیبت در درس $effectiveCourseName به سقف مجاز ($newCount از $maxAllowed) رسید.",
                    isDanger = true
                )
            } else if (delta > 0) {
                addNotification("ثبت غیبت", "یک جلسه غیبت برای درس $effectiveCourseName ثبت شد.")
            }
            triggerCloudSync()
        }
    }

    fun setAttendanceCount(courseName: String, count: Int) {
        viewModelScope.launch {
            val currentList = attendance.value
            val existing = currentList.find { it.courseName == courseName || it.courseId == courseName }
            val course = courses.value.find { it.name == courseName || it.id == courseName }
            val courseId = course?.id ?: existing?.courseId ?: "att_${courseName.hashCode()}"
            val effectiveCourseName = course?.name ?: existing?.courseName ?: courseName
            val maxAllowed = if (effectiveCourseName.contains("آزمایشگاه") || effectiveCourseName.contains("کارگاه")) 2 else 3
            val newCount = count.coerceIn(0, 50)

            val updated = AttendanceEntity(
                courseId = courseId,
                courseName = effectiveCourseName,
                absentCount = newCount,
                maxAllowed = maxAllowed
            )
            repository.updateAttendance(updated)

            if (newCount >= maxAllowed) {
                addNotification(
                    "⚠️ اخطار سقف غیبت!",
                    "تعداد غیبت در درس $effectiveCourseName ($newCount جلسه) ثبت شد.",
                    isDanger = true
                )
            }
            triggerCloudSync()
        }
    }

    fun saveTask(title: String, courseName: String, dueDate: String, courseId: String? = null) {
        viewModelScope.launch {
            val resolvedCourse = courses.value.find { it.name == courseName || it.id == courseId }
            val resolvedCourseId = courseId ?: resolvedCourse?.id ?: ""
            val currentSem = repository.getCurrentSemesterSync()
            repository.saveTask(
                TaskEntity(
                    title = title,
                    courseName = courseName,
                    dueDate = dueDate,
                    isCompleted = false,
                    courseId = resolvedCourseId,
                    semesterId = currentSem?.id
                )
            )
            addNotification("تکلیف جدید", "تکلیف «$title» به لیست افزوده شد.")
            triggerCloudSync()
        }
    }

    fun startNewSemester(
        title: String,
        academicYear: Int,
        termNumber: Int,
        newCourses: List<CourseEntity> = emptyList(),
        finalRecordedGpa: Double? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.startNewSemester(
                com.example.domain.usecase.NewSemesterRequest(
                    title = title,
                    academicYear = academicYear,
                    termNumber = termNumber,
                    newCourses = newCourses,
                    finalRecordedGpa = finalRecordedGpa
                )
            )
            addNotification(
                "آغاز ترم تحصیلی جدید",
                "ترم «${result.createdSemester.title}» به عنوان ترم جاری فعال شد و اطلاعات ترم قبلی با موفقیت بایگانی گردید."
            )
            triggerCloudSync()
            onSuccess()
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
            if (!task.isCompleted) {
                addNotification("انجام تکلیف", "آفرین! تکلیف «${task.title}» انجام شد.")
            }
            triggerCloudSync()
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            triggerCloudSync()
        }
    }

    fun updateGrade(grade: GradeEntity, newMid: Double, newFin: Double) {
        viewModelScope.launch {
            repository.updateGrade(grade.copy(midtermGrade = newMid, finalGrade = newFin))
            triggerCloudSync()
        }
    }

    fun updateProfile(profileEntity: StudentProfileEntity) {
        _optimisticProfile.value = profileEntity
        preferencesRepository.recordSuccessfulSave()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.updateProfile(profileEntity)
            addNotification("ویرایش مشخصات", "اطلاعات پروفایل دانشجویی به‌روزرسانی شد.")
            triggerCloudSync()
        }
    }

    fun updateProfile(name: String, studentId: String) {
        val current = profile.value
        val updated = current.copy(name = name, studentId = studentId)
        _optimisticProfile.value = updated
        preferencesRepository.recordSuccessfulSave()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.updateProfile(updated)
            addNotification("ویرایش مشخصات", "اطلاعات پروفایل دانشجویی به‌روزرسانی شد.")
            triggerCloudSync()
        }
    }

    fun updateFullProfile(
        name: String,
        studentId: String,
        university: String,
        major: String,
        entryYear: Int,
        currentSemester: Int,
        passedUnits: Int,
        activeUnits: Int
    ) {
        val current = profile.value
        val updated = current.copy(
            name = name,
            studentId = studentId,
            university = university,
            major = major,
            entryYear = entryYear,
            currentSemester = currentSemester,
            passedUnits = passedUnits,
            declaredPassedCredits = passedUnits,
            activeUnits = activeUnits,
            term = "ترم $currentSemester $major",
            faculty = "دانشکده $major · $activeUnits واحد فعال",
            isOnboardingCompleted = true
        )
        _optimisticProfile.value = updated
        preferencesRepository.recordSuccessfulSave()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.updateProfile(updated)
            addNotification("به‌روزرسانی پروفایل", "مشخصات دانشگاهی شما با موفقیت ذخیره شد.")
            triggerCloudSync()
        }
    }

    fun saveNotes(notes: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val current = profile.value
            repository.updateProfile(current.copy(notes = notes))
            triggerCloudSync()
        }
    }

    fun completeQuickAcademicSetup(
        name: String = "دانشجو",
        studentId: String = "",
        university: String,
        major: String,
        entryYear: Int,
        currentSemester: Int,
        passedCredits: Int,
        currentGpa: Double,
        selectedCourses: List<com.example.data.local.entity.CurriculumCourseEntity>
    ) {
        val resolvedName = name.ifBlank { "دانشجو" }
        val totalActiveUnits = selectedCourses.sumOf { it.units }

        // 1. Immediately persist synchronously to SharedPreferences (prevents loss on Samsung Galaxy A73 One UI process kill)
        preferencesRepository.setOnboardingCompleted(true)
        preferencesRepository.recordSuccessfulSave()

        // 2. Instantly update in-memory state so UI immediately responds (0ms delay - prevents stale state)
        _isOnboardingCompleted.value = true
        _optimisticProfile.value = StudentProfileEntity(
            id = 1,
            name = resolvedName,
            studentId = studentId.trim(),
            university = university,
            major = major,
            entryYear = entryYear,
            currentSemester = currentSemester,
            passedUnits = passedCredits,
            declaredPassedCredits = passedCredits,
            declaredGpa = currentGpa,
            activeUnits = totalActiveUnits,
            term = "ترم $currentSemester $major",
            faculty = "دانشکده $major · $totalActiveUnits واحد فعال",
            isOnboardingCompleted = true
        )

        // 3. Write to Room database and store local backup snapshot
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                repository.completeQuickAcademicSetup(
                    name = resolvedName,
                    studentId = studentId,
                    university = university,
                    major = major,
                    entryYear = entryYear,
                    currentSemester = currentSemester,
                    passedCredits = passedCredits,
                    currentGpa = currentGpa,
                    selectedCourses = selectedCourses
                )
                preferencesRepository.recordSuccessfulSave()
                try {
                    val snapshot = repository.exportFullBackupJson()
                    LocalDataBackupManager.saveLocalSnapshot(application, snapshot)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                addNotification("پیکربندی موفق", "سیستم‌عامل تحصیلی شما برای ترم $currentSemester $major با موفقیت راه‌اندازی شد.")
            } catch (e: Throwable) {
                e.printStackTrace()
                _userMessage.emit("خطا در ذخیره‌سازی پایگاه داده: ${e.message}")
            }
        }
    }

    fun importParsedCourses(
        drafts: List<com.example.data.parser.ParsedCourseDraft>,
        clearExisting: Boolean = false,
        studentName: String = "",
        studentId: String = "",
        university: String = "",
        major: String = "",
        entryYear: Int = 1403,
        currentSemester: Int = 1
    ) {
        val resolvedName = studentName.ifBlank { "دانشجو" }
        val totalUnits = drafts.sumOf { it.units }

        preferencesRepository.setOnboardingCompleted(true)
        preferencesRepository.recordSuccessfulSave()
        _isOnboardingCompleted.value = true

        val opt = StudentProfileEntity(
            id = 1,
            name = resolvedName,
            studentId = studentId.trim(),
            university = university,
            major = major,
            entryYear = entryYear,
            currentSemester = currentSemester,
            activeUnits = totalUnits,
            term = if (currentSemester > 0 && major.isNotBlank()) "ترم $currentSemester $major" else "",
            faculty = if (major.isNotBlank()) "دانشکده $major · $totalUnits واحد فعال" else "",
            isOnboardingCompleted = true
        )
        _optimisticProfile.value = opt

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                repository.importParsedCourses(
                    drafts = drafts,
                    semesterId = "current",
                    clearExisting = clearExisting,
                    studentName = resolvedName,
                    studentId = studentId,
                    university = university,
                    major = major,
                    entryYear = entryYear,
                    currentSemester = currentSemester
                )
                try {
                    val snapshot = repository.exportFullBackupJson()
                    LocalDataBackupManager.saveLocalSnapshot(application, snapshot)
                } catch (_: Throwable) {}
                addNotification("واردسازی برنامه", "${drafts.size} درس جدید از متن انتخاب واحد استخراج و ثبت شد.")
            } catch (e: Throwable) {
                e.printStackTrace()
                _userMessage.emit("خطا در ثبت دروس: ${e.message}")
            }
        }
    }

    fun setOnboardingCompleted(
        completed: Boolean,
        name: String = "",
        studentId: String = "",
        university: String = "",
        major: String = "",
        entryYear: Int = 0,
        currentSemester: Int = 0,
        passedCredits: Int = 0,
        declaredGpa: Double = 0.0
    ) {
        preferencesRepository.setOnboardingCompleted(completed)
        preferencesRepository.recordSuccessfulSave()
        _isOnboardingCompleted.value = completed

        val resolvedName = name.ifBlank { "دانشجو" }
        val opt = StudentProfileEntity(
            id = 1,
            name = resolvedName,
            studentId = studentId.trim(),
            university = university,
            major = major,
            entryYear = entryYear,
            currentSemester = currentSemester,
            passedUnits = passedCredits,
            declaredPassedCredits = passedCredits,
            declaredGpa = declaredGpa,
            term = "ترم $currentSemester $major",
            faculty = "دانشکده $major",
            isOnboardingCompleted = completed
        )
        _optimisticProfile.value = opt

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.setOnboardingCompleted(
                completed = completed,
                name = resolvedName,
                studentId = studentId,
                university = university,
                major = major,
                entryYear = entryYear,
                currentSemester = currentSemester,
                passedCredits = passedCredits,
                declaredGpa = declaredGpa
            )
        }
    }

    fun savePastSemesterHistory(
        selectedSemester: Int,
        totalPassedCredits: Int,
        overallGpa: Double,
        summaries: List<com.example.ui.components.SemesterSummaryItem>
    ) {
        val current = profile.value
        val updated = current.copy(
            currentSemester = selectedSemester,
            passedUnits = totalPassedCredits,
            declaredPassedCredits = totalPassedCredits,
            declaredGpa = overallGpa,
            term = "ترم $selectedSemester ${current.major}"
        )
        _optimisticProfile.value = updated
        preferencesRepository.recordSuccessfulSave()

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.updateProfile(updated)

            // Save individual attempts for past semesters if available
            summaries.forEach { s ->
                val u = s.units.toIntOrNull() ?: 17
                val g = s.gpa.toDoubleOrNull()
                val attempt = com.example.data.local.entity.StudentCourseAttemptEntity(
                    id = "sem_summary_${s.semesterIndex}_${System.currentTimeMillis()}",
                    profileId = 1,
                    courseId = null,
                    courseName = "مجموع دروس گذرانده ترم ${s.semesterIndex}",
                    units = u,
                    attemptNumber = 1,
                    semesterIndex = s.semesterIndex,
                    status = "PASSED",
                    grade = g,
                    source = "QUICK_SETUP"
                )
                repository.saveStudentAttempt(attempt)
            }

            addNotification(
                "به‌روزرسانی سوابق تحصیلی",
                "ترم تحصیلی به ترم $selectedSemester تغییر یافت و $totalPassedCredits واحد پاس‌شده با معدل ${String.format(Locale.US, "%.2f", overallGpa)} ثبت گردید."
            )
        }
    }

    fun executeCopilotPayload(payload: com.example.domain.model.CopilotPayload) {
        viewModelScope.launch {
            when (payload) {
                is com.example.domain.model.CopilotPayload.AddTask -> {
                    saveTask(payload.title, payload.courseName, payload.dueDate)
                }
                is com.example.domain.model.CopilotPayload.CompleteTask -> {
                    val task = tasks.value.find { it.id.toString() == payload.taskId || it.title == payload.taskTitle }
                    if (task != null) {
                        toggleTask(task)
                    }
                }
                is com.example.domain.model.CopilotPayload.ChangeCurrentSemester -> {
                    val current = profile.value
                    repository.updateProfile(current.copy(currentSemester = payload.newSemesterIndex, term = "ترم ${payload.newSemesterIndex} ${current.major}"))
                    addNotification("تغییر ترم تحصیلی", "ترم جاری به ترم ${payload.newSemesterIndex} تغییر یافت.")
                }
                is com.example.domain.model.CopilotPayload.UpdatePastSemesterSummary -> {
                    val current = profile.value
                    val newPassed = current.passedUnits + payload.units
                    repository.updateProfile(current.copy(passedUnits = newPassed, declaredPassedCredits = newPassed))
                    addNotification("ثبت واحد گذشته", "${payload.units} واحد برای ترم ${payload.semesterIndex} اضافه شد.")
                }
                is com.example.domain.model.CopilotPayload.ClearAttendanceWarning -> {
                    val item = attendance.value.find { it.courseName == payload.courseName }
                    if (item != null) {
                        repository.updateAttendance(item.copy(absentCount = 0))
                        addNotification("پاکسازی غیبت", "غیبت‌های درس ${payload.courseName} صفر شد.")
                    }
                }
                is com.example.domain.model.CopilotPayload.QuickEnrollCourse -> {
                    val dayInt = when (payload.dayOfWeek.trim()) {
                        "یکشنبه" -> 1
                        "دوشنبه" -> 2
                        "سه‌شنبه" -> 3
                        "چهارشنبه" -> 4
                        else -> 0
                    }
                    val newCourseId = java.util.UUID.randomUUID().toString()
                    val course = CourseEntity(
                        id = newCourseId,
                        name = payload.courseName,
                        colorHex = "#4F46E5",
                        units = payload.units
                    )
                    val session = CourseSessionEntity(
                        courseId = newCourseId,
                        day = dayInt,
                        start = payload.startTime,
                        end = payload.endTime,
                        location = ""
                    )
                    saveCourse(course, listOf(session))
                }
                is com.example.domain.model.CopilotPayload.NavigateToTab -> {
                    // Handled at UI level
                }
                is com.example.domain.model.CopilotPayload.SimulateGpaTarget -> {
                    // Analytical
                }
            }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetDefaults()
            addNotification("بازنشانی سامانه", "کلیه اطلاعات به حالت پیش‌فرض بازگشت.")
        }
    }

    fun loadDemoData() {
        viewModelScope.launch {
            repository.loadRichDemoData()
            addNotification("حالت دمو فعال شد", "داده‌های کامل و نمونه دانشگاهی بارگذاری شد 🎓")
        }
    }

    fun clearToFreshSlate(
        name: String = "دانشجو",
        studentId: String = "",
        university: String = "",
        major: String = "",
        entryYear: Int = 0,
        currentSemester: Int = 0
    ) {
        viewModelScope.launch {
            repository.clearToFreshSlate(name, studentId, university, major, entryYear, currentSemester)
            addNotification("شروع نو و پاکسازی", "سیستم‌عامل تحصیلی با یک بوم پاک و آماده ثبت دروس شما آماده شد ✨")
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        preferencesRepository.setThemeMode(mode)
    }

    fun toggleThemeQuickly() {
        val current = themeMode.value
        val next = when (current) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        preferencesRepository.setThemeMode(next)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        preferencesRepository.setNotificationsEnabled(enabled)
        val statusText = if (enabled) "فعال شد" else "غیرفعال شد"
        addNotification("تنظیمات اعلان", "دریافت اعلان‌ها و هشدارهای تحصیلی $statusText.")
    }

    fun setCustomGeminiApiKey(key: String) {
        preferencesRepository.setCustomGeminiApiKey(key)
    }

    fun dismissRecoveryWarning() {
        _databaseRecoveryWarning.value = false
        preferencesRepository.setDataLossWarningDismissed(true)
    }

    suspend fun exportDatabaseBackup(): String {
        return repository.exportFullBackupJson()
    }

    fun importDatabaseBackup(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.restoreFullBackupJson(jsonString)
            if (result.isSuccess) {
                preferencesRepository.recordSuccessfulSave()
                LocalDataBackupManager.saveLocalSnapshot(application, jsonString)
                _userMessage.emit("داده‌های شما با موفقیت از فایل پشتیبان بازیابی شدند.")
                addNotification("بازیابی نسخه پشتیبان", "دیتابیس سیستم با موفقیت به‌روزرسانی و بازیابی شد.")
                onResult(true, "بازیابی موفقیت‌آمیز بود.")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "خطا در پردازش فایل پشتیبان"
                onResult(false, errorMsg)
            }
        }
    }

    fun restoreAllDataFromCloud(onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value
        if (user.isGuest || user.uid.isBlank()) {
            onResult(false, "برای بازیابی اطلاعات از فضای ابری، ابتدا باید وارد حساب کاربری خود شوید.")
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val db = repository.database ?: AppDatabase.getDatabase(application, viewModelScope)
            val result = com.example.data.cloud.BackendSyncManager.pullAllData(application)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    preferencesRepository.recordSuccessfulSave()
                    addNotification("بازیابی ابری", "$count رکورد اطلاعات با موفقیت از سرور ابری بازیابی شد.")
                    _userMessage.emit("اطلاعات شما با موفقیت از فضای ابری بازیابی شد.")
                    onResult(true, "بازیابی ابری با موفقیت انجام شد ($count مورد به‌روزرسانی شد).")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "خطا در برقراری ارتباط با فضای ابری"
                    onResult(false, errorMsg)
                }
            }
        }
    }

    fun signInWithEmail(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val res = authManager.signInWithEmail(email, password)) {
                is com.example.domain.model.AuthResult.Success -> {
                    addNotification("ورود به حساب", "با موفقیت به حساب ${res.user.displayName} وارد شدید.")
                    _userMessage.emit(res.message)
                    onResult(true, res.message)
                }
                is com.example.domain.model.AuthResult.Error -> {
                    onResult(false, res.errorMessage)
                }
                else -> {}
            }
        }
    }

    fun signUpWithEmail(name: String, email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val res = authManager.signUpWithEmail(name, email, password)) {
                is com.example.domain.model.AuthResult.Success -> {
                    addNotification("ثبت‌نام حساب", "حساب کاربری جدید ایجاد شد.")
                    _userMessage.emit(res.message)
                    onResult(true, res.message)
                }
                is com.example.domain.model.AuthResult.Error -> {
                    onResult(false, res.errorMessage)
                }
                else -> {}
            }
        }
    }

    fun signInWithBackend(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val res = authManager.signInWithBackend(email, password)) {
                is com.example.domain.model.AuthResult.Success -> {
                    addNotification("ورود به سرور", "با موفقیت به حساب ${res.user.displayName} در سرور متصل شدید.")
                    _userMessage.emit(res.message)
                    onResult(true, res.message)
                }
                is com.example.domain.model.AuthResult.Error -> {
                    onResult(false, res.errorMessage)
                }
                else -> {}
            }
        }
    }

    fun signUpWithBackend(name: String, email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val res = authManager.signUpWithBackend(name, email, password)) {
                is com.example.domain.model.AuthResult.Success -> {
                    addNotification("ثبت‌نام در سرور", "حساب کاربری جدید شما در سرور اختصاصی ایجاد شد.")
                    _userMessage.emit(res.message)
                    onResult(true, res.message)
                }
                is com.example.domain.model.AuthResult.Error -> {
                    onResult(false, res.errorMessage)
                }
                else -> {}
            }
        }
    }

    fun syncWithBackendNow(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _syncUiState.value = SyncUiState.Syncing
            try {
                com.example.data.cloud.worker.BackendSyncWorker.triggerImmediateSync(application, pullOnly = false)
                val pullRes = com.example.data.cloud.BackendSyncManager.pullAllData(application)
                if (pullRes.isSuccess) {
                    _syncUiState.value = SyncUiState.Success("اطلاعات با سرور همگام شد.", System.currentTimeMillis())
                    _userMessage.emit("همگام‌سازی با سرور اختصاصی با موفقیت انجام شد.")
                    onResult(true, "اطلاعات با سرور همگام شد.")
                } else {
                    _syncUiState.value = SyncUiState.Error("برخی داده‌ها در همگام‌سازی دریافت نشدند.", System.currentTimeMillis())
                    onResult(false, "برخی داده‌ها در همگام‌سازی دریافت نشدند.")
                }
            } catch (e: Exception) {
                _syncUiState.value = SyncUiState.Error("خطا در همگام‌سازی: ${e.localizedMessage ?: "اتصال برقرار نشد."}", System.currentTimeMillis())
                onResult(false, "خطا در همگام‌سازی: ${e.localizedMessage ?: "اتصال برقرار نشد."}")
            }
        }
    }

    fun signInWithGoogle(idToken: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val res = authManager.signInWithGoogle(idToken)) {
                is com.example.domain.model.AuthResult.Success -> {
                    addNotification("ورود گوگل", "اتصال به حساب گوگل با موفقیت برقرار شد.")
                    _userMessage.emit(res.message)
                    onResult(true, res.message)
                }
                is com.example.domain.model.AuthResult.Error -> {
                    onResult(false, res.errorMessage)
                }
                else -> {}
            }
        }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = authManager.sendPasswordResetEmail(email)
            if (res.isSuccess) {
                addNotification("بازیابی رمز عبور", "لینک بازیابی رمز عبور برای $email ارسال گردید.")
                onResult(true, "لینک بازیابی رمز عبور با موفقیت به ایمیل شما ارسال شد.")
            } else {
                val err = res.exceptionOrNull()?.message ?: "خطا در ارسال ایمیل بازیابی."
                onResult(false, err)
            }
        }
    }

    fun applyPromoCode(code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = authManager.applyPromoCode(code)
            if (res.isSuccess) {
                val tier = res.getOrNull()
                addNotification("ارتقای اشتراک", "اشتراک شما به ${tier?.titleFa} ارتقا یافت! ★")
                _userMessage.emit("کد هدیه با موفقیت فعال شد.")
                onResult(true, "اشتراک ${tier?.titleFa} با موفقیت برای شما فعال گردید.")
            } else {
                val err = res.exceptionOrNull()?.message ?: "کد وارد شده نامعتبر است."
                onResult(false, err)
            }
        }
    }

    fun upgradeSubscriptionTier(tier: com.example.domain.model.SubscriptionTier, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            authManager.upgradeSubscriptionTier(tier)
            addNotification("ارتقای حساب", "طرح ${tier.titleFa} فعال گردید.")
            _userMessage.emit("اشتراک شما ارتقا یافت.")
            onResult(true, "طرح ${tier.titleFa} فعال شد.")
        }
    }

    fun signOutUser() {
        viewModelScope.launch {
            authManager.signOutUser()
            // Clear onboarding preferences
            preferencesRepository.setOnboardingCompleted(false)
            _isOnboardingCompleted.value = false
            _optimisticProfile.value = null
            
            // Remove the previous account's local academic data without inserting demo/default identity.
            repository.clearAllUserData()

            addNotification("خروج از حساب", "از حساب کاربری خارج شدید؛ اطلاعات حساب قبلی از این دستگاه پاک شد.")
        }
    }

    fun deleteUserAccount(onCompleted: () -> Unit) {
        viewModelScope.launch {
            val result = authManager.deleteUserAccount()
            if (result.isSuccess) {
                repository.clearAllUserData()
                preferencesRepository.setOnboardingCompleted(false)
                _isOnboardingCompleted.value = false
                _optimisticProfile.value = null
                addNotification("حذف حساب", "حساب کاربری حذف شد و داده‌های محلی پاکسازی شدند.", isDanger = true)
                onCompleted()
            } else {
                _userMessage.emit(
                    result.exceptionOrNull()?.localizedMessage
                        ?: "حذف حساب انجام نشد؛ اطلاعات شما بدون تغییر باقی ماند."
                )
            }
        }
    }

    fun triggerManualCloudSync(onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value
        if (user.isGuest) {
            onResult(false, "لطفاً ابتدا وارد حساب کاربری خود شوید.")
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _syncUiState.value = SyncUiState.Syncing
            try {
                val db = AppDatabase.getDatabase(application)
                val dao = db.studentDao()
                val profile = dao.getProfileSync()
                val courses = dao.getAllCoursesIncludingArchivedSync()
                val grades = dao.getAllGradesSync()
                val tasks = dao.getAllTasksSync()
                val attendance = dao.getAllAttendanceSync()
                val exams = dao.getAllExamsSync()
                val notes = dao.getAllNotesSync()

                val pushResult = com.example.data.cloud.BackendSyncManager.pushAllData(application)

                if (pushResult.isSuccess) {
                    val restoreResult = com.example.data.cloud.BackendSyncManager.pullAllData(application)
                    val success = restoreResult.isSuccess
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        if (success) {
                            _syncUiState.value = SyncUiState.Success("همگام‌سازی با سرور اختصاصی با موفقیت انجام شد.", System.currentTimeMillis())
                        } else {
                            _syncUiState.value = SyncUiState.Error("ارسال انجام شد، اما دریافت نهایی از سرور ناموفق بود.", System.currentTimeMillis())
                        }
                        onResult(
                            success,
                            if (success) "همگام‌سازی با سرور اختصاصی با موفقیت انجام شد."
                            else "ارسال انجام شد، اما دریافت نهایی از سرور ناموفق بود."
                        )
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _syncUiState.value = SyncUiState.Error("ارسال اطلاعات به سرور اختصاصی ناموفق بود.", System.currentTimeMillis())
                        onResult(false, "ارسال اطلاعات به سرور اختصاصی ناموفق بود.")
                    }
                }

            } catch (e: Exception) {
                _syncUiState.value = SyncUiState.Error("خطا در اتصال به سرور: ${e.localizedMessage ?: "اتصال برقرار نشد."}", System.currentTimeMillis())
                android.util.Log.e("StudentViewModel", "Manual cloud sync failed: ${e.message}", e)
                onResult(false, "خطا در اتصال به سرور: ${e.localizedMessage ?: "اتصال برقرار نشد."}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pomodoroJob?.cancel()
    }
}

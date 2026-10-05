package com.example.ui

import androidx.room.withTransaction

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.LocalDataBackupManager
import com.example.data.cloud.SyncStatusStore
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
import com.example.ui.util.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.example.domain.engine.AcademicGamificationEngine
import com.example.domain.engine.AcademicContextGraphBuilder
import com.example.domain.engine.AcademicPriorityEngine
import com.example.domain.engine.AcademicPlanningCollisionEngine
import com.example.domain.engine.AcademicRiskEngine
import com.example.domain.engine.GlobalSearchEngine
import com.example.domain.engine.SemesterPlannerEngine
import com.example.domain.engine.StudyPlannerEngine
import com.example.domain.engine.WorkloadEngine
import com.example.domain.model.AcademicBadge
import com.example.domain.model.AcademicContextGraph
import com.example.domain.model.AcademicPlanningCollision
import com.example.domain.model.AcademicPriorityItem
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val guestModeEnabled: StateFlow<Boolean> = preferencesRepository.guestModeEnabled
    val syncStatus: StateFlow<com.example.domain.model.SyncStatusSnapshot> = SyncStatusStore.status
    val acceptedStudyPlanIds: StateFlow<Set<String>> = preferencesRepository.acceptedStudyPlanIds

    fun continueAsGuest() {
        preferencesRepository.setGuestModeEnabled(true)
    }

    fun acceptStudyRecommendation(recommendation: StudySessionRecommendation) {
        preferencesRepository.acceptStudyRecommendation(recommendation.id)
    }

    private val _databaseRecoveryWarning = MutableStateFlow(false)
    val databaseRecoveryWarning: StateFlow<Boolean> = _databaseRecoveryWarning.asStateFlow()

    private val _isSavingRecord = MutableStateFlow(false)
    val isSavingRecord = _isSavingRecord.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    init {
        SyncStatusStore.initialize(application)
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
                val loc = c.examLocation.trim().ifBlank { "هنوز مشخص نشده" }
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

    val curriculumCourses: StateFlow<List<com.example.data.local.entity.CurriculumCourseEntity>> = (repository.curriculumCourses ?: kotlinx.coroutines.flow.flowOf(com.example.data.seed.CurriculumSeedData.chemicalEngineeringCourses))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.seed.CurriculumSeedData.chemicalEngineeringCourses)

    val attendance: StateFlow<List<AttendanceEntity>> = combine(repository.attendance, courses) { records, current ->
        records.filter { item -> current.any { it.id == item.courseId || (item.courseId.isBlank() && it.name == item.courseName) } }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val grades: StateFlow<List<GradeEntity>> = combine(repository.grades, courses) { records, current ->
        records.filter { item -> current.any { it.id == item.courseId || (item.courseId.isBlank() && it.name == item.courseName) } }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = combine(repository.tasks, courses, repository.currentSemester) { records, current, semester ->
        records.filter { item ->
            (item.semesterId == null || item.semesterId == semester?.id) &&
                (item.courseId.isBlank() || current.any { it.id == item.courseId })
        }
    }
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
    private val referenceCatalog = combine(repository.curriculumUniversities, repository.curriculumMajors, repository.curriculumVersions) { universities, majors, versions -> Triple(universities, majors, versions) }
    val curriculumMatchUiState: StateFlow<CurriculumMatchUiState> = combine(
        profile, courses, curriculumCourses, repository.studentAttempts, referenceCatalog
    ) { p, currentCoursesList, currCoursesList, attemptsList, catalog ->
        fun norm(value: String) = value.trim().replace("ي", "ی").replace("ك", "ک").replace("\u200C", "")
        val universityId = p.universityId ?: catalog.first.find { norm(it.displayNameFa) == norm(p.university) || norm(it.shortName) == norm(p.university) }?.id.orEmpty()
        val majorId = p.majorId ?: catalog.second.find { it.universityId == universityId && norm(it.majorDisplayNameFa) == norm(p.major) }?.id.orEmpty()
        val entryYear = p.entryYear
        val universities = catalog.first.map { ResolvedUniversity(it.id, it.displayNameFa, it.shortName) }
        val majors = catalog.second.map { ResolvedMajor(it.id, it.universityId, it.facultyId, it.majorDisplayNameFa) }
        val versions = catalog.third.map { ResolvedCurriculumVersion(it.id, it.majorId, it.title, it.entryYearMin, it.entryYearMax, it.totalCreditsRequired) }
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

                val resolvableCourses = currCoursesList.filter { it.majorId == majorId || (majorId == "MAJ_AUT_CHEM_ENG" && it.majorId == "CHEM_ENG") }.map { cc ->
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
        tasks,
        attendance
    ) { currentExams, tasksList, attendanceList ->
        StudyPlannerEngine.generateStudyPlan(
            exams = currentExams,
            tasks = tasksList,
            attendanceList = attendanceList,
            todayDate = com.example.domain.util.JalaliCalendarUtil.today().format("/")
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Shared Academic Context Graph (v2)
    val academicContextGraph: StateFlow<AcademicContextGraph> = combine(
        courses,
        coursesWithSessions,
        attendance,
        tasks,
        exams
    ) { coursesList, sessionsList, attendanceList, tasksList, examsList ->
        AcademicContextGraphBuilder.build(
            courses = coursesList,
            coursesWithSessions = sessionsList,
            attendance = attendanceList,
            tasks = tasksList,
            exams = examsList
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AcademicContextGraph(emptyList(), emptyList(), emptyList())
    )

    // Shared academic priorities (single source for Dashboard / future Copilot)
    val academicPriorities: StateFlow<List<AcademicPriorityItem>> = combine(
        courses,
        coursesWithSessions,
        attendance,
        tasks,
        exams
    ) { coursesList, sessionsList, attendanceList, tasksList, examsList ->
        AcademicPriorityEngine.rank(
            courses = coursesList,
            coursesWithSessions = sessionsList,
            attendance = attendanceList,
            tasks = tasksList,
            exams = examsList,
            todayWeekdayIndex = com.example.domain.util.JalaliCalendarUtil.getTodayWeekdayIndex(),
            todayDate = com.example.domain.util.JalaliCalendarUtil.today().format("/")
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Planning collisions derived from canonical tasks/exams
    val planningCollisions: StateFlow<List<AcademicPlanningCollision>> = combine(
        tasks,
        exams
    ) { tasksList, examsList ->
        AcademicPlanningCollisionEngine.detect(
            tasks = tasksList,
            exams = examsList
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

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
            com.example.data.cloud.worker.FirestoreSyncWorker.triggerImmediateSync(application)
            com.example.data.cloud.worker.BackendSyncWorker.triggerImmediateSync(application, dataType)
        } catch (e: Throwable) {
            android.util.Log.w("StudentViewModel", "Failed to trigger cloud sync: ${e.message}")
        }
    }

    val focusSession = preferencesRepository.focusSession
    val selectedSemesterPlan = preferencesRepository.selectedSemesterPlan
    val examReminderIds = preferencesRepository.examReminderIds
    private val _pomodoroSeconds = MutableStateFlow(focusSession.value.remainingSeconds(System.currentTimeMillis()))
    val pomodoroSeconds: StateFlow<Int> = _pomodoroSeconds.asStateFlow()
    private val _isPomodoroRunning = MutableStateFlow(focusSession.value.isRunning)
    val isPomodoroRunning: StateFlow<Boolean> = _isPomodoroRunning.asStateFlow()

    init {
        pomodoroJob = viewModelScope.launch {
            while (true) {
                val state = focusSession.value
                _pomodoroSeconds.value = state.remainingSeconds(System.currentTimeMillis())
                _isPomodoroRunning.value = state.isRunning && _pomodoroSeconds.value > 0
                if (preferencesRepository.completeFocusSession(System.currentTimeMillis())) {
                    addNotification("جلسه تمرکز تمام شد", "چند دقیقه استراحت کنید؛ سپس سراغ کار بعدی بروید.")
                }
                delay(500)
            }
        }
    }

    fun startStudyRecommendation(recommendation: StudySessionRecommendation) {
        if (!focusSession.value.isRunning) {
            setFocusDuration(recommendation.recommendedDurationMinutes.coerceIn(5, 120), recommendation.courseName)
            togglePomodoro()
        }
        preferencesRepository.acceptStudyRecommendation(recommendation.id)
        selectTab(AppTab.POMODORO)
    }

    fun setFocusDuration(minutes: Int, label: String = "تمرکز آزاد") {
        if (focusSession.value.isRunning) return
        val duration = minutes.coerceIn(5, 120) * 60
        preferencesRepository.saveFocusSession(com.example.domain.model.FocusSession(duration, duration, label = label))
        _pomodoroSeconds.value = duration
        com.example.data.local.AcademicReminderWorker.cancelFocus(getApplication())
    }

    fun togglePomodoro() {
        val state = focusSession.value
        val next = if (state.isRunning) state.pause(System.currentTimeMillis()) else state.start(System.currentTimeMillis())
        preferencesRepository.saveFocusSession(next)
        _pomodoroSeconds.value = next.remainingSeconds(System.currentTimeMillis())
        _isPomodoroRunning.value = next.isRunning
        if (next.isRunning) com.example.data.local.AcademicReminderWorker.scheduleFocus(getApplication(), next.endsAtMillis)
        else com.example.data.local.AcademicReminderWorker.cancelFocus(getApplication())
    }

    fun resetPomodoro() {
        preferencesRepository.saveFocusSession(focusSession.value.copy(pausedSeconds = focusSession.value.durationSeconds, endsAtMillis = 0))
        _isPomodoroRunning.value = false
        _pomodoroSeconds.value = focusSession.value.durationSeconds
        com.example.data.local.AcademicReminderWorker.cancelFocus(getApplication())
    }

    fun selectSemesterPlan(plan: SemesterPlan) {
        preferencesRepository.saveSemesterPlan(plan.copy(isDraft = false, isActive = true))
        _userMessage.tryEmit("برنامه هدف ذخیره شد؛ ثبت‌نام دانشگاه باید جداگانه انجام شود.")
    }

    fun importPersonalCurriculum(university: String, major: String, year: Int, credits: Int, text: String, onSaved: () -> Unit) {
        if (_isSavingRecord.value) return
        _isSavingRecord.value = true
        viewModelScope.launch {
            try {
                repository.savePersonalCurriculum(university, major, year, credits, text)
                _optimisticProfile.value = null
                preferencesRepository.saveSemesterPlan(null)
                onSaved()
                _userMessage.emit("چارت شخصی ذخیره شد؛ برای انتقال آن از پشتیبان کامل استفاده کنید.")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _userMessage.emit(e.localizedMessage ?: "چارت ذخیره نشد.") }
            finally { _isSavingRecord.value = false }
        }
    }

    fun cancelExamReminder(examId: String) {
        androidx.work.WorkManager.getInstance(getApplication()).cancelUniqueWork("reminder_$examId")
        preferencesRepository.setExamReminder(examId, false)
    }

    fun scheduleExamReminder(exam: ExamItem, minutesBefore: Int = 60) {
        viewModelScope.launch {
            try {
                require(notificationsEnabled.value) { "ابتدا اعلان‌ها را در تنظیمات برنامه فعال کنید." }
                require(androidx.core.app.NotificationManagerCompat.from(getApplication()).areNotificationsEnabled()) { "مجوز اعلان گوشی فعال نیست؛ آن را در تنظیمات فعال کنید." }
                val date = com.example.domain.util.AcademicInputValidator.examTimestamp(exam.solarDate, exam.time)
                    ?: error("برای یادآور، تاریخ و ساعت معتبر امتحان را ثبت کنید.")
                val trigger = date - minutesBefore.coerceIn(15, 1440) * 60_000L
                require(trigger > System.currentTimeMillis()) { "زمان این یادآور گذشته است؛ زمان نزدیک‌تری انتخاب کنید." }
                val courseId = exam.id.removePrefix("exam_")
                val request = androidx.work.OneTimeWorkRequestBuilder<com.example.data.local.AcademicReminderWorker>()
                    .setInitialDelay(trigger - System.currentTimeMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                    .setInputData(androidx.work.workDataOf("kind" to "exam", "courseId" to courseId, "date" to exam.solarDate, "time" to exam.time))
                    .addTag(com.example.data.local.AcademicReminderWorker.TAG).build()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    androidx.work.WorkManager.getInstance(getApplication()).enqueueUniqueWork("reminder_${exam.id}", androidx.work.ExistingWorkPolicy.REPLACE, request).result.get()
                }
                preferencesRepository.setExamReminder(exam.id, true)
                _userMessage.emit("یادآور $minutesBefore دقیقه قبل ثبت شد؛ زمان ارسال تابع محدودیت باتری گوشی است.")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _userMessage.emit(e.localizedMessage ?: "یادآور ثبت نشد؛ دوباره تلاش کنید.") }
        }
    }

    // 8 Semester Curriculum Map for Chemical Engineering
    val curriculumList = listOf(
        SemesterCurriculum("ترم 1 (پاییز)", 17, listOf("ریاضی عمومی 1", "فیزیک عمومی 1", "شیمی عمومی و آزمایشگاه", "زبان عمومی")),
        SemesterCurriculum("ترم 2 (بهار)", 18, listOf("ریاضی عمومی 2", "معادلات دیفرانسیل", "فیزیک عمومی 2", "شیمی آلی 1")),
        SemesterCurriculum("ترم 3 (ترم جاری)", 19, listOf("ترمودینامیک مهندسی شیمی 1", "مکانیک سیالات 1", "ریاضی مهندسی", "محاسبات عددی"), isCurrent = true),
        SemesterCurriculum("ترم 4 (پیش‌رو)", 20, listOf("ترمودینامیک مهندسی شیمی 2", "انتقال حرارت 1", "موازنه انرژی و مواد", "کنترل فرآیندها")),
        SemesterCurriculum("ترم 5", 18, listOf("انتقال جرم", "عملیات واحد 1", "سینتیک و طراحی رآکتور", "شیمی تجزیه")),
        SemesterCurriculum("ترم 6", 17, listOf("عملیات واحد 2", "انتقال حرارت 2", "کاربرد کامپیوتر در مهندسی شیمی", "ایمنی در صنایع نفت")),
        SemesterCurriculum("ترم 7", 16, listOf("طراحی فرآیند به کمک نرم‌افزار", "شبیه‌سازی فرآیندها", "آزمایشگاه عملیات واحد", "پروژه کارشناسی 1")),
        SemesterCurriculum("ترم 8 (فارغ‌التحصیلی)", 15, listOf("پروژه کارشناسی 2", "کارآموزی صنعت نفت و پتروشیمی", "اقتصاد مهندسی", "دروس عمومی اختیاری"))
    )

    // Actions
    fun saveCourse(course: CourseEntity, sessions: List<CourseSessionEntity> = emptyList(), onSaved: () -> Unit = {}) {
        if (_isSavingRecord.value) return
        _isSavingRecord.value = true
        viewModelScope.launch {
            try {
                repository.saveCourse(course, sessions)
                cancelExamReminder("exam_${course.id}")
                triggerCloudSync()
                onSaved()
                _userMessage.emit("درس ${course.name} ذخیره شد.")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _userMessage.emit("درس ذخیره نشد؛ ورودی‌ها را بررسی و دوباره تلاش کنید.") }
            finally { _isSavingRecord.value = false }
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            repository.deleteCourse(courseId)
            cancelExamReminder("exam_$courseId")
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

    fun saveTask(title: String, courseName: String, dueDate: String, courseId: String? = null, onSaved: () -> Unit = {}) {
        if (_isSavingRecord.value) return
        _isSavingRecord.value = true
        viewModelScope.launch {
            try {
                val date = com.example.domain.util.JalaliCalendarUtil.parse(dueDate) ?: error("تاریخ تحویل معتبر نیست.")
                require(title.isNotBlank()) { "عنوان تکلیف را وارد کنید." }
                val resolvedCourse = courses.value.find { if (!courseId.isNullOrBlank()) it.id == courseId else it.name == courseName }
                val currentSem = repository.getCurrentSemesterSync()
                repository.saveTask(TaskEntity(title = title.trim(), courseName = resolvedCourse?.name ?: courseName,
                    dueDate = date.format(), courseId = resolvedCourse?.id.orEmpty(), semesterId = currentSem?.id))
                triggerCloudSync()
                onSaved()
                _userMessage.emit("تکلیف ذخیره شد.")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _userMessage.emit(e.localizedMessage ?: "تکلیف ذخیره نشد؛ دوباره تلاش کنید.") }
            finally { _isSavingRecord.value = false }
        }
    }

    fun editTask(task: TaskEntity, title: String, courseName: String, dueDate: String, onSaved: () -> Unit = {}) {
        if (_isSavingRecord.value) return
        _isSavingRecord.value = true
        viewModelScope.launch {
            try {
                val date = com.example.domain.util.JalaliCalendarUtil.parse(dueDate) ?: error("تاریخ تحویل معتبر نیست.")
                val course = courses.value.find { it.name == courseName }
                repository.updateTask(task.copy(title = title.trim(), courseName = courseName, dueDate = date.format(), courseId = course?.id.orEmpty()))
                triggerCloudSync()
                onSaved()
                _userMessage.emit("تغییرات تکلیف ذخیره شد.")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _userMessage.emit(e.localizedMessage ?: "تکلیف ذخیره نشد.") }
            finally { _isSavingRecord.value = false }
        }
    }

    fun saveExamDetails(course: CourseEntity, date: String, time: String, location: String, onSaved: () -> Unit) {
        val dateValue = if (date.isBlank() && time.isBlank()) "" else com.example.domain.util.JalaliCalendarUtil.parse(date)?.format()
        val timeValue = if (date.isBlank() && time.isBlank()) "" else com.example.domain.util.AcademicInputValidator.time(time)
        if (dateValue == null || timeValue == null) { _userMessage.tryEmit("تاریخ و ساعت امتحان معتبر نیست."); return }
        val sessions = coursesWithSessions.value.find { it.course.id == course.id }?.sessions.orEmpty()
        saveCourse(course.copy(examDate = dateValue, examTime = timeValue, examLocation = location.trim()), sessions, onSaved)
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
            try {
            require(title.isNotBlank() && academicYear in 1300..1500 && termNumber in 1..20) { "عنوان، سال و شماره ترم را بررسی کنید." }
            val result = repository.startNewSemester(
                com.example.domain.usecase.NewSemesterRequest(
                    title = title,
                    academicYear = academicYear,
                    termNumber = termNumber,
                    newCourses = newCourses,
                    finalRecordedGpa = finalRecordedGpa
                )
            )
            preferencesRepository.clearAcademicChoices()
            com.example.data.local.AcademicReminderWorker.cancelAll(getApplication())
            addNotification(
                "آغاز ترم تحصیلی جدید",
                "ترم «${result.createdSemester.title}» به عنوان ترم جاری فعال شد و اطلاعات ترم قبلی با موفقیت بایگانی گردید."
            )
            triggerCloudSync()
            onSuccess()
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _userMessage.emit(e.localizedMessage ?: "انتقال ترم انجام نشد؛ داده‌های پیشین حفظ شد.") }
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

    fun updateGrade(grade: GradeEntity, newMid: Double, newFin: Double, onSaved: () -> Unit = {}) {
        if (_isSavingRecord.value) return
        _isSavingRecord.value = true
        viewModelScope.launch {
            try {
                repository.updateGrade(grade.copy(midtermGrade = newMid, finalGrade = newFin))
                triggerCloudSync()
                onSaved()
                _userMessage.emit("نمره ذخیره شد.")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _userMessage.emit(e.localizedMessage ?: "نمره ذخیره نشد.") }
            finally { _isSavingRecord.value = false }
        }
    }

    fun updateProfile(profileEntity: StudentProfileEntity) {
        _optimisticProfile.value = profileEntity
        preferencesRepository.recordSuccessfulSave()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.updateProfile(profileEntity)
            _optimisticProfile.value = null
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
            _optimisticProfile.value = null
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
            _optimisticProfile.value = null
            addNotification("به‌روزرسانی پروفایل", "مشخصات دانشگاهی شما با موفقیت ذخیره شد.")
            triggerCloudSync()
        }
    }

    fun saveNotes(notes: String) {
        viewModelScope.launch {
            try {
                val current = repository.getProfileSync() ?: profile.value
                repository.updateProfile(current.copy(notes = notes))
                _optimisticProfile.value = null
                triggerCloudSync()
                _userMessage.emit("یادداشت‌ها ذخیره شد.")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _userMessage.emit("یادداشت ذخیره نشد؛ متن را حفظ و دوباره تلاش کنید.") }
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
        drafts: List<com.example.data.parser.ParsedCourseDraft>, clearExisting: Boolean = false,
        studentName: String = "", studentId: String = "", university: String = "", major: String = "",
        entryYear: Int = 0, currentSemester: Int = 0, onSaved: () -> Unit = {}
    ) {
        if (_isSavingRecord.value) return
        _isSavingRecord.value = true
        viewModelScope.launch {
            try {
                require(drafts.isNotEmpty()) { "حداقل یک درس انتخاب کنید." }
                repository.importParsedCourses(drafts, clearExisting = clearExisting, studentName = studentName,
                    studentId = studentId, university = university, major = major, entryYear = entryYear, currentSemester = currentSemester)
                _optimisticProfile.value = null
                preferencesRepository.setOnboardingCompleted(true)
                preferencesRepository.recordSuccessfulSave()
                _isOnboardingCompleted.value = true
                triggerCloudSync()
                onSaved()
                _userMessage.emit("${drafts.size} جلسه درس ثبت شد؛ مشخصات و سوابق پیشین حفظ شد.")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _userMessage.emit("واردسازی انجام نشد؛ داده‌های پیشین حفظ شد. دوباره تلاش کنید.") }
            finally { _isSavingRecord.value = false }
        }
    }

    fun setOnboardingCompleted(
        completed: Boolean,
        name: String = "",
        studentId: String = "",
        university: String = "",
        major: String = "",
        entryYear: Int = 1403,
        currentSemester: Int = 1,
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

    fun savePastSemesterHistory(selectedSemester: Int, totalPassedCredits: Int, overallGpa: Double,
        summaries: List<com.example.ui.components.SemesterSummaryItem>) {
        viewModelScope.launch {
            try {
                require(selectedSemester in 1..20 && totalPassedCredits in 0..500 && overallGpa.isFinite() && overallGpa in 0.0..20.0) { "ترم، واحد و معدل واردشده معتبر نیست." }
                val parsed = summaries.map { summary ->
                    val units = com.example.data.local.util.DateTimeNormalizer.normalizeDigits(summary.units).toIntOrNull()
                    val gpa = com.example.data.local.util.DateTimeNormalizer.normalizeDigits(summary.gpa).replace('٫', '.').toDoubleOrNull()
                    require(units != null && units in 0..40 && gpa != null && gpa.isFinite() && gpa in 0.0..20.0) { "خلاصه ترم ${summary.semesterIndex} را بررسی کنید؛ مقدار نامعتبر ذخیره نمی‌شود." }
                    Triple(summary.semesterIndex, units, gpa)
                }
                val current = repository.getProfileSync() ?: profile.value
                val persist = suspend {
                    repository.updateProfile(current.copy(currentSemester = selectedSemester, passedUnits = totalPassedCredits,
                        declaredPassedCredits = totalPassedCredits, declaredGpa = overallGpa, term = "ترم $selectedSemester ${current.major}"))
                    parsed.forEach { (term, units, gpa) ->
                        repository.replaceSemesterSummary(com.example.data.local.entity.StudentCourseAttemptEntity(id = "sem_summary_$term", profileId = 1,
                            courseId = null, courseName = "مجموع دروس گذرانده ترم $term", units = units, semesterIndex = term, status = "PASSED", grade = gpa, source = "QUICK_SETUP"))
                        if (term < selectedSemester) repository.saveSemester(SemesterEntity(id = "history_summary_${current.entryYear}_$term", title = "خلاصه ترم $term",
                            academicYear = current.entryYear + (term - 1) / 2, termNumber = term, semesterNumber = term,
                            status = "ARCHIVED", isArchived = true, totalUnits = units, gpa = gpa))
                    }
                }
                if (repository.database != null) repository.database.withTransaction { persist() } else persist()
                _optimisticProfile.value = null
                triggerCloudSync()
                _userMessage.emit("سوابق تحصیلی ذخیره شد؛ ثبت دوباره همان ترم، رکورد تکراری نمی‌سازد.")
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _userMessage.emit(e.localizedMessage ?: "سوابق ذخیره نشد.") }
        }
    }

    fun executeCopilotPayload(payload: com.example.domain.model.CopilotPayload) {
        viewModelScope.launch {
            try {
                when (payload) {
                    is com.example.domain.model.CopilotPayload.AddTask -> saveTask(payload.title, payload.courseName, payload.dueDate)
                    is com.example.domain.model.CopilotPayload.CompleteTask -> {
                        val task = tasks.value.find { it.id.toString() == payload.taskId }
                            ?: tasks.value.filter { it.title == payload.taskTitle }.singleOrNull()
                            ?: error("تکلیف مشخصی پیدا نشد؛ آن را از صفحه کارها انتخاب کنید.")
                        if (!task.isCompleted) repository.updateTask(task.copy(isCompleted = true))
                        triggerCloudSync()
                        _userMessage.emit("تکلیف انجام‌شده ثبت شد.")
                    }
                    is com.example.domain.model.CopilotPayload.ChangeCurrentSemester -> {
                        require(payload.newSemesterIndex in 1..20)
                        val current = profile.value
                        if (current.currentSemester != payload.newSemesterIndex) startNewSemester(
                            "ترم ${payload.newSemesterIndex}", current.entryYear + (payload.newSemesterIndex - 1) / 2, payload.newSemesterIndex)
                    }
                    is com.example.domain.model.CopilotPayload.UpdatePastSemesterSummary -> {
                        require(payload.semesterIndex in 1..20 && payload.units in 0..40)
                        require(payload.gpa == null || (payload.gpa.isFinite() && payload.gpa in 0.0..20.0))
                        val id = "sem_summary_${payload.semesterIndex}"
                        val old = repository.dao.getStudentAttemptsSync(1).find { it.id == id }
                        repository.replaceSemesterSummary(com.example.data.local.entity.StudentCourseAttemptEntity(id = id, profileId = 1,
                            courseId = null, courseName = "مجموع دروس گذرانده ترم ${payload.semesterIndex}", units = payload.units,
                            semesterIndex = payload.semesterIndex, status = "PASSED", grade = payload.gpa, source = "USER_DECLARED"))
                        val current = repository.getProfileSync() ?: profile.value
                        val units = (current.passedUnits - (old?.units ?: 0) + payload.units).coerceAtLeast(0)
                        repository.updateProfile(current.copy(passedUnits = units, declaredPassedCredits = units))
                        _optimisticProfile.value = null
                        triggerCloudSync()
                        _userMessage.emit("خلاصه ترم ذخیره شد.")
                    }
                    is com.example.domain.model.CopilotPayload.ClearAttendanceWarning -> {
                        val item = attendance.value.filter { it.courseName == payload.courseName }.singleOrNull()
                            ?: error("درس مشخصی پیدا نشد؛ از صفحه حضور و غیاب انتخاب کنید.")
                        repository.updateAttendance(item.copy(absentCount = 0))
                        triggerCloudSync()
                        _userMessage.emit("شمار غیبت این درس صفر شد.")
                    }
                    is com.example.domain.model.CopilotPayload.QuickEnrollCourse -> {
                        val days = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه")
                        val day = days.indexOf(payload.dayOfWeek.trim())
                        val start = com.example.domain.util.AcademicInputValidator.time(payload.startTime)
                        val end = com.example.domain.util.AcademicInputValidator.time(payload.endTime)
                        require(day >= 0 && payload.units in 1..10 && payload.courseName.isNotBlank() && start != null && end != null && start < end) { "نام، واحد، روز یا ساعت درس معتبر نیست." }
                        require(courses.value.none { CourseIdentityNormalizer.normalize(it.name) == CourseIdentityNormalizer.normalize(payload.courseName) }) { "این درس ثبت شده؛ آن را در برنامه کلاسی ویرایش کنید." }
                        val id = java.util.UUID.randomUUID().toString()
                        saveCourse(CourseEntity(id = id, name = payload.courseName.trim(), colorHex = "#59652F", units = payload.units),
                            listOf(CourseSessionEntity(courseId = id, day = day, start = start, end = end, location = "")))
                    }
                    is com.example.domain.model.CopilotPayload.NavigateToTab -> runCatching { AppTab.valueOf(payload.tabName) }.getOrNull()?.let { selectTab(it) }
                    is com.example.domain.model.CopilotPayload.SimulateGpaTarget -> selectTab(AppTab.GRADES)
                }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { _userMessage.emit(e.localizedMessage ?: "این فرمان اجرا نشد؛ دوباره بررسی کنید.") }
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
        name: String = "دانشجوی جدید",
        studentId: String = "۴۰۳۰۰۰۰۱",
        university: String = "دانشگاه سراسری",
        major: String = "مهندسی",
        entryYear: Int = 1403,
        currentSemester: Int = 1
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
            val result = com.example.data.cloud.FirestoreSyncManager.restoreAllDataFromCloud(
                userId = user.uid,
                dao = db.studentDao(),
                curriculumDao = db.curriculumDao()
            )
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
            try {
                if (currentUser.value.isGuest || !com.example.data.api.backend.BackendConfig.isConfigured) {
                    onResult(false, "برای همگام‌سازی سرور، یک حساب سرور متصل لازم است.")
                    return@launch
                }
                SyncStatusStore.markSyncing(application)
                com.example.data.cloud.BackendSyncManager.pushAllData(application).getOrThrow()
                com.example.data.cloud.BackendSyncManager.pullAllData(application).getOrThrow()
                SyncStatusStore.markSynced(application)
                onResult(true, "اطلاعات با سرور همگام شد.")
            } catch (e: Exception) {
                SyncStatusStore.markNeedsAttention(application)
                onResult(false, "خطا در همگام‌سازی: ${e.message}")
            }
        }
    }

    fun signInWithGoogle(activityContext: android.content.Context? = null, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val res = authManager.signInWithGoogle(activityContext = activityContext)) {
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

    fun applyVerifiedSubscription(details: com.example.domain.model.SubscriptionDetails) {
        authManager.applyVerifiedSubscription(details)
    }

    fun signOutUser() {
        viewModelScope.launch {
            preferencesRepository.setGuestModeEnabled(true)
            preferencesRepository.clearAcademicChoices()
            com.example.data.local.AcademicReminderWorker.cancelAll(getApplication())
            SyncStatusStore.markLocal(application)
            authManager.signOutUser()
            // Clear onboarding preferences
            preferencesRepository.setOnboardingCompleted(false)
            _isOnboardingCompleted.value = false
            _optimisticProfile.value = null
            
            // Clear the local cache to prevent previous user data residue
            repository.clearToFreshSlate("دانشجو", "", "", "", 1403, 1)
            
            addNotification("خروج از حساب", "از حساب کاربری خارج شدید و به حالت مهمان تغییر کردید.")
        }
    }

    fun deleteUserAccount(onCompleted: () -> Unit) {
        viewModelScope.launch {
            val result = authManager.deleteUserAccount()
            if (result.isFailure) {
                _userMessage.emit(result.exceptionOrNull()?.localizedMessage ?: "حذف حساب انجام نشد؛ دوباره وارد حساب شوید و تلاش کنید.")
                return@launch
            }
            preferencesRepository.clearAcademicChoices()
            com.example.data.local.AcademicReminderWorker.cancelAll(getApplication())
            SyncStatusStore.markLocal(application)
            preferencesRepository.setGuestModeEnabled(true)
            repository.clearToFreshSlate("دانشجوی جدید", "", "", "", 1403, 1)
            _userMessage.emit("حساب کاربری حذف شد.")
            onCompleted()
        }
    }

    fun triggerManualCloudSync(onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value
        if (user.isGuest) {
            onResult(false, "لطفاً ابتدا وارد حساب کاربری خود شوید.")
            return
        }

        if (!authManager.usesFirebaseAccount) {
            syncWithBackendNow(onResult)
            return
        }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                SyncStatusStore.markSyncing(application)
                val db = AppDatabase.getDatabase(application)
                val dao = db.studentDao()
                val profile = dao.getProfileSync()
                val courses = dao.getAllCoursesIncludingArchivedSync()
                val grades = dao.getAllGradesSync()
                val tasks = dao.getAllTasksSync()
                val attendance = dao.getAllAttendanceSync()
                val exams = dao.getAllExamsSync()
                val notes = dao.getAllNotesSync()

                // 1. Push local changes to cloud
                val pushResult = com.example.data.cloud.FirestoreSyncManager.syncAllDataToCloud(
                    userId = user.uid,
                    profile = profile,
                    courses = courses,
                    grades = grades,
                    tasks = tasks,
                    attendance = attendance,
                    exams = exams,
                    notes = notes
                )

                // 2. Pull down any remote changes from cloud
                val pullResult = com.example.data.cloud.FirestoreSyncManager.restoreAllDataFromCloud(
                    userId = user.uid,
                    dao = dao,
                    curriculumDao = db.curriculumDao()
                )

                if (pushResult.isSuccess && pullResult.isSuccess) {
                    SyncStatusStore.markSynced(application)
                    withContext(kotlinx.coroutines.Dispatchers.Main) { onResult(true, "همگام‌سازی دوطرفه ابری با موفقیت کامل شد. ✨") }
                } else {
                    val errMsg = pushResult.exceptionOrNull()?.message ?: pullResult.exceptionOrNull()?.message ?: "خطای ناشناخته شبکه"
                    SyncStatusStore.markNeedsAttention(application)
                    withContext(kotlinx.coroutines.Dispatchers.Main) { onResult(false, "خطا در همگام‌سازی: $errMsg") }
                }
            } catch (e: Exception) {
                android.util.Log.e("StudentViewModel", "Manual cloud sync failed: ${e.message}", e)
                SyncStatusStore.markNeedsAttention(application)
                withContext(kotlinx.coroutines.Dispatchers.Main) { onResult(false, "خطا در اتصال به سرور: ${e.localizedMessage}") }
            }
        }
    }

    data class UndoAction(
        val message: String,
        val snapshot: String = "",
        val generation: Long = 0L
    )

    override fun onCleared() {
        super.onCleared()
        pomodoroJob?.cancel()
    }
}

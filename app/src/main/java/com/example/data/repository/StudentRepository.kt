package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.backup.LocalDataBackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.dao.CurriculumDao
import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.entity.CurriculumCourseEntity
import com.example.data.local.entity.CurriculumVersionEntity
import com.example.data.local.entity.MajorEntity
import com.example.data.local.entity.UniversityEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ProfessorEntity
import com.example.data.local.entity.SemesterEntity
import com.example.data.local.entity.StudentCourseAttemptEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.relation.CourseWithSessions
import com.example.data.parser.ParsedCourseDraft
import com.example.data.seed.CurriculumSeedData
import com.example.domain.usecase.NewSemesterRequest
import com.example.domain.usecase.SemesterTransitionResult
import com.example.domain.usecase.SemesterTransitionUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * StudentRepository serving as the single Source of Truth for Student OS.
 * All relational operations rely on stable IDs (courseId, semesterId, professorId).
 */
class StudentRepository(
    val dao: StudentDao,
    val curriculumDao: CurriculumDao? = null,
    val database: AppDatabase? = null
) {
    // Reactive Streams from Room
    val semesters: Flow<List<SemesterEntity>> = dao.getAllSemesters()
    val archivedSemesters: Flow<List<SemesterEntity>> = dao.getArchivedSemesters()
    val currentSemester: Flow<SemesterEntity?> = dao.getCurrentSemester()
    val courses: Flow<List<CourseEntity>> = dao.getCurrentSemesterCourses()
    val allCourses: Flow<List<CourseEntity>> = dao.getAllCourses()
    val allCoursesIncludingArchived: Flow<List<CourseEntity>> = dao.getAllCoursesIncludingArchived()
    val coursesWithSessions: Flow<List<CourseWithSessions>> = dao.getCurrentSemesterCoursesWithSessions()
    val allCoursesWithSessions: Flow<List<CourseWithSessions>> = dao.getAllCoursesWithSessions()
    val allCoursesWithSessionsIncludingArchived: Flow<List<CourseWithSessions>> = dao.getAllCoursesWithSessionsIncludingArchived()
    val attendance: Flow<List<AttendanceEntity>> = dao.getAllAttendance()
    val grades: Flow<List<GradeEntity>> = dao.getAllGrades()
    val tasks: Flow<List<TaskEntity>> = dao.getAllTasks()
    val professors: Flow<List<ProfessorEntity>> = dao.getAllProfessors()
    val exams: Flow<List<ExamEntity>> = dao.getAllExams()
    val notes: Flow<List<NoteEntity>> = dao.getAllNotes()
    val profile: Flow<StudentProfileEntity?> = dao.getProfile()
    val curriculumCourses: Flow<List<CurriculumCourseEntity>>? = curriculumDao?.getAllCurriculumCourses()
    val curriculumUniversities: Flow<List<UniversityEntity>> = curriculumDao?.getAllUniversities() ?: flowOf(emptyList())
    val curriculumMajors: Flow<List<MajorEntity>> = curriculumDao?.getAllMajors() ?: flowOf(emptyList())
    val curriculumVersions: Flow<List<CurriculumVersionEntity>> = curriculumDao?.getAllCurriculumVersions() ?: flowOf(emptyList())
    val studentAttempts: Flow<List<StudentCourseAttemptEntity>> = dao.getStudentAttempts(1)

    // ==========================================
    // Semester Lifecycle & History
    // ==========================================
    suspend fun getCurrentSemesterSync(): SemesterEntity? = withContext(Dispatchers.IO) {
        dao.getCurrentSemesterSync()
    }

    suspend fun saveSemester(semester: SemesterEntity) = withContext(Dispatchers.IO) {
        dao.insertSemester(semester.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun createNewSemester(
        title: String,
        academicYear: Int,
        termNumber: Int,
        setAsCurrent: Boolean = true
    ): SemesterEntity = withContext(Dispatchers.IO) {
        if (setAsCurrent) {
            dao.clearCurrentSemesterFlag()
        }
        val newSemester = SemesterEntity(
            id = "sem_${academicYear}_${termNumber}_${UUID.randomUUID().toString().take(6)}",
            title = title,
            year = academicYear,
            academicYear = academicYear,
            semesterNumber = termNumber,
            termNumber = termNumber,
            isCurrent = setAsCurrent,
            isArchived = false,
            status = if (setAsCurrent) "ACTIVE" else "UPCOMING",
            totalUnits = 0,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertSemester(newSemester)
        newSemester
    }

    suspend fun archiveSemester(semesterId: String) = withContext(Dispatchers.IO) {
        val sem = dao.getSemesterById(semesterId)
        if (sem != null) {
            dao.updateSemester(
                sem.copy(
                    isArchived = true,
                    isCurrent = false,
                    status = "ARCHIVED",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun startNewSemester(request: NewSemesterRequest): SemesterTransitionResult = withContext(Dispatchers.IO) {
        if (database != null) {
            val useCase = SemesterTransitionUseCase(database, dao)
            useCase.startNewSemester(request)
        } else {
            // Fallback manual transition
            val current = dao.getCurrentSemesterSync()
            if (current != null) {
                dao.updateSemester(current.copy(isCurrent = false, isArchived = true, status = "ARCHIVED"))
            }
            dao.clearCurrentSemesterFlag()
            val created = createNewSemester(request.title, request.academicYear, request.termNumber, true)
            SemesterTransitionResult(current, created, 0, 0)
        }
    }

    fun getCoursesForSemester(semesterId: String): Flow<List<CourseEntity>> =
        dao.getCoursesBySemester(semesterId)

    suspend fun getCoursesForSemesterSync(semesterId: String): List<CourseEntity> = withContext(Dispatchers.IO) {
        dao.getCoursesBySemesterSync(semesterId)
    }

    // ==========================================
    // Course Operations with Stable IDs & Duplicate Prevention
    // ==========================================
    suspend fun isDuplicateCourse(course: CourseEntity): Boolean = withContext(Dispatchers.IO) {
        val existingInSemester = dao.getCoursesBySemesterSync(course.semesterId)
        existingInSemester.any { existing ->
            if (existing.id == course.id) return@any false
            val sameCode = course.courseCode.isNotBlank() && existing.courseCode.trim().equals(course.courseCode.trim(), ignoreCase = true)
            val sameName = course.name.isNotBlank() && existing.name.trim().equals(course.name.trim(), ignoreCase = true)
            sameCode || sameName
        }
    }

    suspend fun saveCourse(
        course: CourseEntity,
        sessions: List<CourseSessionEntity> = emptyList()
    ) = withContext(Dispatchers.IO) {
        val courseId = if (course.id.isNotBlank()) course.id else "c_${UUID.randomUUID().toString().take(8)}"
        val entity = course.copy(id = courseId)
        dao.insertCourse(entity)

        // Sync course sessions
        dao.deleteSessionsByCourseId(courseId)
        if (sessions.isNotEmpty()) {
            val sessionsToInsert = sessions.mapIndexed { idx, s ->
                s.copy(
                    id = if (s.id.isNotBlank()) s.id else "sess_${courseId.take(8)}_$idx",
                    courseId = courseId
                )
            }
            dao.insertCourseSessions(sessionsToInsert)
        }

        // Ensure Attendance record with courseId as primary key
        val existingAtt = dao.getAttendanceByCourseId(courseId)
        if (existingAtt == null) {
            dao.insertAttendance(
                AttendanceEntity(
                    courseId = courseId,
                    courseName = entity.name,
                    absentCount = 0,
                    maxAllowed = if (entity.name.contains("آزمایشگاه") || entity.name.contains("کارگاه")) 2 else 3
                )
            )
        } else if (existingAtt.courseName != entity.name) {
            dao.updateAttendance(existingAtt.copy(courseName = entity.name))
        }

        // Existing grades are updated when present; a new course does not receive
        // a fabricated zero grade. A grade record is created only when the user
        // actually records a score.
        dao.getGradeByCourseId(courseId)?.let { existingGrade ->
            if (existingGrade.courseName != entity.name || existingGrade.units != entity.units) {
                dao.updateGrade(existingGrade.copy(courseName = entity.name, units = entity.units))
            }
        }

        // Sync Exam record if exam details are provided
        if (entity.examDate.isNotBlank() || entity.examTime.isNotBlank()) {
            dao.insertExam(
                ExamEntity(
                    id = "exam_$courseId",
                    courseId = courseId,
                    courseName = entity.name,
                    date = entity.examDate,
                    time = entity.examTime,
                    location = entity.examLocation
                )
            )
        }
    }

    suspend fun deleteCourse(courseId: String) = withContext(Dispatchers.IO) {
        val course = dao.getCourseById(courseId)
        dao.deleteCourseById(courseId)
        dao.deleteSessionsByCourseId(courseId)
        dao.deleteAttendanceByCourseId(courseId)
        dao.deleteGradeByCourseId(courseId)
        dao.deleteTasksByCourseId(courseId)
        dao.deleteExamByCourseId(courseId)
        if (course != null) {
            dao.deleteAttendanceForCourse(course.name, courseId)
            dao.deleteGradeForCourse(course.name, courseId)
            dao.deleteTasksForCourse(courseId, course.name)
        }
        dao.cleanupOrphans()
    }

    suspend fun archiveCourse(courseId: String) = withContext(Dispatchers.IO) {
        val course = dao.getCourseById(courseId)
        if (course != null) {
            dao.updateCourse(course.copy(isArchived = true))
        }
    }

    suspend fun restoreCourse(courseId: String) = withContext(Dispatchers.IO) {
        val course = dao.getCourseById(courseId)
        if (course != null) {
            dao.updateCourse(course.copy(isArchived = false))
        }
    }

    // ==========================================
    // Attendance & Grades
    // ==========================================
    suspend fun updateAttendance(attendanceEntity: AttendanceEntity) = withContext(Dispatchers.IO) {
        dao.insertAttendance(attendanceEntity)
    }

    suspend fun updateGrade(grade: GradeEntity) = withContext(Dispatchers.IO) {
        dao.updateGrade(grade)
    }

    // ==========================================
    // Tasks
    // ==========================================
    suspend fun saveTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        dao.insertTask(task)
    }

    suspend fun toggleTaskCompletion(task: TaskEntity) = withContext(Dispatchers.IO) {
        dao.updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    suspend fun deleteTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        dao.deleteTask(task)
    }

    // ==========================================
    // Exams & Notes & Professors
    // ==========================================
    suspend fun saveExam(exam: ExamEntity) = withContext(Dispatchers.IO) {
        dao.insertExam(exam)
    }

    suspend fun deleteExam(courseId: String) = withContext(Dispatchers.IO) {
        dao.deleteExamByCourseId(courseId)
    }

    suspend fun saveNote(note: NoteEntity) = withContext(Dispatchers.IO) {
        dao.insertNote(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        dao.deleteNote(id)
    }

    suspend fun saveProfessor(professor: ProfessorEntity) = withContext(Dispatchers.IO) {
        dao.insertProfessor(professor)
    }

    // ==========================================
    // Profile & Transcript Attempts
    // ==========================================
    suspend fun getProfileSync(): StudentProfileEntity? = withContext(Dispatchers.IO) {
        dao.getProfileSync()
    }

    suspend fun updateProfile(profile: StudentProfileEntity) = withContext(Dispatchers.IO) {
        dao.insertProfile(profile.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun setOnboardingCompleted(
        completed: Boolean,
        name: String = "",
        studentId: String = "",
        university: String = "",
        major: String = "",
        entryYear: Int = 0,
        currentSemester: Int = 0,
        passedCredits: Int = 0,
        declaredGpa: Double = 0.0
    ) = withContext(Dispatchers.IO) {
        val current = dao.getProfileSync() ?: StudentProfileEntity()
        val updated = current.copy(
            isOnboardingCompleted = completed,
            name = if (name.isNotBlank()) name else current.name,
            studentId = if (studentId.isNotBlank()) studentId else current.studentId,
            university = if (university.isNotBlank()) university else current.university,
            major = if (major.isNotBlank()) major else current.major,
            entryYear = if (entryYear > 0) entryYear else current.entryYear,
            currentSemester = if (currentSemester > 0) currentSemester else current.currentSemester,
            passedUnits = if (passedCredits > 0) passedCredits else current.passedUnits,
            declaredPassedCredits = if (passedCredits > 0) passedCredits else current.declaredPassedCredits,
            declaredGpa = if (declaredGpa > 0.0) declaredGpa else current.declaredGpa,
            term = if (currentSemester > 0 && major.isNotBlank()) "ترم $currentSemester $major" else current.term,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertProfile(updated)
    }

    suspend fun saveStudentAttempt(attempt: StudentCourseAttemptEntity) = withContext(Dispatchers.IO) {
        dao.insertStudentAttempt(attempt)
    }

    suspend fun deleteStudentAttempt(attemptId: String) = withContext(Dispatchers.IO) {
        dao.deleteStudentAttempt(attemptId)
    }

    suspend fun clearStudentAttempts() = withContext(Dispatchers.IO) {
        dao.clearStudentAttempts(1)
    }

    // ==========================================
    // Database Backup & Restore Delegation
    // ==========================================
    suspend fun exportFullBackupJson(): String = withContext(Dispatchers.IO) {
        LocalDataBackupManager.exportCompleteDatabaseJson(dao, curriculumDao)
    }

    suspend fun restoreFullBackupJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        LocalDataBackupManager.restoreDatabaseFromJson(jsonString, dao, curriculumDao)
    }

    // ==========================================
    // Bulk Course Import (Atomic Transaction)
    // ==========================================
    // Course Bulk Import & Parsed Text Ingestion
    // ==========================================
    suspend fun importParsedCourses(
        drafts: List<ParsedCourseDraft>,
        semesterId: String = "current",
        clearExisting: Boolean = false,
        studentName: String = "",
        studentId: String = "",
        university: String = "",
        major: String = "",
        entryYear: Int = 0,
        currentSemester: Int = 0
    ) {
        val performImport = suspend {
            val currentProfile = dao.getProfileSync()
            val resolvedSemNum = currentSemester.takeIf { it > 0 }
                ?: currentProfile?.currentSemester?.takeIf { it > 0 }
                ?: 0
            val resolvedMajor = major.ifBlank { currentProfile?.major.orEmpty() }
            val activeSemId = if (semesterId.isNotBlank() && semesterId != "current") {
                semesterId
            } else {
                "sem_current"
            }
            val baseEntryYear = entryYear.takeIf { it > 0 }
                ?: currentProfile?.entryYear?.takeIf { it > 0 }
                ?: 0
            val calculatedAcademicYear = if (baseEntryYear > 0 && resolvedSemNum > 0) {
                baseEntryYear + ((resolvedSemNum - 1) / 2)
            } else {
                0
            }

            // Ensure active current semester exists in database
            dao.clearCurrentSemesterFlag()
            dao.insertSemester(
                SemesterEntity(
                    id = activeSemId,
                    title = "ترم $resolvedSemNum ($resolvedMajor)",
                    year = calculatedAcademicYear,
                    academicYear = calculatedAcademicYear,
                    semesterNumber = resolvedSemNum,
                    termNumber = resolvedSemNum,
                    isCurrent = true,
                    isArchived = false,
                    totalUnits = drafts.sumOf { it.units }
                )
            )

            if (clearExisting) {
                dao.clearCourses()
                dao.clearCourseSessions()
                dao.clearAttendance()
                dao.clearGrades()
                dao.clearExams()
            }

            val colors = listOf("#10B981", "#3B82F6", "#F59E0B", "#EF4444", "#8B5CF6", "#0EA5E9", "#6366F1")

            // Group drafts by logical course identity to prevent duplicate course rows for multi-session classes
            val draftGroups = drafts.groupBy { draft ->
                val code = draft.courseCode.trim()
                val name = draft.name.trim().lowercase()
                val targetSem = if (draft.targetSemester > 0) "sem_${draft.targetSemester}" else activeSemId
                if (code.isNotBlank()) "code_${code}_sem_${targetSem}"
                else "name_${name}_sem_${targetSem}"
            }

            var colorIndex = 0
            val insertedCourses = mutableListOf<CourseEntity>()

            for ((_, groupDrafts) in draftGroups) {
                if (groupDrafts.isEmpty()) continue
                val primaryDraft = groupDrafts.maxWithOrNull(
                    compareBy<ParsedCourseDraft> { it.units }
                        .thenBy { it.examDate.isNotBlank() || it.examTime.isNotBlank() }
                        .thenBy { it.instructor.isNotBlank() }
                ) ?: groupDrafts.first()

                val courseId = UUID.randomUUID().toString()
                val itemSemId = if (primaryDraft.targetSemester > 0) "sem_${primaryDraft.targetSemester}" else activeSemId
                val bestProf = groupDrafts.firstOrNull { it.instructor.isNotBlank() }?.instructor ?: primaryDraft.instructor
                val bestExamDate = groupDrafts.firstOrNull { it.examDate.isNotBlank() }?.examDate ?: primaryDraft.examDate
                val bestExamTime = groupDrafts.firstOrNull { it.examTime.isNotBlank() }?.examTime ?: primaryDraft.examTime
                val bestExamLoc = groupDrafts.firstOrNull { it.examLocation.isNotBlank() }?.examLocation ?: primaryDraft.examLocation
                val bestNotes = groupDrafts.firstOrNull { it.notes.isNotBlank() }?.notes ?: primaryDraft.notes
                val bestUnits = groupDrafts.maxOf { it.units }

                val entity = CourseEntity(
                    id = courseId,
                    name = primaryDraft.name.trim(),
                    colorHex = colors[colorIndex % colors.size],
                    units = bestUnits,
                    semesterId = itemSemId,
                    courseCode = primaryDraft.courseCode,
                    professor = bestProf,
                    examDate = bestExamDate,
                    examTime = bestExamTime,
                    examLocation = bestExamLoc,
                    notes = bestNotes
                )
                dao.insertCourse(entity)
                insertedCourses.add(entity)

                // Insert sessions for this logical course (ensure at least 1 session exists)
                val validSessions = groupDrafts.filter { it.startTime.isNotBlank() && it.endTime.isNotBlank() }
                if (validSessions.isNotEmpty()) {
                    val sessions = validSessions.mapIndexed { sIdx, draft ->
                        CourseSessionEntity(
                            id = "sess_${courseId.take(8)}_$sIdx",
                            courseId = courseId,
                            day = draft.dayOfWeek.coerceIn(0, 6),
                            start = draft.startTime,
                            end = draft.endTime,
                            location = draft.location.ifBlank { "دانشکده" }
                        )
                    }
                    dao.insertCourseSessions(sessions)
                } else {
                    // Create default scheduled slot based on index
                    val day = (colorIndex % 5)
                    val start = if (colorIndex % 2 == 0) "08:00" else "10:00"
                    val end = if (colorIndex % 2 == 0) "10:00" else "12:00"
                    dao.insertCourseSession(
                        CourseSessionEntity(
                            id = "sess_${courseId.take(8)}_0",
                            courseId = courseId,
                            day = day,
                            start = start,
                            end = end,
                            location = "دانشکده"
                        )
                    )
                }
                colorIndex++

                // Attendance with courseId as primary key (counted once per course)
                dao.insertAttendance(
                    AttendanceEntity(
                        courseId = courseId,
                        courseName = primaryDraft.name.trim(),
                        absentCount = 0,
                        maxAllowed = if (primaryDraft.name.contains("آزمایشگاه") || primaryDraft.name.contains("کارگاه")) 2 else 3
                    )
                )

                // Grade with courseId foreign key (counted once per course)
                dao.insertGrade(
                    GradeEntity(
                        courseId = courseId,
                        courseName = primaryDraft.name.trim(),
                        units = bestUnits,
                        midtermGrade = 0.0,
                        finalGrade = 0.0
                    )
                )

                // Exam if date/time present (created once per course)
                if (bestExamDate.isNotBlank() || bestExamTime.isNotBlank()) {
                    dao.insertExam(
                        ExamEntity(
                            id = "exam_$courseId",
                            courseId = courseId,
                            courseName = primaryDraft.name.trim(),
                            date = bestExamDate,
                            time = bestExamTime,
                            location = bestExamLoc
                        )
                    )
                }
            }

            val current = currentProfile
            val totalUnits = insertedCourses.sumOf { it.units }
            val resolvedProfileMajor = major.ifBlank { current?.major.orEmpty() }
            val resolvedUniversity = university.ifBlank { current?.university.orEmpty() }
            val resolvedEntryYear = entryYear.takeIf { it > 0 } ?: current?.entryYear ?: 0
            val profileToSave = (current ?: StudentProfileEntity(id = 1)).copy(
                name = if (studentName.isNotBlank()) studentName else (current?.name ?: "دانشجو"),
                studentId = if (studentId.isNotBlank()) studentId else (current?.studentId ?: ""),
                university = resolvedUniversity,
                major = resolvedProfileMajor,
                entryYear = resolvedEntryYear,
                activeUnits = totalUnits,
                currentSemester = resolvedSemNum,
                term = if (resolvedSemNum > 0 && resolvedProfileMajor.isNotBlank()) "ترم $resolvedSemNum $resolvedProfileMajor" else (current?.term ?: ""),
                faculty = if (resolvedProfileMajor.isNotBlank()) "دانشکده $resolvedProfileMajor · $totalUnits واحد فعال" else (current?.faculty ?: ""),
                isOnboardingCompleted = true,
                updatedAt = System.currentTimeMillis()
            )
            dao.insertProfile(profileToSave)
        }

        if (database != null) {
            database.withTransaction { performImport() }
        } else {
            performImport()
        }
    }

    suspend fun completeQuickAcademicSetup(
        name: String = "دانشجو",
        studentId: String = "",
        university: String,
        major: String,
        entryYear: Int,
        currentSemester: Int,
        passedCredits: Int,
        currentGpa: Double,
        selectedCourses: List<CurriculumCourseEntity>
    ) = withContext(Dispatchers.IO) {
        val totalActiveUnits = selectedCourses.sumOf { it.units }
        val activeSemesterId = "sem_$currentSemester"
        val calculatedAcademicYear = entryYear + ((currentSemester - 1) / 2)

        val performSetup: suspend () -> Unit = {
            // 1. Seed complete curriculum catalog for this major if needed
            val fullCurriculum = CurriculumSeedData.getCoursesForMajor(major)
            if (fullCurriculum.isNotEmpty()) {
                curriculumDao?.insertCurriculumCourses(fullCurriculum)
            }

            // 2. Mark existing semesters as non-current and insert active current semester
            dao.clearCurrentSemesterFlag()
            dao.insertSemester(
                SemesterEntity(
                    id = activeSemesterId,
                    title = "ترم $currentSemester ($major)",
                    year = calculatedAcademicYear,
                    academicYear = calculatedAcademicYear,
                    semesterNumber = currentSemester,
                    termNumber = currentSemester,
                    isCurrent = true,
                    isArchived = false,
                    totalUnits = totalActiveUnits
                )
            )

            // 3. Update student profile
            val updatedProfile = StudentProfileEntity(
                id = 1,
                name = name.ifBlank { "دانشجو" },
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
                faculty = "دانشکده $major · $totalActiveUnits واحد فعال ترم جاری",
                isOnboardingCompleted = true,
                updatedAt = System.currentTimeMillis()
            )
            dao.insertProfile(updatedProfile)

            // 4. Passed credits/GPA remain user-declared summary values until real attempts are entered.

            // 5. Populate active enrolled courses for current semester.
            // 5. Populate active enrolled courses for current semester
            if (selectedCourses.isNotEmpty()) {
                dao.clearCourses()
                dao.clearCourseSessions()
                dao.clearAttendance()
                dao.clearGrades()
                dao.clearExams()

                val colors = listOf("#10B981", "#3B82F6", "#F59E0B", "#EF4444", "#8B5CF6", "#0EA5E9", "#6366F1")
                selectedCourses.forEachIndexed { index, cc ->
                    val courseId = UUID.randomUUID().toString()

                    val course = CourseEntity(
                        id = courseId,
                        name = cc.name,
                        colorHex = colors[index % colors.size],
                        units = cc.units,
                        semesterId = activeSemesterId,
                        courseCode = cc.code
                    )
                    dao.insertCourse(course)

                    // Attendance starts at zero absences; no schedule, professor, exam or grade is inferred.
                    val maxAllowed = if (cc.courseType == "آزمایشگاهی") 2 else 3
                    dao.insertAttendance(
                        AttendanceEntity(
                            courseId = courseId,
                            courseName = cc.name,
                            absentCount = 0,
                            maxAllowed = maxAllowed
                        )
                    )

                    // No grades or exams are synthesized during quick setup.
                }
            }
        }

        try {
            if (database != null) {
                database.withTransaction { performSetup() }
            } else {
                performSetup()
            }
        } catch (e: Exception) {
            android.util.Log.e("StudentRepository", "Error during completeQuickAcademicSetup withTransaction, fallback executing sequentially", e)
            performSetup()
        }
    }

    suspend fun resetDefaults() {
        dao.clearCourses()
        dao.clearAttendance()
        dao.clearGrades()
        dao.clearTasks()
        dao.clearExams()
        dao.clearStudentAttempts(1)
        AppDatabase.populateInitialData(dao, curriculumDao)
    }

    suspend fun loadRichDemoData() {
        val performDemo = suspend {
            dao.clearCourses()
            dao.clearAttendance()
            dao.clearGrades()
            dao.clearTasks()
            dao.clearExams()
            dao.clearStudentAttempts(1)

            val demoSemester = SemesterEntity(
                id = "sem_demo_3",
                title = "ترم ۳ (پاییز ۱۴۰۳)",
                year = 1403,
                academicYear = 1403,
                semesterNumber = 3,
                termNumber = 3,
                isCurrent = true,
                isArchived = false,
                totalUnits = 19
            )
            dao.insertSemester(demoSemester)

            val demoProfile = StudentProfileEntity(
                id = 1,
                name = "دانشجو",
                studentId = "",
                faculty = "دانشکده مهندسی شیمی و نفت · ۱۹ واحد فعال",
                term = "ترم ۳ مهندسی شیمی",
                activeUnits = 19,
                passedUnits = 54,
                notes = "فرمول‌های مهم ترمودینامیک و سیالات:\n• گاز ایده‌آل: PV = nRT\n• ضریب تراکم‌پذیری: Z = PV / RT",
                isOnboardingCompleted = true,
                universityId = "UNI_AUT",
                facultyId = "FAC_AUT_CHEM_OIL",
                majorId = "MAJ_AUT_CHEM_ENG",
                declaredPassedCredits = 54,
                declaredGpa = 17.40,
                updatedAt = System.currentTimeMillis()
            )
            dao.insertProfile(demoProfile)

            val demoCourses = listOf(
                CourseEntity(id = "c1", name = "محاسبات عددی", colorHex = "#10B981", units = 2, semesterId = "sem_demo_3", courseCode = "CE101", professor = "دکتر احمدی", examDate = "1403/10/23", examTime = "08:30", examLocation = "کلاس 107 فنی"),
                CourseEntity(id = "c2", name = "ریاضی مهندسی", colorHex = "#3B82F6", units = 3, semesterId = "sem_demo_3", courseCode = "CE102", professor = "دکتر حسینی", examDate = "1403/10/28", examTime = "08:30", examLocation = "کلاس 108 فنی"),
                CourseEntity(id = "c3", name = "ترمودینامیک مهندسی شیمی ۱", colorHex = "#F59E0B", units = 3, semesterId = "sem_demo_3", courseCode = "CE103", professor = "دکتر رضایی", examDate = "1403/10/26", examTime = "10:45", examLocation = "کلاس 108 فنی"),
                CourseEntity(id = "c4", name = "فیزیک ۲", colorHex = "#EF4444", units = 3, semesterId = "sem_demo_3", courseCode = "CE104", professor = "دکتر محمدی", examDate = "1403/10/19", examTime = "08:30", examLocation = "کلاس 107 فنی"),
                CourseEntity(id = "c6", name = "آزمایشگاه شیمی عمومی", colorHex = "#14B8A6", units = 1, semesterId = "sem_demo_3", courseCode = "CE105", professor = "مهندس کریمی", examDate = "1403/10/15", examTime = "14:00", examLocation = "آزمایشگاه"),
                CourseEntity(id = "c7", name = "تفسیر موضوعی قرآن", colorHex = "#8B5CF6", units = 2, semesterId = "sem_demo_3", courseCode = "CE106", professor = "استاد تقوی", examDate = "1403/11/02", examTime = "14:00", examLocation = "کلاس 118"),
                CourseEntity(id = "c8", name = "نقشه کشی صنعتی", colorHex = "#0EA5E9", units = 2, semesterId = "sem_demo_3", courseCode = "CE107", professor = "مهندس عباسی", examDate = "1403/10/18", examTime = "10:00", examLocation = "آتلیه 2"),
                CourseEntity(id = "c9", name = "مکانیک سیالات ۱", colorHex = "#6366F1", units = 3, semesterId = "sem_demo_3", courseCode = "CE108", professor = "دکتر اکبری", examDate = "1403/10/21", examTime = "10:45", examLocation = "کلاس 111 فنی")
            )
            dao.insertCourses(demoCourses)

            val demoSessions = listOf(
                CourseSessionEntity(id = "s_demo_c1", courseId = "c1", day = 0, start = "08:00", end = "10:00", location = "کلاس 107 فنی"),
                CourseSessionEntity(id = "s_demo_c2", courseId = "c2", day = 0, start = "14:00", end = "15:00", location = "کلاس 108 فنی"),
                CourseSessionEntity(id = "s_demo_c3_1", courseId = "c3", day = 0, start = "15:00", end = "16:00", location = "کلاس 108 فنی"),
                CourseSessionEntity(id = "s_demo_c3_2", courseId = "c3", day = 1, start = "10:00", end = "12:00", location = "کلاس 108 فنی"),
                CourseSessionEntity(id = "s_demo_c4", courseId = "c4", day = 1, start = "08:00", end = "10:00", location = "کلاس 107 فنی"),
                CourseSessionEntity(id = "s_demo_c6", courseId = "c6", day = 1, start = "14:00", end = "16:00", location = "آزمایشگاه شیمی ۱"),
                CourseSessionEntity(id = "s_demo_c7", courseId = "c7", day = 2, start = "08:00", end = "10:00", location = "کلاس 118 کشاورزی"),
                CourseSessionEntity(id = "s_demo_c8", courseId = "c8", day = 2, start = "10:00", end = "12:00", location = "کلاس 108 فنی"),
                CourseSessionEntity(id = "s_demo_c9", courseId = "c9", day = 2, start = "14:00", end = "16:00", location = "کلاس 111 فنی")
            )
            dao.insertCourseSessions(demoSessions)

            demoCourses.distinctBy { it.id }.forEach { c ->
                dao.insertAttendance(AttendanceEntity(courseId = c.id, courseName = c.name, absentCount = 1, maxAllowed = 3))
                dao.insertGrade(GradeEntity(courseId = c.id, courseName = c.name, units = c.units, midtermGrade = 5.5, finalGrade = 11.5))
                if (c.examDate.isNotBlank()) {
                    dao.insertExam(
                        ExamEntity(
                            id = "exam_${c.id}",
                            courseId = c.id,
                            courseName = c.name,
                            date = c.examDate,
                            time = c.examTime,
                            location = c.examLocation
                        )
                    )
                }
            }

            dao.insertTask(TaskEntity(title = "حل تمرین سری ۴ سیالات", courseName = "مکانیک سیالات ۱", dueDate = "1403/10/15", isCompleted = false, courseId = "c9", semesterId = "sem_demo_3"))
            dao.insertTask(TaskEntity(title = "پروژه شبیه‌سازی متلب", courseName = "محاسبات عددی", dueDate = "1403/10/20", isCompleted = true, courseId = "c1", semesterId = "sem_demo_3"))
        }

        if (database != null) {
            database.withTransaction { performDemo() }
        } else {
            performDemo()
        }
    }

    suspend fun clearAllUserData() {
        if (database != null) {
            database.withTransaction { dao.clearAllUserData() }
        } else {
            dao.clearAllUserData()
        }
    }

    suspend fun clearToFreshSlate(
        name: String = "دانشجو",
        studentId: String = "",
        university: String = "",
        major: String = "",
        entryYear: Int = 0,
        currentSemester: Int = 0
    ) {
        val performClear = suspend {
            dao.clearCourses()
            dao.clearAttendance()
            dao.clearGrades()
            dao.clearTasks()
            dao.clearExams()
            dao.clearStudentAttempts(1)

            val normalizedSemester = currentSemester.takeIf { it > 0 } ?: 0
            val normalizedYear = entryYear.takeIf { it > 0 } ?: 0
            val freshSem = SemesterEntity(
                id = "sem_current",
                title = if (normalizedSemester > 0) "ترم $normalizedSemester" else "ترم جاری",
                year = normalizedYear,
                academicYear = normalizedYear,
                semesterNumber = normalizedSemester,
                termNumber = normalizedSemester,
                isCurrent = true,
                isArchived = false,
                totalUnits = 0
            )
            dao.insertSemester(freshSem)

            val emptyProfile = StudentProfileEntity(
                id = 1,
                name = name,
                studentId = studentId,
                university = university,
                major = major,
                entryYear = entryYear,
                currentSemester = currentSemester,
                faculty = if (major.isNotBlank()) "دانشکده $major · ۰ واحد فعال" else "",
                term = if (currentSemester > 0 && major.isNotBlank()) "ترم $currentSemester $major" else "",
                activeUnits = 0,
                passedUnits = 0,
                notes = "",
                isOnboardingCompleted = true,
                universityId = null,
                facultyId = null,
                majorId = null,
                declaredPassedCredits = 0,
                declaredGpa = null,
                updatedAt = System.currentTimeMillis()
            )
            dao.insertProfile(emptyProfile)
        }

        if (database != null) {
            database.withTransaction { performClear() }
        } else {
            performClear()
        }
    }
}

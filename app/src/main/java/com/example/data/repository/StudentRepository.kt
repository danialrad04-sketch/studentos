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
    private fun normalizeReferenceText(value: String): String =
        value.trim().replace("\u200C", "").replace("ي", "ی").replace("ك", "ک")

    private suspend fun resolveReferenceIds(
        university: String,
        major: String
    ): Triple<String?, String?, String?> {
        val universities = curriculumDao?.getAllUniversitiesSync().orEmpty()
        val majors = curriculumDao?.getAllMajorsSync().orEmpty()

        val normalizedUniversity = normalizeReferenceText(university)
        val currentProfile = dao.getProfileSync()
        val preferredUniversity = universities.find { it.id == currentProfile?.universityId && normalizeReferenceText(it.displayNameFa) == normalizedUniversity }
        val universityRow = preferredUniversity ?: universities.firstOrNull {
            val display = normalizeReferenceText(it.displayNameFa)
            val short = normalizeReferenceText(it.shortName)
            normalizedUniversity == display ||
                normalizedUniversity == short ||
                (normalizedUniversity.contains("امیرکبیر") && display.contains("امیرکبیر"))
        }
        val preferredMajor = majors.find { it.id == currentProfile?.majorId && it.universityId == universityRow?.id && normalizeReferenceText(it.majorDisplayNameFa) == normalizeReferenceText(major) }
        val majorRow = preferredMajor ?: majors.firstOrNull {
            normalizeReferenceText(it.majorDisplayNameFa) == normalizeReferenceText(major) &&
                (universityRow != null && it.universityId == universityRow.id)
        }

        return Triple(universityRow?.id, majorRow?.id, majorRow?.facultyId)
    }

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

    suspend fun replaceSemesterSummary(attempt: StudentCourseAttemptEntity) = withContext(Dispatchers.IO) {
        val persist = suspend {
            dao.deleteLegacySemesterSummaries(attempt.profileId, attempt.semesterIndex)
            dao.insertStudentAttempt(attempt)
        }
        if (database != null) database.withTransaction { persist() } else persist()
    }

    suspend fun savePersonalCurriculum(university: String, major: String, year: Int, requiredCredits: Int, text: String) = withContext(Dispatchers.IO) {
        require(university.isNotBlank() && major.isNotBlank() && year in 1300..1500 && requiredCredits in 1..500) { "دانشگاه، رشته، سال ورود و تعداد واحد کل را بررسی کنید." }
        val catalog = curriculumDao ?: error("ذخیره چارت در دسترس نیست.")
        fun stable(value: String) = java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }.take(20)
        val uniId = "USER_UNI_${stable(normalizeReferenceText(university))}"
        val majorId = "USER_MAJOR_${stable(uniId + normalizeReferenceText(major))}"
        val versionId = "USER_CHART_${majorId}_${year}"
        val rows = com.example.data.parser.ManualCurriculumParser.parse(text, majorId)
        val persist = suspend {
            catalog.insertUniversities(listOf(UniversityEntity(uniId, university.trim(), university.trim())))
            catalog.insertMajors(listOf(MajorEntity(majorId, uniId, "USER_FAC", major.trim(), major.trim())))
            catalog.upsertPersonalCurriculumVersion(CurriculumVersionEntity(versionId, majorId, "چارت شخصی ${major.trim()} · ورودی $year", year, year, requiredCredits))
            catalog.deletePersonalCourses(majorId)
            catalog.insertCurriculumCourses(rows)
            val p = dao.getProfileSync() ?: StudentProfileEntity()
            dao.insertProfile(p.copy(university = university.trim(), major = major.trim(), entryYear = year,
                universityId = uniId, majorId = majorId, facultyId = "USER_FAC", updatedAt = System.currentTimeMillis()))
        }
        if (database != null) database.withTransaction { persist() } else persist()
    }

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
        val persistCourse = suspend {
            val courseId = if (course.id.isNotBlank()) course.id else "c_${UUID.randomUUID().toString().take(8)}"
            val activeSemester = dao.getCurrentSemesterSync()
            val entity = course.copy(id = courseId, semesterId = if (course.semesterId.isBlank() || course.semesterId == "current") activeSemester?.id ?: "current" else course.semesterId)
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

            // Removing exam fields must also remove the previously persisted exam.
            if (entity.examDate.isBlank() && entity.examTime.isBlank()) dao.deleteExamByCourseId(courseId)
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
        // Readers must see the complete course and session set, including on updates.
        if (database != null) {
            database.withTransaction { persistCourse() }
        } else {
            persistCourse()
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

    /** Read and write together so rapid taps do not lose an increment or reset a custom limit. */
    suspend fun editAttendance(identifier: String, delta: Int = 0, count: Int? = null, limit: Int? = null): AttendanceEntity = withContext(Dispatchers.IO) {
        require(limit == null || limit in 1..16) { "سقف غیبت باید بین ۱ و ۱۶ جلسه باشد." }
        val persist: suspend () -> AttendanceEntity = {
            val records = dao.getAllAttendanceSync()
            val course = dao.getAllCoursesIncludingArchivedSync().find { it.id == identifier || it.name == identifier }
            val existing = records.find { it.courseId == (course?.id ?: identifier) }
                ?: records.find { it.courseName == identifier }
            require(course != null || existing != null) { "درس پیدا نشد." }
            val name = course?.name ?: existing!!.courseName
            val fallbackLimit = if (name.contains("آزمایشگاه") || name.contains("کارگاه")) 2 else 3
            AttendanceEntity(
                courseId = course?.id ?: existing!!.courseId, courseName = name,
                absentCount = (count ?: ((existing?.absentCount ?: 0) + delta)).coerceIn(0, 50),
                maxAllowed = limit ?: existing?.maxAllowed?.takeIf { it > 0 } ?: fallbackLimit
            ).also { dao.insertAttendance(it) }
        }
        if (database != null) database.withTransaction { persist() } else persist()
    }

    suspend fun updateGrade(grade: GradeEntity) = withContext(Dispatchers.IO) {
        require(grade.midtermGrade.isFinite() && grade.finalGrade.isFinite() && grade.midtermGrade >= 0 && grade.finalGrade >= 0 && grade.midtermGrade + grade.finalGrade <= 20) { "مجموع نمره باید بین صفر و ۲۰ باشد." }
        val persist = suspend {
            val existing = dao.getGradeByCourseId(grade.courseId)
            dao.insertGrade(grade.copy(id = existing?.id ?: grade.id, isRecorded = true))
        }
        if (database != null) database.withTransaction { persist() } else persist()
    }

    // ==========================================
    // Tasks
    // ==========================================
    suspend fun saveTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        com.example.domain.engine.TaskPlanningEngine.validate(task)
        val normalized = task.copy(reminderTime = com.example.domain.util.AcademicInputValidator.time(task.reminderTime).orEmpty())
        normalized.copy(id = dao.insertTask(normalized))
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        require(task.id != 0L)
        com.example.domain.engine.TaskPlanningEngine.validate(task)
        dao.updateTask(task.copy(reminderTime = com.example.domain.util.AcademicInputValidator.time(task.reminderTime).orEmpty()))
    }

    suspend fun toggleTaskCompletion(task: TaskEntity, today: com.example.domain.util.JalaliDate = com.example.domain.util.JalaliCalendarUtil.today()) =
        setTaskCompletion(task.id, null, today)

    suspend fun markTaskCompleted(id: Long, today: com.example.domain.util.JalaliDate = com.example.domain.util.JalaliCalendarUtil.today()) =
        setTaskCompletion(id, true, today)

    private suspend fun setTaskCompletion(id: Long, completed: Boolean?, today: com.example.domain.util.JalaliDate) = withContext(Dispatchers.IO) {
        val persist: suspend () -> TaskEntity? = {
            val stored = dao.getTaskById(id)
            if (stored != null) {
                val updated = stored.copy(isCompleted = completed ?: !stored.isCompleted)
                dao.updateTask(updated)
                val semester = stored.semesterId?.let { dao.getSemesterById(it) }
                val course = stored.courseId.takeIf { it.isNotBlank() }?.let { dao.getCourseById(it) }
                if (updated.isCompleted && (stored.semesterId == null || (semester?.isCurrent == true && !semester.isArchived)) &&
                    (stored.courseId.isBlank() || (course != null && !course.isArchived)) && dao.getNextOccurrence(stored.id) == null) {
                    com.example.domain.engine.TaskPlanningEngine.nextDate(stored.dueDate, stored.repeatEveryDays, today)?.let { next ->
                        dao.insertTaskOccurrence(stored.copy(id = 0, dueDate = next, isCompleted = false, recurrenceParentId = stored.id))
                    }
                }
                updated
            } else null
        }
        if (database != null) database.withTransaction { persist() } else persist()
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

    /** Optimistic note edits preserve concurrent sync updates and archive state. */
    suspend fun saveStudyNote(note: NoteEntity, creating: Boolean): NoteEntity = withContext(Dispatchers.IO) {
        val save: suspend () -> NoteEntity = {
            require(note.id.isNotBlank() && note.id.length <= 128) { "شناسهٔ یادداشت معتبر نیست." }
            require(note.title.trim().length in 1..180 && note.content.isNotBlank() && note.content.length <= 30000) { "عنوان و متن یادداشت را بررسی کنید." }
            val existing = dao.getNoteById(note.id)
            check(if (creating) existing == null else existing != null && existing.updatedAt == note.updatedAt) { "یادداشت تغییر کرده است؛ متن را نگه دارید و نسخهٔ تازه را باز کنید." }
            check(existing?.isArchived != true) { "ابتدا یادداشت را از بایگانی برگردانید." }
            val courseId = note.courseId?.ifBlank { null }
            require(courseId == null || dao.getCourseById(courseId) != null || existing?.courseId == courseId) { "درس انتخاب‌شده پیدا نشد." }
            val saved = note.copy(title = note.title.trim(), courseId = courseId, isArchived = false,
                updatedAt = maxOf(System.currentTimeMillis(), (existing?.updatedAt ?: 0L) + 1))
            dao.insertNote(saved); saved
        }
        if (database != null) database.withTransaction { save() } else save()
    }

    suspend fun setNoteArchived(note: NoteEntity, archived: Boolean): NoteEntity = withContext(Dispatchers.IO) {
        val save: suspend () -> NoteEntity = {
            val existing = dao.getNoteById(note.id)
            check(existing != null && existing.updatedAt == note.updatedAt) { "نسخهٔ یادداشت تغییر کرده؛ دوباره آن را بررسی کنید." }
            val saved = existing.copy(isArchived = archived, updatedAt = maxOf(System.currentTimeMillis(), existing.updatedAt + 1))
            dao.insertNote(saved); saved
        }
        if (database != null) database.withTransaction { save() } else save()
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
        require(profile.passedUnits >= 0 && profile.activeUnits >= 0) { "تعداد واحد نمی‌تواند منفی باشد." }
        require(profile.declaredGpa == null || (profile.declaredGpa.isFinite() && profile.declaredGpa in 0.0..20.0)) { "معدل باید بین صفر و بیست باشد." }
        val (universityId, majorId, facultyId) = resolveReferenceIds(profile.university, profile.major)
        dao.insertProfile(profile.copy(universityId = universityId, majorId = majorId, facultyId = facultyId, updatedAt = System.currentTimeMillis()))
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
        val resolvedUniversity = university.ifBlank { current.university }
        val resolvedMajor = major.ifBlank { current.major }
        val (referenceUniversityId, referenceMajorId, referenceFacultyId) =
            resolveReferenceIds(resolvedUniversity, resolvedMajor)
        val updated = current.copy(
            isOnboardingCompleted = completed,
            name = if (name.isNotBlank()) name else current.name,
            studentId = if (studentId.isNotBlank()) studentId else current.studentId,
            university = resolvedUniversity,
            major = resolvedMajor,
            entryYear = if (entryYear > 0) entryYear else current.entryYear,
            currentSemester = if (currentSemester > 0) currentSemester else current.currentSemester,
            passedUnits = if (passedCredits > 0) passedCredits else current.passedUnits,
            declaredPassedCredits = if (passedCredits > 0) passedCredits else current.declaredPassedCredits,
            declaredGpa = if (declaredGpa > 0.0) declaredGpa else current.declaredGpa,
            term = if (currentSemester > 0 && resolvedMajor.isNotBlank()) "ترم $currentSemester $resolvedMajor" else current.term,
            universityId = referenceUniversityId,
            facultyId = referenceFacultyId,
            majorId = referenceMajorId,
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
        try {
            val restore = suspend { LocalDataBackupManager.restoreDatabaseFromJson(jsonString, dao, curriculumDao).getOrThrow() }
            val count = if (database != null) database.withTransaction { restore() } else restore()
            Result.success(count)
        } catch (error: kotlinx.coroutines.CancellationException) { throw error }
        catch (error: Exception) { Result.failure(error) }
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
            val activeSemester = dao.getCurrentSemesterSync()
            val resolvedSemNum = currentSemester.takeIf { it > 0 }
                ?: currentProfile?.currentSemester?.takeIf { it > 0 }
                ?: 0
            val resolvedMajor = major.ifBlank { currentProfile?.major.orEmpty() }
            val activeSemId = if (semesterId.isNotBlank() && semesterId != "current") {
                semesterId
            } else {
                activeSemester?.id ?: "sem_current"
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
                (activeSemester?.takeIf { it.id == activeSemId } ?: SemesterEntity(
                    id = activeSemId,
                    title = "ترم $resolvedSemNum ($resolvedMajor)",
                    year = calculatedAcademicYear,
                    academicYear = calculatedAcademicYear,
                    semesterNumber = resolvedSemNum,
                    termNumber = resolvedSemNum,
                    isCurrent = true,
                    isArchived = false,
                    totalUnits = drafts.sumOf { it.units }
                )).copy(isCurrent = true, isArchived = false)
            )

            if (clearExisting) {
                dao.clearCourses()
                dao.clearCourseSessions()
                dao.clearAttendance()
                dao.clearGrades()
                dao.clearExams()
            }

            val colors = listOf("#10B981", "#3B82F6", "#F59E0B", "#EF4444", "#C0CD8C", "#0EA5E9", "#C0CD8C")

            // Group drafts by logical course identity to prevent duplicate course rows for multi-session classes
            val draftGroups = drafts.groupBy { draft ->
                val code = draft.courseCode.trim()
                val name = draft.name.trim().lowercase()
                val targetSem = if (draft.targetSemester > 0 && draft.targetSemester != resolvedSemNum) "sem_${draft.targetSemester}" else activeSemId
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

                val itemSemId = if (primaryDraft.targetSemester > 0 && primaryDraft.targetSemester != resolvedSemNum) "sem_${primaryDraft.targetSemester}" else activeSemId
                // Re-running setup/import must not duplicate a course or reset its records.
                val existingCourse = dao.getCoursesBySemesterSync(itemSemId).firstOrNull {
                    (primaryDraft.courseCode.isNotBlank() && it.courseCode == primaryDraft.courseCode.trim()) ||
                        it.name.trim() == primaryDraft.name.trim()
                }
                val courseId = existingCourse?.id ?: UUID.randomUUID().toString()
                val bestProf = groupDrafts.firstOrNull { it.instructor.isNotBlank() }?.instructor ?: primaryDraft.instructor
                val bestExamDate = groupDrafts.firstOrNull { it.examDate.isNotBlank() }?.examDate ?: primaryDraft.examDate
                val bestExamTime = groupDrafts.firstOrNull { it.examTime.isNotBlank() }?.examTime ?: primaryDraft.examTime
                val bestExamLoc = groupDrafts.firstOrNull { it.examLocation.isNotBlank() }?.examLocation ?: primaryDraft.examLocation
                val bestNotes = groupDrafts.firstOrNull { it.notes.isNotBlank() }?.notes ?: primaryDraft.notes
                val bestUnits = groupDrafts.maxOf { it.units }

                val entity = (existingCourse ?: CourseEntity(id = courseId, name = primaryDraft.name.trim(),
                    colorHex = colors[colorIndex % colors.size])).copy(
                    name = primaryDraft.name.trim(),
                    units = bestUnits,
                    semesterId = itemSemId,
                    courseCode = primaryDraft.courseCode.trim().ifBlank { existingCourse?.courseCode.orEmpty() },
                    professor = bestProf.ifBlank { existingCourse?.professor.orEmpty() },
                    examDate = bestExamDate.ifBlank { existingCourse?.examDate.orEmpty() },
                    examTime = bestExamTime.ifBlank { existingCourse?.examTime.orEmpty() },
                    examLocation = bestExamLoc.ifBlank { existingCourse?.examLocation.orEmpty() },
                    notes = bestNotes.ifBlank { existingCourse?.notes.orEmpty() }
                )
                dao.insertCourse(entity)
                insertedCourses.add(entity)

                // An unknown class time remains unscheduled; never invent a timetable.
                val existingSessions = dao.getSessionsForCourseSync(courseId)
                val validSessions = groupDrafts.filter { it.dayOfWeek in 0..6 && it.startTime.isNotBlank() && it.endTime.isNotBlank() }
                    .distinctBy { Triple(it.dayOfWeek, it.startTime, it.endTime) }
                    .filter { draft -> existingSessions.none { it.day == draft.dayOfWeek && it.start == draft.startTime && it.end == draft.endTime } }
                if (validSessions.isNotEmpty()) {
                    val sessions = validSessions.map { draft ->
                        CourseSessionEntity(
                            id = "sess_${UUID.randomUUID()}",
                            courseId = courseId,
                            day = draft.dayOfWeek.coerceIn(0, 6),
                            start = draft.startTime,
                            end = draft.endTime,
                            location = draft.location
                        )
                    }
                    dao.insertCourseSessions(sessions)
                }
                colorIndex++

                val existingAttendance = dao.getAttendanceByCourseId(courseId)
                dao.insertAttendance(existingAttendance?.copy(courseName = entity.name) ?: AttendanceEntity(
                    courseId = courseId, courseName = entity.name, absentCount = 0,
                    maxAllowed = if (entity.name.contains("آزمایشگاه") || entity.name.contains("کارگاه")) 2 else 3
                ))
                val existingGrade = dao.getGradeByCourseId(courseId)
                dao.insertGrade(existingGrade?.copy(courseName = entity.name, units = entity.units) ?: GradeEntity(
                    courseId = courseId, courseName = entity.name, units = entity.units
                ))

                // Exam if date/time present (created once per course)
                if (bestExamDate.isNotBlank() || bestExamTime.isNotBlank()) {
                    dao.insertExam(
                        ExamEntity(
                            id = "exam_$courseId",
                            courseId = courseId,
                            courseName = primaryDraft.name.trim(),
                            date = entity.examDate,
                            time = entity.examTime,
                            location = entity.examLocation
                        )
                    )
                }
            }

            val current = currentProfile
            val totalUnits = dao.getCoursesBySemesterSync(activeSemId).filter { !it.isArchived }.sumOf { it.units }
            dao.getSemesterById(activeSemId)?.let { dao.updateSemester(it.copy(totalUnits = totalUnits, updatedAt = System.currentTimeMillis())) }
            val resolvedProfileMajor = major.ifBlank { current?.major.orEmpty() }
            val resolvedUniversity = university.ifBlank { current?.university.orEmpty() }
            val resolvedEntryYear = entryYear.takeIf { it > 0 } ?: current?.entryYear ?: 0
            val (referenceUniversityId, referenceMajorId, referenceFacultyId) =
                resolveReferenceIds(resolvedUniversity, resolvedProfileMajor)
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
                universityId = referenceUniversityId,
                facultyId = referenceFacultyId,
                majorId = referenceMajorId,
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
        currentGpa: Double?,
        selectedCourses: List<CurriculumCourseEntity>,
        replaceExisting: Boolean = true
    ) = withContext(Dispatchers.IO) {
        require(university.isNotBlank()) { "دانشگاه باید مشخص شده باشد." }
        require(major.isNotBlank()) { "رشته باید مشخص شده باشد." }
        require(entryYear > 0) { "سال ورود باید مشخص شده باشد." }
        require(currentSemester > 0) { "ترم جاری باید مشخص شده باشد." }
        require(passedCredits >= 0) { "واحد گذرانده نمی‌تواند منفی باشد." }
        require(currentGpa == null || (currentGpa.isFinite() && currentGpa in 0.0..20.0)) { "معدل باید بین صفر و بیست باشد." }

        val totalActiveUnits = selectedCourses.sumOf { it.units }
        val activeSemesterId = if (replaceExisting) "sem_$currentSemester" else
            dao.getCurrentSemesterSync()?.id ?: "sem_$currentSemester"
        val calculatedAcademicYear = entryYear + ((currentSemester - 1) / 2)
        val (universityId, majorId, facultyId) = resolveReferenceIds(university, major)

        val performSetup: suspend () -> Unit = {
            val previousProfile = dao.getProfileSync() ?: StudentProfileEntity()
            val existingCourses = if (replaceExisting) emptyList() else dao.getAllCoursesIncludingArchivedSync()
            val coursesToAdd = selectedCourses.filter { selected ->
                existingCourses.none { existing -> existing.semesterId == activeSemesterId &&
                    ((selected.code.isNotBlank() && existing.courseCode == selected.code) || existing.name == selected.name) }
            }
            val resolvedActiveUnits = if (replaceExisting) totalActiveUnits else
                existingCourses.filter { it.semesterId == activeSemesterId }.sumOf { it.units } + coursesToAdd.sumOf { it.units }
            // 1. Seed complete curriculum catalog for this major if needed
            val fullCurriculum = CurriculumSeedData.getCoursesForMajor(major)
            if (fullCurriculum.isNotEmpty()) {
                curriculumDao?.insertCurriculumCourses(fullCurriculum)
            }

            // 2. Mark existing semesters as non-current and insert active current semester
            val existingSemester = if (replaceExisting) null else dao.getSemesterById(activeSemesterId)
            dao.clearCurrentSemesterFlag()
            dao.insertSemester(
                (existingSemester ?: SemesterEntity(id = activeSemesterId, title = "ترم $currentSemester ($major)")).copy(
                    year = calculatedAcademicYear, academicYear = calculatedAcademicYear,
                    semesterNumber = currentSemester, termNumber = currentSemester,
                    isCurrent = true, isArchived = false, totalUnits = resolvedActiveUnits
                )
            )

            // 3. Update student profile
            val updatedProfile = previousProfile.copy(
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
                activeUnits = resolvedActiveUnits,
                term = "ترم $currentSemester $major",
                faculty = "دانشکده $major · $resolvedActiveUnits واحد فعال ترم جاری",
                isOnboardingCompleted = true,
                universityId = universityId,
                facultyId = facultyId,
                majorId = majorId,
                updatedAt = System.currentTimeMillis()
            )
            dao.insertProfile(updatedProfile)

            // 4. Passed credits/GPA remain user-declared summary values until real attempts are entered.

            // 5. Populate active enrolled courses for current semester.
            // Always replace the current local course set; an empty selection means
            // an intentionally empty semester, not "keep whatever was there before".
            if (replaceExisting) {
                dao.clearCourses()
                dao.clearCourseSessions()
                dao.clearAttendance()
                dao.clearGrades()
                dao.clearExams()
            }

            if (selectedCourses.isNotEmpty()) {
                val colors = listOf("#10B981", "#3B82F6", "#F59E0B", "#EF4444", "#C0CD8C", "#0EA5E9", "#C0CD8C")
                coursesToAdd.forEachIndexed { index, cc ->
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
            android.util.Log.e("StudentRepository", "Error during completeQuickAcademicSetup transaction", e)
            throw e
        }
    }

    suspend fun resetDefaults() {
        val reset = suspend {
            // Reset is a full local-data operation, not a partial field cleanup.
            // Reference curriculum data is preserved; personal academic records are recreated blank.
            dao.clearAllUserData()
            AppDatabase.populateInitialData(dao, curriculumDao)
        }

        if (database != null) {
            database.withTransaction { reset() }
        } else {
            reset()
        }
    }

    suspend fun loadRichDemoData() {
        val performDemo = suspend {
            dao.clearStudyGoals()
            dao.clearStudyAvailability()
            dao.clearStudyCompletions()
            dao.clearReviewCards()
            dao.clearCourses()
            dao.clearAttendance()
            dao.clearGrades()
            dao.clearTasks()
            dao.clearExams()
            dao.clearAllCourseSessions()
            dao.clearAllNotes()
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
                CourseEntity(id = "c7", name = "تفسیر موضوعی قرآن", colorHex = "#C0CD8C", units = 2, semesterId = "sem_demo_3", courseCode = "CE106", professor = "استاد تقوی", examDate = "1403/11/02", examTime = "14:00", examLocation = "کلاس 118"),
                CourseEntity(id = "c8", name = "نقشه کشی صنعتی", colorHex = "#0EA5E9", units = 2, semesterId = "sem_demo_3", courseCode = "CE107", professor = "مهندس عباسی", examDate = "1403/10/18", examTime = "10:00", examLocation = "آتلیه 2"),
                CourseEntity(id = "c9", name = "مکانیک سیالات ۱", colorHex = "#C0CD8C", units = 3, semesterId = "sem_demo_3", courseCode = "CE108", professor = "دکتر اکبری", examDate = "1403/10/21", examTime = "10:45", examLocation = "کلاس 111 فنی")
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
                isOnboardingCompleted = false,
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

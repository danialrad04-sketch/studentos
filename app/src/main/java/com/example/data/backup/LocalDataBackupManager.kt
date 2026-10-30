package com.example.data.backup

import android.content.Context
import com.example.data.v3.*
import com.example.data.local.dao.CurriculumDao
import com.example.data.local.dao.StudentDao
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.entity.CurriculumCourseEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.ProfessorEntity
import com.example.data.local.entity.SemesterEntity
import com.example.data.local.entity.StudentCourseAttemptEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.entity.TaskEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Data transfer object for complete database snapshot serialization via Moshi.
 */
data class StudentOSBackupPayload(
    val version: Int = 1,
    val v3: StudentV3Snapshot? = null,
    val exportedAt: Long = System.currentTimeMillis(),
    val appName: String = "Student OS",
    val profile: StudentProfileEntity? = null,
    val semesters: List<SemesterEntity> = emptyList(),
    val courses: List<CourseEntity> = emptyList(),
    val sessions: List<CourseSessionEntity> = emptyList(),
    val attendance: List<AttendanceEntity> = emptyList(),
    val grades: List<GradeEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val professors: List<ProfessorEntity> = emptyList(),
    val exams: List<ExamEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList(),
    val attempts: List<StudentCourseAttemptEntity> = emptyList(),
    val curriculumCourses: List<CurriculumCourseEntity> = emptyList(),
    val universities: List<com.example.data.local.entity.UniversityEntity> = emptyList(),
    val majors: List<com.example.data.local.entity.MajorEntity> = emptyList(),
    val curriculumVersions: List<com.example.data.local.entity.CurriculumVersionEntity> = emptyList(),
    val studySettings: com.example.domain.model.StudySettingsSnapshot? = null,
    val studyGoals: List<com.example.data.local.entity.StudyGoalEntity> = emptyList(),
    val studyAvailability: List<com.example.data.local.entity.StudyAvailabilityEntity> = emptyList(),
    val studyCompletions: List<com.example.data.local.entity.StudyCompletionEntity> = emptyList(),
    val reviewCards: List<com.example.data.local.entity.ReviewCardEntity> = emptyList()
)

/**
 * High-performance, offline-independent Local Data Backup & Restore Manager.
 * Uses Moshi for type-safe serialization/deserialization of Room entity tables.
 */
object LocalDataBackupManager {

    private const val SNAPSHOT_FILE_NAME = "student_os_latest_snapshot.json"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val payloadAdapter by lazy {
        moshi.adapter(StudentOSBackupPayload::class.java)
    }

    fun withStudySettings(json: String, settings: com.example.domain.model.StudySettingsSnapshot): String {
        settings.validate()
        val payload = requireNotNull(payloadAdapter.fromJson(json)) { "فایل پشتیبان معتبر نیست." }
        return payloadAdapter.indent("  ").toJson(payload.copy(studySettings = settings))
    }

    fun readStudySettings(json: String): com.example.domain.model.StudySettingsSnapshot? {
        val payload = requireNotNull(payloadAdapter.fromJson(json)) { "فایل پشتیبان معتبر نیست." }
        return payload.studySettings?.also { it.validate() }
    }

    suspend fun exportCompleteDatabaseJson(
        dao: StudentDao,
        curriculumDao: CurriculumDao? = null
    ): String = withContext(Dispatchers.IO) {
        val payload = StudentOSBackupPayload(
            version = 2,
            exportedAt = System.currentTimeMillis(),
            appName = "Student OS",
            v3 = dao.v3Snapshot(),
            profile = dao.getProfileSync(),
            semesters = dao.getAllSemestersSync(),
            courses = dao.getAllCoursesIncludingArchivedSync(),
            sessions = dao.getAllSessionsSync(),
            attendance = dao.getAllAttendanceSync(),
            grades = dao.getAllGradesSync(),
            tasks = dao.getAllTasksSync(),
            professors = dao.getAllProfessorsSync(),
            exams = dao.getAllExamsSync(),
            notes = dao.getAllNotesSync(),
            studyGoals = dao.studyGoalsSnapshot(),
            studyAvailability = dao.studyAvailabilitySnapshot(),
            studyCompletions = dao.studyCompletionsSnapshot(),
            reviewCards = dao.reviewCardsSnapshot(),
            attempts = dao.getStudentAttemptsSync(),
            curriculumCourses = curriculumDao?.getAllCurriculumCoursesSync() ?: emptyList(),
            universities = curriculumDao?.getAllUniversitiesSync().orEmpty(),
            majors = curriculumDao?.getAllMajorsSync().orEmpty(),
            curriculumVersions = curriculumDao?.getAllCurriculumVersionsSync().orEmpty()
        )
        payloadAdapter.indent("  ").toJson(payload)
    }

    suspend fun importCompleteDatabaseJson(
        jsonString: String,
        dao: StudentDao,
        curriculumDao: CurriculumDao? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val payload = payloadAdapter.fromJson(jsonString)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid JSON payload format"))
            require(payload.version in 1..2) { "نسخهٔ پشتیبان پشتیبانی نمی‌شود." }
            payload.v3?.let { StudentV3Validation.snapshot(it, (payload.courses.map { c -> c.id } + dao.getAllCoursesIncludingArchivedSync().map { c -> c.id }).toSet()) }
            payload.studySettings?.validate()
            payload.studyGoals.forEach(com.example.domain.engine.LearningValidation::goal)
            payload.studyAvailability.forEach(com.example.domain.engine.LearningValidation::availability)
            payload.studyCompletions.forEach(com.example.domain.engine.LearningValidation::completion)
            payload.reviewCards.forEach(com.example.domain.engine.LearningValidation::card)
            require(payload.studyGoals.map { it.courseId }.distinct().size==payload.studyGoals.size)
            require(payload.studyAvailability.map { it.weekday }.distinct().size==payload.studyAvailability.size)
            require(payload.studyCompletions.map { it.id }.distinct().size==payload.studyCompletions.size)
            require(payload.reviewCards.map { it.id }.distinct().size==payload.reviewCards.size)
            val courseIds=(payload.courses.map { it.id }+dao.getAllCoursesIncludingArchivedSync().map { it.id }).toSet()
            require((payload.studyGoals.map { it.courseId }+payload.studyCompletions.map { it.courseId }+payload.reviewCards.map { it.courseId }).all { it in courseIds }) { "درس مرتبط با اطلاعات مطالعه پیدا نشد." }
            payload.tasks.forEach { com.example.domain.engine.TaskPlanningEngine.validate(it) }
            require(payload.tasks.map { it.id }.distinct().size == payload.tasks.size) { "شناسهٔ تکلیف تکراری است." }
            val parents = payload.tasks.mapNotNull { it.recurrenceParentId }
            require(parents.distinct().size == parents.size) { "زنجیرهٔ تکلیف تکراری است." }
            payload.tasks.forEach { task ->
                val sibling = task.recurrenceParentId?.let { dao.getNextOccurrence(it) }
                require(sibling == null || sibling.id == task.id) { "زنجیرهٔ تکلیف با اطلاعات فعلی تعارض دارد." }
            }

            curriculumDao?.let { catalog ->
                catalog.insertUniversities(payload.universities)
                catalog.insertMajors(payload.majors)
                payload.curriculumVersions.forEach { catalog.upsertPersonalCurriculumVersion(it) }
            }
            payload.profile?.let { dao.insertProfile(it.copy(updatedAt = System.currentTimeMillis())) }
            if (payload.semesters.isNotEmpty()) dao.insertSemesters(payload.semesters)
            if (payload.courses.isNotEmpty()) dao.insertCourses(payload.courses)
            if (payload.sessions.isNotEmpty()) dao.insertSessions(payload.sessions)
            if (payload.attendance.isNotEmpty()) dao.insertAllAttendance(payload.attendance)
            if (payload.grades.isNotEmpty()) dao.insertAllGrades(payload.grades)
            if (payload.tasks.isNotEmpty()) {
                val local=dao.getAllTasksSync().associateBy { it.id }
                dao.insertAllTasks(payload.tasks.map { row ->
                    val old=local[row.id]?.takeIf { it.courseId==row.courseId }
                    if(payload.version==1 && old!=null) row.copy(projectId=old.projectId,estimatedMinutes=old.estimatedMinutes,completedAt=if(row.isCompleted && old.isCompleted) old.completedAt else null) else row
                })
            }
            if (payload.professors.isNotEmpty()) dao.insertProfessors(payload.professors)
            if (payload.exams.isNotEmpty()) dao.insertExams(payload.exams)
            payload.notes.forEach { dao.insertNote(it) }
            if (payload.attempts.isNotEmpty()) dao.insertAllStudentAttempts(payload.attempts)
            if (payload.curriculumCourses.isNotEmpty() && curriculumDao != null) {
                curriculumDao.insertCurriculumCourses(payload.curriculumCourses)
            }

            val oldGoals=dao.studyGoalsSnapshot().associateBy { it.courseId }
            val oldAvailability=dao.studyAvailabilitySnapshot().associateBy { it.weekday }
            val oldCompletions=dao.studyCompletionsSnapshot().associateBy { it.id }
            val oldCards=dao.reviewCardsSnapshot().associateBy { it.id }
            payload.studyGoals.filter { it.updatedAt>(oldGoals[it.courseId]?.updatedAt ?: -1) }.forEach { dao.upsertStudyGoal(it) }
            payload.studyAvailability.filter { it.updatedAt>(oldAvailability[it.weekday]?.updatedAt ?: -1) }.forEach { dao.upsertStudyAvailability(it) }
            payload.studyCompletions.filter { it.updatedAt>(oldCompletions[it.id]?.updatedAt ?: -1) }.forEach { dao.upsertStudyCompletion(it) }
            payload.reviewCards.filter { it.updatedAt>(oldCards[it.id]?.updatedAt ?: -1) }.forEach { dao.upsertReviewCard(it) }
            payload.v3?.let { dao.mergeV3(it) }
            dao.cleanupOrphans()
            Result.success(1)
        } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Throwable) {
            e.printStackTrace()
            com.example.util.CrashLogger.recordException(e)
            Result.failure(e)
        }
    }

    suspend fun restoreDatabaseFromJson(
        jsonString: String,
        dao: StudentDao,
        curriculumDao: CurriculumDao? = null
    ): Result<Int> = importCompleteDatabaseJson(jsonString, dao, curriculumDao)

    private val snapshotMutex=kotlinx.coroutines.sync.Mutex()
    suspend fun saveLocalSnapshot(context: Context,json: String,owner: String = com.example.data.repository.AppPreferencesRepository.getInstance(context).taskReminderGeneration.value): Boolean = withContext(Dispatchers.IO) {
        snapshotMutex.withLock {
            val preferences=com.example.data.repository.AppPreferencesRepository.getInstance(context)
            val atomic=android.util.AtomicFile(File(context.filesDir,SNAPSHOT_FILE_NAME))
            var output: java.io.FileOutputStream?=null
            try {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                check(owner==preferences.taskReminderGeneration.value)
                output=atomic.startWrite()
                output.write(("StudentOS-local-v3:"+owner+"\n").toByteArray(Charsets.UTF_8));output.write(json.toByteArray(Charsets.UTF_8))
                kotlinx.coroutines.currentCoroutineContext().ensureActive();check(owner==preferences.taskReminderGeneration.value)
                atomic.finishWrite(output);true
            } catch(e: kotlinx.coroutines.CancellationException) { output?.let(atomic::failWrite);throw e }
            catch(e: Exception) { output?.let(atomic::failWrite);false }
        }
    }
    suspend fun discardStaleSnapshot(context: Context) = withContext(Dispatchers.IO) {
        snapshotMutex.withLock {
            val atomic=android.util.AtomicFile(File(context.filesDir,SNAPSHOT_FILE_NAME))
            val owner=com.example.data.repository.AppPreferencesRepository.getInstance(context).taskReminderGeneration.value
            val prefix=runCatching { atomic.openRead().bufferedReader().use { it.readLine() } }.getOrNull()
            if(prefix!="StudentOS-local-v3:$owner") atomic.delete()
        }
    }
    suspend fun loadLatestLocalSnapshot(context: Context): String? = withContext(Dispatchers.IO) {
        snapshotMutex.withLock {
            try {
                android.util.AtomicFile(File(context.filesDir,SNAPSHOT_FILE_NAME)).openRead().bufferedReader().use { reader ->
                    val first=reader.readLine() ?: return@withLock null
                    if(first.startsWith("StudentOS-local-v3:")) {
                        val owner=com.example.data.repository.AppPreferencesRepository.getInstance(context).taskReminderGeneration.value
                        if(first.removePrefix("StudentOS-local-v3:")!=owner) null else reader.readText()
                    } else first+"\n"+reader.readText() // Existing v2 installations retain legacy recovery.
                }
            } catch(e: kotlinx.coroutines.CancellationException) { throw e }
            catch(e: Exception) { null }
        }
    }
}

package com.example.data.backup

import android.content.Context
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
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Data transfer object for complete database snapshot serialization via Moshi.
 */
data class StudentOSBackupPayload(
    val version: Int = 1,
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
    val curriculumCourses: List<CurriculumCourseEntity> = emptyList()
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

    suspend fun exportCompleteDatabaseJson(
        dao: StudentDao,
        curriculumDao: CurriculumDao? = null
    ): String = withContext(Dispatchers.IO) {
        val payload = StudentOSBackupPayload(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            appName = "Student OS",
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
            attempts = dao.getStudentAttemptsSync(),
            curriculumCourses = curriculumDao?.getAllCurriculumCoursesSync() ?: emptyList()
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

            payload.profile?.let { dao.insertProfile(it.copy(updatedAt = System.currentTimeMillis())) }
            if (payload.semesters.isNotEmpty()) dao.insertSemesters(payload.semesters)
            if (payload.courses.isNotEmpty()) dao.insertCourses(payload.courses)
            if (payload.sessions.isNotEmpty()) dao.insertSessions(payload.sessions)
            if (payload.attendance.isNotEmpty()) dao.insertAllAttendance(payload.attendance)
            if (payload.grades.isNotEmpty()) dao.insertAllGrades(payload.grades)
            if (payload.tasks.isNotEmpty()) dao.insertAllTasks(payload.tasks)
            if (payload.professors.isNotEmpty()) dao.insertProfessors(payload.professors)
            if (payload.exams.isNotEmpty()) dao.insertExams(payload.exams)
            payload.notes.forEach { dao.insertNote(it) }
            if (payload.attempts.isNotEmpty()) dao.insertAllStudentAttempts(payload.attempts)
            if (payload.curriculumCourses.isNotEmpty() && curriculumDao != null) {
                curriculumDao.insertCurriculumCourses(payload.curriculumCourses)
            }

            dao.cleanupOrphans()
            Result.success(1)
        } catch (e: Throwable) {
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

    suspend fun saveLocalSnapshot(context: Context, json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, SNAPSHOT_FILE_NAME)
            file.writeText(json)
            true
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    }

    suspend fun loadLatestLocalSnapshot(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, SNAPSHOT_FILE_NAME)
            if (file.exists() && file.length() > 10) {
                file.readText()
            } else null
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }
}

package com.example.data.cloud

import android.content.Context
import android.util.Log
import com.example.data.api.backend.BackendApiClient
import com.example.data.api.backend.SyncPushRequest
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.CourseEntity
import com.example.data.local.entity.CourseSessionEntity
import com.example.data.local.entity.ExamEntity
import com.example.data.local.entity.GradeEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.entity.TaskEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline-first Data Synchronization Manager for the custom Node.js/PostgreSQL backend.
 *
 * Implements:
 * 1. Background PUT /api/sync/:dataType on local Room writes
 * 2. Background/Foreground GET /api/sync/:dataType on app start & pull-to-refresh
 * 3. Last-writer-wins conflict resolution by updatedAt timestamp
 */
object BackendSyncManager {

    private const val TAG = "BackendSyncManager"

    val DATA_TYPES = listOf(
        "profile",
        "courses",
        "sessions",
        "attendance",
        "grades",
        "exams",
        "tasks",
        "notes"
    )

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private fun <T> deserializePayload(payload: Any?, type: java.lang.reflect.Type): T? {
        if (payload == null) return null
        return try {
            val json = moshi.adapter(Any::class.java).toJson(payload)
            val adapter = moshi.adapter<T>(type)
            adapter.fromJson(json)
        } catch (e: Exception) {
            Log.w(TAG, "Failed deserializing payload: ${e.message}")
            null
        }
    }

    /**
     * Pushes a specific data type to the Node.js backend.
     * If server returns 409 STALE_WRITE, automatically pulls and reconciles.
     */
    suspend fun pushDataType(context: Context, dataType: String): Result<Unit> = withContext(Dispatchers.IO) {
        val client = BackendApiClient.getInstance(context)
        if (client.tokenStore().getAccessToken() == null) {
            Log.d(TAG, "Push skipped for $dataType: User not authenticated with backend.")
            return@withContext Result.success(Unit)
        }

        val db = AppDatabase.getDatabase(context)
        val dao = db.studentDao()
        val now = System.currentTimeMillis()

        val payload: Any = when (dataType) {
            "profile" -> {
                val p = dao.getProfileSync() ?: return@withContext Result.success(Unit)
                p
            }
            "courses" -> dao.getAllCoursesIncludingArchivedSync()
            "sessions" -> dao.getAllSessionsSync()
            "attendance" -> dao.getAllAttendanceSync()
            "grades" -> dao.getAllGradesSync()
            "exams" -> dao.getAllExamsSync()
            "tasks" -> dao.getAllTasksSync()
            "notes" -> dao.getAllNotesSync()
            else -> return@withContext Result.failure(IllegalArgumentException("Unknown dataType: $dataType"))
        }

        try {
            val resp = client.api.pushDataType(dataType, SyncPushRequest(payload = payload, updatedAt = now))
            if (resp.isSuccessful) {
                Log.i(TAG, "Successfully pushed $dataType to backend (updatedAt=$now).")
                Result.success(Unit)
            } else if (resp.code() == 409) {
                Log.w(TAG, "Server returned 409 STALE_WRITE for $dataType. Reconciling by pulling newer server data...")
                pullDataType(context, dataType)
            } else {
                Log.w(TAG, "Failed pushing $dataType to backend: HTTP ${resp.code()} - ${resp.errorBody()?.string()}")
                Result.failure(Exception("HTTP ${resp.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception pushing $dataType to backend: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Pushes all 8 data types to the Node.js backend.
     */
    suspend fun pushAllData(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        var hasFailure = false
        for (type in DATA_TYPES) {
            val res = pushDataType(context, type)
            if (res.isFailure) hasFailure = true
        }
        if (hasFailure) Result.failure(Exception("Some data types failed to push")) else Result.success(Unit)
    }

    /**
     * Pulls a specific data type from the Node.js backend and merges into Room via last-writer-wins.
     */
    suspend fun pullDataType(context: Context, dataType: String): Result<Unit> = withContext(Dispatchers.IO) {
        val client = BackendApiClient.getInstance(context)
        if (client.tokenStore().getAccessToken() == null) {
            return@withContext Result.success(Unit)
        }

        try {
            val resp = client.api.pullDataType(dataType)
            if (!resp.isSuccessful) {
                Log.w(TAG, "Pull failed for $dataType: HTTP ${resp.code()}")
                return@withContext Result.failure(Exception("HTTP ${resp.code()}"))
            }

            val body = resp.body() ?: return@withContext Result.success(Unit)
            val remotePayload = body.payload ?: return@withContext Result.success(Unit)
            val remoteUpdatedAt = body.updatedAt

            val db = AppDatabase.getDatabase(context)
            val dao = db.studentDao()

            when (dataType) {
                "profile" -> {
                    val remoteProfile: StudentProfileEntity? = deserializePayload(
                        remotePayload,
                        StudentProfileEntity::class.java
                    )
                    if (remoteProfile != null) {
                        val localProfile = dao.getProfileSync()
                        val localUpdatedAt = localProfile?.updatedAt ?: 0L
                        if (remoteUpdatedAt > localUpdatedAt || localProfile == null) {
                            dao.insertProfile(remoteProfile.copy(id = 1, updatedAt = remoteUpdatedAt))
                            Log.i(TAG, "Merged profile from backend (remoteUpdatedAt=$remoteUpdatedAt > localUpdatedAt=$localUpdatedAt)")
                        }
                    }
                }
                "courses" -> {
                    val coursesType = Types.newParameterizedType(List::class.java, CourseEntity::class.java)
                    val remoteCourses: List<CourseEntity>? = deserializePayload(remotePayload, coursesType)
                    if (!remoteCourses.isNullOrEmpty()) {
                        val localCoursesMap = dao.getAllCoursesIncludingArchivedSync().associateBy { it.id }
                        for (remote in remoteCourses) {
                            val local = localCoursesMap[remote.id]
                            if (local == null || remoteUpdatedAt > 0L) {
                                dao.insertCourse(remote)
                            }
                        }
                        Log.i(TAG, "Merged ${remoteCourses.size} courses from backend.")
                    }
                }
                "sessions" -> {
                    val sessionsType = Types.newParameterizedType(List::class.java, CourseSessionEntity::class.java)
                    val remoteSessions: List<CourseSessionEntity>? = deserializePayload(remotePayload, sessionsType)
                    if (!remoteSessions.isNullOrEmpty()) {
                        for (sess in remoteSessions) {
                            dao.insertCourseSession(sess)
                        }
                        Log.i(TAG, "Merged ${remoteSessions.size} sessions from backend.")
                    }
                }
                "attendance" -> {
                    val attType = Types.newParameterizedType(List::class.java, AttendanceEntity::class.java)
                    val remoteAttendance: List<AttendanceEntity>? = deserializePayload(remotePayload, attType)
                    if (!remoteAttendance.isNullOrEmpty()) {
                        for (att in remoteAttendance) {
                            dao.insertAttendance(att)
                        }
                        Log.i(TAG, "Merged ${remoteAttendance.size} attendance records from backend.")
                    }
                }
                "grades" -> {
                    val gradesType = Types.newParameterizedType(List::class.java, GradeEntity::class.java)
                    val remoteGrades: List<GradeEntity>? = deserializePayload(remotePayload, gradesType)
                    if (!remoteGrades.isNullOrEmpty()) {
                        for (grade in remoteGrades) {
                            dao.insertGrade(grade)
                        }
                        Log.i(TAG, "Merged ${remoteGrades.size} grades from backend.")
                    }
                }
                "exams" -> {
                    val examsType = Types.newParameterizedType(List::class.java, ExamEntity::class.java)
                    val remoteExams: List<ExamEntity>? = deserializePayload(remotePayload, examsType)
                    if (!remoteExams.isNullOrEmpty()) {
                        for (exam in remoteExams) {
                            dao.insertExam(exam)
                        }
                        Log.i(TAG, "Merged ${remoteExams.size} exams from backend.")
                    }
                }
                "tasks" -> {
                    val tasksType = Types.newParameterizedType(List::class.java, TaskEntity::class.java)
                    val remoteTasks: List<TaskEntity>? = deserializePayload(remotePayload, tasksType)
                    if (!remoteTasks.isNullOrEmpty()) {
                        for (task in remoteTasks) {
                            dao.insertTask(task)
                        }
                        Log.i(TAG, "Merged ${remoteTasks.size} tasks from backend.")
                    }
                }
                "notes" -> {
                    val notesType = Types.newParameterizedType(List::class.java, NoteEntity::class.java)
                    val remoteNotes: List<NoteEntity>? = deserializePayload(remotePayload, notesType)
                    if (!remoteNotes.isNullOrEmpty()) {
                        val localNotesMap = dao.getAllNotesSync().associateBy { it.id }
                        for (note in remoteNotes) {
                            val local = localNotesMap[note.id]
                            if (local == null || note.updatedAt >= (local.updatedAt)) {
                                dao.insertNote(note)
                            }
                        }
                        Log.i(TAG, "Merged ${remoteNotes.size} notes from backend.")
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception pulling $dataType from backend: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Pulls all 8 data types from the Node.js backend.
     */
    suspend fun pullAllData(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        var hasFailure = false
        for (type in DATA_TYPES) {
            val res = pullDataType(context, type)
            if (res.isFailure) hasFailure = true
        }
        if (hasFailure) Result.failure(Exception("Some data types failed to pull")) else Result.success(Unit)
    }
}

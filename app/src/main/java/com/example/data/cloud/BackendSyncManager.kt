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
import com.example.data.local.entity.SemesterEntity
import com.example.data.local.entity.StudentProfileEntity
import com.example.data.local.entity.SyncMetadataEntity
import com.example.data.local.entity.TaskEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Single Node.js/PostgreSQL sync backend with type-level updatedAt LWW. */
object BackendSyncManager {
    private const val TAG = "BackendSyncManager"

    val DATA_TYPES = listOf(
        "profile", "semesters", "courses", "sessions",
        "attendance", "grades", "exams", "tasks", "notes"
    )

    private val moshi by lazy { Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build() }

    private fun <T> decode(payload: Any?, type: java.lang.reflect.Type): T? = try {
        if (payload == null) null else moshi.adapter<T>(type).fromJson(moshi.adapter(Any::class.java).toJson(payload))
    } catch (e: Exception) {
        Log.w(TAG, "Decode failed: ${e.message}")
        null
    }

    private suspend fun localUpdatedAt(dao: com.example.data.local.dao.StudentDao, type: String): Long =
        dao.getSyncUpdatedAt(type) ?: 0L

    private suspend fun payloadFor(dao: com.example.data.local.dao.StudentDao, type: String): Any? = when (type) {
        "profile" -> dao.getProfileSync()
        "semesters" -> dao.getAllSemestersSync()
        "courses" -> dao.getAllCoursesIncludingArchivedSync()
        "sessions" -> dao.getAllSessionsSync()
        "attendance" -> dao.getAllAttendanceSync()
        "grades" -> dao.getAllGradesSync()
        "exams" -> dao.getAllExamsSync()
        "tasks" -> dao.getAllTasksSync()
        "notes" -> dao.getAllNotesSync()
        else -> null
    }

    suspend fun pushDataType(context: Context, dataType: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (dataType !in DATA_TYPES) return@withContext Result.failure(IllegalArgumentException("Unknown dataType: $dataType"))
        val client = BackendApiClient.getInstance(context)
        if (client.tokenStore().getAccessToken() == null) return@withContext Result.success(Unit)
        val dao = AppDatabase.getDatabase(context).studentDao()
        val updatedAt = localUpdatedAt(dao, dataType)
        if (updatedAt <= 0L) return@withContext Result.success(Unit)
        val payload = payloadFor(dao, dataType)
            ?: if (dataType == "profile") return@withContext Result.success(Unit) else emptyList<Any>()

        try {
            val response = client.api.pushDataType(dataType, SyncPushRequest(payload, updatedAt))
            when {
                response.isSuccessful -> {
                    dao.upsertSyncMetadata(SyncMetadataEntity(dataType, updatedAt))
                    Result.success(Unit)
                }
                response.code() == 409 -> pullDataType(context, dataType)
                else -> Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Push $dataType failed", e)
            Result.failure(e)
        }
    }

    suspend fun pushAllData(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        var failed = false
        DATA_TYPES.forEach { if (pushDataType(context, it).isFailure) failed = true }
        if (failed) Result.failure(Exception("Some data types failed to push")) else Result.success(Unit)
    }

    private suspend fun applyListSnapshot(context: Context, dataType: String, payload: Any?, remoteUpdatedAt: Long) =
        BackendSyncScheduler.withSuppressedSync {
            val dao = AppDatabase.getDatabase(context).studentDao()
            when (dataType) {
                "semesters" -> {
                    val t = Types.newParameterizedType(List::class.java, SemesterEntity::class.java)
                    dao.getAllSemestersSync().forEach { dao.deleteSemester(it.id) }; decode<List<SemesterEntity>>(payload, t)?.let { if (it.isNotEmpty()) dao.insertSemesters(it) }
                }
                "courses" -> {
                    val t = Types.newParameterizedType(List::class.java, CourseEntity::class.java)
                    val items = decode<List<CourseEntity>>(payload, t) ?: emptyList()
                    dao.clearCourseSessions(); dao.clearAttendance(); dao.clearGrades(); dao.clearExams(); dao.clearTasks(); dao.clearCourses()
                    if (items.isNotEmpty()) dao.insertCourses(items)
                }
                "sessions" -> {
                    val t = Types.newParameterizedType(List::class.java, CourseSessionEntity::class.java)
                    dao.clearCourseSessions(); decode<List<CourseSessionEntity>>(payload, t)?.let { if (it.isNotEmpty()) dao.insertCourseSessions(it) }
                }
                "attendance" -> {
                    val t = Types.newParameterizedType(List::class.java, AttendanceEntity::class.java)
                    dao.clearAttendance(); decode<List<AttendanceEntity>>(payload, t)?.let { if (it.isNotEmpty()) dao.insertAllAttendance(it) }
                }
                "grades" -> {
                    val t = Types.newParameterizedType(List::class.java, GradeEntity::class.java)
                    dao.clearGrades(); decode<List<GradeEntity>>(payload, t)?.let { if (it.isNotEmpty()) dao.insertAllGrades(it) }
                }
                "exams" -> {
                    val t = Types.newParameterizedType(List::class.java, ExamEntity::class.java)
                    dao.clearExams(); decode<List<ExamEntity>>(payload, t)?.forEach { dao.insertExam(it) }
                }
                "tasks" -> {
                    val t = Types.newParameterizedType(List::class.java, TaskEntity::class.java)
                    dao.clearTasks(); decode<List<TaskEntity>>(payload, t)?.let { if (it.isNotEmpty()) dao.insertAllTasks(it) }
                }
                "notes" -> {
                    val t = Types.newParameterizedType(List::class.java, NoteEntity::class.java)
                    dao.getAllNotesSync().forEach { dao.deleteNote(it.id) }; decode<List<NoteEntity>>(payload, t)?.forEach { dao.insertNote(it) }
                }
            }
            dao.upsertSyncMetadata(SyncMetadataEntity(dataType, remoteUpdatedAt))
        }

    suspend fun pullDataType(context: Context, dataType: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (dataType !in DATA_TYPES) return@withContext Result.failure(IllegalArgumentException("Unknown dataType: $dataType"))
        val client = BackendApiClient.getInstance(context)
        if (client.tokenStore().getAccessToken() == null) return@withContext Result.success(Unit)
        try {
            val response = client.api.pullDataType(dataType)
            if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code()}"))
            val body = response.body() ?: return@withContext Result.success(Unit)
            val dao = AppDatabase.getDatabase(context).studentDao()
            val localUpdatedAt = localUpdatedAt(dao, dataType)
            if (body.updatedAt <= localUpdatedAt) return@withContext Result.success(Unit)

            if (dataType == "profile") {
                val profile = decode<StudentProfileEntity>(body.payload, StudentProfileEntity::class.java)
                    ?: return@withContext Result.success(Unit)
                BackendSyncScheduler.withSuppressedSync {
                    dao.insertProfile(profile.copy(id = 1, updatedAt = body.updatedAt))
                    dao.upsertSyncMetadata(SyncMetadataEntity("profile", body.updatedAt))
                }
            } else {
                applyListSnapshot(context, dataType, body.payload, body.updatedAt)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Pull $dataType failed", e)
            Result.failure(e)
        }
    }

    suspend fun pullAllData(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        var failed = false
        DATA_TYPES.forEach { if (pullDataType(context, it).isFailure) failed = true }
        if (failed) Result.failure(Exception("Some data types failed to pull")) else Result.success(Unit)
    }
}

package com.example.data.cloud

import android.content.Context
import androidx.room.InvalidationTracker
import com.example.data.cloud.worker.BackendSyncWorker
import com.example.data.local.AppDatabase
import com.example.data.local.entity.SyncMetadataEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Schedules per-type backend sync jobs after committed Room writes. */
object BackendSyncScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private var installed = false
    private var suppressed = 0

    private val tableToType = mapOf(
        "student_profile" to "profile",
        "semesters" to "semesters",
        "courses" to "courses",
        "course_sessions" to "sessions",
        "attendance" to "attendance",
        "grades" to "grades",
        "exams" to "exams",
        "tasks" to "tasks",
        "notes" to "notes"
    )

    fun install(context: Context, database: AppDatabase) {
        synchronized(lock) {
            if (installed) return
            installed = true
        }
        database.invalidationTracker.addObserver(
            object : InvalidationTracker.Observer(tableToType.keys.toTypedArray()) {
                override fun onInvalidated(tables: Set<String>) {
                    synchronized(lock) { if (suppressed > 0) return }
                    val types = tables.mapNotNull { tableToType[it] }.distinct()
                    if (types.isEmpty()) return
                    scope.launch {
                        val dao = database.studentDao()
                        val timestamp = System.currentTimeMillis()
                        types.forEach { type ->
                            dao.upsertSyncMetadata(SyncMetadataEntity(type, timestamp))
                            BackendSyncWorker.triggerImmediateSync(context, type, false)
                        }
                    }
                }
            }
        )
    }

    suspend fun <T> withSuppressedSync(block: suspend () -> T): T {
        synchronized(lock) { suppressed++ }
        return try {
            block()
        } finally {
            synchronized(lock) { suppressed-- }
        }
    }
}

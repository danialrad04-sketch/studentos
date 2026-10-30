package com.example.data.local

import android.content.Context
import androidx.room.InvalidationTracker
import androidx.room.withTransaction
import androidx.work.*
import com.example.data.backup.LocalDataBackupManager
import com.example.data.repository.AppPreferencesRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*

/** Coalesced local recovery snapshot. It never starts a network request. */
class StudentSnapshotWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result =
        withContext(Dispatchers.IO) {
            val preferences = AppPreferencesRepository.getInstance(applicationContext)
            val owner = preferences.taskReminderGeneration.value
            if (!preferences.isAutoBackupEnabled.value) return@withContext Result.success()
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val json =
                    db.withTransaction {
                        if (db.studentDao().getProfileSync()?.isOnboardingCompleted != true)
                            return@withTransaction null
                        LocalDataBackupManager.withStudySettings(
                            LocalDataBackupManager.exportCompleteDatabaseJson(
                                db.studentDao(),
                                db.curriculumDao(),
                            ),
                            preferences.exportStudySettings(),
                        )
                    }
                if (json == null) Result.success()
                else if (LocalDataBackupManager.saveLocalSnapshot(applicationContext, json, owner))
                    Result.success()
                else if (runAttemptCount < 2) Result.retry() else Result.failure()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                com.example.util.CrashLogger.recordException(e)
                if (runAttemptCount < 2) Result.retry() else Result.failure()
            }
        }

    companion object {
        fun request(context: Context) {
            try {
                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        "student_local_recovery",
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequestBuilder<StudentSnapshotWorker>()
                            .setInitialDelay(2, TimeUnit.SECONDS)
                            .build(),
                    )
            } catch (error: IllegalStateException) {
                // An unavailable scheduler must not turn a committed Room write into a crash.
                com.example.util.CrashLogger.recordException(error)
            }
        }

        private var installed = false

        @Synchronized
        fun install(context: Context, db: AppDatabase) {
            if (installed) return
            installed = true
            db.invalidationTracker.addObserver(
                object :
                    InvalidationTracker.Observer(
                        arrayOf(
                            "student_profile",
                            "semesters",
                            "courses",
                            "course_sessions",
                            "tasks",
                            "grades",
                            "attendance",
                            "notes",
                            "exams",
                            "study_goals",
                            "study_availability",
                            "study_completions",
                            "review_cards",
                            "library_files",
                            "page_bookmarks",
                            "study_projects",
                            "project_steps",
                            "practice_questions",
                            "practice_attempts",
                        )
                    ) {
                    override fun onInvalidated(tables: Set<String>) {
                        request(context)
                    }
                }
            )
        }
    }
}

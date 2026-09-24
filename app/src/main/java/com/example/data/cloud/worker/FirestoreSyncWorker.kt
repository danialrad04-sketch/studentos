package com.example.data.cloud.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.cloud.FirestoreSyncManager
import com.example.data.local.AppDatabase
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Production WorkManager Worker for periodic background cloud synchronization.
 * Handles offline-to-online data sync for Firestore when connected to network.
 */
class FirestoreSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val fbUser = try {
                Firebase.auth.currentUser
            } catch (_: Throwable) {
                FirebaseAuth.getInstance().currentUser
            }

            if (fbUser == null || fbUser.isAnonymous) {
                Log.d(TAG, "Background sync skipped: User is guest or not signed in.")
                return@withContext Result.success()
            }

            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.studentDao()
            val profile = dao.getProfileSync()
            val courses = dao.getAllCoursesIncludingArchivedSync()

            // If local data is empty, restore from cloud first to prevent overwriting cloud with empty state
            if (courses.isEmpty() && (profile == null || profile.name == "دانشجو" || profile.name.isBlank())) {
                Log.i(TAG, "Local Room database is empty in worker. Restoring from cloud first...")
                FirestoreSyncManager.restoreAllDataFromCloud(fbUser.uid, dao, db.curriculumDao())
                return@withContext Result.success()
            }

            if (profile == null) return@withContext Result.success()

            val grades = dao.getAllGradesSync()
            val tasks = dao.getAllTasksSync()
            val attendance = dao.getAllAttendanceSync()
            val exams = dao.getAllExamsSync()
            val notes = dao.getAllNotesSync()

            val result = FirestoreSyncManager.syncAllDataToCloud(
                userId = fbUser.uid,
                profile = profile,
                courses = courses,
                grades = grades,
                tasks = tasks,
                attendance = attendance,
                exams = exams,
                notes = notes
            )

            if (result.isSuccess) {
                Log.i(TAG, "Background Firestore synchronization completed successfully.")
                Result.success()
            } else {
                Log.w(TAG, "Background sync failed: ${result.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during background Firestore sync", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "FirestoreSyncWorker"
        const val WORK_NAME = "FirestoreSyncPeriodicWork"

        /**
         * Schedule periodic background sync every 1 hour when network is connected.
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<FirestoreSyncWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        /**
         * Enqueues a one-time immediate background sync work request.
         */
        fun triggerImmediateSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = androidx.work.OneTimeWorkRequestBuilder<FirestoreSyncWorker>()
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(
                    "FirestoreSyncImmediateWork",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
                Log.d(TAG, "Immediate cloud sync enqueued successfully.")
            } catch (e: Throwable) {
                Log.w(TAG, "Failed enqueuing immediate sync: ${e.message}")
            }
        }
    }
}

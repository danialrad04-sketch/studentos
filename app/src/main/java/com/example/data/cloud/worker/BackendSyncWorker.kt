package com.example.data.cloud.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.api.backend.BackendApiClient
import com.example.data.cloud.BackendSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Production WorkManager Worker for offline-first synchronization with the
 * self-hosted Node.js / PostgreSQL backend.
 */
class BackendSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val client = BackendApiClient.getInstance(applicationContext)
        val token = client.tokenStore().getAccessToken()
        if (token == null) {
            Log.d(TAG, "Backend sync skipped: No access token stored.")
            return@withContext Result.success()
        }

        val dataType = inputData.getString(KEY_DATA_TYPE)
        val isPullOnly = inputData.getBoolean(KEY_PULL_ONLY, false)

        try {
            if (isPullOnly) {
                if (dataType != null) {
                    BackendSyncManager.pullDataType(applicationContext, dataType)
                } else {
                    BackendSyncManager.pullAllData(applicationContext)
                }
            } else {
                if (dataType != null) {
                    BackendSyncManager.pushDataType(applicationContext, dataType)
                } else {
                    BackendSyncManager.pushAllData(applicationContext)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "BackendSyncWorker error: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "BackendSyncWorker"
        const val KEY_DATA_TYPE = "data_type"
        const val KEY_PULL_ONLY = "pull_only"
        const val WORK_NAME_PERIODIC = "BackendSyncPeriodicWork"

        /**
         * Schedules periodic background sync every 1 hour when network is connected.
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<BackendSyncWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        /**
         * Enqueues an immediate background sync request for a specific data type or all data.
         */
        fun triggerImmediateSync(context: Context, dataType: String? = null, pullOnly: Boolean = false) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val dataBuilder = Data.Builder()
                if (dataType != null) {
                    dataBuilder.putString(KEY_DATA_TYPE, dataType)
                }
                dataBuilder.putBoolean(KEY_PULL_ONLY, pullOnly)

                val syncRequest = OneTimeWorkRequestBuilder<BackendSyncWorker>()
                    .setConstraints(constraints)
                    .setInputData(dataBuilder.build())
                    .build()

                val uniqueName = if (dataType != null) "BackendSync_$dataType" else "BackendSync_Immediate"
                WorkManager.getInstance(context).enqueueUniqueWork(
                    uniqueName,
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
                Log.d(TAG, "Enqueued backend sync job (dataType=$dataType, pullOnly=$pullOnly)")
            } catch (e: Throwable) {
                Log.w(TAG, "Failed enqueuing backend sync job: ${e.message}")
            }
        }
    }
}

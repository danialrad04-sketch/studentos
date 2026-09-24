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

/** WorkManager bridge for reliable offline-to-online backend synchronization. */
class BackendSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val client = BackendApiClient.getInstance(applicationContext)
        if (client.tokenStore().getAccessToken() == null) return@withContext Result.success()

        val dataType = inputData.getString(KEY_DATA_TYPE)
        val pullOnly = inputData.getBoolean(KEY_PULL_ONLY, false)

        try {
            val syncResult = if (pullOnly) {
                if (dataType != null) BackendSyncManager.pullDataType(applicationContext, dataType)
                else BackendSyncManager.pullAllData(applicationContext)
            } else {
                if (dataType != null) BackendSyncManager.pushDataType(applicationContext, dataType)
                else BackendSyncManager.pushAllData(applicationContext)
            }
            if (syncResult.isSuccess) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Backend sync worker failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "BackendSyncWorker"
        const val KEY_DATA_TYPE = "data_type"
        const val KEY_PULL_ONLY = "pull_only"
        const val WORK_NAME_PERIODIC = "BackendSyncPeriodicWork"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<BackendSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun triggerImmediateSync(
            context: Context,
            dataType: String? = null,
            pullOnly: Boolean = false
        ) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val data = Data.Builder().apply {
                    dataType?.let { putString(KEY_DATA_TYPE, it) }
                    putBoolean(KEY_PULL_ONLY, pullOnly)
                }.build()

                val request = OneTimeWorkRequestBuilder<BackendSyncWorker>()
                    .setConstraints(constraints)
                    .setInputData(data)
                    .build()

                val name = if (dataType != null) "BackendSync_$dataType" else "BackendSync_Immediate"
                WorkManager.getInstance(context).enqueueUniqueWork(
                    name,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to enqueue backend sync", e)
            }
        }
    }
}

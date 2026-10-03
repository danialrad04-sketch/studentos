package com.example.data.cloud

import android.content.Context
import com.example.domain.model.SyncState
import com.example.domain.model.SyncStatusSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SyncStatusStore {
    private const val PREFS = "student_os_sync_status"
    private const val KEY_STATE = "state"
    private const val KEY_LAST_SYNCED_AT = "last_synced_at"

    private var appContext: Context? = null
    private val _status = MutableStateFlow(SyncStatusSnapshot(SyncState.LOCAL))
    val status: StateFlow<SyncStatusSnapshot> = _status.asStateFlow()

    fun initialize(context: Context) {
        if (appContext != null) return
        val ctx = context.applicationContext
        appContext = ctx
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val state = runCatching {
            SyncState.valueOf(prefs.getString(KEY_STATE, SyncState.LOCAL.name) ?: SyncState.LOCAL.name)
        }.getOrDefault(SyncState.LOCAL)
        _status.value = SyncStatusSnapshot(
            state = if (state == SyncState.SYNCING) SyncState.NEEDS_ATTENTION else state,
            lastSyncedAt = prefs.getLong(KEY_LAST_SYNCED_AT, 0L)
        )
    }

    fun markLocal(context: Context) = update(context, SyncState.LOCAL, 0L)

    fun markSyncing(context: Context) = update(context, SyncState.SYNCING, _status.value.lastSyncedAt)

    fun markSynced(context: Context, timestamp: Long = System.currentTimeMillis()) =
        update(context, SyncState.SYNCED, timestamp)

    fun markNeedsAttention(context: Context) =
        update(context, SyncState.NEEDS_ATTENTION, _status.value.lastSyncedAt)

    private fun update(context: Context, state: SyncState, lastSyncedAt: Long) {
        initialize(context)
        _status.value = SyncStatusSnapshot(state, lastSyncedAt)
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_STATE, state.name)
            ?.putLong(KEY_LAST_SYNCED_AT, lastSyncedAt)
            ?.apply()
    }
}

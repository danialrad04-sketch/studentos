package com.example.domain.model

enum class SyncState {
    LOCAL,
    SYNCING,
    SYNCED,
    NEEDS_ATTENTION
}

data class SyncStatusSnapshot(
    val state: SyncState,
    val lastSyncedAt: Long = 0L
)

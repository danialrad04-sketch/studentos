package com.example.util

import android.util.Log

object CrashLogger {
    private const val TAG = "CrashLogger"

    fun recordException(throwable: Throwable) {
        try {
            Log.e(TAG, "Exception recorded: ${throwable.message}", throwable)
        } catch (_: Throwable) {
        }
    }

    fun log(message: String) {
        try {
            Log.i(TAG, message)
        } catch (_: Throwable) {
        }
    }
}


package com.example.domain.model

/** Persisted deadline, rather than a coroutine tick count, survives process recreation. */
data class FocusSession(
    val durationSeconds: Int = 1500,
    val pausedSeconds: Int = 1500,
    val endsAtMillis: Long = 0,
    val label: String = "تمرکز آزاد"
) {
    val isRunning: Boolean get() = endsAtMillis > 0
    fun remainingSeconds(now: Long): Int = if (isRunning) {
        ((endsAtMillis - now + 999) / 1000).coerceIn(0, durationSeconds.toLong()).toInt()
    } else pausedSeconds.coerceIn(0, durationSeconds)

    fun start(now: Long): FocusSession {
        val remaining = pausedSeconds.takeIf { it > 0 } ?: durationSeconds
        return copy(pausedSeconds = remaining, endsAtMillis = now + remaining * 1000L)
    }
    fun pause(now: Long): FocusSession = copy(pausedSeconds = remainingSeconds(now), endsAtMillis = 0)
}

package com.example.taskoday.update

/** Controls automatic checks without treating an unsuccessful request as a successful check. */
object UpdateCheckSchedule {
    const val SUCCESS_INTERVAL_MS = 24L * 60L * 60L * 1000L
    const val FAILURE_RETRY_INTERVAL_MS = 30L * 60L * 1000L

    fun shouldCheckAutomatically(
        nowMillis: Long,
        lastSuccessfulCheckMillis: Long,
        lastAttemptMillis: Long,
        successIntervalMillis: Long = SUCCESS_INTERVAL_MS,
        failureRetryIntervalMillis: Long = FAILURE_RETRY_INTERVAL_MS,
    ): Boolean {
        // A clock correction must not turn a recent attempt into an immediate retry loop.
        if (nowMillis < lastAttemptMillis || nowMillis < lastSuccessfulCheckMillis) return false
        if (lastAttemptMillis > 0L && nowMillis - lastAttemptMillis < failureRetryIntervalMillis) return false
        return lastSuccessfulCheckMillis <= 0L ||
            nowMillis - lastSuccessfulCheckMillis >= successIntervalMillis
    }
}

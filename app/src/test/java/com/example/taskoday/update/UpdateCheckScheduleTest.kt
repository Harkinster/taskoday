package com.example.taskoday.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckScheduleTest {
    private val now = 10_000_000L

    @Test fun firstAutomaticCheckIsDue() {
        assertTrue(UpdateCheckSchedule.shouldCheckAutomatically(now, 0L, 0L))
    }

    @Test fun recentSuccessfulCheckWaitsTwentyFourHours() {
        assertFalse(UpdateCheckSchedule.shouldCheckAutomatically(now, now - 1L, now - 1L))
        assertTrue(UpdateCheckSchedule.shouldCheckAutomatically(
            now,
            now - UpdateCheckSchedule.SUCCESS_INTERVAL_MS,
            now - UpdateCheckSchedule.SUCCESS_INTERVAL_MS,
        ))
    }

    @Test fun failedAttemptDoesNotResetSuccessfulCheckAndRetriesAfterThirtyMinutes() {
        val lastSuccess = now - UpdateCheckSchedule.SUCCESS_INTERVAL_MS - 1L
        val lastFailure = now - UpdateCheckSchedule.FAILURE_RETRY_INTERVAL_MS + 1L
        assertFalse(UpdateCheckSchedule.shouldCheckAutomatically(now, lastSuccess, lastFailure))
        assertTrue(UpdateCheckSchedule.shouldCheckAutomatically(
            now,
            lastSuccess,
            now - UpdateCheckSchedule.FAILURE_RETRY_INTERVAL_MS,
        ))
    }

    @Test fun clockMovingBackwardsDoesNotCauseRepeatedChecks() {
        assertFalse(UpdateCheckSchedule.shouldCheckAutomatically(now, now + 10L, now + 10L))
    }
}

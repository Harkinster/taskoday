package com.example.taskoday.features.planning

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteCompletionGuardTest {
    @Test
    fun `remote failure blocks local completion`() {
        assertFalse(
            shouldApplyLocalCompletionAfterRemoteAttempt(
                remoteCompletionRequired = true,
                remoteCompletionSucceeded = false,
            ),
        )
    }

    @Test
    fun `remote success allows local completion`() {
        assertTrue(
            shouldApplyLocalCompletionAfterRemoteAttempt(
                remoteCompletionRequired = true,
                remoteCompletionSucceeded = true,
            ),
        )
    }

    @Test
    fun `local-only action can still be completed locally`() {
        assertTrue(
            shouldApplyLocalCompletionAfterRemoteAttempt(
                remoteCompletionRequired = false,
                remoteCompletionSucceeded = false,
            ),
        )
    }
}

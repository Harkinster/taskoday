package com.example.taskoday.features.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletionCelebrationPolicyTest {
    @Test
    fun childAccountWithRemoteDataSeesCompletionCelebration() {
        assertTrue(
            shouldShowCompletionCelebration(
                isParentUser = false,
                isLocalChildMode = false,
                usingRemoteData = true,
            ),
        )
    }

    @Test
    fun localChildModeWithParentTokenSeesCompletionCelebration() {
        assertTrue(
            shouldShowCompletionCelebration(
                isParentUser = true,
                isLocalChildMode = true,
                usingRemoteData = true,
            ),
        )
    }

    @Test
    fun parentDashboardDoesNotSeeCompletionCelebration() {
        assertFalse(
            shouldShowCompletionCelebration(
                isParentUser = true,
                isLocalChildMode = false,
                usingRemoteData = true,
            ),
        )
    }

    @Test
    fun localOnlyModeDoesNotSeeServerCompletionCelebration() {
        assertFalse(
            shouldShowCompletionCelebration(
                isParentUser = false,
                isLocalChildMode = false,
                usingRemoteData = false,
            ),
        )
    }
}

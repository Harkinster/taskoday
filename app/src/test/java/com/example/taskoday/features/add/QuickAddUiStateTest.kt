package com.example.taskoday.features.add

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickAddUiStateTest {
    @Test
    fun `quick add is available only to a connected parent`() {
        val parentState =
            QuickAddUiState(
                isLoading = false,
                hasRemoteSession = true,
                isParent = true,
            )

        assertTrue(parentState.canOpenQuickAdd)
        assertTrue(parentState.canCreateRoutine)
        assertTrue(parentState.canCreateMission)
        assertTrue(parentState.canCreateQuest)
        assertNull(parentState.denialMessage)
    }

    @Test
    fun `connected child cannot access quick add actions`() {
        val childState =
            QuickAddUiState(
                isLoading = false,
                hasRemoteSession = true,
                isParent = false,
            )

        assertFalse(childState.canOpenQuickAdd)
        assertFalse(childState.canCreateRoutine)
        assertFalse(childState.canCreateMission)
        assertFalse(childState.canCreateQuest)
        assertTrue(childState.denialMessage?.contains("parent") == true)
    }

    @Test
    fun `anonymous local mode cannot access quick add actions`() {
        val localState =
            QuickAddUiState(
                isLoading = false,
                hasRemoteSession = false,
                isParent = false,
            )

        assertFalse(localState.canOpenQuickAdd)
        assertFalse(localState.canCreateRoutine)
        assertFalse(localState.canCreateMission)
        assertFalse(localState.canCreateQuest)
    }
}

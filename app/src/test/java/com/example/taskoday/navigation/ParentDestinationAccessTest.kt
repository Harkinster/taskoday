package com.example.taskoday.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentDestinationAccessTest {
    @Test fun `verified parent can enter administration`() {
        assertTrue(canEnterParentDestination(true, false, false))
    }
    @Test fun `child cannot enter even with stale parent capabilities`() {
        assertFalse(canEnterParentDestination(true, true, false))
    }
    @Test fun `local child mode cannot enter parent destinations`() {
        assertFalse(canEnterParentDestination(true, false, true))
    }
    @Test fun `unverified or expired session fails closed`() {
        assertFalse(canEnterParentDestination(false, false, false))
    }
}

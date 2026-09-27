package com.example.taskoday.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanningCachePolicyTest {
    @Test fun `real account rejects local examples even offline`() {
        assertFalse(planningCacheEntryVisible(true, false))
        assertTrue(planningCacheEntryVisible(true, true))
    }
    @Test fun `local account cannot read previous remote account cache`() {
        assertTrue(planningCacheEntryVisible(false, false))
        assertFalse(planningCacheEntryVisible(false, true))
    }
}

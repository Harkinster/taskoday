package com.example.taskoday.core.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskodayPlanPolicyTest {
    @Test
    fun freePlanLimitsMatchProductRules() {
        assertFalse(TaskodayPlanPolicy.isPremium)
        assertEquals(1, TaskodayPlanPolicy.limitFor(TaskodayPlanFeature.Child))
        assertEquals(5, TaskodayPlanPolicy.limitFor(TaskodayPlanFeature.Routine))
        assertEquals(3, TaskodayPlanPolicy.limitFor(TaskodayPlanFeature.Mission))
        assertEquals(1, TaskodayPlanPolicy.limitFor(TaskodayPlanFeature.Quest))
        assertEquals(3, TaskodayPlanPolicy.limitFor(TaskodayPlanFeature.Wish))
    }

    @Test
    fun creationIsBlockedAtFreeLimit() {
        assertTrue(TaskodayPlanPolicy.canCreate(TaskodayPlanFeature.Routine, currentCount = 4))
        assertFalse(TaskodayPlanPolicy.canCreate(TaskodayPlanFeature.Routine, currentCount = 5))
        assertFalse(TaskodayPlanPolicy.canCreate(TaskodayPlanFeature.Child, currentCount = 1))
        assertFalse(TaskodayPlanPolicy.canCreate(TaskodayPlanFeature.Wish, currentCount = 3))
    }
}

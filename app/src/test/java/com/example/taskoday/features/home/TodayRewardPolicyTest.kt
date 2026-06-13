package com.example.taskoday.features.home

import com.example.taskoday.domain.model.PlanningItemType
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayRewardPolicyTest {
    @Test
    fun completionRewardUsesCompactChildFriendlyLabel() {
        val reward = rewardPreviewFor(PlanningItemType.ROUTINE)

        assertEquals(5, reward.xp)
        assertEquals(2, reward.flammeches)
        assertEquals(1, reward.crystals)
        assertEquals("+5 XP • +2 Flammèches • +1 Cristal", reward.compactLabel())
    }
}

package com.example.taskoday.features.gamification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentNestRewardPolicyTest {
    @Test
    fun rewardWithConfirmedGainShowsRecentNestReward() {
        assertTrue(
            shouldShowRecentNestReward(
                RecentNestReward(
                    actionTitle = "Brosser les dents",
                    xp = 5,
                    flammeches = 2,
                    crystals = 1,
                ),
            ),
        )
    }

    @Test
    fun rewardWithoutGainDoesNotShowRecentNestReward() {
        assertFalse(
            shouldShowRecentNestReward(
                RecentNestReward(
                    actionTitle = "Action sans gain",
                ),
            ),
        )
    }

    @Test
    fun blankActionTitleDoesNotShowRecentNestReward() {
        assertFalse(
            shouldShowRecentNestReward(
                RecentNestReward(
                    actionTitle = " ",
                    xp = 5,
                ),
            ),
        )
    }

    @Test
    fun missingRewardDoesNotShowRecentNestReward() {
        assertFalse(shouldShowRecentNestReward(null))
    }
}

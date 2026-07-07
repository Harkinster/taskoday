package com.example.taskoday.features.shop

import com.example.taskoday.domain.model.Reward
import com.example.taskoday.domain.model.RewardRedemptionRequest
import com.example.taskoday.domain.model.RewardRequestStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WishRequestGuardTest {
    @Test
    fun `parent mode cannot request a wish for the child`() {
        val result =
            wishRequestGuard(
                state = baseState(isParent = true),
                reward = reward(childId = 19L),
                allowParentLocalChildMode = false,
            )

        assertFalse(result.allowed)
        assertTrue(result.message?.contains("parent") == true)
    }

    @Test
    fun `local child mode can request a wish for the active child`() {
        val result =
            wishRequestGuard(
                state = baseState(isParent = true, activeChildId = 19L),
                reward = reward(childId = 19L),
                allowParentLocalChildMode = true,
            )

        assertTrue(result.allowed)
    }

    @Test
    fun `missing active child blocks wish request`() {
        val result =
            wishRequestGuard(
                state = baseState(isParent = false, activeChildId = null),
                reward = reward(childId = 19L),
                allowParentLocalChildMode = false,
            )

        assertFalse(result.allowed)
        assertTrue(result.message?.contains("enfant") == true)
    }

    @Test
    fun `wish from another child is blocked`() {
        val result =
            wishRequestGuard(
                state = baseState(isParent = true, activeChildId = 19L),
                reward = reward(childId = 42L),
                allowParentLocalChildMode = true,
            )

        assertFalse(result.allowed)
        assertTrue(result.message?.contains("sélectionné") == true)
    }

    @Test
    fun `pending request blocks duplicate wish request`() {
        val result =
            wishRequestGuard(
                state =
                    baseState(
                        isParent = true,
                        activeChildId = 19L,
                        requests = listOf(request(rewardId = 7L)),
                    ),
                reward = reward(id = 7L, childId = 19L),
                allowParentLocalChildMode = true,
            )

        assertFalse(result.allowed)
        assertTrue(result.message?.contains("attente") == true)
    }
}

private fun baseState(
    isParent: Boolean,
    activeChildId: Long? = 19L,
    requests: List<RewardRedemptionRequest> = emptyList(),
): ShopUiState =
    ShopUiState(
        activeChildId = activeChildId,
        scalesBalance = 20,
        requests = requests,
        hasRemoteSession = true,
        isParent = isParent,
        isLoading = false,
    )

private fun reward(
    id: Long = 7L,
    childId: Long? = 19L,
): Reward =
    Reward(
        id = id,
        title = "Souhait test",
        cost = 5,
        childId = childId,
        createdAt = 0L,
        updatedAt = 0L,
    )

private fun request(rewardId: Long): RewardRedemptionRequest =
    RewardRedemptionRequest(
        id = 99L,
        childId = 19L,
        rewardId = rewardId,
        rewardTitle = "Souhait test",
        costScales = 5,
        status = RewardRequestStatus.PENDING,
        requestedAt = null,
        decidedAt = null,
        expiresAt = null,
        note = null,
        parentNote = null,
        coupon = null,
    )

package com.example.taskoday.features.shop

import com.example.taskoday.data.remote.gamification.NestApi
import com.example.taskoday.data.repository.NestRepository
import com.example.taskoday.domain.model.*
import com.example.taskoday.domain.repository.*
import java.lang.reflect.Proxy
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FreeWishCreationTest {
    @Test fun `four existing wishes do not prevent another parent wish`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val fixture = Fixture("PARENT")
            fixture.vm.createReward("Fifth wish", null, 10)
            assertEquals(1, fixture.created)
            assertEquals(5, fixture.vm.uiState.value.rewards.size)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun `free access does not allow children to create parent offers`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val fixture = Fixture("CHILD")
            fixture.vm.createReward("Forbidden offer", null, 10)
            assertEquals(0, fixture.created)
        } finally { Dispatchers.resetMain() }
    }
    private inner class Fixture(role: String) {
        var created = 0
        val rewards = (1L..4L).map { Reward(it, "Wish $it", cost = 10, createdAt = 0, updatedAt = 0) }.toMutableList()
        private val auth = object : AuthRepository by unused() {
            override fun getAccessToken() = "test-session"
            override suspend fun fetchMe() = AuthenticatedUser(1,"qa@example.test",role,true,listOf(7),"QA")
            override suspend fun getActiveChildId(forceRefresh: Boolean) = 2L
        }
        private val repository = object : RewardRepository by unused() {
            override fun observeActiveRewards() = flowOf(rewards.toList())
            override suspend fun fetchRemoteShopSnapshot(childId: Long?, includeInactiveRewards: Boolean) =
                Result.success(RewardShopSnapshot(childId = 2, rewards = rewards.toList(), scalesBalance = 0, requests = emptyList()))
            override suspend fun createRemoteReward(title: String, description: String?, costScales: Int, isActive: Boolean): Result<Reward> {
                created++
                val reward = Reward(5, title, description, costScales, isActive = isActive, createdAt = 0, updatedAt = 0)
                rewards += reward
                return Result.success(reward)
            }
        }
        private val points = object : PointsRepository by unused() {
            override fun observeBalance() = flowOf(0)
            override fun observeRecentTransactions(limit: Int) = flowOf(emptyList<PointsTransaction>())
        }
        val vm = ShopViewModel(repository, points, auth, NestRepository(unused(), auth))
    }
    @Suppress("UNCHECKED_CAST") private inline fun <reified T> unused(): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, _, _ -> throw IOException("Unused QA dependency") } as T
}

package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.Reward
import com.example.taskoday.domain.model.RewardRedemptionRequest
import com.example.taskoday.domain.model.RewardRequestStatus
import com.example.taskoday.domain.model.RewardShopSnapshot
import kotlinx.coroutines.flow.Flow

interface RewardRepository {
    fun observeActiveRewards(): Flow<List<Reward>>
    suspend fun getReward(rewardId: Long): Reward?
    suspend fun upsertReward(reward: Reward): Long
    suspend fun fetchRemoteShopSnapshot(): Result<RewardShopSnapshot>
    suspend fun createRemoteReward(title: String, description: String?, costScales: Int): Result<Reward>
    suspend fun requestRemoteReward(rewardId: Long, note: String? = null): Result<RewardRedemptionRequest>
    suspend fun decideRemoteRequest(requestId: Long, status: RewardRequestStatus): Result<RewardRedemptionRequest>
    suspend fun useRemoteCoupon(couponId: Long): Result<RewardRedemptionRequest>
}

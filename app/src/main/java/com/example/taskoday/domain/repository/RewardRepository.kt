package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.Reward
import kotlinx.coroutines.flow.Flow

interface RewardRepository {
    fun observeActiveRewards(): Flow<List<Reward>>
    suspend fun getReward(rewardId: Long): Reward?
    suspend fun upsertReward(reward: Reward): Long
}

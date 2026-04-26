package com.example.taskoday.data.repository

import com.example.taskoday.data.local.dao.RewardDao
import com.example.taskoday.data.mapper.toDomain
import com.example.taskoday.data.mapper.toEntity
import com.example.taskoday.domain.model.Reward
import com.example.taskoday.domain.repository.RewardRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RewardRepositoryImpl
    @Inject
    constructor(
        private val rewardDao: RewardDao,
    ) : RewardRepository {
        override fun observeActiveRewards(): Flow<List<Reward>> =
            rewardDao.observeActive().map { entities -> entities.map { it.toDomain() } }

        override suspend fun getReward(rewardId: Long): Reward? = rewardDao.getById(rewardId)?.toDomain()

        override suspend fun upsertReward(reward: Reward): Long = rewardDao.upsert(reward.toEntity())
    }

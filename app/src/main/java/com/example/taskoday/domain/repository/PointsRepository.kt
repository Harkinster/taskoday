package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.PointsSourceType
import com.example.taskoday.domain.model.PointsTransaction
import kotlinx.coroutines.flow.Flow

interface PointsRepository {
    fun observeBalance(): Flow<Int>
    fun observeRecentTransactions(limit: Int = 20): Flow<List<PointsTransaction>>
    suspend fun grantForTask(taskId: Long, dayStartMillis: Long, sourceType: PointsSourceType, points: Int, reason: String)
    suspend fun revokeForTask(taskId: Long, dayStartMillis: Long, sourceType: PointsSourceType)
    suspend fun grantForQuest(questId: Long, dayStartMillis: Long, points: Int, reason: String)
    suspend fun revokeForQuest(questId: Long, dayStartMillis: Long)
    suspend fun spendForReward(rewardId: Long, cost: Int, reason: String): Boolean
}

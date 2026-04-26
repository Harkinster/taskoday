package com.example.taskoday.data.repository

import com.example.taskoday.data.local.dao.PointsTransactionDao
import com.example.taskoday.data.local.entity.PointsTransactionEntity
import com.example.taskoday.data.mapper.toDomain
import com.example.taskoday.domain.model.PointsSourceType
import com.example.taskoday.domain.model.PointsTransaction
import com.example.taskoday.domain.repository.PointsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PointsRepositoryImpl
    @Inject
    constructor(
        private val pointsTransactionDao: PointsTransactionDao,
    ) : PointsRepository {
        override fun observeBalance(): Flow<Int> = pointsTransactionDao.observeBalance()

        override fun observeRecentTransactions(limit: Int): Flow<List<PointsTransaction>> =
            pointsTransactionDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }

        override suspend fun grantForTask(
            taskId: Long,
            dayStartMillis: Long,
            sourceType: PointsSourceType,
            points: Int,
            reason: String,
        ) {
            require(sourceType == PointsSourceType.ROUTINE || sourceType == PointsSourceType.MISSION)
            val existing =
                pointsTransactionDao.findBySourceForDay(
                    sourceType = sourceType,
                    sourceId = taskId,
                    dayStartMillis = dayStartMillis,
                )
            if (existing != null) return

            pointsTransactionDao.insert(
                PointsTransactionEntity(
                    amount = points,
                    reason = reason,
                    sourceType = sourceType,
                    sourceId = taskId,
                    dayStartMillis = dayStartMillis,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }

        override suspend fun revokeForTask(taskId: Long, dayStartMillis: Long, sourceType: PointsSourceType) {
            pointsTransactionDao.deleteBySourceForDay(
                sourceType = sourceType,
                sourceId = taskId,
                dayStartMillis = dayStartMillis,
            )
        }

        override suspend fun grantForQuest(questId: Long, dayStartMillis: Long, points: Int, reason: String) {
            val existing =
                pointsTransactionDao.findBySourceForDay(
                    sourceType = PointsSourceType.QUEST,
                    sourceId = questId,
                    dayStartMillis = dayStartMillis,
                )
            if (existing != null) return

            pointsTransactionDao.insert(
                PointsTransactionEntity(
                    amount = points,
                    reason = reason,
                    sourceType = PointsSourceType.QUEST,
                    sourceId = questId,
                    dayStartMillis = dayStartMillis,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }

        override suspend fun revokeForQuest(questId: Long, dayStartMillis: Long) {
            pointsTransactionDao.deleteBySourceForDay(
                sourceType = PointsSourceType.QUEST,
                sourceId = questId,
                dayStartMillis = dayStartMillis,
            )
        }

        override suspend fun spendForReward(rewardId: Long, cost: Int, reason: String): Boolean {
            val balance = pointsTransactionDao.getBalance()
            if (balance < cost) return false

            pointsTransactionDao.insert(
                PointsTransactionEntity(
                    amount = -cost,
                    reason = reason,
                    sourceType = PointsSourceType.REWARD_PURCHASE,
                    sourceId = rewardId,
                    dayStartMillis = null,
                    createdAt = System.currentTimeMillis(),
                ),
            )
            return true
        }
    }

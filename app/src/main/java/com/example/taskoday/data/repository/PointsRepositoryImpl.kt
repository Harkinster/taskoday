package com.example.taskoday.data.repository

import com.example.taskoday.data.local.dao.PointsTransactionDao
import com.example.taskoday.data.local.entity.PointsTransactionEntity
import com.example.taskoday.data.mapper.toDomain
import com.example.taskoday.data.demo.DemoModeStore
import com.example.taskoday.data.demo.DemoPointsDataSource
import com.example.taskoday.domain.model.PointsSourceType
import com.example.taskoday.domain.model.PointsTransaction
import com.example.taskoday.domain.repository.PointsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class PointsRepositoryImpl
    @Inject
    constructor(
        private val pointsTransactionDao: PointsTransactionDao,
        private val demoModeStore: DemoModeStore,
        private val demoDataSource: DemoPointsDataSource,
    ) : PointsRepository {
        override fun observeBalance(): Flow<Int> =
            demoModeStore.enabledFlow.flatMapLatest { enabled ->
                if (enabled) demoDataSource.observeBalance() else pointsTransactionDao.observeBalance()
            }

        override fun observeRecentTransactions(limit: Int): Flow<List<PointsTransaction>> =
            demoModeStore.enabledFlow.flatMapLatest { enabled ->
                if (enabled) {
                    kotlinx.coroutines.flow.flowOf(emptyList())
                } else {
                    pointsTransactionDao.observeRecent(limit).map { list -> list.map { it.toDomain() } }
                }
            }

        override suspend fun grantForTask(
            taskId: Long,
            dayStartMillis: Long,
            sourceType: PointsSourceType,
            points: Int,
            reason: String,
        ) {
            if (demoModeStore.isEnabled) {
                demoDataSource.grant(sourceType, taskId, dayStartMillis, points)
                return
            }
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
            if (demoModeStore.isEnabled) {
                val points = if (sourceType == PointsSourceType.ROUTINE) 1 else 2
                demoDataSource.revoke(sourceType, taskId, dayStartMillis, points)
                return
            }
            pointsTransactionDao.deleteBySourceForDay(
                sourceType = sourceType,
                sourceId = taskId,
                dayStartMillis = dayStartMillis,
            )
        }

        override suspend fun grantForQuest(questId: Long, dayStartMillis: Long, points: Int, reason: String) {
            if (demoModeStore.isEnabled) {
                demoDataSource.grant(PointsSourceType.QUEST, questId, dayStartMillis, points)
                return
            }
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
            if (demoModeStore.isEnabled) {
                demoDataSource.revoke(PointsSourceType.QUEST, questId, dayStartMillis, 5)
                return
            }
            pointsTransactionDao.deleteBySourceForDay(
                sourceType = PointsSourceType.QUEST,
                sourceId = questId,
                dayStartMillis = dayStartMillis,
            )
        }

        override suspend fun spendForReward(rewardId: Long, cost: Int, reason: String): Boolean {
            if (demoModeStore.isEnabled) return demoDataSource.spend(cost)
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

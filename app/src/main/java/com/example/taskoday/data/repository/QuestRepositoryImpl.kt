package com.example.taskoday.data.repository

import com.example.taskoday.data.local.dao.QuestCompletionDao
import com.example.taskoday.data.local.dao.QuestDao
import com.example.taskoday.data.demo.DemoModeStore
import com.example.taskoday.data.demo.DemoQuestDataSource
import com.example.taskoday.data.local.entity.QuestCompletionEntity
import com.example.taskoday.data.mapper.toDomain
import com.example.taskoday.data.mapper.toEntity
import com.example.taskoday.domain.model.Quest
import com.example.taskoday.domain.model.QuestForDay
import com.example.taskoday.domain.repository.QuestRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
class QuestRepositoryImpl
    @Inject
    constructor(
        private val questDao: QuestDao,
        private val questCompletionDao: QuestCompletionDao,
        private val demoModeStore: DemoModeStore,
        private val demoDataSource: DemoQuestDataSource,
    ) : QuestRepository {
        override fun observeActiveQuests(): Flow<List<Quest>> =
            demoModeStore.enabledFlow.flatMapLatest { enabled ->
                if (enabled) demoDataSource.observeActiveQuests() else questDao.observeActive().map { entities -> entities.map { it.toDomain() } }
            }

        override fun observeQuestsForDay(dayStartMillis: Long): Flow<List<QuestForDay>> =
            demoModeStore.enabledFlow.flatMapLatest { enabled ->
                if (enabled) {
                    demoDataSource.observeQuestsForDay(dayStartMillis)
                } else {
                    questCompletionDao.observeForDay(dayStartMillis).map { rows ->
                        rows.map { row ->
                            QuestForDay(
                                quest = row.quest.toDomain(),
                                isCompletedForDay = row.isCompletedForDay,
                            )
                        }
                    }
                }
            }

        override suspend fun upsertQuest(quest: Quest): Long =
            if (demoModeStore.isEnabled) quest.id else questDao.upsert(quest.toEntity())

        override suspend fun deleteQuest(questId: Long) {
            if (!demoModeStore.isEnabled) questDao.deleteById(questId)
        }

        override suspend fun setQuestCompletedForDay(questId: Long, dayStartMillis: Long, completed: Boolean) {
            if (demoModeStore.isEnabled) {
                demoDataSource.setCompleted(completed)
                return
            }
            if (completed) {
                questCompletionDao.upsert(
                    QuestCompletionEntity(
                        questId = questId,
                        dayStartMillis = dayStartMillis,
                        completedAt = System.currentTimeMillis(),
                    ),
                )
            } else {
                questCompletionDao.deleteForDay(questId, dayStartMillis)
            }
        }

        override suspend fun clearRemoteCache() {
            if (!demoModeStore.isEnabled) {
                questCompletionDao.deleteRemoteCachedCompletions()
                questDao.deleteRemoteCachedQuests()
            }
        }
    }

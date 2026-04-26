package com.example.taskoday.data.repository

import com.example.taskoday.data.local.dao.QuestCompletionDao
import com.example.taskoday.data.local.dao.QuestDao
import com.example.taskoday.data.local.entity.QuestCompletionEntity
import com.example.taskoday.data.mapper.toDomain
import com.example.taskoday.data.mapper.toEntity
import com.example.taskoday.domain.model.Quest
import com.example.taskoday.domain.model.QuestForDay
import com.example.taskoday.domain.repository.QuestRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class QuestRepositoryImpl
    @Inject
    constructor(
        private val questDao: QuestDao,
        private val questCompletionDao: QuestCompletionDao,
    ) : QuestRepository {
        override fun observeActiveQuests(): Flow<List<Quest>> =
            questDao.observeActive().map { entities -> entities.map { it.toDomain() } }

        override fun observeQuestsForDay(dayStartMillis: Long): Flow<List<QuestForDay>> =
            questCompletionDao.observeForDay(dayStartMillis).map { rows ->
                rows.map { row ->
                    QuestForDay(
                        quest = row.quest.toDomain(),
                        isCompletedForDay = row.isCompletedForDay,
                    )
                }
            }

        override suspend fun upsertQuest(quest: Quest): Long = questDao.upsert(quest.toEntity())

        override suspend fun deleteQuest(questId: Long) {
            questDao.deleteById(questId)
        }

        override suspend fun setQuestCompletedForDay(questId: Long, dayStartMillis: Long, completed: Boolean) {
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
            questCompletionDao.deleteRemoteCachedCompletions()
            questDao.deleteRemoteCachedQuests()
        }
    }

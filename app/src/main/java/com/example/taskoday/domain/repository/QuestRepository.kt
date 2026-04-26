package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.Quest
import com.example.taskoday.domain.model.QuestForDay
import kotlinx.coroutines.flow.Flow

interface QuestRepository {
    fun observeActiveQuests(): Flow<List<Quest>>
    fun observeQuestsForDay(dayStartMillis: Long): Flow<List<QuestForDay>>
    suspend fun upsertQuest(quest: Quest): Long
    suspend fun deleteQuest(questId: Long)
    suspend fun setQuestCompletedForDay(questId: Long, dayStartMillis: Long, completed: Boolean)
    suspend fun clearRemoteCache()
}

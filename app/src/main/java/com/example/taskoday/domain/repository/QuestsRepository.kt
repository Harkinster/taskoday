package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.Quest

data class QuestsSyncResult(
    val usedRemoteData: Boolean,
    val errorMessage: String? = null,
)

interface QuestsRepository {
    suspend fun syncQuests(): QuestsSyncResult

    suspend fun completeQuest(localQuestId: Long): Result<Unit>

    suspend fun createQuest(quest: Quest): Result<Quest>

    suspend fun updateQuest(quest: Quest): Result<Quest>

    suspend fun deleteQuest(localQuestId: Long): Result<Unit>
}

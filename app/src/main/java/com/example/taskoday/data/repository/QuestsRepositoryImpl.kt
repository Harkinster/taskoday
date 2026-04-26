package com.example.taskoday.data.repository

import android.util.Log
import com.example.taskoday.data.remote.dto.QuestCreateRequestDto
import com.example.taskoday.data.remote.dto.QuestItemDto
import com.example.taskoday.data.remote.dto.QuestUpdateRequestDto
import com.example.taskoday.data.remote.quests.QuestsApi
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.PlanningItemType
import com.example.taskoday.domain.model.Quest
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.QuestRepository
import com.example.taskoday.domain.repository.QuestsRepository
import com.example.taskoday.domain.repository.QuestsSyncResult
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestsRepositoryImpl
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val questsApi: QuestsApi,
        private val questRepository: QuestRepository,
    ) : QuestsRepository {
        override suspend fun syncQuests(): QuestsSyncResult {
            if (authRepository.getAccessToken().isNullOrBlank()) {
                return QuestsSyncResult(usedRemoteData = false)
            }
            val childId = runCatching { authRepository.getActiveChildId(forceRefresh = true) }.getOrNull()
            if (childId == null) {
                return QuestsSyncResult(usedRemoteData = false)
            }

            return runCatching {
                val quests = questsApi.getQuests(childId)
                questRepository.clearRemoteCache()
                val now = System.currentTimeMillis()
                quests
                    .filter { it.isActive }
                    .forEach { dto ->
                        val quest = dto.toDomain(now)
                        questRepository.upsertQuest(quest)
                    }
                QuestsSyncResult(usedRemoteData = true)
            }.getOrElse { error ->
                Log.w(TAG, "Impossible de synchroniser les quetes", error)
                QuestsSyncResult(
                    usedRemoteData = false,
                    errorMessage = error.toRemoteUserMessage("Erreur reseau, fallback local."),
                )
            }
        }

        override suspend fun completeQuest(localQuestId: Long): Result<Unit> =
            runCatching {
                val remoteRef = RemotePlanningIdCodec.decodeQuestId(localQuestId)
                require(remoteRef?.itemType == PlanningItemType.QUEST) { "Identifiant quete invalide." }
                questsApi.completeQuest(remoteRef.remoteItemId)
            }.onFailure { error ->
                Log.w(TAG, "Completion quete distante echouee", error)
            }

        override suspend fun createQuest(quest: Quest): Result<Quest> =
            runCatching {
                val me = authRepository.fetchMe()
                require(me.role.equals("PARENT", ignoreCase = true)) { "Action non autorisee." }
                val childId =
                    authRepository.getActiveChildId(forceRefresh = true)
                        ?: error("Aucun enfant actif pour creer la quete.")
                val created =
                    questsApi.createQuest(
                        childId = childId,
                        payload =
                            QuestCreateRequestDto(
                                title = quest.title.trim(),
                                description = quest.description,
                                dayPart = quest.dayPart.name,
                                scheduledDate = LocalDate.now().toString(),
                                pointsReward = quest.pointsReward,
                                isActive = true,
                            ),
                    )
                created.toDomain(System.currentTimeMillis())
            }.onFailure { error ->
                Log.w(TAG, "Creation quete distante echouee", error)
            }

        override suspend fun updateQuest(quest: Quest): Result<Quest> =
            runCatching {
                val me = authRepository.fetchMe()
                require(me.role.equals("PARENT", ignoreCase = true)) { "Action non autorisee." }
                val remoteRef = RemotePlanningIdCodec.decodeQuestId(quest.id)
                require(remoteRef?.itemType == PlanningItemType.QUEST) { "Identifiant quete invalide." }
                val updated =
                    questsApi.updateQuest(
                        questId = remoteRef.remoteItemId,
                        payload =
                            QuestUpdateRequestDto(
                                title = quest.title.trim(),
                                description = quest.description,
                                dayPart = quest.dayPart.name,
                                pointsReward = quest.pointsReward,
                                isActive = quest.isActive,
                            ),
                    )
                updated.toDomain(System.currentTimeMillis())
            }.onFailure { error ->
                Log.w(TAG, "Mise a jour quete distante echouee", error)
            }

        override suspend fun deleteQuest(localQuestId: Long): Result<Unit> =
            runCatching {
                val me = authRepository.fetchMe()
                require(me.role.equals("PARENT", ignoreCase = true)) { "Action non autorisee." }
                val remoteRef = RemotePlanningIdCodec.decodeQuestId(localQuestId)
                require(remoteRef?.itemType == PlanningItemType.QUEST) { "Identifiant quete invalide." }
                questsApi.deleteQuest(remoteRef.remoteItemId)
            }.onFailure { error ->
                Log.w(TAG, "Suppression quete distante echouee", error)
            }

        private companion object {
            const val TAG = "QuestsRepository"
        }
    }

private fun QuestItemDto.toDomain(nowMillis: Long): Quest =
    Quest(
        id = RemotePlanningIdCodec.encodeQuestId(id),
        title = title,
        description = description,
        emoji = "\u2B50",
        pointsReward = pointsReward ?: 3,
        isActive = isActive,
        dayPart = runCatching { DayPart.valueOf(dayPart.orEmpty()) }.getOrDefault(DayPart.APRES_MIDI),
        createdAt = nowMillis,
        updatedAt = nowMillis,
    )

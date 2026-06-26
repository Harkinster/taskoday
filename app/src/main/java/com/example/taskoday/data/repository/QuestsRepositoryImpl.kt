package com.example.taskoday.data.repository

import android.util.Log
import com.example.taskoday.core.util.DateTimeUtils
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
                val quests = questsApi.getQuests(childId).data
                questRepository.clearRemoteCache()
                val now = System.currentTimeMillis()
                quests
                    .filter { it.isActive != false }
                    .forEach { dto ->
                        val quest = dto.toDomain(now)
                        questRepository.upsertQuest(quest)
                        questRepository.setQuestCompletedForDay(
                            questId = quest.id,
                            dayStartMillis = DateTimeUtils.startOfDayMillis(),
                            completed = dto.isCompleted == true || dto.status.equals("completed", ignoreCase = true),
                        )
                    }
                QuestsSyncResult(usedRemoteData = true)
            }.getOrElse { error ->
                Log.w(TAG, "Impossible de synchroniser les quêtes", error)
                QuestsSyncResult(
                    usedRemoteData = false,
                    errorMessage = error.toRemoteUserMessage("Erreur réseau, fallback local."),
                )
            }
        }

        override suspend fun completeQuest(localQuestId: Long): Result<Unit> =
            runCatching {
                val remoteRef = RemotePlanningIdCodec.decodeQuestId(localQuestId)
                require(remoteRef?.itemType == PlanningItemType.QUEST) { "Identifiant quête invalide." }
                questsApi.completeQuest(remoteRef.remoteItemId)
                Unit
            }.onFailure { error ->
                Log.w(TAG, "Complétion quête distante échouée", error)
            }

        override suspend fun createQuest(quest: Quest): Result<Quest> =
            runCatching {
                val me = authRepository.fetchMe()
                require(me.role.equals("PARENT", ignoreCase = true)) { "Action non autorisée." }
                val childId =
                    authRepository.getActiveChildId(forceRefresh = true)
                        ?: error("Aucun enfant actif pour créer la quête.")
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
                    ).data
                created.toDomain(System.currentTimeMillis())
            }.onFailure { error ->
                Log.w(TAG, "Création quête distante échouée", error)
            }

        override suspend fun updateQuest(quest: Quest): Result<Quest> =
            runCatching {
                val me = authRepository.fetchMe()
                require(me.role.equals("PARENT", ignoreCase = true)) { "Action non autorisée." }
                val remoteRef = RemotePlanningIdCodec.decodeQuestId(quest.id)
                require(remoteRef?.itemType == PlanningItemType.QUEST) { "Identifiant quête invalide." }
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
                    ).data
                updated.toDomain(System.currentTimeMillis())
            }.onFailure { error ->
                Log.w(TAG, "Mise à jour quête distante échouée", error)
            }

        override suspend fun deleteQuest(localQuestId: Long): Result<Unit> =
            runCatching {
                val me = authRepository.fetchMe()
                require(me.role.equals("PARENT", ignoreCase = true)) { "Action non autorisée." }
                val remoteRef = RemotePlanningIdCodec.decodeQuestId(localQuestId)
                require(remoteRef?.itemType == PlanningItemType.QUEST) { "Identifiant quête invalide." }
                questsApi.deleteQuest(remoteRef.remoteItemId)
            }.onFailure { error ->
                Log.w(TAG, "Suppression quête distante échouée", error)
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
        pointsReward = xpReward ?: pointsReward ?: 3,
        isActive = isActive != false,
        dayPart = runCatching { DayPart.valueOf(dayPart.orEmpty()) }.getOrDefault(DayPart.APRES_MIDI),
        createdAt = nowMillis,
        updatedAt = nowMillis,
    )

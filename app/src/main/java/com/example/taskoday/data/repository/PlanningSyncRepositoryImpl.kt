package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.planning.PlanningApi
import com.example.taskoday.domain.model.CompletionReward
import com.example.taskoday.domain.model.PlanningItemType
import com.example.taskoday.domain.model.RemotePlanningRef
import com.example.taskoday.domain.repository.MissionsRepository
import com.example.taskoday.domain.repository.PlanningSyncRepository
import com.example.taskoday.domain.repository.PlanningSyncResult
import com.example.taskoday.domain.repository.QuestsRepository
import com.example.taskoday.domain.repository.RoutinesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanningSyncRepositoryImpl
    @Inject
    constructor(
        private val planningApi: PlanningApi,
        private val routinesRepository: RoutinesRepository,
        private val missionsRepository: MissionsRepository,
        private val questsRepository: QuestsRepository,
    ) : PlanningSyncRepository {
        override suspend fun syncDay(dayStartMillis: Long): PlanningSyncResult {
            val routinesSyncResult = routinesRepository.syncRoutinesForDay(dayStartMillis)
            val missionsSyncResult = missionsRepository.syncMissions()
            val questsSyncResult = questsRepository.syncQuests()

            return PlanningSyncResult(
                usedRemoteData =
                    routinesSyncResult.usedRemoteData ||
                        missionsSyncResult.usedRemoteData ||
                        questsSyncResult.usedRemoteData,
                errorMessage =
                    listOfNotNull(
                        routinesSyncResult.errorMessage,
                        missionsSyncResult.errorMessage,
                        questsSyncResult.errorMessage,
                    ).firstOrNull(),
            )
        }

        override suspend fun setCompletion(
            dayStartMillis: Long,
            remoteRef: RemotePlanningRef,
            completed: Boolean,
        ): Result<CompletionReward?> =
            runCatching {
                val response =
                    when (remoteRef.itemType) {
                        PlanningItemType.ROUTINE -> {
                            if (completed) {
                                planningApi.completeRoutine(remoteRef.remoteItemId).data
                            } else {
                                planningApi.uncompleteRoutine(remoteRef.remoteItemId).data
                            }
                        }

                        PlanningItemType.MISSION -> {
                            require(completed) { "La devalidation distante des missions n'est pas disponible." }
                            planningApi.completeMission(remoteRef.remoteItemId).data
                        }

                        PlanningItemType.QUEST -> {
                            require(completed) { "La dévalidation distante des quêtes n'est pas disponible." }
                            planningApi.completeQuest(remoteRef.remoteItemId).data
                        }
                    }
                response.award?.let { award ->
                    CompletionReward(
                        xp = award.guardianXpAwarded,
                        flammeches = award.flammechesAwarded,
                        crystals = award.crystalsAwarded,
                    )
                }
            }
    }

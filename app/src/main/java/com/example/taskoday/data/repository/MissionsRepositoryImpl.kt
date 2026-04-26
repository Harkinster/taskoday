package com.example.taskoday.data.repository

import android.util.Log
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.data.remote.dto.MissionCreateRequestDto
import com.example.taskoday.data.remote.dto.MissionItemDto
import com.example.taskoday.data.remote.dto.MissionUpdateRequestDto
import com.example.taskoday.data.remote.missions.MissionsApi
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.PlanningItemType
import com.example.taskoday.domain.model.Task
import com.example.taskoday.domain.model.TaskPriority
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.model.TaskType
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.MissionsRepository
import com.example.taskoday.domain.repository.MissionsSyncResult
import com.example.taskoday.domain.repository.TaskRepository
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MissionsRepositoryImpl
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val missionsApi: MissionsApi,
        private val taskRepository: TaskRepository,
    ) : MissionsRepository {
        override suspend fun syncMissions(): MissionsSyncResult {
            if (authRepository.getAccessToken().isNullOrBlank()) {
                return MissionsSyncResult(usedRemoteData = false)
            }
            val childId = runCatching { authRepository.getActiveChildId(forceRefresh = true) }.getOrNull()
            if (childId == null) {
                return MissionsSyncResult(usedRemoteData = false)
            }

            return runCatching {
                val missions = missionsApi.getMissions(childId)
                taskRepository.clearRemoteMissionCache()
                val now = System.currentTimeMillis()
                missions
                    .filter { it.isActive }
                    .forEach { mission ->
                        taskRepository.upsertTask(mission.toTask(now))
                    }
                MissionsSyncResult(usedRemoteData = true)
            }.getOrElse { error ->
                Log.w(TAG, "Impossible de synchroniser les missions", error)
                MissionsSyncResult(
                    usedRemoteData = false,
                    errorMessage = error.toRemoteUserMessage("Erreur reseau, fallback local."),
                )
            }
        }

        override suspend fun createMissionFromTask(task: Task): Result<Task> =
            runCatching {
                val childId =
                    authRepository.getActiveChildId(forceRefresh = true)
                        ?: error("Aucun enfant actif pour creer la mission.")
                val scheduledDate =
                    task.scheduledDate?.let { DateTimeUtils.toLocalDate(it) }
                        ?: task.dueDate?.let { DateTimeUtils.toLocalDate(it) }
                        ?: LocalDate.now()
                val created =
                    missionsApi.createMission(
                        childId = childId,
                        payload =
                            MissionCreateRequestDto(
                                title = task.title.trim(),
                                description = task.description,
                                dayPart = task.dayPart.name,
                                scheduledDate = scheduledDate.toString(),
                                pointsReward = task.priority.toPointsReward(),
                                isActive = true,
                            ),
                    )
                created.toTask(nowMillis = System.currentTimeMillis())
            }.onFailure { error ->
                Log.w(TAG, "Creation mission distante echouee", error)
            }

        override suspend fun updateMissionFromTask(localTaskId: Long, task: Task): Result<Task> =
            runCatching {
                val remoteRef = RemotePlanningIdCodec.decodeTaskId(localTaskId)
                require(remoteRef?.itemType == PlanningItemType.MISSION) { "Identifiant mission invalide." }
                val scheduledDate =
                    task.scheduledDate?.let { DateTimeUtils.toLocalDate(it) }
                        ?: task.dueDate?.let { DateTimeUtils.toLocalDate(it) }
                        ?: LocalDate.now()
                val updated =
                    missionsApi.updateMission(
                        missionId = remoteRef.remoteItemId,
                        payload =
                            MissionUpdateRequestDto(
                                title = task.title.trim(),
                                description = task.description,
                                dayPart = task.dayPart.name,
                                scheduledDate = scheduledDate.toString(),
                                pointsReward = task.priority.toPointsReward(),
                                isActive = true,
                            ),
                    )
                updated.toTask(nowMillis = System.currentTimeMillis())
            }.onFailure { error ->
                Log.w(TAG, "Mise a jour mission distante echouee", error)
            }

        override suspend fun completeMission(localTaskId: Long): Result<Unit> =
            runCatching {
                val remoteRef = RemotePlanningIdCodec.decodeTaskId(localTaskId)
                require(remoteRef?.itemType == PlanningItemType.MISSION) { "Identifiant mission invalide." }
                missionsApi.completeMission(remoteRef.remoteItemId)
            }.onFailure { error ->
                Log.w(TAG, "Completion mission distante echouee", error)
            }

        override suspend fun deleteMission(localTaskId: Long): Result<Unit> =
            runCatching {
                val remoteRef = RemotePlanningIdCodec.decodeTaskId(localTaskId)
                require(remoteRef?.itemType == PlanningItemType.MISSION) { "Identifiant mission invalide." }
                missionsApi.deleteMission(remoteRef.remoteItemId)
            }.onFailure { error ->
                Log.w(TAG, "Suppression mission distante echouee", error)
            }

        private companion object {
            const val TAG = "MissionsRepository"
        }
    }

private fun MissionItemDto.toTask(nowMillis: Long): Task {
    val parsedDayPart = runCatching { DayPart.valueOf(dayPart.orEmpty()) }.getOrDefault(DayPart.MATIN)
    val scheduledLocalDate = scheduledDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val scheduledDayStart = scheduledLocalDate?.let { DateTimeUtils.startOfDayMillis(it) }
    val dueDate =
        scheduledLocalDate?.let { date ->
            DateTimeUtils.epochMillisAtHour(date, parsedDayPart.defaultHour())
        }

    return Task(
        id = RemotePlanningIdCodec.encodeTaskId(PlanningItemType.MISSION, id),
        title = title,
        emoji = "\uD83C\uDFAF",
        description = description,
        dueDate = dueDate,
        priority = TaskPriority.NORMAL,
        status = if (isCompleted == true) TaskStatus.DONE else TaskStatus.TODO,
        taskType = TaskType.ONE_TIME,
        dayPart = parsedDayPart,
        scheduledDate = scheduledDayStart,
        routineDays = emptySet(),
        isRoutine = false,
        createdAt = nowMillis,
        updatedAt = nowMillis,
    )
}

private fun TaskPriority.toPointsReward(): Int =
    when (this) {
        TaskPriority.LOW -> 1
        TaskPriority.NORMAL -> 2
        TaskPriority.HIGH -> 3
        TaskPriority.URGENT -> 4
    }

private fun DayPart.defaultHour(): Int =
    when (this) {
        DayPart.MATIN -> 7
        DayPart.MATINEE -> 9
        DayPart.MIDI -> 12
        DayPart.APRES_MIDI -> 15
        DayPart.SOIR -> 18
        DayPart.SOIREE -> 20
    }

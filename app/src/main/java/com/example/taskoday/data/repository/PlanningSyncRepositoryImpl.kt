package com.example.taskoday.data.repository

import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.data.remote.dto.PlanningItemDto
import com.example.taskoday.data.remote.dto.PlanningResponseDto
import com.example.taskoday.data.remote.planning.PlanningApi
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.PlanningItemType
import com.example.taskoday.domain.model.Quest
import com.example.taskoday.domain.model.RemotePlanningRef
import com.example.taskoday.domain.model.Task
import com.example.taskoday.domain.model.TaskPriority
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.model.TaskType
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.PlanningSyncRepository
import com.example.taskoday.domain.repository.PlanningSyncResult
import com.example.taskoday.domain.repository.QuestRepository
import com.example.taskoday.domain.repository.RoutinesRepository
import com.example.taskoday.domain.repository.TaskRepository
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class PlanningSyncRepositoryImpl
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val planningApi: PlanningApi,
        private val routinesRepository: RoutinesRepository,
        private val taskRepository: TaskRepository,
        private val questRepository: QuestRepository,
    ) : PlanningSyncRepository {
        override suspend fun syncDay(dayStartMillis: Long): PlanningSyncResult {
            val routinesSyncResult = routinesRepository.syncRoutinesForDay(dayStartMillis)
            var usedRemoteData = routinesSyncResult.usedRemoteData
            var errorMessage = routinesSyncResult.errorMessage

            if (authRepository.getAccessToken().isNullOrBlank()) {
                return PlanningSyncResult(usedRemoteData = usedRemoteData, errorMessage = errorMessage)
            }

            val childId = runCatching { authRepository.getActiveChildId() }.getOrNull()
            if (childId == null) {
                return PlanningSyncResult(usedRemoteData = usedRemoteData, errorMessage = errorMessage)
            }

            val planningDate = DateTimeUtils.toLocalDate(dayStartMillis).toString()
            val response =
                runCatching {
                    planningApi.getPlanning(
                        childId = childId,
                        date = planningDate,
                    )
                }.getOrElse { error ->
                    val planningErrorMessage = error.toMessage()
                    return PlanningSyncResult(
                        usedRemoteData = usedRemoteData,
                        errorMessage =
                            if (usedRemoteData) {
                                errorMessage
                            } else {
                                errorMessage ?: planningErrorMessage
                            },
                    )
                }

            replaceRemoteCache(dayStartMillis = dayStartMillis, response = response)
            usedRemoteData = true
            errorMessage = null
            return PlanningSyncResult(usedRemoteData = usedRemoteData, errorMessage = errorMessage)
        }

        override suspend fun setCompletion(dayStartMillis: Long, remoteRef: RemotePlanningRef, completed: Boolean): Result<Unit> =
            runCatching {
                val date = DateTimeUtils.toLocalDate(dayStartMillis).toString()
                if (completed) {
                    planningApi.completeItem(
                        itemType = remoteRef.itemType.apiValue,
                        itemId = remoteRef.remoteItemId,
                        date = date,
                    )
                } else {
                    planningApi.uncompleteItem(
                        itemType = remoteRef.itemType.apiValue,
                        itemId = remoteRef.remoteItemId,
                        date = date,
                    )
                }
            }.map {}

        private suspend fun replaceRemoteCache(dayStartMillis: Long, response: PlanningResponseDto) {
            taskRepository.clearRemoteCache()
            questRepository.clearRemoteCache()

            val now = System.currentTimeMillis()
            val allItems = response.sections.flatMap { it.items }
            allItems.forEach { item ->
                val type = item.toPlanningItemType() ?: return@forEach
                when (type) {
                    PlanningItemType.ROUTINE, PlanningItemType.MISSION -> {
                        val localTaskId = RemotePlanningIdCodec.encodeTaskId(type, item.itemId)
                        val task =
                            Task(
                                id = localTaskId,
                                title = item.title,
                                emoji = type.defaultEmoji(),
                                description = item.description,
                                dueDate = item.resolveDueDate(dayStartMillis),
                                priority = TaskPriority.NORMAL,
                                status = if (item.isCompleted) TaskStatus.DONE else TaskStatus.TODO,
                                taskType = if (type == PlanningItemType.ROUTINE) TaskType.DAILY else TaskType.ONE_TIME,
                                dayPart = item.toDayPart(),
                                scheduledDate = item.resolveScheduledDay(dayStartMillis),
                                routineDays = if (type == PlanningItemType.ROUTINE) item.daysOfWeek.toRoutineDays() else emptySet(),
                                isRoutine = type == PlanningItemType.ROUTINE,
                                createdAt = now,
                                updatedAt = now,
                            )
                        taskRepository.upsertTask(task)
                        taskRepository.setTaskCheckedForDay(localTaskId, dayStartMillis, item.isCompleted)
                    }

                    PlanningItemType.QUEST -> {
                        val localQuestId = RemotePlanningIdCodec.encodeQuestId(item.itemId)
                        val quest =
                            Quest(
                                id = localQuestId,
                                title = item.title,
                                description = item.description,
                                emoji = type.defaultEmoji(),
                                pointsReward = item.pointsReward,
                                isActive = true,
                                dayPart = item.toDayPart(),
                                createdAt = now,
                                updatedAt = now,
                            )
                        questRepository.upsertQuest(quest)
                        questRepository.setQuestCompletedForDay(localQuestId, dayStartMillis, item.isCompleted)
                    }
                }
            }
        }
    }

private fun PlanningItemDto.toPlanningItemType(): PlanningItemType? =
    when (itemType.lowercase()) {
        PlanningItemType.ROUTINE.apiValue -> PlanningItemType.ROUTINE
        PlanningItemType.MISSION.apiValue -> PlanningItemType.MISSION
        PlanningItemType.QUEST.apiValue -> PlanningItemType.QUEST
        else -> null
    }

private fun PlanningItemDto.toDayPart(): DayPart = runCatching { DayPart.valueOf(dayPart) }.getOrDefault(DayPart.MATIN)

private fun PlanningItemType.defaultEmoji(): String =
    when (this) {
        PlanningItemType.ROUTINE -> "🔁"
        PlanningItemType.MISSION -> "🎯"
        PlanningItemType.QUEST -> "⭐"
    }

private fun PlanningItemDto.resolveDueDate(fallbackDayStart: Long): Long {
    val hour =
        when (toDayPart()) {
            DayPart.MATIN -> 7
            DayPart.MATINEE -> 9
            DayPart.MIDI -> 12
            DayPart.APRES_MIDI -> 15
            DayPart.SOIR -> 18
            DayPart.SOIREE -> 20
        }
    val scheduled = resolveScheduledDay(fallbackDayStart)
    return DateTimeUtils.epochMillisAtHour(DateTimeUtils.toLocalDate(scheduled), hour)
}

private fun PlanningItemDto.resolveScheduledDay(fallbackDayStart: Long): Long =
    scheduledDate
        ?.let {
            runCatching {
                DateTimeUtils.startOfDayMillis(LocalDate.parse(it))
            }.getOrNull()
        } ?: fallbackDayStart

private fun String?.toRoutineDays(): Set<Int> {
    if (isNullOrBlank()) return emptySet()
    return split(',')
        .mapNotNull { token -> token.trim().toIntOrNull() }
        .filter { day -> day in 1..7 }
        .toSet()
}

private fun Throwable.toMessage(): String =
    when (this) {
        is UnknownHostException, is ConnectException -> "Serveur backend indisponible. Passage en cache local."
        is SocketTimeoutException -> "Requete backend expiree. Passage en cache local."
        is HttpException -> "Erreur backend (${code()}). Passage en cache local."
        else -> message ?: "Erreur reseau. Passage en cache local."
    }

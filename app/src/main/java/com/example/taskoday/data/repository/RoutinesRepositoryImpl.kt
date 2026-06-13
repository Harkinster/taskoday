package com.example.taskoday.data.repository

import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.data.remote.children.ChildrenApi
import com.example.taskoday.data.remote.dto.RoutineItemDto
import com.example.taskoday.data.remote.dto.RoutineUpdateRequestDto
import com.example.taskoday.data.remote.planning.PlanningApi
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.PlanningItemType
import com.example.taskoday.domain.model.Task
import com.example.taskoday.domain.model.TaskPriority
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.model.TaskType
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.RoutinesRepository
import com.example.taskoday.domain.repository.RoutinesSyncResult
import com.example.taskoday.domain.repository.TaskRepository
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

@Singleton
class RoutinesRepositoryImpl
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val childrenApi: ChildrenApi,
        private val planningApi: PlanningApi,
        private val taskRepository: TaskRepository,
    ) : RoutinesRepository {
        override suspend fun syncRoutinesForDay(dayStartMillis: Long): RoutinesSyncResult {
            if (authRepository.getAccessToken().isNullOrBlank()) {
                return RoutinesSyncResult(usedRemoteData = false)
            }

            val childId = runCatching { authRepository.getActiveChildId(forceRefresh = true) }.getOrNull()
            if (childId == null) {
                return RoutinesSyncResult(usedRemoteData = false)
            }

            val routines =
                runCatching { childrenApi.getRoutines(childId).data }
                    .getOrElse { error ->
                        return RoutinesSyncResult(
                            usedRemoteData = false,
                            errorMessage = error.toMessage(),
                        )
                    }

            taskRepository.clearRemoteRoutineCache()
            val now = System.currentTimeMillis()

            routines
                .filter { it.isActive }
                .forEach { routine ->
                    val dayPart = routine.toDayPart()
                    val localTaskId = RemotePlanningIdCodec.encodeTaskId(PlanningItemType.ROUTINE, routine.id)
                    val task =
                        Task(
                            id = localTaskId,
                            title = routine.title,
                            emoji = "\uD83D\uDD01",
                            description = routine.description,
                            dueDate = DateTimeUtils.epochMillisAtHour(DateTimeUtils.toLocalDate(dayStartMillis), dayPart.defaultHour()),
                            priority = TaskPriority.NORMAL,
                            status = if (routine.completed) TaskStatus.DONE else TaskStatus.TODO,
                            taskType = TaskType.DAILY,
                            dayPart = dayPart,
                            scheduledDate = null,
                            routineDays = routine.toRoutineDays(),
                            isRoutine = true,
                            createdAt = now,
                            updatedAt = now,
                        )
                    taskRepository.upsertTask(task)
                    taskRepository.setTaskCheckedForDay(
                        taskId = localTaskId,
                        dayStartMillis = dayStartMillis,
                        checked = routine.completed,
                    )
                }

            return RoutinesSyncResult(usedRemoteData = true)
        }

        override suspend fun updateRoutineFromTask(localTaskId: Long, task: Task): Result<Task> =
            runCatching {
                val remoteRef = RemotePlanningIdCodec.decodeTaskId(localTaskId)
                require(remoteRef?.itemType == PlanningItemType.ROUTINE) { "Identifiant routine invalide." }
                val updated =
                    planningApi.updateRoutine(
                        routineId = remoteRef.remoteItemId,
                        payload =
                            RoutineUpdateRequestDto(
                                title = task.title.trim(),
                                description = task.description,
                            ),
                    ).data
                task.copy(
                    id = localTaskId,
                    title = updated.title,
                    description = updated.description,
                    updatedAt = System.currentTimeMillis(),
                )
            }

        override suspend fun deleteRoutine(localTaskId: Long): Result<Unit> =
            runCatching {
                val remoteRef = RemotePlanningIdCodec.decodeTaskId(localTaskId)
                require(remoteRef?.itemType == PlanningItemType.ROUTINE) { "Identifiant routine invalide." }
                planningApi.deleteRoutine(remoteRef.remoteItemId)
            }
    }

private fun RoutineItemDto.toDayPart(): DayPart = runCatching { DayPart.valueOf(dayPart.orEmpty()) }.getOrDefault(DayPart.MATIN)

private fun RoutineItemDto.toRoutineDays(): Set<Int> {
    val isDaily = frequency.equals("DAILY", ignoreCase = true)
    if (isDaily) return emptySet()
    return daysOfWeek
        .orEmpty()
        .split(",")
        .mapNotNull { token -> token.trim().toIntOrNull() }
        .filter { day -> day in 1..7 }
        .toSet()
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

private fun Throwable.toMessage(): String =
    when (this) {
        is UnknownHostException, is ConnectException -> "Serveur backend indisponible. Passage en mode local."
        is SocketTimeoutException -> "Requête routines expirée. Passage en mode local."
        is HttpException -> "Erreur backend (${code()}). Passage en mode local."
        else -> message ?: "Erreur reseau. Passage en mode local."
    }

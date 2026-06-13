package com.example.taskoday.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.data.repository.NestRepository
import com.example.taskoday.data.repository.RemotePlanningIdCodec
import com.example.taskoday.domain.model.CompletionReward
import com.example.taskoday.domain.model.PlanningItemType
import com.example.taskoday.domain.model.PointsSourceType
import com.example.taskoday.domain.model.QuestForDay
import com.example.taskoday.domain.model.TaskForDay
import com.example.taskoday.domain.repository.PlanningSyncRepository
import com.example.taskoday.domain.repository.PointsRepository
import com.example.taskoday.domain.repository.QuestRepository
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.RoutinesRepository
import com.example.taskoday.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val taskRepository: TaskRepository,
        private val questRepository: QuestRepository,
        private val pointsRepository: PointsRepository,
        private val planningSyncRepository: PlanningSyncRepository,
        private val nestRepository: NestRepository,
        private val authRepository: AuthRepository,
        private val routinesRepository: RoutinesRepository,
    ) : ViewModel() {
        private val selectedDay = MutableStateFlow(DateTimeUtils.startOfDayMillis())

        private val _uiState =
            MutableStateFlow(
                HomeUiState(
                    selectedDayStartMillis = selectedDay.value,
                    dateLabel = DateTimeUtils.formatDayLabel(selectedDay.value),
                    isLoading = true,
                ),
            )
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        init {
            observeHomeData()
            observeRemoteSync()
            resolveCanManageActions()
            cleanupOldChecks()
            observeDayRollover()
        }

        private fun resolveCanManageActions() {
            if (authRepository.getAccessToken().isNullOrBlank()) {
                _uiState.update { it.copy(canManageActions = true) }
                return
            }
            viewModelScope.launch {
                val canManage =
                    runCatching { authRepository.fetchMe().role.equals("PARENT", ignoreCase = true) }
                        .getOrDefault(false)
                _uiState.update { it.copy(canManageActions = canManage) }
            }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        private fun observeHomeData() {
            viewModelScope.launch {
                selectedDay
                    .flatMapLatest { dayStart ->
                        combine(
                            taskRepository.observeTasksForDay(dayStart),
                            questRepository.observeQuestsForDay(dayStart),
                            pointsRepository.observeBalance(),
                        ) { tasks, quests, pointsBalance ->
                            HomeSnapshot(dayStart, tasks, quests, pointsBalance)
                        }
                    }.collect { snapshot ->
                        _uiState.update {
                            it.copy(
                                selectedDayStartMillis = snapshot.dayStartMillis,
                                dateLabel = DateTimeUtils.formatDayLabel(snapshot.dayStartMillis),
                                tasksForDay = snapshot.tasks,
                                questsForDay = snapshot.quests,
                                pointsBalance = snapshot.pointsBalance,
                                isLoading = false,
                            )
                        }
                    }
            }
        }

        private fun observeRemoteSync() {
            viewModelScope.launch {
                selectedDay.collect { dayStart ->
                    _uiState.update { it.copy(isLoading = true) }
                    val syncResult = planningSyncRepository.syncDay(dayStart)
                    val remoteProgress =
                        if (syncResult.usedRemoteData) {
                            nestRepository.getProgress().getOrNull()
                        } else {
                            null
                        }
                    _uiState.update {
                        it.copy(
                            usingRemoteData = syncResult.usedRemoteData,
                            remoteXp = remoteProgress?.guardian?.xp,
                            remoteFlammeches = remoteProgress?.wallet?.flammeches,
                            remoteCrystals = remoteProgress?.wallet?.crystals,
                            errorMessage = syncResult.errorMessage,
                        )
                    }
                }
            }
        }

        private fun cleanupOldChecks() {
            viewModelScope.launch {
                val keepFrom = DateTimeUtils.plusDays(DateTimeUtils.startOfDayMillis(), -30L)
                taskRepository.cleanupOldTaskChecks(keepFrom)
            }
        }

        private fun observeDayRollover() {
            viewModelScope.launch {
                while (isActive) {
                    val now = System.currentTimeMillis()
                    val currentToday = DateTimeUtils.startOfDayMillis(now)
                    val nextDay = DateTimeUtils.plusDays(currentToday, 1L)
                    val delayMillis = (nextDay - now).coerceAtLeast(1_000L)
                    delay(delayMillis)

                    val newToday = DateTimeUtils.startOfDayMillis()
                    if (selectedDay.value == currentToday) {
                        selectDay(newToday)
                    }
                    cleanupOldChecks()
                }
            }
        }

        fun goToPreviousDay() {
            selectDay(DateTimeUtils.plusDays(selectedDay.value, -1L))
        }

        fun goToNextDay() {
            selectDay(DateTimeUtils.plusDays(selectedDay.value, 1L))
        }

        fun goToToday() {
            selectDay(DateTimeUtils.startOfDayMillis())
        }

        fun setTaskChecked(item: TaskForDay, checked: Boolean) {
            val dayStart = selectedDay.value
            val itemType = if (item.task.isDaily) PlanningItemType.ROUTINE else PlanningItemType.MISSION
            val completionKey = "${itemType.apiValue}-${item.task.id}"
            if (!beginCompletion(completionKey)) return
            viewModelScope.launch {
                val remoteRef = RemotePlanningIdCodec.decodeTaskId(item.task.id)
                var reward: CompletionReward? = null
                if (remoteRef != null) {
                    val remoteResult = planningSyncRepository.setCompletion(dayStart, remoteRef, checked)
                    if (remoteResult.isFailure) {
                        failCompletion(completionKey, remoteResult.exceptionOrNull()?.message ?: "Erreur reseau.")
                        return@launch
                    }
                    reward = remoteResult.getOrNull()
                    planningSyncRepository.syncDay(dayStart)
                }

                taskRepository.setTaskCheckedForDay(
                    taskId = item.task.id,
                    dayStartMillis = dayStart,
                    checked = checked,
                )
                applyTaskPoints(item = item, dayStart = dayStart, checked = checked)
                finishCompletion(
                    completionKey = completionKey,
                    actionTitle = item.task.title,
                    reward = reward ?: rewardPreviewFor(itemType).takeIf { remoteRef == null },
                    checked = checked,
                    refreshNest = remoteRef != null,
                )
            }
        }

        fun setQuestChecked(item: QuestForDay, checked: Boolean) {
            val dayStart = selectedDay.value
            val completionKey = "quest-${item.quest.id}"
            if (!beginCompletion(completionKey)) return
            viewModelScope.launch {
                val remoteRef = RemotePlanningIdCodec.decodeQuestId(item.quest.id)
                var reward: CompletionReward? = null
                if (remoteRef != null) {
                    val remoteResult = planningSyncRepository.setCompletion(dayStart, remoteRef, checked)
                    if (remoteResult.isFailure) {
                        failCompletion(completionKey, remoteResult.exceptionOrNull()?.message ?: "Erreur reseau.")
                        return@launch
                    }
                    reward = remoteResult.getOrNull()
                    planningSyncRepository.syncDay(dayStart)
                }

                questRepository.setQuestCompletedForDay(item.quest.id, dayStart, checked)
                applyQuestPoints(item = item, dayStart = dayStart, checked = checked)
                finishCompletion(
                    completionKey = completionKey,
                    actionTitle = item.quest.title,
                    reward = reward ?: rewardPreviewFor(PlanningItemType.QUEST).takeIf { remoteRef == null },
                    checked = checked,
                    refreshNest = remoteRef != null,
                )
            }
        }

        fun deleteRoutine(item: TaskForDay) {
            if (!_uiState.value.canManageActions || !item.task.isDaily) return
            val key = "routine-${item.task.id}"
            if (_uiState.value.pendingManagementKeys.contains(key)) return
            _uiState.update {
                it.copy(
                    pendingManagementKeys = it.pendingManagementKeys + key,
                    errorMessage = null,
                )
            }
            viewModelScope.launch {
                val remoteRef = RemotePlanningIdCodec.decodeTaskId(item.task.id)
                if (remoteRef?.itemType == PlanningItemType.ROUTINE) {
                    val result = routinesRepository.deleteRoutine(item.task.id)
                    if (result.isFailure) {
                        _uiState.update {
                            it.copy(
                                pendingManagementKeys = it.pendingManagementKeys - key,
                                errorMessage = result.exceptionOrNull()?.message ?: "Suppression impossible.",
                            )
                        }
                        return@launch
                    }
                }
                taskRepository.deleteTask(item.task.id)
                if (remoteRef != null) planningSyncRepository.syncDay(selectedDay.value)
                _uiState.update {
                    it.copy(
                        pendingManagementKeys = it.pendingManagementKeys - key,
                        successMessage = "Routine désactivée.",
                    )
                }
            }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        fun clearCompletionFeedback() {
            _uiState.update { it.copy(completionFeedback = null) }
        }

        fun selectDay(dayStartMillis: Long) {
            val normalized = DateTimeUtils.startOfDayMillis(dayStartMillis)
            selectedDay.value = normalized
            _uiState.update {
                it.copy(
                    selectedDayStartMillis = normalized,
                    dateLabel = DateTimeUtils.formatDayLabel(normalized),
                    isLoading = true,
                )
            }
        }

        private suspend fun applyTaskPoints(item: TaskForDay, dayStart: Long, checked: Boolean) {
            val sourceType = if (item.task.isDaily) PointsSourceType.ROUTINE else PointsSourceType.MISSION
            val rewardPoints = if (item.task.isDaily) 1 else 2
            val reasonPrefix = if (item.task.isDaily) "Routine" else "Mission"
            if (checked) {
                pointsRepository.grantForTask(
                    taskId = item.task.id,
                    dayStartMillis = dayStart,
                    sourceType = sourceType,
                    points = rewardPoints,
                    reason = "$reasonPrefix: ${item.task.title}",
                )
            } else {
                pointsRepository.revokeForTask(
                    taskId = item.task.id,
                    dayStartMillis = dayStart,
                    sourceType = sourceType,
                )
            }
        }

        private suspend fun applyQuestPoints(item: QuestForDay, dayStart: Long, checked: Boolean) {
            if (checked) {
                pointsRepository.grantForQuest(
                    questId = item.quest.id,
                    dayStartMillis = dayStart,
                    points = item.quest.pointsReward,
                    reason = "Quête : ${item.quest.title}",
                )
            } else {
                pointsRepository.revokeForQuest(item.quest.id, dayStart)
            }
        }

        private fun beginCompletion(completionKey: String): Boolean {
            if (_uiState.value.pendingCompletionKeys.contains(completionKey)) return false
            _uiState.update {
                it.copy(
                    pendingCompletionKeys = it.pendingCompletionKeys + completionKey,
                    completionFeedback = null,
                    errorMessage = null,
                )
            }
            return true
        }

        private fun failCompletion(
            completionKey: String,
            message: String,
        ) {
            _uiState.update {
                it.copy(
                    pendingCompletionKeys = it.pendingCompletionKeys - completionKey,
                    errorMessage = message,
                )
            }
        }

        private suspend fun finishCompletion(
            completionKey: String,
            actionTitle: String,
            reward: CompletionReward?,
            checked: Boolean,
            refreshNest: Boolean,
        ) {
            val progress = if (refreshNest) nestRepository.getProgress().getOrNull() else null
            if (refreshNest) nestRepository.notifyProgressChanged()
            _uiState.update {
                it.copy(
                    pendingCompletionKeys = it.pendingCompletionKeys - completionKey,
                    completionFeedback = CompletionFeedback(actionTitle, reward).takeIf { checked },
                    remoteXp = progress?.guardian?.xp ?: it.remoteXp,
                    remoteFlammeches = progress?.wallet?.flammeches ?: it.remoteFlammeches,
                    remoteCrystals = progress?.wallet?.crystals ?: it.remoteCrystals,
                )
            }
        }
    }

private data class HomeSnapshot(
    val dayStartMillis: Long,
    val tasks: List<TaskForDay>,
    val quests: List<QuestForDay>,
    val pointsBalance: Int,
)

package com.example.taskoday.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.data.repository.RemotePlanningIdCodec
import com.example.taskoday.domain.model.PointsSourceType
import com.example.taskoday.domain.model.QuestForDay
import com.example.taskoday.domain.model.TaskForDay
import com.example.taskoday.domain.repository.PlanningSyncRepository
import com.example.taskoday.domain.repository.PointsRepository
import com.example.taskoday.domain.repository.QuestRepository
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
            cleanupOldChecks()
            observeDayRollover()
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
                    _uiState.update {
                        it.copy(
                            usingRemoteData = syncResult.usedRemoteData,
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
            viewModelScope.launch {
                val remoteRef = RemotePlanningIdCodec.decodeTaskId(item.task.id)
                if (remoteRef != null) {
                    val remoteResult = planningSyncRepository.setCompletion(dayStart, remoteRef, checked)
                    if (remoteResult.isFailure) {
                        _uiState.update { state ->
                            state.copy(errorMessage = remoteResult.exceptionOrNull()?.message ?: "Erreur reseau.")
                        }
                        return@launch
                    }
                }

                taskRepository.setTaskCheckedForDay(
                    taskId = item.task.id,
                    dayStartMillis = dayStart,
                    checked = checked,
                )
                applyTaskPoints(item = item, dayStart = dayStart, checked = checked)
            }
        }

        fun setQuestChecked(item: QuestForDay, checked: Boolean) {
            val dayStart = selectedDay.value
            viewModelScope.launch {
                val remoteRef = RemotePlanningIdCodec.decodeQuestId(item.quest.id)
                if (remoteRef != null) {
                    val remoteResult = planningSyncRepository.setCompletion(dayStart, remoteRef, checked)
                    if (remoteResult.isFailure) {
                        _uiState.update { state ->
                            state.copy(errorMessage = remoteResult.exceptionOrNull()?.message ?: "Erreur reseau.")
                        }
                        return@launch
                    }
                }

                questRepository.setQuestCompletedForDay(item.quest.id, dayStart, checked)
                applyQuestPoints(item = item, dayStart = dayStart, checked = checked)
            }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
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
                    reason = "Quete: ${item.quest.title}",
                )
            } else {
                pointsRepository.revokeForQuest(item.quest.id, dayStart)
            }
        }
    }

private data class HomeSnapshot(
    val dayStartMillis: Long,
    val tasks: List<TaskForDay>,
    val quests: List<QuestForDay>,
    val pointsBalance: Int,
)

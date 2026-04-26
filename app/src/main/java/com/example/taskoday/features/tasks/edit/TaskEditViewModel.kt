package com.example.taskoday.features.tasks.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.data.repository.RemotePlanningIdCodec
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.PlanningItemType
import com.example.taskoday.domain.model.Task
import com.example.taskoday.domain.model.TaskPriority
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.model.TaskType
import com.example.taskoday.domain.repository.MissionsRepository
import com.example.taskoday.domain.repository.ProjectRepository
import com.example.taskoday.domain.repository.TaskRepository
import com.example.taskoday.navigation.TaskodayDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class TaskEditViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val taskRepository: TaskRepository,
        private val projectRepository: ProjectRepository,
        private val missionsRepository: MissionsRepository,
    ) : ViewModel() {
        private val rawTaskIdArg: Long = savedStateHandle[TaskodayDestination.TaskEdit.ARG_TASK_ID] ?: -1L
        private val editingTaskId: Long? =
            rawTaskIdArg.takeIf { it != -1L }

        private var loadedTask: Task? = null

        private val _uiState =
            MutableStateFlow(
                TaskEditUiState(
                    scheduledDate = DateTimeUtils.startOfDayMillis(),
                ),
            )
        val uiState: StateFlow<TaskEditUiState> = _uiState.asStateFlow()

        init {
            observeProjects()
            loadTaskIfNeeded()
        }

        private fun observeProjects() {
            viewModelScope.launch {
                projectRepository.observeProjects().collect { projects ->
                    _uiState.update { current ->
                        current.copy(
                            projects = projects,
                            projectId = current.projectId ?: projects.firstOrNull()?.id,
                        )
                    }
                }
            }
        }

        private fun loadTaskIfNeeded() {
            viewModelScope.launch {
                if (editingTaskId == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                val task = taskRepository.observeTask(editingTaskId).first()
                loadedTask = task
                if (task == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Tache introuvable.",
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        taskId = task.id,
                        title = task.title,
                        emoji = task.emoji,
                        description = task.description.orEmpty(),
                        dueDate = task.dueDate,
                        priority = task.priority,
                        status = task.status,
                        taskType = task.taskType,
                        dayPart = task.dayPart,
                        scheduledDate = task.scheduledDate ?: DateTimeUtils.startOfDayMillis(),
                        routineDays = task.routineDays,
                        projectId = task.projectId,
                        isRoutine = task.isRoutine,
                        isLoading = false,
                    )
                }
            }
        }

        fun onTitleChanged(value: String) {
            _uiState.update { it.copy(title = value, errorMessage = null) }
        }

        fun onDescriptionChanged(value: String) {
            _uiState.update { it.copy(description = value) }
        }

        fun onEmojiChanged(value: String) {
            _uiState.update { it.copy(emoji = value) }
        }

        fun onPriorityChanged(priority: TaskPriority) {
            _uiState.update { it.copy(priority = priority) }
        }

        fun onStatusChanged(status: TaskStatus) {
            _uiState.update { it.copy(status = status) }
        }

        fun onTaskTypeChanged(taskType: TaskType) {
            _uiState.update {
                it.copy(
                    taskType = taskType,
                    isRoutine = taskType == TaskType.DAILY,
                    scheduledDate = if (taskType == TaskType.DAILY) DateTimeUtils.startOfDayMillis() else it.scheduledDate,
                )
            }
        }

        fun onDayPartChanged(dayPart: DayPart) {
            _uiState.update { it.copy(dayPart = dayPart) }
        }

        fun onScheduledDateChanged(dayStartMillis: Long) {
            _uiState.update { it.copy(scheduledDate = dayStartMillis) }
        }

        fun onProjectChanged(projectId: Long?) {
            _uiState.update { it.copy(projectId = projectId) }
        }

        fun onRoutineChanged(isRoutine: Boolean) {
            _uiState.update {
                it.copy(
                    isRoutine = isRoutine,
                    taskType = if (isRoutine) TaskType.DAILY else TaskType.ONE_TIME,
                    routineDays = if (isRoutine) it.routineDays else emptySet(),
                )
            }
        }

        fun onEveryDayRoutineSelected() {
            _uiState.update { it.copy(routineDays = emptySet()) }
        }

        fun onRoutineDayToggled(dayOfWeekIso: Int) {
            if (dayOfWeekIso !in 1..7) return
            _uiState.update { current ->
                val updated =
                    if (current.routineDays.contains(dayOfWeekIso)) {
                        current.routineDays - dayOfWeekIso
                    } else {
                        current.routineDays + dayOfWeekIso
                    }
                current.copy(routineDays = updated)
            }
        }

        fun onDueDateChanged(epochMillis: Long?) {
            _uiState.update { it.copy(dueDate = epochMillis) }
        }

        fun saveTask() {
            val state = uiState.value
            val trimmedTitle = state.title.trim()
            if (trimmedTitle.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Le titre est obligatoire.") }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, errorMessage = null) }

                val now = System.currentTimeMillis()
                val existing = loadedTask
                val normalizedTaskType = if (state.isRoutine) TaskType.DAILY else state.taskType
                val taskToSave =
                    Task(
                        id = existing?.id ?: 0L,
                        title = trimmedTitle,
                        emoji = state.emoji,
                        description = state.description.trim().ifBlank { null },
                        dueDate = state.dueDate,
                        priority = state.priority,
                        status = state.status,
                        taskType = normalizedTaskType,
                        dayPart = state.dayPart,
                        scheduledDate = if (normalizedTaskType == TaskType.DAILY) null else state.scheduledDate,
                        routineDays = if (normalizedTaskType == TaskType.DAILY) state.routineDays else emptySet(),
                        projectId = state.projectId,
                        isRoutine = normalizedTaskType == TaskType.DAILY,
                        createdAt = existing?.createdAt ?: now,
                        updatedAt = now,
                    )

                val saveResult =
                    when {
                        existing == null && normalizedTaskType == TaskType.ONE_TIME -> {
                            missionsRepository.createMissionFromTask(taskToSave)
                        }

                        existing != null &&
                            normalizedTaskType == TaskType.ONE_TIME &&
                            RemotePlanningIdCodec.decodeTaskId(existing.id)?.itemType == PlanningItemType.MISSION -> {
                            missionsRepository.updateMissionFromTask(existing.id, taskToSave)
                        }

                        else -> Result.success(taskToSave)
                    }

                if (saveResult.isFailure) {
                    val error = saveResult.exceptionOrNull()
                    if (error is HttpException && error.code() == 403) {
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = "Action non autorisee.",
                            )
                        }
                        return@launch
                    }
                }

                val taskToPersist = saveResult.getOrDefault(taskToSave)
                val savedId = taskRepository.upsertTask(taskToPersist)
                val resolvedSavedTaskId = if (taskToPersist.id != 0L) taskToPersist.id else savedId
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveCompleted = true,
                        savedTaskId = resolvedSavedTaskId,
                        errorMessage =
                            if (saveResult.isFailure) {
                                "Serveur indisponible, mission enregistree en local."
                            } else {
                                null
                            },
                    )
                }
            }
        }

        fun consumeSaveCompletion() {
            _uiState.update { it.copy(saveCompleted = false) }
        }
    }

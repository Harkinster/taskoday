package com.example.taskoday.features.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.data.repository.RemotePlanningIdCodec
import com.example.taskoday.domain.model.PlanningItemType
import com.example.taskoday.domain.model.Task
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.MissionsRepository
import com.example.taskoday.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class TasksViewModel
    @Inject
    constructor(
        private val taskRepository: TaskRepository,
        private val missionsRepository: MissionsRepository,
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(TasksUiState())
        val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()
        private var canManageAllTasks = authRepository.getAccessToken().isNullOrBlank()

        init {
            observeTasks()
            syncRemoteMissions()
            resolveCanManageMissions()
        }

        private fun observeTasks() {
            viewModelScope.launch {
                taskRepository.observeMissionTasks().collect { tasks ->
                    _uiState.update {
                        it.copy(
                            tasks = tasks,
                            manageableTaskIds = tasks.manageableTaskIds(),
                            isLoading = false,
                        )
                    }
                }
            }
        }

        private fun resolveCanManageMissions() {
            if (authRepository.getAccessToken().isNullOrBlank()) {
                canManageAllTasks = true
                _uiState.update {
                    it.copy(
                        canManageMissions = true,
                        manageableTaskIds = it.tasks.manageableTaskIds(),
                    )
                }
                return
            }

            viewModelScope.launch {
                val canManage =
                    runCatching { authRepository.fetchMe().role.equals("PARENT", ignoreCase = true) }
                        .getOrDefault(false)
                canManageAllTasks = canManage
                _uiState.update {
                    it.copy(
                        canManageMissions = canManage,
                        manageableTaskIds = it.tasks.manageableTaskIds(),
                    )
                }
            }
        }

        private fun syncRemoteMissions() {
            viewModelScope.launch {
                val syncResult = missionsRepository.syncMissions()
                if (syncResult.errorMessage != null) {
                    _uiState.update { it.copy(errorMessage = syncResult.errorMessage) }
                }
            }
        }

        fun markTaskAsDone(taskId: Long) {
            viewModelScope.launch {
                val remoteRef = RemotePlanningIdCodec.decodeTaskId(taskId)
                if (remoteRef?.itemType == PlanningItemType.MISSION) {
                    val remoteResult = missionsRepository.completeMission(taskId)
                    if (remoteResult.isFailure) {
                        val error = remoteResult.exceptionOrNull()
                        _uiState.update {
                            it.copy(errorMessage = error?.toMessage() ?: "Erreur backend.")
                        }
                        if (error.isForbidden()) return@launch
                    }
                }
                taskRepository.updateTaskStatus(taskId = taskId, status = TaskStatus.DONE)
            }
        }

        fun deleteTask(taskId: Long) {
            viewModelScope.launch {
                val remoteRef = RemotePlanningIdCodec.decodeTaskId(taskId)
                if (remoteRef?.itemType == PlanningItemType.MISSION) {
                    val remoteResult = missionsRepository.deleteMission(taskId)
                    if (remoteResult.isFailure) {
                        val error = remoteResult.exceptionOrNull()
                        _uiState.update {
                            it.copy(errorMessage = error?.toMessage() ?: "Erreur backend.")
                        }
                        if (error.isForbidden()) return@launch
                    }
                }
                taskRepository.deleteTask(taskId)
            }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        private fun List<Task>.manageableTaskIds(): Set<Long> =
            filter { task -> canManageTask(task) }
                .map { task -> task.id }
                .toSet()

        private fun canManageTask(task: Task): Boolean {
            if (canManageAllTasks) return true
            val remoteRef = RemotePlanningIdCodec.decodeTaskId(task.id)
            return task.isDaily && remoteRef == null
        }
    }

private fun Throwable.toMessage(): String =
    when (this) {
        is UnknownHostException, is ConnectException -> "Serveur indisponible."
        is SocketTimeoutException -> "Requête expirée."
        is HttpException ->
            when (code()) {
                403 -> "Action non autorisee."
                else -> "Erreur API (${code()})."
            }

        is IOException -> "Erreur reseau."
        else -> message ?: "Erreur inconnue."
    }

private fun Throwable?.isForbidden(): Boolean = this is HttpException && this.code() == 403

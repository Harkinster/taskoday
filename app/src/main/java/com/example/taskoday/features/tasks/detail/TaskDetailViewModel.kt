package com.example.taskoday.features.tasks.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.data.repository.RemotePlanningIdCodec
import com.example.taskoday.domain.model.PlanningItemType
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.MissionsRepository
import com.example.taskoday.domain.repository.TaskRepository
import com.example.taskoday.navigation.TaskodayDestination
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
class TaskDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val taskRepository: TaskRepository,
        private val missionsRepository: MissionsRepository,
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val taskId: Long = savedStateHandle[TaskodayDestination.TaskDetail.ARG_TASK_ID] ?: -1L

        private val _uiState = MutableStateFlow(TaskDetailUiState())
        val uiState: StateFlow<TaskDetailUiState> = _uiState.asStateFlow()

        init {
            if (taskId == 0L) {
                _uiState.value = TaskDetailUiState(isLoading = false, errorMessage = "Identifiant de mission invalide.")
            } else {
                observeTask(taskId)
                resolveCanManageMissions()
            }
        }

        private fun observeTask(id: Long) {
            viewModelScope.launch {
                taskRepository.observeTask(id).collect { task ->
                    _uiState.update {
                        it.copy(
                            task = task,
                            isLoading = false,
                            errorMessage = if (task == null) "Mission introuvable." else null,
                        )
                    }
                }
            }
        }

        private fun resolveCanManageMissions() {
            if (authRepository.getAccessToken().isNullOrBlank()) {
                _uiState.update { it.copy(canManageMission = true) }
                return
            }

            viewModelScope.launch {
                val canManage =
                    runCatching { authRepository.fetchMe().role.equals("PARENT", ignoreCase = true) }
                        .getOrDefault(false)
                _uiState.update { it.copy(canManageMission = canManage) }
            }
        }

        fun updateStatus(status: TaskStatus) {
            val id = uiState.value.task?.id ?: return
            viewModelScope.launch {
                val remoteRef = RemotePlanningIdCodec.decodeTaskId(id)
                if (status == TaskStatus.DONE && remoteRef?.itemType == PlanningItemType.MISSION) {
                    val remoteResult = missionsRepository.completeMission(id)
                    if (remoteResult.isFailure) {
                        val error = remoteResult.exceptionOrNull()
                        _uiState.update { it.copy(errorMessage = error?.toMessage() ?: "Erreur backend.") }
                        if (error is HttpException && error.code() == 403) {
                            return@launch
                        }
                    }
                }
                taskRepository.updateTaskStatus(taskId = id, status = status)
            }
        }

        fun markTaskAsDone() {
            updateStatus(TaskStatus.DONE)
        }

        fun deleteTask() {
            val id = uiState.value.task?.id ?: return
            viewModelScope.launch {
                val remoteRef = RemotePlanningIdCodec.decodeTaskId(id)
                if (remoteRef?.itemType == PlanningItemType.MISSION) {
                    val remoteResult = missionsRepository.deleteMission(id)
                    if (remoteResult.isFailure) {
                        val error = remoteResult.exceptionOrNull()
                        _uiState.update { it.copy(errorMessage = error?.toMessage() ?: "Erreur backend.") }
                        if (error is HttpException && error.code() == 403) {
                            return@launch
                        }
                    }
                }
                taskRepository.deleteTask(id)
                _uiState.update { it.copy(isDeleted = true) }
            }
        }
    }

private fun Throwable.toMessage(): String =
    when (this) {
        is UnknownHostException, is ConnectException -> "Serveur indisponible."
        is SocketTimeoutException -> "Requete expiree."
        is HttpException ->
            when (code()) {
                403 -> "Action non autorisee."
                else -> "Erreur API (${code()})."
            }

        is IOException -> "Erreur reseau."
        else -> message ?: "Erreur inconnue."
    }

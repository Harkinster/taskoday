package com.example.taskoday.features.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.repository.ParentPlanningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class ParentPlanningViewModel
    @Inject
    constructor(
        private val parentPlanningRepository: ParentPlanningRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ParentPlanningUiState())
        val uiState: StateFlow<ParentPlanningUiState> = _uiState.asStateFlow()

        init {
            loadContext()
        }

        fun loadContext() {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            viewModelScope.launch {
                runCatching {
                    val isParent = parentPlanningRepository.isCurrentUserParent()
                    if (!isParent) {
                        ParentPlanningUiState(
                            isLoading = false,
                            hasParentAccess = false,
                            errorMessage = "Acces reserve au compte parent.",
                        )
                    } else {
                        val children = parentPlanningRepository.fetchChildren()
                        val selected = parentPlanningRepository.getSelectedChildId() ?: children.firstOrNull()?.id
                        selected?.let { parentPlanningRepository.setSelectedChildId(it) }
                        ParentPlanningUiState(
                            isLoading = false,
                            hasParentAccess = true,
                            children = children,
                            selectedChildId = selected,
                        )
                    }
                }.onSuccess { state ->
                    _uiState.value = state
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasParentAccess = false,
                            errorMessage = throwable.toMessage(),
                        )
                    }
                }
            }
        }

        fun selectChild(childId: Long) {
            parentPlanningRepository.setSelectedChildId(childId)
            _uiState.update { it.copy(selectedChildId = childId, successMessage = null, errorMessage = null) }
        }

        fun createRoutine(
            title: String,
            description: String?,
            dayPart: DayPart,
            selectedWeekdays: Set<Int>,
            points: Int,
        ) {
            val childId = _uiState.value.selectedChildId ?: return
            if (title.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Le titre de la routine est requis.") }
                return
            }
            submitAction {
                parentPlanningRepository.createRoutine(
                    childId = childId,
                    title = title,
                    description = description,
                    dayPart = dayPart,
                    selectedWeekdays = selectedWeekdays,
                    points = points,
                )
                "Routine ajoutee."
            }
        }

        fun createMission(
            title: String,
            description: String?,
            dayPart: DayPart,
            scheduledDate: LocalDate,
            points: Int,
        ) {
            val childId = _uiState.value.selectedChildId ?: return
            if (title.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Le titre de la mission est requis.") }
                return
            }
            submitAction {
                parentPlanningRepository.createMission(
                    childId = childId,
                    title = title,
                    description = description,
                    dayPart = dayPart,
                    scheduledDate = scheduledDate,
                    points = points,
                )
                "Mission ajoutee."
            }
        }

        fun createQuest(
            title: String,
            description: String?,
            dayPart: DayPart,
            points: Int,
        ) {
            val childId = _uiState.value.selectedChildId ?: return
            if (title.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Le titre de la quete est requis.") }
                return
            }
            submitAction {
                parentPlanningRepository.createQuest(
                    childId = childId,
                    title = title,
                    description = description,
                    dayPart = dayPart,
                    points = points,
                )
                "Quete ajoutee."
            }
        }

        fun clearMessages() {
            _uiState.update { it.copy(successMessage = null, errorMessage = null) }
        }

        private fun submitAction(action: suspend () -> String) {
            _uiState.update { it.copy(isSubmitting = true, successMessage = null, errorMessage = null) }
            viewModelScope.launch {
                runCatching { action() }
                    .onSuccess { success ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                successMessage = success,
                            )
                        }
                    }.onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage = throwable.toMessage(),
                            )
                        }
                    }
            }
        }
    }

private fun Throwable.toMessage(): String =
    when (this) {
        is UnknownHostException, is ConnectException -> "Serveur indisponible."
        is SocketTimeoutException -> "Requete expiree."
        is HttpException -> "Erreur backend (${code()})."
        is IOException -> "Erreur reseau."
        else -> message ?: "Erreur inconnue."
    }

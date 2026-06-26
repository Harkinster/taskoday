package com.example.taskoday.features.parent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.core.plan.TaskodayPlanFeature
import com.example.taskoday.core.plan.TaskodayPlanPolicy
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.ParentPlanUsage
import com.example.taskoday.domain.model.PlanningFormType
import com.example.taskoday.domain.repository.ParentPlanningRepository
import com.example.taskoday.domain.repository.PlanningSyncRepository
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
        private val planningSyncRepository: PlanningSyncRepository,
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
                        val storedChildId = parentPlanningRepository.getSelectedChildId()
                        val selected =
                            storedChildId?.takeIf { id -> children.any { child -> child.id == id } }
                                ?: children.singleOrNull()?.id
                        selected?.let { parentPlanningRepository.setSelectedChildId(it) }
                        val planUsage =
                            selected
                                ?.let { childId ->
                                    runCatching { parentPlanningRepository.fetchPlanUsage(childId) }
                                        .getOrDefault(ParentPlanUsage())
                                } ?: ParentPlanUsage()
                        ParentPlanningUiState(
                            isLoading = false,
                            hasParentAccess = true,
                            children = children,
                            selectedChildId = selected,
                            planUsage = planUsage,
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
            viewModelScope.launch {
                runCatching { parentPlanningRepository.fetchPlanUsage(childId) }
                    .onSuccess { usage ->
                        _uiState.update { it.copy(planUsage = usage) }
                    }.onFailure { throwable ->
                        _uiState.update { it.copy(errorMessage = throwable.toMessage()) }
                    }
            }
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
            if (!TaskodayPlanPolicy.canCreate(TaskodayPlanFeature.Routine, _uiState.value.planUsage.activeRoutines)) {
                _uiState.update { it.copy(errorMessage = TaskodayPlanPolicy.limitReachedMessage()) }
                return
            }
            submitAction(PlanningFormType.ROUTINE) {
                parentPlanningRepository.createRoutine(
                    childId = childId,
                    title = title,
                    description = description,
                    dayPart = dayPart,
                    selectedWeekdays = selectedWeekdays,
                    points = points,
                )
                "Routine ajoutée."
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
            if (!TaskodayPlanPolicy.canCreate(TaskodayPlanFeature.Mission, _uiState.value.planUsage.activeMissions)) {
                _uiState.update { it.copy(errorMessage = TaskodayPlanPolicy.limitReachedMessage()) }
                return
            }
            submitAction(PlanningFormType.MISSION) {
                parentPlanningRepository.createMission(
                    childId = childId,
                    title = title,
                    description = description,
                    dayPart = dayPart,
                    scheduledDate = scheduledDate,
                    points = points,
                )
                "Mission ajoutée."
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
                _uiState.update { it.copy(errorMessage = "Le titre de la quête est requis.") }
                return
            }
            if (!TaskodayPlanPolicy.canCreate(TaskodayPlanFeature.Quest, _uiState.value.planUsage.activeQuests)) {
                _uiState.update { it.copy(errorMessage = TaskodayPlanPolicy.limitReachedMessage()) }
                return
            }
            submitAction(PlanningFormType.QUEST) {
                parentPlanningRepository.createQuest(
                    childId = childId,
                    title = title,
                    description = description,
                    dayPart = dayPart,
                    points = points,
                )
                "Quête ajoutée."
            }
        }

        fun clearMessages() {
            _uiState.update { it.copy(successMessage = null, errorMessage = null) }
        }

        fun consumeCreationResult() {
            _uiState.update { it.copy(createdFormType = null) }
        }

        private fun submitAction(
            formType: PlanningFormType,
            action: suspend () -> String,
        ) {
            _uiState.update { it.copy(isSubmitting = true, successMessage = null, errorMessage = null) }
            viewModelScope.launch {
                runCatching { action() }
                    .onSuccess { success ->
                        val syncResult = planningSyncRepository.syncDay(DateTimeUtils.startOfDayMillis())
                        val usage =
                            _uiState.value.selectedChildId
                                ?.let { childId ->
                                    runCatching { parentPlanningRepository.fetchPlanUsage(childId) }.getOrNull()
                                }
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                successMessage = success,
                                errorMessage = syncResult.errorMessage,
                                createdFormType = formType,
                                planUsage = usage ?: it.planUsage,
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
        is SocketTimeoutException -> "Requête expirée."
        is HttpException -> "Erreur backend (${code()})."
        is IOException -> "Erreur réseau."
        else -> message ?: "Erreur inconnue."
    }

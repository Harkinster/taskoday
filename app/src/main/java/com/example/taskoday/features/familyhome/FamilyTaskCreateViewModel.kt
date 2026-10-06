package com.example.taskoday.features.familyhome

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.data.repository.toRemoteUserMessage
import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskRecurrence
import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.repository.FamilyTasksRepository
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.FamilyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FamilyTaskCreateViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val familyTasksRepository: FamilyTasksRepository,
        private val authRepository: AuthRepository,
        private val familyRepository: FamilyRepository,
    ) : ViewModel() {
        private val taskId: Long? = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0L }
        private val reuseTaskId: Long? = savedStateHandle.get<Long>("copyFromTaskId")?.takeIf { it > 0L }
        private var loadedFamilyId: Long? = null
        private val prefilledDate: String? = savedStateHandle.get<String>("date")
        private val quickMode: Boolean = savedStateHandle.get<Boolean>("quick") == true
        private val requestedType: FamilyActionType =
            runCatching { FamilyActionType.valueOf(savedStateHandle.get<String>("kind") ?: "HOUSE_QUEST") }
                .getOrDefault(FamilyActionType.HOUSE_QUEST)
        private val targetMemberId: Long? = savedStateHandle.get<Long>("memberId")?.takeIf { it > 0L }
        private val _uiState =
            MutableStateFlow(
                FamilyTaskCreateUiState(
                    taskId = taskId,
                    actionType = requestedType,
                    category = requestedType.category,
                    isLoadingTask = taskId != null || reuseTaskId != null,
                    date = resolveFamilyTaskInitialDate(prefilledDate),
                    recurrence = if (requestedType.kind == com.example.taskoday.domain.model.FamilyActionKind.ROUTINE) FamilyTaskRecurrence.DAILY else FamilyTaskRecurrence.NONE,
                ),
            )
        val uiState: StateFlow<FamilyTaskCreateUiState> = _uiState.asStateFlow()

        init {
            loadInitialForm()
        }

        fun loadInitialForm() {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isLoadingMembers = true,
                        isLoadingTask = taskId != null || reuseTaskId != null,
                        errorMessage = null,
                    )
                }
                loadedFamilyId = familyRepository.getActiveFamilyId()
                familyTasksRepository
                    .fetchMembers()
                    .onSuccess { members ->
                        val ownUserId = runCatching { authRepository.fetchMe().id }.getOrNull()
                        _uiState.update {
                            it.copy(
                                isLoadingMembers = false,
                                members = members,
                                selectedAssigneeUserIds =
                                    if (it.actionType.scope == com.example.taskoday.domain.model.FamilyActionScope.PERSONAL && !it.isEditing) {
                                        setOfNotNull(targetMemberId ?: ownUserId).filter { id -> members.any { member -> member.userId == id } }.toSet()
                                    } else it.selectedAssigneeUserIds,
                                errorMessage = null,
                            )
                        }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoadingMembers = false,
                                members = emptyList(),
                                errorMessage = throwable.toRemoteUserMessage("Impossible de charger les membres."),
                            )
                        }
                    }

                taskId?.let { id ->
                    familyTasksRepository
                        .fetchTask(id)
                        .onSuccess { task ->
                            _uiState.update { it.withTask(task) }
                        }
                        .onFailure { throwable ->
                            _uiState.update {
                                it.copy(
                                    isLoadingTask = false,
                                    errorMessage = throwable.toRemoteUserMessage("Impossible de charger la tâche."),
                                )
                            }
                        }
                }
                reuseTaskId?.let { id ->
                    familyTasksRepository.fetchTask(id)
                        .onSuccess { task ->
                            _uiState.update { state ->
                                runCatching { reuseTaskPrefill(state, task, loadedFamilyId ?: -1L, state.members) }
                                    .getOrElse { state.copy(errorMessage = it.message, isLoadingTask = false) }
                                    .copy(isLoadingTask = false)
                            }
                        }
                        .onFailure { throwable ->
                            _uiState.update { it.copy(isLoadingTask = false, errorMessage = throwable.toRemoteUserMessage("Impossible de charger l'action.")) }
                        }
                }
                if (taskId == null) {
                    familyTasksRepository.fetchTasks().onSuccess { tasks ->
                        _uiState.update { state ->
                            state.copy(recentTasks = recentTasksForContext(tasks, loadedFamilyId ?: -1L, requestedType, targetMemberId ?: state.selectedAssigneeUserIds.singleOrNull()))
                        }
                    }
                }
            }
        }

        fun selectRecentTask(id: Long) {
            val source = _uiState.value.recentTasks.firstOrNull { it.id == id } ?: return
            _uiState.update { state ->
                runCatching { reuseTaskPrefill(state, source, loadedFamilyId ?: -1L, state.members) }
                    .getOrElse { state.copy(errorMessage = it.message) }
            }
        }

        fun selectPersonalAssignee(userId: Long) {
            _uiState.update { state ->
                if (state.actionType.scope == com.example.taskoday.domain.model.FamilyActionScope.HOUSE || state.members.none { it.userId == userId && it.isActive }) state
                else state.copy(selectedAssigneeUserIds = setOf(userId), prefillWarning = null, requiresAssigneeReview = false, errorMessage = null)
            }
        }

        fun onTitleChanged(value: String) {
            _uiState.update { it.copy(title = value, errorMessage = null) }
        }

        fun onDescriptionChanged(value: String) {
            _uiState.update { it.copy(description = value, errorMessage = null) }
        }

        fun onDateChanged(value: String) {
            _uiState.update { it.copy(date = value, errorMessage = null) }
        }

        fun onEndDateChanged(value: String) {
            _uiState.update { it.copy(endDate = value, errorMessage = null) }
        }

        fun onKindChanged(type: FamilyActionType) {
            _uiState.update { state ->
                if (state.isEditing || state.reusedSourceId != null || state.actionType.scope != type.scope) state
                else state.copy(
                    actionType = type, category = type.category, recentTasks = emptyList(),
                    date = if (state.date.isBlank() && type.kind != com.example.taskoday.domain.model.FamilyActionKind.MISSION) java.time.LocalDate.now().toString() else state.date,
                    recurrence = if (type.kind == com.example.taskoday.domain.model.FamilyActionKind.ROUTINE) FamilyTaskRecurrence.DAILY else FamilyTaskRecurrence.NONE,
                    endDate = "", selectedWeekdays = emptySet(), errorMessage = null,
                )
            }
            val state = _uiState.value
            viewModelScope.launch {
                familyTasksRepository.fetchTasks().onSuccess { tasks ->
                    _uiState.update { current -> current.copy(recentTasks = recentTasksForContext(tasks, loadedFamilyId ?: -1L, current.actionType, targetMemberId ?: current.selectedAssigneeUserIds.singleOrNull())) }
                }
            }
        }

        fun onTimeChanged(value: String) {
            _uiState.update { it.copy(time = value, errorMessage = null) }
        }

        fun clearTime() {
            _uiState.update { it.copy(time = "", errorMessage = null) }
        }

        fun onRecurrenceChanged(value: FamilyTaskRecurrence) {
            _uiState.update {
                it.copy(
                    recurrence = value,
                    isCustomRecurrence = false,
                    recurrenceInterval = 1,
                    selectedWeekdays =
                        if (value == FamilyTaskRecurrence.SELECTED_WEEKDAYS) {
                            it.selectedWeekdays
                        } else {
                            emptySet()
                        },
                    errorMessage = null,
                )
            }
        }

        fun customizeRecurrence() {
            _uiState.update { it.copy(isCustomRecurrence = true, recurrence = FamilyTaskRecurrence.DAILY, recurrenceInterval = 1, selectedWeekdays = emptySet(), errorMessage = null) }
        }

        fun onRecurrenceIntervalChanged(value: Int) {
            _uiState.update { it.copy(recurrenceInterval = value.coerceIn(1, 52), errorMessage = null) }
        }

        fun onCustomRecurrenceUnitChanged(value: CustomRecurrenceUnit) {
            _uiState.update {
                it.copy(
                    customRecurrenceUnit = value,
                    recurrence = if (value == CustomRecurrenceUnit.DAYS) FamilyTaskRecurrence.DAILY else FamilyTaskRecurrence.SELECTED_WEEKDAYS,
                    selectedWeekdays = if (value == CustomRecurrenceUnit.DAYS) emptySet() else it.selectedWeekdays,
                    errorMessage = null,
                )
            }
        }

        fun toggleWeekday(day: Int) {
            _uiState.update {
                val next =
                    if (day in it.selectedWeekdays) {
                        it.selectedWeekdays - day
                    } else {
                        it.selectedWeekdays + day
                    }
                it.copy(selectedWeekdays = next, errorMessage = null)
            }
        }

        fun selectHouseTask() {
            _uiState.update { if (it.actionType.scope == com.example.taskoday.domain.model.FamilyActionScope.HOUSE) it.copy(selectedAssigneeUserIds = emptySet(), prefillWarning = null, requiresAssigneeReview = false, errorMessage = null) else it }
        }

        fun toggleAssignee(userId: Long) {
            _uiState.update {
                if (it.actionType.scope != com.example.taskoday.domain.model.FamilyActionScope.HOUSE) return@update it
                val next =
                    if (userId in it.selectedAssigneeUserIds) {
                        it.selectedAssigneeUserIds - userId
                    } else {
                        it.selectedAssigneeUserIds + userId
                    }
                it.copy(selectedAssigneeUserIds = next, prefillWarning = null, requiresAssigneeReview = false, errorMessage = null)
            }
        }

        fun onValidationRequiredChanged(value: Boolean) {
            _uiState.update { it.copy(validationRequired = value, errorMessage = null) }
        }

        fun onGamificationEnabledChanged(value: Boolean) {
            _uiState.update { it.copy(gamificationEnabled = value, errorMessage = null) }
        }

        fun onPriorityChanged(value: FamilyTaskPriority) {
            _uiState.update { it.copy(priority = value, errorMessage = null) }
        }

        fun submit() {
            val current = _uiState.value
            if (current.isSubmitting) return
            if (current.requiresAssigneeReview) {
                _uiState.update { it.copy(errorMessage = "Vérifiez et choisissez l'attribution avant de créer.") }
                return
            }

            val validation =
                validateFamilyTaskCreateForm(
                    FamilyTaskCreateForm(
                        title = current.title,
                        description = current.description,
                        date = current.date,
                        endDate = current.endDate,
                        time = current.time,
                        recurrence = current.recurrence,
                        recurrenceInterval = current.recurrenceInterval,
                        selectedWeekdays = current.selectedWeekdays,
                        assigneeUserIds = current.selectedAssigneeUserIds,
                        validationRequired = current.validationRequired,
                        gamificationEnabled = current.gamificationEnabled,
                        priority = current.priority,
                    ),
                    // An empty assignee list is the explicit Maison target.
                    requireAssignee = false,
                )
            val input = validation.input
            if (!validation.isValid || input == null) {
                _uiState.update { it.copy(errorMessage = validation.errorMessage) }
                return
            }
            val legacyRecurringMission = current.isEditing && current.actionType.kind == com.example.taskoday.domain.model.FamilyActionKind.MISSION &&
                current.originalRecurrence == current.recurrence && current.recurrence != FamilyTaskRecurrence.NONE
            val ruleError = familyActionRuleError(current.actionType, current.recurrence, current.selectedAssigneeUserIds, current.date.isNotBlank())
                .takeUnless { legacyRecurringMission && it == "Une mission est ponctuelle et ne peut pas se répéter." }
            if (ruleError != null) {
                _uiState.update { it.copy(errorMessage = ruleError) }
                return
            }

            viewModelScope.launch {
                _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
                val canManage = runCatching { FamilyTaskAccessPolicy.forUser(authRepository.fetchMe()).canManage }
                    .getOrDefault(false)
                if (!canManage) {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = "Action réservée aux parents.") }
                    return@launch
                }
                if (familyRepository.getActiveFamilyId() != loadedFamilyId || loadedFamilyId == null) {
                    _uiState.update { it.copy(isSubmitting = false, recentTasks = emptyList(), errorMessage = "La famille active a changé. Fermez ce formulaire et recommencez.") }
                    return@launch
                }
                val currentMembers = familyTasksRepository.fetchMembers().getOrNull()
                if (currentMembers == null || !current.selectedAssigneeUserIds.all { id -> currentMembers.any { it.userId == id && it.isActive } }) {
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = "Un participant n'est plus membre de cette famille. Vérifiez l'attribution.") }
                    return@launch
                }
                val result =
                    if (current.isEditing && current.taskId != null) {
                        familyTasksRepository.updateTask(current.taskId, input.copy(category = current.category, scope = current.actionType.scope, kind = current.actionType.kind))
                    } else {
                        familyTasksRepository.createTask(input.copy(category = current.category, scope = current.actionType.scope, kind = current.actionType.kind))
                    }
                result
                    .onSuccess {
                        _uiState.update { it.copy(isSubmitting = false, created = true) }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errorMessage =
                                    throwable.toRemoteUserMessage(
                                        if (current.isEditing) {
                                            "Impossible de modifier la tâche."
                                        } else {
                                            "Impossible de créer la tâche."
                                        },
                                    ),
                            )
                        }
                    }
            }
        }
    }

private fun FamilyTaskCreateUiState.withTask(task: FamilyTaskDefinition): FamilyTaskCreateUiState =
    copy(
        isLoadingTask = false,
        actionType = FamilyActionType.fromWire(task.scope.name, task.kind.name, task.category),
        category = task.category,
        title = task.title,
        description = task.description.orEmpty(),
        date = familyTaskDateFromFields(dueDate = task.dueDate, dueAt = task.dueAt).orEmpty(),
        endDate = task.endDate.orEmpty(),
        time =
            familyTaskTimeFromFields(
                hasDueTime = task.hasDueTime,
                dueTime = task.dueTime,
                dueAt = task.dueAt,
            ),
        recurrence = task.recurrence,
        originalRecurrence = task.recurrence,
        recurrenceInterval = task.recurrenceInterval.coerceIn(1, 52),
        isCustomRecurrence = task.recurrenceInterval > 1,
        customRecurrenceUnit = if (task.recurrence == FamilyTaskRecurrence.DAILY) CustomRecurrenceUnit.DAYS else CustomRecurrenceUnit.WEEKS,
        selectedWeekdays = task.selectedWeekdays.toSet(),
        selectedAssigneeUserIds = task.assignees.mapNotNull { assignee -> assignee.id }.toSet(),
        validationRequired = task.validationRequired,
        gamificationEnabled = task.gamificationEnabled,
        priority = task.priority,
        errorMessage = null,
    )

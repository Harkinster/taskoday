package com.example.taskoday.features.exploration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.data.repository.RemotePlanningIdCodec
import com.example.taskoday.data.repository.toRemoteUserMessage
import com.example.taskoday.features.familyhome.FamilyTaskAccessPolicy
import com.example.taskoday.features.familyhome.FamilyTaskQuickAction
import com.example.taskoday.domain.model.TaskForDay
import com.example.taskoday.domain.model.TaskStatus
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.ChildrenRepository
import com.example.taskoday.domain.repository.FamilyTasksRepository
import com.example.taskoday.domain.repository.MissionsRepository
import com.example.taskoday.domain.repository.PlanningSyncRepository
import com.example.taskoday.domain.repository.QuestRepository
import com.example.taskoday.domain.repository.QuestsRepository
import com.example.taskoday.domain.repository.RoutinesRepository
import com.example.taskoday.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException

@HiltViewModel
class ExplorationViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val childrenRepository: ChildrenRepository,
        private val familyTasksRepository: FamilyTasksRepository,
        private val taskRepository: TaskRepository,
        private val routinesRepository: RoutinesRepository,
        private val missionsRepository: MissionsRepository,
        private val questRepository: QuestRepository,
        private val questsRepository: QuestsRepository,
        private val planningSyncRepository: PlanningSyncRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ExplorationUiState())
        val uiState: StateFlow<ExplorationUiState> = _uiState.asStateFlow()

        private var childProfileIds: Map<Long, Long> = emptyMap()
        private var loadJob: Job? = null

        init {
            refresh()
        }

        fun refresh() {
            loadJob?.cancel()
            _uiState.value = ExplorationUiState()
            loadJob = viewModelScope.launch {
                runCatching {
                    val me = authRepository.fetchMe()
                    val access = FamilyTaskAccessPolicy.forUser(me)
                    val familyMembers = familyTasksRepository.fetchMembers().getOrThrow()
                    val children = runCatching { childrenRepository.fetchChildren() }.getOrDefault(emptyList())
                    val members =
                        familyMembers.filter { it.userId == me.id }.map { member ->
                            val childLabel =
                                children.firstOrNull { child ->
                                    !member.email.isNullOrBlank() && child.email.equals(member.email, ignoreCase = true)
                                }?.displayName
                            ExplorationMember(member.userId, childLabel ?: member.displayName)
                        }
                    childProfileIds =
                        members.mapNotNull { member ->
                            val email = familyMembers.firstOrNull { it.userId == member.id }?.email
                            children.firstOrNull { child -> !email.isNullOrBlank() && child.email.equals(email, ignoreCase = true) }
                                ?.let { child -> member.id to child.id }
                        }.toMap()
                    if (!access.canManage) {
                        val ownChildId = authRepository.getActiveChildId(forceRefresh = true)
                        childProfileIds = ownChildId?.let { mapOf(me.id to it) }.orEmpty()
                    }
                    val selected = me.id to (members.firstOrNull()?.displayName ?: "Moi")
                    _uiState.update {
                        it.copy(
                            access = access,
                            members = members,
                            selectedMemberId = selected.first,
                            selectedMemberName = selected.second,
                        )
                    }
                    loadSelectedMember(selected.first, selected.second)
                }.onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        ExplorationUiState(isLoading = false, errorMessage = error.toRemoteUserMessage("Exploration indisponible."))
                    }
                }
            }
        }

        fun toggleFamilyTask(item: ExplorationTask) {
            val occurrence = item.occurrence
            val action = _uiState.value.access.quickAction(occurrence) ?: return
            if (occurrence.occurrenceId <= 0L) return
            val key = "family-${occurrence.occurrenceId}"
            if (_uiState.value.actingKey != null) return
            _uiState.update { it.copy(actingKey = key, errorMessage = null) }
            viewModelScope.launch {
                val result = when (action) {
                    FamilyTaskQuickAction.COMPLETE -> familyTasksRepository.completeOccurrence(occurrence.occurrenceId)
                    FamilyTaskQuickAction.VALIDATE -> familyTasksRepository.validateOccurrence(occurrence.occurrenceId)
                    FamilyTaskQuickAction.REOPEN -> familyTasksRepository.reopenOccurrence(occurrence.occurrenceId)
                    FamilyTaskQuickAction.START,
                    FamilyTaskQuickAction.JOIN,
                    FamilyTaskQuickAction.RESCHEDULE,
                    FamilyTaskQuickAction.FAIL,
                    -> Result.failure(IllegalStateException("Cette action n'est pas disponible dans Exploration."))
                }
                result.onFailure { error -> _uiState.update { it.copy(errorMessage = error.message ?: "Action impossible.") } }
                _uiState.update { it.copy(actingKey = null) }
                if (result.isSuccess) refresh()
            }
        }

        fun toggleRoutine(item: TaskForDay) = toggleLegacyTask(item)

        fun toggleRoutineItem(item: ExplorationRoutineItem) {
            item.familyTask?.let { toggleFamilyTask(it); return }
            item.legacyTask?.let { toggleLegacyTask(it) }
        }

        private fun toggleLegacyTask(item: TaskForDay) {
            if (RemotePlanningIdCodec.decodeTaskId(item.task.id) == null) return
            if (item.isCompleted && !_uiState.value.access.canManage) return
            val key = "task-${item.task.id}"
            if (_uiState.value.actingKey != null) return
            _uiState.update { it.copy(actingKey = key, errorMessage = null) }
            viewModelScope.launch {
                val checked = !item.isCompleted
                val remoteRef = RemotePlanningIdCodec.decodeTaskId(item.task.id)
                val remoteResult =
                    remoteRef?.let { planningSyncRepository.setCompletion(DateTimeUtils.startOfDayMillis(), it, checked) }
                if (remoteResult != null && remoteResult.isFailure) {
                    _uiState.update { it.copy(actingKey = null, errorMessage = remoteResult.exceptionOrNull()?.message ?: "Action impossible.") }
                    return@launch
                }
                taskRepository.setTaskCheckedForDay(item.task.id, DateTimeUtils.startOfDayMillis(), checked)
                taskRepository.updateTaskStatus(item.task.id, if (checked) TaskStatus.DONE else TaskStatus.TODO)
                _uiState.update { it.copy(actingKey = null) }
                refresh()
            }
        }

        private suspend fun loadSelectedMember(memberId: Long?, memberName: String) {
            val today = LocalDate.now()
            val todayResult = familyTasksRepository.fetchToday()
            val overdueResult = familyTasksRepository.fetchOverdueOccurrences()
            val allToday = todayResult.getOrThrow().tasks
            val overdue = overdueResult.getOrNull()?.occurrences.orEmpty()
            val personal = allToday.filter { task -> memberId != null && task.assignees.any { it.id == memberId } && task.explorationCategory() == ExplorationCategory.PERSONAL_TASK }
            val overduePersonal = overdue.filter { task -> memberId != null && task.assignees.any { it.id == memberId } && task.explorationCategory() == ExplorationCategory.PERSONAL_TASK }
            val recurringPersonal = allToday.filter { task -> memberId != null && task.assignees.any { it.id == memberId } && task.explorationCategory() == ExplorationCategory.PERSONAL_ROUTINE }
            val overdueRecurringPersonal = overdue.filter { task -> memberId != null && task.assignees.any { it.id == memberId } && task.explorationCategory() == ExplorationCategory.PERSONAL_ROUTINE }
            val childId = memberId?.let { childProfileIds[it] }
            var routinesSynced = false
            var missionsSynced = false
            if (childId != null) {
                authRepository.setActiveChildId(childId)
                routinesSynced = routinesRepository.syncRoutinesForDay(DateTimeUtils.startOfDayMillis()).usedRemoteData
                missionsSynced = missionsRepository.syncMissions().usedRemoteData
            }
            // Never fall back to seeds or a previous member's cache when sync is unavailable.
            val tasks = if (childId != null) remoteExplorationTasks(
                taskRepository.observeTasksForDay(DateTimeUtils.startOfDayMillis()).first()
            ).filter { if (it.isRoutineItem()) routinesSynced else missionsSynced } else emptyList()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    selectedMemberName = memberName,
                    personalTasks = personal.map { task -> ExplorationTask(task, overdue.any { old -> old.occurrenceId == task.occurrenceId }) } +
                        overduePersonal.filter { old -> allToday.none { task -> task.occurrenceId == old.occurrenceId } }.map { task -> ExplorationTask(task, true) },
                    routines = tasks.filter(TaskForDay::isRoutineItem).map { task ->
                        ExplorationRoutineItem(
                            title = task.task.title,
                            subtitle = task.task.dayPart.name.lowercase().replace('_', ' '),
                            completed = task.isCompleted,
                            legacyTask = task,
                        )
                    } + recurringPersonal.map { task ->
                        ExplorationRoutineItem(
                            title = task.title,
                            subtitle = familyTaskOccurrenceRecurrenceLabel(task.recurrenceLabel).orEmpty(),
                            completed = task.status.countsAsDone,
                            familyTask = ExplorationTask(task, overdue.any { old -> old.occurrenceId == task.occurrenceId }),
                        )
                    } + overdueRecurringPersonal.filter { old -> recurringPersonal.none { task -> task.occurrenceId == old.occurrenceId } }.map { task ->
                        ExplorationRoutineItem(
                            title = task.title,
                            subtitle = familyTaskOccurrenceRecurrenceLabel(task.recurrenceLabel).orEmpty(),
                            completed = task.status.countsAsDone,
                            familyTask = ExplorationTask(task, true),
                        )
                    },
                    missions = tasks.filterNot(TaskForDay::isRoutineItem),
                    houseTasks = emptyList(),
                    objectives = emptyList(),
                    errorMessage = null,
                )
            }
        }

    }

private fun TaskForDay.isRoutineItem(): Boolean = task.isRoutine || task.isDaily

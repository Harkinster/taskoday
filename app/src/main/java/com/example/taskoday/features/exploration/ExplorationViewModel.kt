package com.example.taskoday.features.exploration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.data.repository.RemotePlanningIdCodec
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

        init {
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                runCatching {
                    val me = authRepository.fetchMe()
                    val familyMembers = familyTasksRepository.fetchMembers().getOrThrow()
                    val children = runCatching { childrenRepository.fetchChildren() }.getOrDefault(emptyList())
                    val members =
                        familyMembers.map { member ->
                            val childLabel =
                                children.firstOrNull { child ->
                                    child.id == member.userId || child.email.equals(member.email, ignoreCase = true)
                                }?.displayName
                            ExplorationMember(member.userId, childLabel ?: member.displayName)
                        }
                    childProfileIds =
                        members.mapNotNull { member ->
                            children.firstOrNull { child -> child.displayName.equals(member.displayName, ignoreCase = true) }
                                ?.let { child -> member.id to child.id }
                        }.toMap()
                    val selected = resolveSelection(me.id, members, _uiState.value.selectedMemberId)
                    _uiState.update {
                        it.copy(
                            members = members,
                            selectedMemberId = selected.first,
                            selectedMemberName = selected.second,
                        )
                    }
                    loadSelectedMember(selected.first, selected.second)
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Exploration indisponible.")
                    }
                }
            }
        }

        fun selectMember(memberId: Long) {
            val member = _uiState.value.members.firstOrNull { it.id == memberId } ?: return
            _uiState.update { it.copy(selectedMemberId = member.id, selectedMemberName = member.displayName) }
            viewModelScope.launch { loadSelectedMember(member.id, member.displayName) }
        }

        fun toggleFamilyTask(item: ExplorationTask) {
            val occurrence = item.occurrence
            if (occurrence.occurrenceId <= 0L) return
            val key = "family-${occurrence.occurrenceId}"
            if (_uiState.value.actingKey != null) return
            _uiState.update { it.copy(actingKey = key, errorMessage = null) }
            viewModelScope.launch {
                val result =
                    if (occurrence.status.countsAsDone) {
                        familyTasksRepository.reopenOccurrence(occurrence.occurrenceId)
                    } else {
                        familyTasksRepository.completeOccurrence(occurrence.occurrenceId)
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
            val house = allToday.filter { it.assignees.isEmpty() }
            val overdueHouse = overdue.filter { it.assignees.isEmpty() }
            val childId = memberId?.let { childProfileIds[it] }
            if (childId != null) {
                authRepository.setActiveChildId(childId)
                routinesRepository.syncRoutinesForDay(DateTimeUtils.startOfDayMillis())
                missionsRepository.syncMissions()
                questsRepository.syncQuests()
            } else {
                // A parent member has no child planning cache to display.
                taskRepository.clearRemoteRoutineCache()
                taskRepository.clearRemoteMissionCache()
                questRepository.clearRemoteCache()
            }
            val tasks = taskRepository.observeTasksForDay(DateTimeUtils.startOfDayMillis()).first()
            val quests = questRepository.observeQuestsForDay(DateTimeUtils.startOfDayMillis()).first()
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
                    houseTasks = house.map { task -> ExplorationTask(task, overdue.any { old -> old.occurrenceId == task.occurrenceId }) } +
                        overdueHouse.filter { old -> house.none { task -> task.occurrenceId == old.occurrenceId } }.map { task -> ExplorationTask(task, true) },
                    objectives = quests,
                    errorMessage = null,
                )
            }
        }

        private fun resolveSelection(userId: Long, members: List<ExplorationMember>, previous: Long?): Pair<Long?, String> {
            val selected = previous?.takeIf { id -> members.any { it.id == id } }
                ?: members.firstOrNull { it.id == userId }?.id
                ?: members.firstOrNull()?.id
            return selected to (members.firstOrNull { it.id == selected }?.displayName ?: "Moi")
        }
    }

private fun TaskForDay.isRoutineItem(): Boolean = task.isRoutine || task.isDaily

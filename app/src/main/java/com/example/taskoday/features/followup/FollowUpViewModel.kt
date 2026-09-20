package com.example.taskoday.features.followup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import com.example.taskoday.domain.model.TaskForDay
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.ChildrenRepository
import com.example.taskoday.domain.repository.FamilyTasksRepository
import com.example.taskoday.domain.repository.MissionsRepository
import com.example.taskoday.domain.repository.PlanningSyncRepository
import com.example.taskoday.domain.repository.RoutinesRepository
import com.example.taskoday.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class FollowUpViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val childrenRepository: ChildrenRepository,
        private val familyTasksRepository: FamilyTasksRepository,
        private val taskRepository: TaskRepository,
        private val routinesRepository: RoutinesRepository,
        private val missionsRepository: MissionsRepository,
        private val planningSyncRepository: PlanningSyncRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(FollowUpUiState())
        val uiState: StateFlow<FollowUpUiState> = _uiState.asStateFlow()

        init { refresh() }

        fun refresh() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                runCatching {
                    val me = authRepository.fetchMe()
                    val members = familyTasksRepository.fetchMembers().getOrThrow()
                    val children = runCatching { childrenRepository.fetchChildren() }.getOrDefault(emptyList())
                    val today = familyTasksRepository.fetchToday().getOrThrow().tasks
                    val overdue = familyTasksRepository.fetchOverdueOccurrences().getOrNull()?.occurrences.orEmpty()
                    val summaries = members.map { member ->
                        val familyItems = personalItems(member.userId, today, overdue)
                        val child = children.firstOrNull { it.id == member.userId || it.email.equals(member.email, ignoreCase = true) }
                        val legacyItems = if (child != null) loadLegacyItems(child.id) else emptyList()
                        summary(member.userId, member.displayName, familyItems + legacyItems)
                    }
                    val houseItems = householdItems(today, overdue)
                    val houseSummary = summary(0L, "Maison", houseItems)
                    val selected = _uiState.value.selectedMemberId?.takeIf { id -> summaries.any { it.memberId == id } }
                        ?: summaries.firstOrNull { it.memberId == me.id }?.memberId
                        ?: summaries.firstOrNull()?.memberId
                    _uiState.update { it.copy(isLoading = false, members = summaries, selectedMemberId = selected, house = houseSummary) }
                }.onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Suivi indisponible.") }
                }
            }
        }

        fun selectMember(memberId: Long) { _uiState.update { it.copy(selectedMemberId = memberId) } }

        private suspend fun loadLegacyItems(childId: Long): List<FollowUpItem> {
            authRepository.setActiveChildId(childId)
            runCatching { routinesRepository.syncRoutinesForDay(DateTimeUtils.startOfDayMillis()) }
            runCatching { missionsRepository.syncMissions() }
            val tasks = taskRepository.observeTasksForDay(DateTimeUtils.startOfDayMillis()).first()
            return tasks.map { task ->
                FollowUpItem("legacy-${task.task.id}", task.task.title, task.isCompleted, legacyTask = task)
            }
        }

        private fun personalItems(memberId: Long, today: List<FamilyTaskTodayItem>, overdue: List<FamilyTaskTodayItem>): List<FollowUpItem> {
            val current = today.filter { task -> task.assignees.any { it.id == memberId } }
            val old = overdue.filter { task -> task.assignees.any { it.id == memberId } && current.none { it.occurrenceId == task.occurrenceId } }
            return current.map { FollowUpItem("family-${it.occurrenceId}", it.title, it.status.countsAsDone, familyTask = it) } +
                old.map { FollowUpItem("family-${it.occurrenceId}", it.title, false, overdue = true, familyTask = it) }
        }

        private fun householdItems(today: List<FamilyTaskTodayItem>, overdue: List<FamilyTaskTodayItem>): List<FollowUpItem> {
            val current = today.filter { it.assignees.isEmpty() }
            val old = overdue.filter { it.assignees.isEmpty() && current.none { currentTask -> currentTask.occurrenceId == it.occurrenceId } }
            return current.map { FollowUpItem("house-${it.occurrenceId}", it.title, it.status.countsAsDone, familyTask = it) } +
                old.map { FollowUpItem("house-${it.occurrenceId}", it.title, false, overdue = true, familyTask = it) }
        }

        private fun summary(id: Long, name: String, items: List<FollowUpItem>): FollowUpMemberSummary =
            FollowUpMemberSummary(
                memberId = id,
                displayName = name,
                total = items.size,
                completed = items.count { it.completed },
                remaining = items.count { !it.completed },
                overdue = items.count { it.overdue },
                items = items,
            )
    }

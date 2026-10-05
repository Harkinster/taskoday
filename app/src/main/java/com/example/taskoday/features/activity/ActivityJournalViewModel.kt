package com.example.taskoday.features.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.data.repository.toRemoteUserMessage
import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.repository.AuthRepository
import com.example.taskoday.domain.repository.FamilyTaskEventsRepository
import com.example.taskoday.domain.repository.FamilyTasksRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ActivityJournalViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val eventsRepository: FamilyTaskEventsRepository,
    private val familyTasksRepository: FamilyTasksRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActivityJournalUiState())
    val uiState: StateFlow<ActivityJournalUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init { refresh() }

    fun refresh() {
        loadJob?.cancel()
        val previous = _uiState.value
        _uiState.value = ActivityJournalUiState(isLoading = true)
        loadJob = viewModelScope.launch {
            try {
                val me = authRepository.fetchMe()
                val familyId = authRepository.getActiveFamilyId()
                val isParent = me.role.equals("PARENT", ignoreCase = true)
                if (familyId == null) {
                    _uiState.value = ActivityJournalUiState(isLoading = false, isParent = isParent)
                    return@launch
                }
                val startAt = LocalDate.now().minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant()
                val events = eventsRepository.fetchSince(startAt).getOrThrow()
                val members = if (isParent) familyTasksRepository.fetchMembers().getOrThrow() else emptyList()
                _uiState.value = ActivityJournalUiState(
                    isLoading = false,
                    isParent = isParent,
                    familyId = familyId,
                    events = events.filter { it.familyId == familyId },
                    members = members,
                    selectedMemberId = previous.selectedMemberId?.takeIf { previous.familyId == familyId && members.any { member -> member.userId == it } },
                    selectedType = previous.selectedType.takeIf { previous.familyId == familyId },
                )
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _uiState.value = ActivityJournalUiState(isLoading = false, errorMessage = error.toRemoteUserMessage("Journal indisponible."))
            }
        }
    }

    fun selectMember(memberId: Long?) {
        _uiState.update { state ->
            if (!state.isParent || (memberId != null && state.members.none { it.userId == memberId })) state
            else state.copy(selectedMemberId = memberId)
        }
    }

    fun selectType(type: FamilyActionType?) {
        _uiState.update { it.copy(selectedType = type) }
    }
}

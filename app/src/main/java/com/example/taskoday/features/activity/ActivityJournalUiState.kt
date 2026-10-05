package com.example.taskoday.features.activity

import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.model.FamilyTaskEvent
import com.example.taskoday.domain.model.FamilyTaskMember

data class ActivityJournalUiState(
    val isLoading: Boolean = true,
    val isParent: Boolean = false,
    val familyId: Long? = null,
    val events: List<FamilyTaskEvent> = emptyList(),
    val members: List<FamilyTaskMember> = emptyList(),
    val selectedMemberId: Long? = null,
    val selectedType: FamilyActionType? = null,
    val errorMessage: String? = null,
) {
    val visibleEvents: List<FamilyTaskEvent>
        get() = events.filter { event ->
            (selectedType == null || event.category == selectedType) &&
                (!isParent || selectedMemberId == null ||
                    event.actorUserId == selectedMemberId || event.completedByUserId == selectedMemberId ||
                    selectedMemberId in event.participantUserIds)
        }
}

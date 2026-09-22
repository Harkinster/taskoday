package com.example.taskoday.features.family

import com.example.taskoday.domain.model.FamilyInvite
import com.example.taskoday.domain.model.FamilyMember
import com.example.taskoday.domain.model.FamilySummary

data class FamilyHouseholdUiState(
    val isLoading: Boolean = true,
    val members: List<FamilyMember> = emptyList(),
    val families: List<FamilySummary> = emptyList(),
    val activeFamilyId: Long? = null,
    val createFamilyName: String = "",
    val isCreateBusy: Boolean = false,
    val invite: FamilyInvite? = null,
    val joinCode: String = "",
    val isInviteBusy: Boolean = false,
    val isJoinBusy: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
)

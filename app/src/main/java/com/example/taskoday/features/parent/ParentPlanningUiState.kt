package com.example.taskoday.features.parent

import com.example.taskoday.domain.model.ParentChild
import com.example.taskoday.domain.model.ParentPlanUsage
import com.example.taskoday.domain.model.PlanningFormType

data class ParentPlanningUiState(
    val isLoading: Boolean = true,
    val hasParentAccess: Boolean = false,
    val children: List<ParentChild> = emptyList(),
    val selectedChildId: Long? = null,
    val planUsage: ParentPlanUsage = ParentPlanUsage(),
    val isSubmitting: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val createdFormType: PlanningFormType? = null,
)

package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.ParentChild

interface ChildrenRepository {
    suspend fun fetchChildren(): List<ParentChild>

    suspend fun createChild(
        displayName: String,
        email: String?,
        birthDate: String?,
    ): ParentChild

    suspend fun updateChildDisplayName(
        childId: Long,
        displayName: String,
    )
}

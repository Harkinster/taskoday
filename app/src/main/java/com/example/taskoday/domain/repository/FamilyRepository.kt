package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.model.FamilyInvite
import com.example.taskoday.domain.model.FamilyMember

interface FamilyRepository {
    suspend fun fetchMembers(): Result<List<FamilyMember>>

    suspend fun createParentInvite(): Result<FamilyInvite>

    suspend fun acceptParentInvite(code: String): Result<AuthenticatedUser>
}

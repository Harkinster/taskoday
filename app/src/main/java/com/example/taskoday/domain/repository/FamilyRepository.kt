package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.model.FamilyInvite
import com.example.taskoday.domain.model.FamilyMember
import com.example.taskoday.domain.model.FamilySummary

interface FamilyRepository {
    suspend fun getCurrentUserId(): Long? = null

    suspend fun isParentAccount(): Boolean = false

    suspend fun getActiveFamilyId(): Long? = null

    fun setActiveFamilyId(familyId: Long) = Unit

    suspend fun fetchFamilies(): Result<List<FamilySummary>> = Result.success(emptyList())

    suspend fun createFamily(name: String): Result<FamilySummary> =
        Result.failure(UnsupportedOperationException("Family creation is unavailable."))

    suspend fun fetchMembers(): Result<List<FamilyMember>>

    suspend fun leaveFamily(familyId: Long): Result<Long?> =
        Result.failure(UnsupportedOperationException("Leaving a family is unavailable."))

    suspend fun removeMember(familyId: Long, userId: Long): Result<Unit> =
        Result.failure(UnsupportedOperationException("Removing a member is unavailable."))

    suspend fun createParentInvite(): Result<FamilyInvite>

    suspend fun acceptParentInvite(code: String): Result<AuthenticatedUser>
}

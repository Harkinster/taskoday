package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.AuthSession
import com.example.taskoday.domain.model.AuthenticatedUser

interface AuthRepository {
    suspend fun registerParent(
        displayName: String,
        birthDate: String,
        email: String,
        password: String,
    ): AuthSession

    suspend fun registerChild(
        email: String,
        password: String,
        displayName: String,
        birthDate: String? = null,
    ): AuthSession

    suspend fun login(email: String, password: String): AuthSession

    suspend fun fetchMe(): AuthenticatedUser

    fun getAccessToken(): String?

    suspend fun getActiveChildId(forceRefresh: Boolean = false): Long?

    fun setActiveChildId(childId: Long)

    suspend fun getActiveFamilyId(forceRefresh: Boolean = false): Long? = fetchMe().familyIds.singleOrNull()

    fun setActiveFamilyId(familyId: Long) = Unit

    fun hasParentPin(): Boolean

    fun saveParentPin(pin: String)

    fun verifyParentPin(pin: String): Boolean

    fun logout()

    fun clearSession()
}

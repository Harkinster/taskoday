package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.AuthSession
import com.example.taskoday.domain.model.AuthenticatedUser

interface AuthRepository {
    suspend fun registerParent(email: String, password: String, familyName: String): AuthSession

    suspend fun login(email: String, password: String): AuthSession

    suspend fun fetchMe(): AuthenticatedUser

    fun getAccessToken(): String?

    suspend fun getActiveChildId(forceRefresh: Boolean = false): Long?

    fun setActiveChildId(childId: Long)

    fun clearSession()
}

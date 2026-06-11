package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.auth.AuthApi
import com.example.taskoday.data.remote.auth.TokenStorage
import com.example.taskoday.data.remote.children.ChildrenApi
import com.example.taskoday.data.remote.dto.LoginRequestDto
import com.example.taskoday.data.remote.dto.RegisterChildRequestDto
import com.example.taskoday.data.remote.dto.RegisterParentRequestDto
import com.example.taskoday.data.remote.dto.toDomain
import com.example.taskoday.domain.model.AuthSession
import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl
    @Inject
    constructor(
        private val authApi: AuthApi,
        private val childrenApi: ChildrenApi,
        private val tokenStorage: TokenStorage,
    ) : AuthRepository {
        override suspend fun registerParent(
            email: String,
            password: String,
            familyName: String,
            birthDate: String,
        ): AuthSession {
            val response =
                authApi.registerParent(
                    RegisterParentRequestDto(
                        email = email.trim(),
                        password = password,
                        familyName = familyName.trim(),
                        birthDate = birthDate.trim(),
                    ),
                )
            return response.toDomain().also { session ->
                tokenStorage.saveAccessToken(session.accessToken)
                tokenStorage.clearActiveChildId()
            }
        }

        override suspend fun registerChild(
            email: String,
            password: String,
            displayName: String,
            birthDate: String?,
        ): AuthSession {
            val normalizedBirthDate = birthDate?.trim()?.takeIf { it.isNotEmpty() }
            val response =
                authApi.registerChild(
                    RegisterChildRequestDto(
                        email = email.trim(),
                        password = password,
                        displayName = displayName.trim(),
                        birthDate = normalizedBirthDate,
                    ),
                )
            return response.toDomain().also { session ->
                tokenStorage.saveAccessToken(session.accessToken)
                tokenStorage.clearActiveChildId()
            }
        }

        override suspend fun login(email: String, password: String): AuthSession {
            val response =
                authApi.login(
                    LoginRequestDto(
                        email = email.trim(),
                        password = password,
                    ),
                )
            return response.toDomain().also { session ->
                tokenStorage.saveAccessToken(session.accessToken)
                tokenStorage.clearActiveChildId()
            }
        }

        override suspend fun fetchMe(): AuthenticatedUser = authApi.me().toDomain()

        override fun getAccessToken(): String? = tokenStorage.getAccessToken()

        override suspend fun getActiveChildId(forceRefresh: Boolean): Long? {
            if (!forceRefresh) {
                tokenStorage.getActiveChildId()?.let { return it }
            }

            val children = childrenApi.getChildren().data
            if (children.isEmpty()) {
                tokenStorage.clearActiveChildId()
                return null
            }
            val storedChildId = tokenStorage.getActiveChildId()
            val selectedChildId =
                storedChildId
                    ?.takeIf { childId -> children.any { child -> child.id == childId } }
                    ?: children.firstOrNull()?.id
                    ?: return null
            tokenStorage.saveActiveChildId(selectedChildId)
            return selectedChildId
        }

        override fun setActiveChildId(childId: Long) {
            if (childId > 0L) tokenStorage.saveActiveChildId(childId)
        }

        override fun clearSession() {
            tokenStorage.clear()
        }
    }

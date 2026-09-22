package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.auth.AuthApi
import com.example.taskoday.data.remote.auth.AuthSessionClient
import com.example.taskoday.data.remote.auth.TokenStorage
import com.example.taskoday.data.remote.children.ChildrenApi
import com.example.taskoday.data.remote.dto.LoginRequestDto
import com.example.taskoday.data.remote.dto.RegisterChildRequestDto
import com.example.taskoday.data.remote.dto.RegisterParentRequestDto
import com.example.taskoday.data.remote.dto.TokenResponseDto
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
        private val authSessionClient: AuthSessionClient,
    ) : AuthRepository {
        override suspend fun registerParent(
            displayName: String,
            birthDate: String,
            email: String,
            password: String,
        ): AuthSession {
            val response =
                authApi.registerParent(
                    RegisterParentRequestDto(
                        displayName = displayName.trim(),
                        email = email.trim(),
                        password = password,
                        birthDate = birthDate.trim(),
                    ),
                )
            return response.toDomain().also {
                tokenStorage.save(response)
                tokenStorage.clearActiveChildId()
                tokenStorage.clearActiveFamilyId()
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
            return response.toDomain().also {
                tokenStorage.save(response)
                tokenStorage.clearActiveChildId()
                tokenStorage.clearActiveFamilyId()
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
            return response.toDomain().also {
                tokenStorage.save(response)
                tokenStorage.clearActiveChildId()
                tokenStorage.clearActiveFamilyId()
            }
        }

        override suspend fun fetchMe(): AuthenticatedUser = authApi.me().toDomain()

        override fun getAccessToken(): String? = tokenStorage.getAccessToken()

        override suspend fun getActiveChildId(forceRefresh: Boolean): Long? {
            if (!forceRefresh) {
                tokenStorage.getActiveChildId()?.let { return it }
            }

            val children = childrenApi.getChildren(getActiveFamilyId()).data
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

        override suspend fun getActiveFamilyId(forceRefresh: Boolean): Long? {
            val familyIds = fetchMe().familyIds.distinct()
            if (familyIds.isEmpty()) {
                tokenStorage.clearActiveFamilyId()
                return null
            }
            val stored = tokenStorage.getActiveFamilyId()
            val selected = stored?.takeIf(familyIds::contains) ?: familyIds.singleOrNull()
            if (selected != null) tokenStorage.saveActiveFamilyId(selected)
            return selected
        }

        override fun setActiveFamilyId(familyId: Long) {
            if (familyId > 0L) {
                tokenStorage.saveActiveFamilyId(familyId)
                tokenStorage.clearActiveChildId()
            }
        }

        override fun hasParentPin(): Boolean = tokenStorage.hasParentPin()

        override fun saveParentPin(pin: String) {
            tokenStorage.saveParentPin(pin)
        }

        override fun verifyParentPin(pin: String): Boolean = tokenStorage.verifyParentPin(pin)

        override fun logout() {
            val currentRefreshToken = tokenStorage.getSessionTokens()?.refreshToken
            try {
                if (!currentRefreshToken.isNullOrBlank()) {
                    runCatching { authSessionClient.logout(currentRefreshToken) }
                }
            } finally {
                tokenStorage.clear()
            }
        }

        override fun clearSession() {
            tokenStorage.clear()
        }
    }

private fun TokenStorage.save(response: TokenResponseDto) {
    saveSessionTokens(
        accessToken = response.accessToken,
        refreshToken = response.refreshToken,
        accessExpiresInSeconds = response.expiresIn,
        refreshExpiresInSeconds = response.refreshExpiresIn,
    )
}

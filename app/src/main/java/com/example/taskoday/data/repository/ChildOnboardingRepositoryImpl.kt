package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.dto.RefreshTokenRequestDto
import com.example.taskoday.data.remote.dto.LoginRequestDto
import com.example.taskoday.data.remote.dto.RegisterChildRequestDto
import com.example.taskoday.data.remote.dto.TokenResponseDto
import com.example.taskoday.data.remote.pairing.ChildOnboardingApi
import com.example.taskoday.domain.repository.ChildOnboardingRepository
import com.example.taskoday.domain.repository.ChildTemporarySession
import javax.inject.Inject

class ChildOnboardingRepositoryImpl @Inject constructor(
    private val api: ChildOnboardingApi,
) : ChildOnboardingRepository {
    override suspend fun register(displayName: String, birthDate: String, email: String, password: String): ChildTemporarySession {
        val response = api.registerChild(RegisterChildRequestDto(email, password, displayName, birthDate))
        return response.toChildTemporarySession()
    }

    override suspend fun login(email: String, password: String): ChildTemporarySession =
        api.login(LoginRequestDto(email, password)).toChildTemporarySession()

    override suspend fun generateCode(childAccessToken: String): String =
        api.generateCode("Bearer $childAccessToken").data.code

    override suspend fun discard(childRefreshToken: String?) {
        if (!childRefreshToken.isNullOrBlank()) api.logout(RefreshTokenRequestDto(childRefreshToken))
    }
}

private fun TokenResponseDto.toChildTemporarySession(): ChildTemporarySession {
    require(role == "CHILD" && accessToken.isNotBlank()) { "Réponse de session enfant invalide." }
    return ChildTemporarySession(accessToken, refreshToken)
}

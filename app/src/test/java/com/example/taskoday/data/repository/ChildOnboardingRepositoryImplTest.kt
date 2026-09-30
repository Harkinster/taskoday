package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
import com.example.taskoday.data.remote.dto.LoginRequestDto
import com.example.taskoday.data.remote.dto.PairingCodeResponseDto
import com.example.taskoday.data.remote.dto.RefreshTokenRequestDto
import com.example.taskoday.data.remote.dto.RegisterChildRequestDto
import com.example.taskoday.data.remote.dto.TokenResponseDto
import com.example.taskoday.data.remote.pairing.ChildOnboardingApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChildOnboardingRepositoryImplTest {
    @Test fun `child token is explicit and temporary refresh token is discarded`() = runTest {
        val api = RecordingApi()
        val repository = ChildOnboardingRepositoryImpl(api)

        val session = repository.register("QA child", "2016-01-01", "child@example.test", "ValidPass26!")
        val recovered = repository.login("child@example.test", "ValidPass26!")
        val code = repository.generateCode(session.accessToken)
        repository.discard(session.refreshToken)

        assertEquals("Bearer child-access", api.authorization)
        assertEquals("ABC123", code)
        assertEquals("child-refresh", api.logoutToken)
        assertEquals("child@example.test", api.registration?.email)
        assertEquals("2016-01-01", api.registration?.birthDate)
        assertEquals("child@example.test", api.loginRequest?.email)
        assertEquals("ValidPass26!", api.loginRequest?.password)
        assertEquals("child-access", recovered.accessToken)
    }

    @Test fun `non child registration response is rejected`() = runTest {
        val api = RecordingApi(role = "PARENT")
        val result = runCatching { ChildOnboardingRepositoryImpl(api).register("Child", "2016-01-01", "child@example.test", "ValidPass26!") }
        assertTrue(result.isFailure)
    }

    @Test fun `non child login response is rejected`() = runTest {
        val api = RecordingApi(role = "PARENT")
        val result = runCatching { ChildOnboardingRepositoryImpl(api).login("child@example.test", "ValidPass26!") }
        assertTrue(result.isFailure)
    }

    private class RecordingApi(private val role: String = "CHILD") : ChildOnboardingApi {
        var registration: RegisterChildRequestDto? = null
        var loginRequest: LoginRequestDto? = null
        var authorization: String? = null
        var logoutToken: String? = null

        override suspend fun registerChild(payload: RegisterChildRequestDto): TokenResponseDto {
            registration = payload
            return TokenResponseDto("child-access", "bearer", 3600, role, "child-refresh")
        }

        override suspend fun login(payload: LoginRequestDto): TokenResponseDto {
            loginRequest = payload
            return TokenResponseDto("child-access", "bearer", 3600, role, "child-refresh")
        }

        override suspend fun generateCode(authorization: String): ApiEnvelopeDto<PairingCodeResponseDto> {
            this.authorization = authorization
            return ApiEnvelopeDto(true, PairingCodeResponseDto("ABC123"))
        }

        override suspend fun logout(payload: RefreshTokenRequestDto) {
            logoutToken = payload.refreshToken
        }
    }
}

package com.example.taskoday.data.repository

import com.example.taskoday.data.remote.dto.ApiEnvelopeDto
import com.example.taskoday.data.remote.dto.AttachChildRequestDto
import com.example.taskoday.data.remote.dto.PairingCodeResponseDto
import com.example.taskoday.data.remote.pairing.PairingApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PairingRepositoryEnvelopeTest {
    @Test fun `child pairing code is read from official response envelope`() = runTest {
        val api = object : PairingApi {
            override suspend fun generateCode() = ApiEnvelopeDto(true, PairingCodeResponseDto("ABC123", expiresAt = "2030-01-01T00:00:00Z"))
            override suspend fun getMyCode() = generateCode()
            override suspend fun attachChild(payload: AttachChildRequestDto) = Unit
        }
        val repository = PairingRepositoryImpl(api)
        assertEquals("ABC123", repository.generateCode().code)
        assertEquals("ABC123", repository.getMyCode().code)
    }
}

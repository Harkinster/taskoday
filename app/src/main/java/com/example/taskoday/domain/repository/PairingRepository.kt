package com.example.taskoday.domain.repository

import com.example.taskoday.domain.model.PairingCode

interface PairingRepository {
    suspend fun generateCode(): PairingCode

    suspend fun getMyCode(): PairingCode

    suspend fun attachChild(code: String, familyId: Long?): Result<Unit>
}

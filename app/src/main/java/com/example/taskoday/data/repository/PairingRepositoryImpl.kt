package com.example.taskoday.data.repository

import android.util.Log
import com.example.taskoday.data.remote.dto.AttachChildRequestDto
import com.example.taskoday.data.remote.dto.PairingCodeResponseDto
import com.example.taskoday.data.remote.pairing.PairingApi
import com.example.taskoday.domain.model.PairingCode
import com.example.taskoday.domain.repository.PairingRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PairingRepositoryImpl
    @Inject
    constructor(
        private val pairingApi: PairingApi,
    ) : PairingRepository {
        override suspend fun generateCode(): PairingCode = pairingApi.generateCode().toDomain()

        override suspend fun getMyCode(): PairingCode = pairingApi.getMyCode().toDomain()

        override suspend fun attachChild(code: String, familyId: Long?): Result<Unit> =
            runCatching {
                pairingApi.attachChild(
                    AttachChildRequestDto(
                        code = code.trim(),
                        familyId = familyId,
                    ),
                )
            }.onFailure { error ->
                Log.w(TAG, "Association parent/enfant echouee", error)
            }

        private companion object {
            const val TAG = "PairingRepository"
        }
    }

private fun PairingCodeResponseDto.toDomain(): PairingCode =
    PairingCode(
        code = code,
        familyId = familyId,
        expiresAt = expiresAt,
    )

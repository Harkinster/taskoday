package com.example.taskoday.domain.model

data class PairingCode(
    val code: String,
    val familyId: Long? = null,
    val expiresAt: String? = null,
)

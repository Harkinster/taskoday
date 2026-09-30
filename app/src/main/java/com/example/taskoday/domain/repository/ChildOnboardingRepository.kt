package com.example.taskoday.domain.repository

/** A child credential is held only in memory while the parent completes pairing. */
interface ChildOnboardingRepository {
    suspend fun register(displayName: String, birthDate: String, email: String, password: String): ChildTemporarySession

    suspend fun login(email: String, password: String): ChildTemporarySession

    suspend fun generateCode(childAccessToken: String): String

    suspend fun discard(childRefreshToken: String?)
}

data class ChildTemporarySession(val accessToken: String, val refreshToken: String?)

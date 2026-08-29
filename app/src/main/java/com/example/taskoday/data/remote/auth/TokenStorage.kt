package com.example.taskoday.data.remote.auth

interface TokenStorage {
    fun getSessionTokens(): SessionTokens?

    fun saveSessionTokens(
        accessToken: String,
        refreshToken: String?,
        accessExpiresInSeconds: Int? = null,
        refreshExpiresInSeconds: Int? = null,
    )

    fun getAccessToken(): String? = getSessionTokens()?.accessToken

    fun getActiveChildId(): Long?

    fun saveActiveChildId(childId: Long)

    fun clearActiveChildId()

    fun hasParentPin(): Boolean

    fun saveParentPin(pin: String)

    fun verifyParentPin(pin: String): Boolean

    fun clear()
}

data class SessionTokens(
    val accessToken: String,
    val refreshToken: String?,
    val accessExpiresAtEpochSeconds: Long?,
    val refreshExpiresAtEpochSeconds: Long?,
)

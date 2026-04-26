package com.example.taskoday.data.remote.dto

import com.example.taskoday.domain.model.AuthSession
import com.example.taskoday.domain.model.AuthenticatedUser
import com.google.gson.annotations.SerializedName

data class RegisterParentRequestDto(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("family_name")
    val familyName: String,
)

data class LoginRequestDto(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String,
)

data class TokenResponseDto(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("expires_in")
    val expiresIn: Int,
    @SerializedName("role")
    val role: String,
)

data class MeResponseDto(
    @SerializedName("id")
    val id: Long,
    @SerializedName("email")
    val email: String,
    @SerializedName("role")
    val role: String,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("family_ids")
    val familyIds: List<Long>,
)

fun TokenResponseDto.toDomain(): AuthSession =
    AuthSession(
        accessToken = accessToken,
        tokenType = tokenType,
        expiresInSeconds = expiresIn,
        role = role,
    )

fun MeResponseDto.toDomain(): AuthenticatedUser =
    AuthenticatedUser(
        id = id,
        email = email,
        role = role,
        isActive = isActive,
        familyIds = familyIds,
    )

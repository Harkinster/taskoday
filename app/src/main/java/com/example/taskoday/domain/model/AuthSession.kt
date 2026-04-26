package com.example.taskoday.domain.model

data class AuthSession(
    val accessToken: String,
    val tokenType: String,
    val expiresInSeconds: Int,
    val role: String,
)

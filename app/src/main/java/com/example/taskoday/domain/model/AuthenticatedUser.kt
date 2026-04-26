package com.example.taskoday.domain.model

data class AuthenticatedUser(
    val id: Long,
    val email: String,
    val role: String,
    val isActive: Boolean,
    val familyIds: List<Long>,
)

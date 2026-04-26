package com.example.taskoday.domain.model

data class ChildProfile(
    val id: Long,
    val familyId: Long?,
    val userId: Long?,
    val email: String,
    val displayName: String,
    val birthDate: String?,
    val role: String?,
)

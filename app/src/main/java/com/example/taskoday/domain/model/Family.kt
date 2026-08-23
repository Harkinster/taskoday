package com.example.taskoday.domain.model

data class FamilyMember(
    val userId: Long,
    val displayName: String,
    val email: String?,
    val role: FamilyMemberRole,
    val isActive: Boolean,
)

enum class FamilyMemberRole {
    PARENT,
    CHILD,
    OTHER,
}

data class FamilyInvite(
    val code: String,
    val expiresAt: String?,
)

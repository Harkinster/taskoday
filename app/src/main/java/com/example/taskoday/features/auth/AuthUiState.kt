package com.example.taskoday.features.auth

import com.example.taskoday.domain.model.AuthenticatedUser

data class AuthUiState(
    val isCheckingSession: Boolean = true,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isLocalMode: Boolean = false,
    val currentUser: AuthenticatedUser? = null,
    val errorMessage: String? = null,
)

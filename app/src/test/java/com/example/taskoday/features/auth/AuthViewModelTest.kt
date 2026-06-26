package com.example.taskoday.features.auth

import com.example.taskoday.domain.model.AuthSession
import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.repository.AuthRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthViewModelTest {
    @Test
    fun `logout clears repository session and local auth state`() {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)
        viewModel.continueInLocalMode()

        viewModel.logout()

        assertTrue(repository.sessionCleared)
        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertFalse(viewModel.uiState.value.isLocalMode)
        assertFalse(viewModel.uiState.value.isCheckingSession)
        assertNull(viewModel.uiState.value.currentUser)
    }
}

private class FakeAuthRepository : AuthRepository {
    var sessionCleared: Boolean = false

    override suspend fun registerParent(
        email: String,
        password: String,
        familyName: String,
        birthDate: String,
    ): AuthSession = error("Not used")

    override suspend fun registerChild(
        email: String,
        password: String,
        displayName: String,
        birthDate: String?,
    ): AuthSession = error("Not used")

    override suspend fun login(email: String, password: String): AuthSession = error("Not used")

    override suspend fun fetchMe(): AuthenticatedUser = error("Not used")

    override fun getAccessToken(): String? = null

    override suspend fun getActiveChildId(forceRefresh: Boolean): Long? = null

    override fun setActiveChildId(childId: Long) = Unit

    override fun hasParentPin(): Boolean = false

    override fun saveParentPin(pin: String) = Unit

    override fun verifyParentPin(pin: String): Boolean = false

    override fun clearSession() {
        sessionCleared = true
    }
}

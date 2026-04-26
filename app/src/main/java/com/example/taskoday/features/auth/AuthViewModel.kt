package com.example.taskoday.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.domain.model.AuthenticatedUser
import com.example.taskoday.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AuthUiState())
        val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

        init {
            checkExistingSession()
        }

        fun checkExistingSession() {
            val accessToken = authRepository.getAccessToken()
            if (accessToken.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isCheckingSession = false,
                        isAuthenticated = false,
                        currentUser = null,
                        isLocalMode = false,
                    )
                }
                return
            }

            _uiState.update { it.copy(isCheckingSession = true, errorMessage = null, isLocalMode = false) }
            viewModelScope.launch {
                runCatching {
                    authRepository.fetchMe()
                }.onSuccess { me ->
                    setAuthenticated(me)
                }.onFailure { throwable ->
                    authRepository.clearSession()
                    _uiState.update {
                        it.copy(
                            isCheckingSession = false,
                            isLoading = false,
                            isAuthenticated = false,
                            currentUser = null,
                            errorMessage = throwable.toMessage(),
                        )
                    }
                }
            }
        }

        fun login(email: String, password: String) {
            if (email.isBlank() || password.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Email et mot de passe sont requis.") }
                return
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null, isLocalMode = false) }
            viewModelScope.launch {
                runCatching {
                    authRepository.login(email = email, password = password)
                    authRepository.fetchMe()
                }.onSuccess { me ->
                    setAuthenticated(me)
                }.onFailure { throwable ->
                    authRepository.clearSession()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = false,
                            currentUser = null,
                            errorMessage = throwable.toMessage(),
                        )
                    }
                }
            }
        }

        fun registerParent(email: String, password: String, familyName: String) {
            if (email.isBlank() || password.isBlank() || familyName.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Email, mot de passe et nom de famille sont requis.") }
                return
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null, isLocalMode = false) }
            viewModelScope.launch {
                runCatching {
                    authRepository.registerParent(
                        email = email,
                        password = password,
                        familyName = familyName,
                    )
                    authRepository.fetchMe()
                }.onSuccess { me ->
                    setAuthenticated(me)
                }.onFailure { throwable ->
                    authRepository.clearSession()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = false,
                            currentUser = null,
                            errorMessage = throwable.toMessage(),
                        )
                    }
                }
            }
        }

        fun continueInLocalMode() {
            _uiState.update {
                it.copy(
                    isCheckingSession = false,
                    isLoading = false,
                    isAuthenticated = false,
                    isLocalMode = true,
                    errorMessage = null,
                )
            }
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        private fun setAuthenticated(me: AuthenticatedUser) {
            _uiState.update {
                it.copy(
                    isCheckingSession = false,
                    isLoading = false,
                    isAuthenticated = true,
                    isLocalMode = false,
                    currentUser = me,
                    errorMessage = null,
                )
            }
        }
    }

private fun Throwable.toMessage(): String =
    when (this) {
        is UnknownHostException, is ConnectException -> "Serveur indisponible. Verifiez la configuration backend."
        is SocketTimeoutException -> "Connexion au serveur expiree."
        is HttpException -> "Erreur API (${code()})."
        is IOException -> "Erreur reseau. Verifiez votre connexion."
        else -> message ?: "Erreur inconnue."
    }

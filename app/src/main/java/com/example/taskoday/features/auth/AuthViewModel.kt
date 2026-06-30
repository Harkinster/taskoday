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

        fun registerParent(email: String, password: String, familyName: String, birthDate: String) {
            if (email.isBlank() || password.isBlank() || familyName.isBlank() || birthDate.isBlank()) {
                _uiState.update {
                    it.copy(errorMessage = "Email, mot de passe, nom de famille et date de naissance sont requis.")
                }
                return
            }

            val normalizedBirthDate = birthDate.trim()
            if (!BIRTH_DATE_REGEX.matches(normalizedBirthDate)) {
                _uiState.update { it.copy(errorMessage = "Date de naissance invalide (format YYYY-MM-DD).") }
                return
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null, isLocalMode = false) }
            viewModelScope.launch {
                runCatching {
                    authRepository.registerParent(
                        email = email,
                        password = password,
                        familyName = familyName,
                        birthDate = normalizedBirthDate,
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

        fun registerChild(email: String, password: String, displayName: String, birthDate: String?) {
            if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Email, mot de passe et prenom sont requis.") }
                return
            }

            val normalizedBirthDate = birthDate?.trim().orEmpty().ifBlank { null }
            if (!normalizedBirthDate.isNullOrBlank() && !BIRTH_DATE_REGEX.matches(normalizedBirthDate)) {
                _uiState.update { it.copy(errorMessage = "Date de naissance invalide (format YYYY-MM-DD).") }
                return
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null, isLocalMode = false) }
            viewModelScope.launch {
                runCatching {
                    authRepository.registerChild(
                        email = email,
                        password = password,
                        displayName = displayName,
                        birthDate = normalizedBirthDate,
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

        fun logout() {
            authRepository.clearSession()
            _uiState.value =
                AuthUiState(
                    isCheckingSession = false,
                    isAuthenticated = false,
                    isLocalMode = false,
                    currentUser = null,
                )
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

        private companion object {
            val BIRTH_DATE_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")
        }
    }

private fun Throwable.toMessage(): String =
    when (this) {
        is UnknownHostException, is ConnectException -> "Serveur indisponible. Vérifiez votre connexion."
        is SocketTimeoutException -> "Connexion au serveur expirée."
        is HttpException -> "Erreur API (${code()})."
        is IOException -> "Erreur réseau. Vérifiez votre connexion."
        else -> message ?: "Erreur inconnue."
    }

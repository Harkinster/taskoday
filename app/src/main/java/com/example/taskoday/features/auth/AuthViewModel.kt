package com.example.taskoday.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskoday.data.demo.DemoModeStore
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
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.HttpException

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val demoModeStore: DemoModeStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AuthUiState())
        val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

        init {
            checkExistingSession()
        }

        /** Keeps the ViewModel convenient to construct in the existing JVM tests. */
        constructor(authRepository: AuthRepository) : this(authRepository, DemoModeStore())

        fun checkExistingSession() {
            val accessToken = authRepository.getAccessToken()
            if (accessToken.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isCheckingSession = false,
                        isAuthenticated = false,
                        currentUser = null,
                        isLocalMode = demoModeStore.isEnabled,
                        canRetrySession = false,
                    )
                }
                return
            }

            // A real session always takes precedence over the DEBUG-only demo path.
            demoModeStore.setEnabled(false)

            _uiState.update {
                it.copy(
                    isCheckingSession = true,
                    errorMessage = null,
                    isLocalMode = false,
                    canRetrySession = false,
                )
            }
            viewModelScope.launch {
                val me = try {
                    withTimeoutOrNull(35_000L) { authRepository.fetchMe() }
                } catch (throwable: Throwable) {
                    handleAuthenticationFailure(throwable)
                    return@launch
                }
                if (me == null) {
                    handleAuthenticationFailure(SocketTimeoutException())
                } else {
                    setAuthenticated(me)
                }
            }
        }

        fun login(email: String, password: String) {
            if (email.isBlank() || password.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Email et mot de passe sont requis.") }
                return
            }

            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, isLocalMode = false, canRetrySession = false)
            }
            demoModeStore.setEnabled(false)
            viewModelScope.launch {
                runCatching {
                    authRepository.login(email = email, password = password)
                    authRepository.fetchMe()
                }.onSuccess { me ->
                    setAuthenticated(me)
                }.onFailure(::handleAuthenticationFailure)
            }
        }

        fun registerParent(
            email: String,
            password: String,
            familyName: String,
            birthDate: String,
            inviteCode: String? = null,
        ) {
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

            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, isLocalMode = false, canRetrySession = false)
            }
            demoModeStore.setEnabled(false)
            viewModelScope.launch {
                runCatching {
                    authRepository.registerParent(
                        email = email,
                        password = password,
                        familyName = familyName,
                        birthDate = normalizedBirthDate,
                        inviteCode = inviteCode?.trim()?.takeIf { it.isNotBlank() },
                    )
                    authRepository.fetchMe()
                }.onSuccess { me ->
                    setAuthenticated(me)
                }.onFailure(::handleAuthenticationFailure)
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

            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, isLocalMode = false, canRetrySession = false)
            }
            demoModeStore.setEnabled(false)
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
                }.onFailure(::handleAuthenticationFailure)
            }
        }

        fun continueInLocalMode() {
            demoModeStore.setEnabled(true)
            _uiState.update {
                it.copy(
                    isCheckingSession = false,
                    isLoading = false,
                    isAuthenticated = false,
                    isLocalMode = true,
                    errorMessage = null,
                    canRetrySession = false,
                )
            }
        }

        fun logout() {
            authRepository.logout()
            demoModeStore.setEnabled(false)
            _uiState.value =
                AuthUiState(
                    isCheckingSession = false,
                    isAuthenticated = false,
                    isLocalMode = false,
                    currentUser = null,
                    canRetrySession = false,
                )
        }

        fun clearError() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        private fun setAuthenticated(me: AuthenticatedUser) {
            demoModeStore.setEnabled(false)
            _uiState.update {
                it.copy(
                    isCheckingSession = false,
                    isLoading = false,
                    isAuthenticated = true,
                    isLocalMode = false,
                    currentUser = me,
                    errorMessage = null,
                    canRetrySession = false,
                )
            }
        }

        private fun handleAuthenticationFailure(throwable: Throwable) {
            val sessionStillStored = !authRepository.getAccessToken().isNullOrBlank()
            _uiState.update {
                it.copy(
                    isCheckingSession = false,
                    isLoading = false,
                    isAuthenticated = false,
                    currentUser = null,
                    errorMessage = throwable.toMessage(),
                    canRetrySession = sessionStillStored,
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
        is HttpException -> when (code()) {
            400, 409, 422 -> "Vérifiez les informations saisies puis réessayez."
            401, 403 -> "Cette action n’est pas autorisée."
            else -> "Impossible de créer le compte pour le moment."
        }
        is IOException -> "Erreur réseau. Vérifiez votre connexion."
        else -> message ?: "Erreur inconnue."
    }

package com.example.taskoday.features.auth

import android.app.Activity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.example.taskoday.core.ui.component.forms.FantasyFormField as OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.theme.ParchmentCream
import com.example.taskoday.core.ui.component.forms.FantasyFormBottomBar as FormBottomBar
import com.example.taskoday.core.ui.component.forms.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.taskoday.domain.model.AuthenticatedUser

@Composable
fun RegisterParentScreen(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit,
    onOpenApp: (AuthenticatedUser?, Boolean) -> Unit,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    FantasyFormTheme {
        RegisterParentScreenContent(viewModel,
            onBackToLogin = { focus.clearFocus(); keyboard?.hide(); onBackToLogin() },
            onOpenApp = { user, local -> focus.clearFocus(); keyboard?.hide(); onOpenApp(user, local) })
    }
}

@Composable
private fun RegisterParentScreenContent(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit,
    onOpenApp: (AuthenticatedUser?, Boolean) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    DisposableEffect(window, view) {
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        onDispose { previous?.let { controller?.isAppearanceLightStatusBars = it } }
    }
    SideEffect {
        if (!view.isInEditMode && window != null) {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    var selectedRole by rememberSaveable { mutableStateOf(RegistrationRole.Parent) }
    var parentDisplayName by rememberSaveable { mutableStateOf("") }
    var parentBirthDate by rememberSaveable { mutableStateOf("") }
    var childDisplayName by rememberSaveable { mutableStateOf("") }
    var childBirthDate by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.isAuthenticated, uiState.isLocalMode) {
        if (uiState.isAuthenticated || uiState.isLocalMode) {
            onOpenApp(uiState.currentUser, uiState.isLocalMode)
        }
    }

    Scaffold(
        containerColor = RegistrationFormBackground,
        topBar = {
            Column(Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                FantasyFormHeader("Créer un compte", "Votre première étape dans Taskoday", onBackToLogin)
            }
        },
        bottomBar = {
            if (!uiState.isCheckingSession) {
                FormBottomBar(
                    onBack = onBackToLogin,
                    onPrimary = {
                        when (selectedRole) {
                            RegistrationRole.Parent -> viewModel.registerParent(parentDisplayName, parentBirthDate, email, password)
                            RegistrationRole.Child -> viewModel.registerChild(email, password, childDisplayName, childBirthDate)
                        }
                    },
                    primaryEnabled = !uiState.isLoading,
                    primaryLabel = if (uiState.isLoading) "Création..." else "Continuer",
                )
            }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (uiState.isCheckingSession) {
                FantasyFormPanel {
                    if (!uiState.canRetrySession) CircularProgressIndicator()
                    Text(uiState.errorMessage ?: "Vérification de session...")
                    if (uiState.canRetrySession) TextButton(onClick = viewModel::checkExistingSession) { Text("Réessayer") }
                }
            } else {
                FantasyFormPanel(title = "Qui utilisera ce compte ?") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FantasyFormChoice(selectedRole == RegistrationRole.Parent,
                            { selectedRole = RegistrationRole.Parent; viewModel.clearError() }, { Text("Parent") })
                        FantasyFormChoice(selectedRole == RegistrationRole.Child,
                            { selectedRole = RegistrationRole.Child; viewModel.clearError() }, { Text("Enfant") })
                    }
                    Text(when (selectedRole) {
                        RegistrationRole.Parent -> "Créez votre compte, puis créez ou rejoignez une famille."
                        RegistrationRole.Child -> "Créez votre compte, puis rejoignez votre famille avec le code d’un parent."
                    }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                FantasyFormPanel(title = if (selectedRole == RegistrationRole.Parent) "Votre profil" else "Profil de l’enfant") {
                    if (selectedRole == RegistrationRole.Parent) {
                        OutlinedTextField(parentDisplayName, { parentDisplayName = it; viewModel.clearError() },
                            label = { Text("Prénom ou pseudo *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(parentBirthDate, { parentBirthDate = it; viewModel.clearError() },
                            label = { Text("Date de naissance * (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    } else {
                        OutlinedTextField(childDisplayName, { childDisplayName = it; viewModel.clearError() },
                            label = { Text("Prénom de l’enfant") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(childBirthDate, { childBirthDate = it; viewModel.clearError() },
                            label = { Text("Date de naissance (ex. 2018-01-01)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    OutlinedTextField(email, { email = it; viewModel.clearError() }, label = { Text("Email") },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(password, { password = it; viewModel.clearError() }, label = { Text("Mot de passe") },
                        singleLine = true, visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                    if (!uiState.errorMessage.isNullOrBlank()) Text(uiState.errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onBackToLogin, enabled = !uiState.isLoading, modifier = Modifier.fillMaxWidth()) { Text("J’ai déjà un compte") }
            }
        }
    }
}

private enum class RegistrationRole { Parent, Child }

internal val RegistrationFormBackground = ParchmentCream

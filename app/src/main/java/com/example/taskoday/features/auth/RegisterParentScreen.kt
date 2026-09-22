package com.example.taskoday.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.core.ui.component.FormBottomBar
import com.example.taskoday.domain.model.AuthenticatedUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterParentScreen(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit,
    onOpenApp: (AuthenticatedUser?, Boolean) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

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
        topBar = { TopAppBar(title = { Text("Créer un compte") }) },
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
        if (uiState.isCheckingSession) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (!uiState.canRetrySession) {
                    CircularProgressIndicator()
                }
                Text(
                    text = uiState.errorMessage ?: "Vérification de session...",
                    modifier = Modifier.padding(top = spacing.small),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (uiState.canRetrySession) {
                    TextButton(onClick = viewModel::checkExistingSession) {
                        Text("Réessayer")
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(spacing.medium)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Text(
                text = "Qui utilisera ce compte ?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = when (selectedRole) {
                    RegistrationRole.Parent -> "Créez votre compte, puis créez ou rejoignez une famille."
                    RegistrationRole.Child -> "Créez votre compte, puis rejoignez votre famille avec le code d’un parent."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                FilterChip(
                    selected = selectedRole == RegistrationRole.Parent,
                    onClick = {
                        selectedRole = RegistrationRole.Parent
                        viewModel.clearError()
                    },
                    label = { Text("Parent") },
                )
                FilterChip(
                    selected = selectedRole == RegistrationRole.Child,
                    onClick = {
                        selectedRole = RegistrationRole.Child
                        viewModel.clearError()
                    },
                    label = { Text("Enfant") },
                )
            }

            if (selectedRole == RegistrationRole.Parent) {
                OutlinedTextField(
                    value = parentDisplayName,
                    onValueChange = {
                        parentDisplayName = it
                        viewModel.clearError()
                    },
                    label = { Text("Prénom ou pseudo *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = parentBirthDate,
                    onValueChange = {
                        parentBirthDate = it
                        viewModel.clearError()
                    },
                    label = { Text("Date de naissance * (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

            } else {
                OutlinedTextField(
                    value = childDisplayName,
                    onValueChange = {
                        childDisplayName = it
                        viewModel.clearError()
                    },
                    label = { Text("Prénom de l’enfant") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = childBirthDate,
                    onValueChange = {
                        childBirthDate = it
                        viewModel.clearError()
                    },
                    label = { Text("Date de naissance (ex. 2018-01-01)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    viewModel.clearError()
                },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.clearError()
                },
                label = { Text("Mot de passe") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            if (!uiState.errorMessage.isNullOrBlank()) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            /* Registration is kept in the persistent bottom action bar. */
            /*
            Button(
                onClick = {
                    when (selectedRole) {
                        RegistrationRole.Parent ->
                            viewModel.registerParent(
                                displayName = parentDisplayName,
                                birthDate = parentBirthDate,
                                email = email,
                                password = password,
                            )
                        RegistrationRole.Child ->
                            viewModel.registerChild(
                                email = email,
                                password = password,
                                displayName = childDisplayName,
                                birthDate = childBirthDate,
                            )
                    }
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (uiState.isLoading) {
                        "Création…"
                    } else {
                        when (selectedRole) {
                            RegistrationRole.Parent -> "Continuer"
                            RegistrationRole.Child -> "Continuer"
                        }
                    },
                )
            }
            */

            TextButton(
                onClick = onBackToLogin,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("J’ai déjà un compte")
            }

            TextButton(
                onClick = viewModel::continueInLocalMode,
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Découvrir sans compte")
            }
        }
    }
}

private enum class RegistrationRole {
    Parent,
    Child,
}

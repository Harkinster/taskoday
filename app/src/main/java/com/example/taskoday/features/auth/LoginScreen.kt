package com.example.taskoday.features.auth

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.example.taskoday.core.ui.component.forms.FantasyFormField as OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.features.taskodayv4.FantasyFramedButton
import com.example.taskoday.core.ui.component.forms.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.example.taskoday.R
import com.example.taskoday.core.ui.component.fantasy.TaskodayWorldBackground
import com.example.taskoday.core.ui.theme.MagicViolet
import com.example.taskoday.core.ui.theme.ParchmentShadow
import com.example.taskoday.core.ui.theme.TextMuted
import com.example.taskoday.core.ui.theme.WoodBrownDark
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.domain.model.AuthenticatedUser

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onOpenRegisterParent: () -> Unit,
    onOpenApp: (AuthenticatedUser?, Boolean) -> Unit,
) {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    FantasyFormTheme {
        LoginScreenContent(viewModel,
            onOpenRegisterParent = { focus.clearFocus(); keyboard?.hide(); onOpenRegisterParent() },
            onOpenApp = { user, local -> focus.clearFocus(); keyboard?.hide(); onOpenApp(user, local) })
    }
}

@Composable
private fun LoginScreenContent(
    viewModel: AuthViewModel,
    onOpenRegisterParent: () -> Unit,
    onOpenApp: (AuthenticatedUser?, Boolean) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.isAuthenticated, uiState.isLocalMode) {
        if (uiState.isAuthenticated || uiState.isLocalMode) {
            onOpenApp(uiState.currentUser, uiState.isLocalMode)
        }
    }

    TaskodayWorldBackground {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = spacing.medium, vertical = spacing.large),
        ) {
            Column(
                modifier = Modifier
                    .align(if (uiState.isCheckingSession) Alignment.Center else Alignment.BottomCenter)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LoginPanel {
                    Image(painterResource(R.drawable.taskoday_reference_wordmark), "Taskoday",
                        Modifier.fillMaxWidth().height(72.dp))
                    if (uiState.isCheckingSession) {
                        if (!uiState.canRetrySession) {
                            CircularProgressIndicator(color = MagicViolet, trackColor = ParchmentShadow.copy(alpha = 0.45f))
                        }
                        Text(
                            text = uiState.errorMessage ?: "Vérification de session...",
                            modifier = Modifier.padding(top = spacing.small),
                            color = if (uiState.canRetrySession) MaterialTheme.colorScheme.error else TextMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (uiState.canRetrySession) {
                            TextButton(onClick = viewModel::checkExistingSession) {
                                Text("Réessayer")
                            }
                        }
                        return@LoginPanel
                    }

                    Text(
                        text = "Bienvenue sur Taskoday",
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleLarge,
                        color = WoodBrownDark,
                    )
                    Text(
                        text = "Retrouvez votre famille, vos missions et vos compagnons.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            viewModel.clearError()
                        },
                        label = { Text("Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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

                    FantasyFramedButton(
                        label = if (uiState.isLoading) "Connexion…" else "Se connecter",
                        onClick = { viewModel.login(email = email, password = password) },
                        enabled = !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    TextButton(
                        onClick = onOpenRegisterParent,
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.textButtonColors(contentColor = MagicViolet),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Créer un compte")
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginPanel(content: @Composable ColumnScope.() -> Unit) {
    FantasyFormPanel {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

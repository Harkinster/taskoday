package com.example.taskoday.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.NeonButton
import com.example.taskoday.core.ui.component.fantasy.TaskodayWorldBackground
import com.example.taskoday.core.ui.theme.DangerGlow
import com.example.taskoday.core.ui.theme.InkBrown
import com.example.taskoday.core.ui.theme.MagicViolet
import com.example.taskoday.core.ui.theme.ParchmentCream
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.ParchmentShadow
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.TextMuted
import com.example.taskoday.core.ui.theme.WoodBrownDark
import com.example.taskoday.core.ui.theme.spacing

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onOpenRegisterParent: () -> Unit,
    onOpenApp: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(uiState.isAuthenticated, uiState.isLocalMode) {
        if (uiState.isAuthenticated || uiState.isLocalMode) {
            onOpenApp()
        }
    }

    TaskodayWorldBackground {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = spacing.medium, vertical = spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (uiState.isCheckingSession) Arrangement.Center else Arrangement.Bottom,
        ) {
            LoginPanel {
                if (uiState.isCheckingSession) {
                    CircularProgressIndicator(color = MagicViolet, trackColor = ParchmentShadow.copy(alpha = 0.45f))
                    Text(
                        text = "Verification de session...",
                        modifier = Modifier.padding(top = spacing.small),
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    return@LoginPanel
                }

                Text(
                    text = "Connexion",
                    style = MaterialTheme.typography.titleLarge,
                    color = WoodBrownDark,
                )
                Text(
                    text = "Connectez-vous au backend Taskoday.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )

                val fieldColors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor = InkBrown,
                        unfocusedTextColor = InkBrown,
                        focusedContainerColor = ParchmentLight.copy(alpha = 0.96f),
                        unfocusedContainerColor = ParchmentLight.copy(alpha = 0.92f),
                        focusedBorderColor = SoftGold,
                        unfocusedBorderColor = ParchmentShadow,
                        focusedLabelColor = MagicViolet,
                        unfocusedLabelColor = TextMuted,
                        cursorColor = MagicViolet,
                    )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        viewModel.clearError()
                    },
                    label = { Text("Email") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
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
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (!uiState.errorMessage.isNullOrBlank()) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = DangerGlow,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                NeonButton(
                    text = if (uiState.isLoading) "Connexion..." else "Se connecter",
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
                    Text("Creer un compte parent ou enfant")
                }

                TextButton(
                    onClick = viewModel::continueInLocalMode,
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.textButtonColors(contentColor = MagicViolet),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Continuer en mode local")
                }
            }
        }
    }
}

@Composable
private fun LoginPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ParchmentLight.copy(alpha = 0.96f),
                            ParchmentCream.copy(alpha = 0.94f),
                        ),
                    ),
                )
                .border(
                    width = 1.6.dp,
                    color = SoftGold.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(26.dp),
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

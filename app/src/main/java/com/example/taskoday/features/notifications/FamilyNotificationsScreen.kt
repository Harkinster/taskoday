package com.example.taskoday.features.notifications

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.theme.DangerGlow
import com.example.taskoday.core.ui.theme.InkBrown
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.WoodBrown
import com.example.taskoday.core.ui.theme.spacing

@Composable
fun FamilyNotificationsScreen(
    viewModel: FamilyNotificationsViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val spacing = MaterialTheme.spacing
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viewModel.setDailySummaryEnabled(true)
            } else {
                viewModel.onNotificationPermissionDenied()
            }
        }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        FantasyScreenBackground(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(innerPadding),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.medium),
                contentPadding = PaddingValues(top = spacing.large, bottom = spacing.xxLarge),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                item {
                    FamilyNotificationsHeader(onBack = onBack)
                }

                item {
                    FamilyNotificationsSummaryCard(
                        uiState = uiState,
                        onToggle = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted =
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    viewModel.setDailySummaryEnabled(true)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                viewModel.setDailySummaryEnabled(enabled)
                            }
                        },
                        onPickTime = {
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    viewModel.updateDailySummaryTime(hour = hourOfDay, minute = minute)
                                },
                                uiState.hour,
                                uiState.minute,
                                true,
                            ).show()
                        },
                    )
                }

                uiState.successMessage?.takeIf { it.isNotBlank() }?.let { message ->
                    item {
                        FamilyNotificationMessageCard(message = message, isError = false, onDismiss = viewModel::clearMessages)
                    }
                }
                uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                    item {
                        FamilyNotificationMessageCard(message = message, isError = true, onDismiss = viewModel::clearMessages)
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyNotificationsHeader(onBack: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = ParchmentLight.copy(alpha = 0.98f),
                contentColor = InkBrown,
            ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour", tint = WoodBrown)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBrown,
                )
                Text(
                    text = "Un resume local des taches du foyer, par appareil.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                )
            }
            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = WoodBrown, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun FamilyNotificationsSummaryCard(
    uiState: FamilyNotificationsUiState,
    onToggle: (Boolean) -> Unit,
    onPickTime: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = ParchmentLight.copy(alpha = 0.98f),
                contentColor = InkBrown,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = WoodBrown)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Resume quotidien",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = InkBrown,
                    )
                    Text(
                        text = "Taches restantes aujourd'hui et taches en retard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted,
                    )
                }
                Switch(
                    checked = uiState.dailySummaryEnabled,
                    onCheckedChange = onToggle,
                    enabled = !uiState.isBusy,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Heure du resume",
                    style = MaterialTheme.typography.titleSmall,
                    color = InkBrown,
                )
                OutlinedButton(
                    onClick = onPickTime,
                    enabled = !uiState.isBusy,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(formatFamilyNotificationTime(uiState.hour, uiState.minute))
                }
            }

            Text(
                text = "Contenu : taches du jour et taches en retard. Aucun rappel exact par tache dans cette V1.",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
        }
    }
}

@Composable
private fun FamilyNotificationMessageCard(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = if (isError) DangerGlow.copy(alpha = 0.14f) else SoftGold.copy(alpha = 0.18f),
                contentColor = InkBrown,
            ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) DangerGlow else InkBrown,
            )
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    }
}

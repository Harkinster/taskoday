package com.example.taskoday.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    Scaffold(topBar = { TopAppBar(title = { Text(text = "Paramètres") }) }) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(spacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Notifications", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = uiState.notificationsEnabled,
                        onCheckedChange = viewModel::setNotificationsEnabled,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(spacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Couleurs dynamiques", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = uiState.useDynamicColors,
                        onCheckedChange = viewModel::setDynamicColors,
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(spacing.medium)) {
                    Text(text = "Version de l’application", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = uiState.appVersionLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = spacing.xSmall),
                    )
                }
            }
        }
    }
}

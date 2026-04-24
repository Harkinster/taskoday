package com.example.taskoday.features.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.taskoday.core.ui.component.PlaceholderScreen
import com.example.taskoday.core.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(viewModel: RoutinesViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    Scaffold(topBar = { TopAppBar(title = { Text(text = "Routines") }) }) { innerPadding ->
        if (uiState.routines.isEmpty() && !uiState.isLoading) {
            PlaceholderScreen(
                title = "Aucune routine",
                description = "La base routines est prête. Vous pouvez ajouter la création ensuite.",
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            items(uiState.routines, key = { routine -> routine.id }) { routine ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(spacing.medium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = spacing.medium)) {
                            Text(text = routine.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Fréquence : ${routine.frequency.label()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Rappel : ${routine.reminderTime ?: "aucun"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = routine.isActive,
                            onCheckedChange = { checked ->
                                viewModel.toggleRoutine(routine.id, checked)
                            },
                        )
                    }
                }
            }
        }
    }
}

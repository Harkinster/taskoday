package com.example.taskoday.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.PlaceholderScreen
import com.example.taskoday.core.ui.component.TaskCard
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.domain.model.TaskStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenTasks: () -> Unit,
    onOpenWeek: () -> Unit,
    onOpenTask: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Aujourd’hui")
                        Text(
                            text = uiState.dateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
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
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(spacing.medium)) {
                        Text(text = "Focus du jour", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${uiState.todayTasks.size} tâche(s) prévue(s) aujourd’hui",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = spacing.xSmall),
                        )
                        Text(
                            text = "${uiState.activeRoutines.size} routine(s) active(s)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = spacing.xSmall),
                        )
                        Row(
                            modifier = Modifier.padding(top = spacing.medium),
                            horizontalArrangement = Arrangement.spacedBy(spacing.small),
                        ) {
                            Button(onClick = onOpenTasks) {
                                Text(text = "Voir les tâches")
                            }
                            TextButton(onClick = onOpenWeek) {
                                Text(text = "Vue semaine")
                            }
                        }
                    }
                }
            }

            if (uiState.todayTasks.isEmpty()) {
                item {
                    PlaceholderScreen(
                        title = "Aucune tâche aujourd’hui",
                        description = "Profitez de l’élan ou créez une nouvelle tâche depuis l’onglet Tâches.",
                    )
                }
            } else {
                items(
                    items = uiState.todayTasks,
                    key = { task -> task.id },
                ) { task ->
                    Column {
                        TaskCard(
                            task = task,
                            onClick = { onOpenTask(task.id) },
                        )
                        Row(
                            modifier = Modifier.padding(top = spacing.xSmall),
                            horizontalArrangement = Arrangement.spacedBy(spacing.small),
                        ) {
                            if (task.status == TaskStatus.TODO) {
                                TextButton(onClick = { viewModel.startTask(task.id) }) {
                                    Text(text = "Démarrer")
                                }
                            }
                            if (task.status != TaskStatus.DONE) {
                                TextButton(onClick = { viewModel.markTaskAsDone(task.id) }) {
                                    Text(text = "Terminer")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

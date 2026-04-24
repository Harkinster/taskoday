package com.example.taskoday.features.tasks.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.PlaceholderScreen
import com.example.taskoday.core.ui.testing.TaskodayTestTags
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.domain.model.TaskStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    viewModel: TaskDetailViewModel,
    onBack: () -> Unit,
    onEditTask: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Détail de la tâche") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    uiState.task?.let { task ->
                        IconButton(
                            onClick = { onEditTask(task.id) },
                            modifier = Modifier.testTag(TaskodayTestTags.TaskDetailEditButton),
                        ) {
                            Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Modifier la tâche")
                        }
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

        val task = uiState.task
        if (task == null) {
            PlaceholderScreen(
                title = "Tâche indisponible",
                description = uiState.errorMessage ?: "Cette tâche n’existe plus.",
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Text(text = task.title, style = MaterialTheme.typography.titleLarge)
            if (!task.description.isNullOrBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Text(
                text = "Échéance : ${DateTimeUtils.formatDate(task.dueDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Priorité : ${task.priority.label()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Statut : ${task.status.label()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(TaskodayTestTags.TaskDetailStatusText),
            )

            if (task.status == TaskStatus.TODO) {
                OutlinedButton(
                    onClick = { viewModel.updateStatus(TaskStatus.IN_PROGRESS) },
                    modifier = Modifier.padding(top = spacing.medium),
                ) {
                    Text(text = "Démarrer la tâche")
                }
            }

            OutlinedButton(
                onClick = {
                    if (task.status == TaskStatus.DONE) {
                        viewModel.updateStatus(TaskStatus.TODO)
                    } else {
                        viewModel.markTaskAsDone()
                    }
                },
                modifier =
                    Modifier
                        .padding(top = spacing.small)
                        .fillMaxWidth()
                        .testTag(TaskodayTestTags.TaskDetailMarkDoneButton),
            ) {
                Text(text = if (task.status == TaskStatus.DONE) "Rouvrir la tâche" else "Marquer comme terminée")
            }

            OutlinedButton(
                onClick = viewModel::deleteTask,
                modifier = Modifier.fillMaxWidth().testTag(TaskodayTestTags.TaskDetailDeleteButton),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.padding(end = spacing.small),
                )
                Text(text = "Supprimer la tâche")
            }
        }
    }
}

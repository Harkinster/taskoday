package com.example.taskoday.features.tasks.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.testing.TaskodayTestTags
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.domain.model.TaskPriority
import com.example.taskoday.domain.model.TaskStatus
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    viewModel: TaskEditViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            viewModel.consumeSaveCompletion()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = if (uiState.taskId == null) "Créer une tâche" else "Modifier la tâche")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::saveTask,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.testTag(TaskodayTestTags.TaskEditSaveButton),
                    ) {
                        Text(text = if (uiState.isSaving) "Enregistrement..." else "Enregistrer")
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

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChanged,
                label = { Text(text = "Titre") },
                modifier = Modifier.fillMaxWidth().testTag(TaskodayTestTags.TaskEditTitleField),
                singleLine = true,
                supportingText = {
                    uiState.errorMessage?.let { error ->
                        Text(text = error, color = MaterialTheme.colorScheme.error)
                    }
                },
            )
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChanged,
                label = { Text(text = "Description") },
                modifier = Modifier.fillMaxWidth().testTag(TaskodayTestTags.TaskEditDescriptionField),
                minLines = 3,
            )

            Text(text = "Priorité", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                TaskPriority.entries.forEach { priority ->
                    FilterChip(
                        selected = uiState.priority == priority,
                        onClick = { viewModel.onPriorityChanged(priority) },
                        label = { Text(text = priority.label()) },
                    )
                }
            }

            Text(text = "Statut", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                TaskStatus.entries.forEach { status ->
                    FilterChip(
                        selected = uiState.status == status,
                        onClick = { viewModel.onStatusChanged(status) },
                        label = { Text(text = status.label()) },
                    )
                }
            }

            Text(text = "Projet", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                uiState.projects.forEach { project ->
                    AssistChip(
                        onClick = { viewModel.onProjectChanged(project.id) },
                        label = { Text(text = project.name) },
                    )
                }
            }

            Text(text = "Date d’échéance", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                AssistChip(
                    onClick = { viewModel.onDueDateChanged(null) },
                    label = { Text(text = "Aucune") },
                )
                AssistChip(
                    onClick = {
                        viewModel.onDueDateChanged(
                            DateTimeUtils.epochMillisAtHour(LocalDate.now(), 18),
                        )
                    },
                    label = { Text(text = "Aujourd’hui 18:00") },
                )
                AssistChip(
                    onClick = {
                        viewModel.onDueDateChanged(
                            DateTimeUtils.epochMillisAtHour(LocalDate.now().plusDays(1), 10),
                        )
                    },
                    label = { Text(text = "Demain 10:00") },
                )
            }
            Text(
                text = "Sélectionnée : ${DateTimeUtils.formatDateTime(uiState.dueDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Tâche de routine", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = uiState.isRoutine,
                    onCheckedChange = viewModel::onRoutineChanged,
                )
            }
        }
    }
}

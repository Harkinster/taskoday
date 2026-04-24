package com.example.taskoday.features.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.PlaceholderScreen
import com.example.taskoday.core.ui.component.TaskCard
import com.example.taskoday.core.ui.testing.TaskodayTestTags
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.domain.model.TaskStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onTaskClick: (Long) -> Unit,
    onCreateTask: () -> Unit,
    onEditTask: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Tâches") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTask,
                modifier = Modifier.testTag(TaskodayTestTags.TasksAddFab),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = "Ajouter une tâche")
            }
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

        if (uiState.tasks.isEmpty() && !uiState.isLoading) {
            PlaceholderScreen(
                title = "Aucune tâche",
                description = "Créez votre première tâche pour organiser votre journée.",
                actionLabel = "Créer une tâche",
                onActionClick = onCreateTask,
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
            items(uiState.tasks, key = { task -> task.id }) { task ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    TaskCard(task = task, onClick = { onTaskClick(task.id) })
                    Row(
                        modifier = Modifier.padding(top = spacing.xSmall),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        IconButton(
                            onClick = { viewModel.markTaskAsDone(task.id) },
                            enabled = task.status != TaskStatus.DONE,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Marquer comme terminée",
                            )
                        }
                        IconButton(
                            onClick = { onEditTask(task.id) },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Modifier la tâche",
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteTask(task.id) },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Supprimer la tâche",
                            )
                        }
                    }
                }
            }
        }
    }
}

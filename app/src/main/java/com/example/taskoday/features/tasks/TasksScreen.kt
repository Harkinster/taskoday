package com.example.taskoday.features.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.MissionCard
import com.example.taskoday.core.ui.component.fantasy.NeonButton
import com.example.taskoday.core.ui.component.fantasy.NeonButtonStyle
import com.example.taskoday.core.ui.component.fantasy.NeonCard
import com.example.taskoday.core.ui.component.fantasy.NeonTone
import com.example.taskoday.core.ui.component.fantasy.ProgressHeroCard
import com.example.taskoday.core.ui.component.fantasy.TaskodayHeader
import com.example.taskoday.core.ui.testing.TaskodayTestTags
import com.example.taskoday.core.ui.theme.NeonBlue
import com.example.taskoday.core.ui.theme.NeonCyan
import com.example.taskoday.core.ui.theme.StarWhite
import com.example.taskoday.core.ui.theme.TextMuted
import com.example.taskoday.core.ui.theme.WarningGlow
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.domain.model.Task
import com.example.taskoday.domain.model.TaskStatus

@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    onTaskClick: (Long) -> Unit,
    onCreateTask: () -> Unit,
    onEditTask: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    val total = uiState.tasks.size
    val doneCount = uiState.tasks.count { it.status == TaskStatus.DONE }
    val progress = if (total == 0) 0f else doneCount.toFloat() / total.toFloat()

    val todoTasks = uiState.tasks.filter { it.status == TaskStatus.TODO }
    val inProgressTasks = uiState.tasks.filter { it.status == TaskStatus.IN_PROGRESS }
    val doneTasks = uiState.tasks.filter { it.status == TaskStatus.DONE }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateTask,
                containerColor = NeonBlue,
                contentColor = StarWhite,
                modifier = Modifier.testTag(TaskodayTestTags.TasksAddFab),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Ajouter une mission",
                )
            }
        },
    ) { innerPadding ->
        FantasyScreenBackground(modifier = Modifier.padding(innerPadding)) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = NeonCyan)
                }
                return@FantasyScreenBackground
            }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.medium),
                contentPadding = PaddingValues(top = spacing.large, bottom = spacing.xxLarge),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                item {
                    TaskodayHeader(
                        title = "Missions",
                        subtitle = "Des actions du quotidien, des victoires concretes.",
                        avatarInitials = "AB",
                    )
                }

                item {
                    ProgressHeroCard(
                        title = "A faire aujourd hui",
                        completed = doneCount,
                        total = total,
                        progress = progress,
                        subtitle = "Complete tes missions pour progresser.",
                        accent = NeonTone.Blue,
                    )
                }

                if (!uiState.errorMessage.isNullOrBlank()) {
                    item {
                        NeonCard(tone = NeonTone.Warning) {
                            Text(
                                text = uiState.errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarningGlow,
                            )
                            NeonButton(
                                text = "Fermer",
                                onClick = viewModel::clearError,
                                style = NeonButtonStyle.Outline,
                            )
                        }
                    }
                }

                if (uiState.tasks.isEmpty()) {
                    item {
                        NeonCard(tone = NeonTone.Violet) {
                            Text(
                                text = "Aucune mission active.",
                                style = MaterialTheme.typography.titleMedium,
                                color = StarWhite,
                            )
                            Text(
                                text = "Cree ta premiere mission pour lancer ton aventure.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                    }
                    return@LazyColumn
                }

                item {
                    MissionSection(
                        title = "A faire aujourd hui",
                        tasks = todoTasks,
                        canManageMissions = uiState.canManageMissions,
                        onTaskClick = onTaskClick,
                        onEditTask = onEditTask,
                        onDeleteTask = viewModel::deleteTask,
                        onMarkTaskDone = viewModel::markTaskAsDone,
                    )
                }

                item {
                    MissionSection(
                        title = "En cours",
                        tasks = inProgressTasks,
                        canManageMissions = uiState.canManageMissions,
                        onTaskClick = onTaskClick,
                        onEditTask = onEditTask,
                        onDeleteTask = viewModel::deleteTask,
                        onMarkTaskDone = viewModel::markTaskAsDone,
                    )
                }

                item {
                    MissionSection(
                        title = "Terminees",
                        tasks = doneTasks,
                        canManageMissions = uiState.canManageMissions,
                        onTaskClick = onTaskClick,
                        onEditTask = onEditTask,
                        onDeleteTask = viewModel::deleteTask,
                        onMarkTaskDone = viewModel::markTaskAsDone,
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionSection(
    title: String,
    tasks: List<Task>,
    canManageMissions: Boolean,
    onTaskClick: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onMarkTaskDone: (Long) -> Unit,
) {
    NeonCard(
        tone = NeonTone.Cyan,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "$title (${tasks.size})",
            style = MaterialTheme.typography.titleLarge,
            color = StarWhite,
        )

        if (tasks.isEmpty()) {
            Text(
                text = "Rien pour cette section.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            return@NeonCard
        }

        tasks.forEach { task ->
            val done = task.status == TaskStatus.DONE
            MissionCard(
                title = task.title,
                description = task.description,
                emoji = task.emoji,
                dueLabel = task.dueDate?.let { "Avant ${DateTimeUtils.formatTimeOnly(it)}" },
                statusLabel = task.status.label(),
                progress = progressFrom(task.status),
                completionLabel = completionLabelFrom(task.status),
                done = done,
                tone = toneFrom(task.status),
                onClick = { onTaskClick(task.id) },
                onToggleDone = { if (!done) onMarkTaskDone(task.id) },
                onEdit = if (canManageMissions) ({ onEditTask(task.id) }) else null,
                onDelete = if (canManageMissions) ({ onDeleteTask(task.id) }) else null,
            )
        }
    }
}

private fun toneFrom(status: TaskStatus): NeonTone =
    when (status) {
        TaskStatus.TODO -> NeonTone.Warning
        TaskStatus.IN_PROGRESS -> NeonTone.Blue
        TaskStatus.DONE -> NeonTone.Success
    }

private fun progressFrom(status: TaskStatus): Float =
    when (status) {
        TaskStatus.TODO -> 0.1f
        TaskStatus.IN_PROGRESS -> 0.6f
        TaskStatus.DONE -> 1f
    }

private fun completionLabelFrom(status: TaskStatus): String =
    when (status) {
        TaskStatus.TODO -> "0/1"
        TaskStatus.IN_PROGRESS -> "En cours"
        TaskStatus.DONE -> "1/1"
    }

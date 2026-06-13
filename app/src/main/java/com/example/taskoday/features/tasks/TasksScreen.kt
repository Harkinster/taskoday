package com.example.taskoday.features.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.FantasyConfirmationDialog
import com.example.taskoday.core.ui.component.fantasy.MissionCard
import com.example.taskoday.core.ui.component.fantasy.NeonButton
import com.example.taskoday.core.ui.component.fantasy.NeonButtonStyle
import com.example.taskoday.core.ui.component.fantasy.NeonCard
import com.example.taskoday.core.ui.component.fantasy.NeonTone
import com.example.taskoday.core.ui.component.fantasy.ProgressHeroCard
import com.example.taskoday.core.ui.component.fantasy.TaskodayTopBar
import com.example.taskoday.core.ui.theme.NeonCyan
import com.example.taskoday.core.ui.theme.SoftGold
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
    onEditTask: (Long) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    val total = uiState.tasks.size
    val doneCount = uiState.tasks.count { it.status == TaskStatus.DONE }
    val progress = if (total == 0) 0f else doneCount.toFloat() / total.toFloat()

    val todoTasks = uiState.tasks.filter { it.status == TaskStatus.TODO }
    val inProgressTasks = uiState.tasks.filter { it.status == TaskStatus.IN_PROGRESS }
    val doneTasks = uiState.tasks.filter { it.status == TaskStatus.DONE }
    var pendingDeleteTask by remember { mutableStateOf<Task?>(null) }

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
                contentPadding = PaddingValues(top = spacing.large, bottom = 148.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                item {
                    TaskodayTopBar(
                        avatarInitials = "AB",
                        compact = true,
                        showNotification = false,
                        onAvatarClick = onOpenProfile,
                    )
                }

                item {
                    ProgressHeroCard(
                        title = "À faire aujourd'hui",
                        completed = doneCount,
                        total = total,
                        progress = progress,
                        subtitle = "Complète tes missions pour progresser.",
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
                                text = "Aucune mission pour le moment.",
                                style = MaterialTheme.typography.titleMedium,
                                color = StarWhite,
                            )
                            Text(
                                text = "Crée ta première mission pour lancer ton aventure.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                    }
                    return@LazyColumn
                }

                if (todoTasks.isNotEmpty()) {
                    item {
                        MissionSection(
                        title = "À faire aujourd'hui",
                        tasks = todoTasks,
                        manageableTaskIds = uiState.manageableTaskIds,
                        onTaskClick = onTaskClick,
                        onEditTask = onEditTask,
                        onDeleteTask = { pendingDeleteTask = it },
                        onMarkTaskDone = viewModel::markTaskAsDone,
                        )
                    }
                }

                if (inProgressTasks.isNotEmpty()) {
                    item {
                        MissionSection(
                        title = "En cours",
                        tasks = inProgressTasks,
                        manageableTaskIds = uiState.manageableTaskIds,
                        onTaskClick = onTaskClick,
                        onEditTask = onEditTask,
                        onDeleteTask = { pendingDeleteTask = it },
                        onMarkTaskDone = viewModel::markTaskAsDone,
                        )
                    }
                }

                if (doneTasks.isNotEmpty()) {
                    item {
                        MissionSection(
                        title = "Terminées",
                        tasks = doneTasks,
                        manageableTaskIds = uiState.manageableTaskIds,
                        onTaskClick = onTaskClick,
                        onEditTask = onEditTask,
                        onDeleteTask = { pendingDeleteTask = it },
                        onMarkTaskDone = viewModel::markTaskAsDone,
                        )
                    }
                }
            }
        }
    }

    pendingDeleteTask?.let { task ->
        FantasyConfirmationDialog(
            title = "Supprimer la mission",
            message = "Supprimer « ${task.title} » ? Cette action est définitive.",
            confirmLabel = "Supprimer",
            onDismiss = { pendingDeleteTask = null },
            onConfirm = {
                pendingDeleteTask = null
                viewModel.deleteTask(task.id)
            },
        )
    }
}

@Composable
private fun MissionSection(
    title: String,
    tasks: List<Task>,
    manageableTaskIds: Set<Long>,
    onTaskClick: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onMarkTaskDone: (Long) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Text(
            text = "$title (${tasks.size})",
            style = MaterialTheme.typography.titleMedium,
            color = SoftGold,
        )

        if (tasks.isEmpty()) {
            Text(
                text = "Rien pour cette section.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            return@Column
        }

        tasks.forEach { task ->
            val done = task.status == TaskStatus.DONE
            val canManageTask = manageableTaskIds.contains(task.id)
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
                onEdit = if (canManageTask) ({ onEditTask(task.id) }) else null,
                onDelete = if (canManageTask) ({ onDeleteTask(task) }) else null,
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

package com.example.taskoday.features.familyhome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.theme.DangerGlow
import com.example.taskoday.core.ui.theme.InkBrown
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.ParchmentCream
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.SoftRed
import com.example.taskoday.core.ui.theme.WoodBrown
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.domain.model.FamilyTaskTodayItem

@Composable
fun FamilyTaskDetailScreen(
    viewModel: FamilyTaskDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) {
            onDeleted()
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
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SoftGold)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = spacing.medium),
                        contentPadding = PaddingValues(top = spacing.large, bottom = spacing.xxLarge),
                        verticalArrangement = Arrangement.spacedBy(spacing.medium),
                    ) {
                        item {
                            FamilyTaskDetailHeader(onBack = onBack)
                        }

                        uiState.task?.let { task ->
                            item {
                                FamilyTaskDefinitionCard(task = task, todayOccurrence = uiState.todayOccurrence)
                            }

                            item {
                                FamilyTaskDetailActions(
                                    task = task,
                                    isDeleting = uiState.isDeleting,
                                    onEdit = onEdit,
                                    onDelete = viewModel::requestDelete,
                                )
                            }
                        }

                        uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
                            item {
                                ErrorCard(error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showDeleteConfirmation) {
        DeleteFamilyTaskDialog(
            isDeleting = uiState.isDeleting,
            onConfirm = viewModel::deleteTask,
            onDismiss = viewModel::dismissDelete,
        )
    }
}

@Composable
private fun FamilyTaskDetailHeader(onBack: () -> Unit) {
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Gérer la tâche",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBrown,
                )
                Text(
                    text = "Consulte et ajuste la définition familiale.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                )
            }
            Icon(Icons.Outlined.Home, contentDescription = null, tint = WoodBrown, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun FamilyTaskDefinitionCard(
    task: FamilyTaskDefinition,
    todayOccurrence: FamilyTaskTodayItem?,
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
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBrown,
                )
                Text(
                    text = task.description ?: "Aucune description.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                )
            }

            DetailLine(label = "Assignation", value = task.assignees.assignmentLabel())
            DetailLine(label = "Date", value = task.dateLabel())
            DetailLine(label = "Heure", value = task.timeLabel())
            DetailLine(label = "Priorité", value = familyTaskPriorityFormLabel(task.priority))
            DetailLine(label = "Récurrence", value = familyTaskRecurrenceSummary(task.recurrence, task.selectedWeekdays, task.recurrenceInterval))
            DetailLine(label = "Validation parent", value = task.validationRequired.yesNoLabel())
            DetailLine(label = "Gamification", value = task.gamificationEnabled.yesNoLabel())
            DetailLine(
                label = "Occurrence du jour",
                value = todayOccurrence?.let { familyTaskStatusLabel(it.status) } ?: "Pas d'occurrence aujourd'hui",
            )
        }
    }
}

@Composable
private fun FamilyTaskDetailActions(
    task: FamilyTaskDefinition,
    isDeleting: Boolean,
    onEdit: (Long) -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = ParchmentLight.copy(alpha = 0.97f),
                contentColor = InkBrown,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = { onEdit(task.id) },
                enabled = !isDeleting,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WoodBrown, contentColor = ParchmentLight),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Modifier")
            }
            OutlinedButton(
                onClick = onDelete,
                enabled = !isDeleting,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = SoftRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isDeleting) "Retrait..." else "Désactiver / Supprimer", color = SoftRed)
            }
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            modifier = Modifier.weight(0.9f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = InkBrown,
            modifier = Modifier.weight(1.1f),
        )
    }
}

@Composable
private fun DeleteFamilyTaskDialog(
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Retirer cette tâche ?") },
        text = { Text("Retirer cette tâche de l'organisation familiale ?") },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                Text(if (isDeleting) "Retrait..." else "Retirer", color = DangerGlow)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text("Annuler")
            }
        },
    )
}

@Composable
private fun ErrorCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = ParchmentLight.copy(alpha = 0.98f),
        contentColor = DangerGlow,
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun List<FamilyTaskAssignee>.assignmentLabel(): String =
    if (isEmpty()) {
        "Maison"
    } else {
        joinToString { assignee -> assignee.displayName }
    }

private fun FamilyTaskDefinition.dateLabel(): String =
    familyTaskDateFromFields(dueDate = dueDate, dueAt = dueAt)
        ?.let { date -> formatFamilyTaskDateLabel(date) }
        ?: "Aucune date"

private fun FamilyTaskDefinition.timeLabel(): String =
    familyTaskTimeFromFields(
        hasDueTime = hasDueTime,
        dueTime = dueTime,
        dueAt = dueAt,
    )
        .takeIf { it.isNotBlank() }
        ?.let { time -> formatFamilyTaskTimeLabel(time) }
        ?: "Sans heure"

private fun Boolean.yesNoLabel(): String = if (this) "Oui" else "Non"

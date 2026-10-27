package com.example.taskoday.features.familyhome

import android.app.DatePickerDialog
import com.example.taskoday.R
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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import com.example.taskoday.core.ui.component.forms.FantasyFormFilledButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.example.taskoday.core.ui.component.forms.FantasyFormOutlineButton as OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
import com.example.taskoday.core.ui.component.forms.*
import java.time.LocalDate

@Composable
fun FamilyTaskDetailScreen(
    viewModel: FamilyTaskDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDuplicate: (FamilyTaskDefinition) -> Unit,
    onDeleted: () -> Unit,
) = FantasyFormTheme { FamilyTaskDetailScreenContent(viewModel, onBack, onEdit, onDuplicate, onDeleted) }

@Composable
private fun FamilyTaskDetailScreenContent(
    viewModel: FamilyTaskDetailViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDuplicate: (FamilyTaskDefinition) -> Unit,
    onDeleted: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    val context = LocalContext.current
    var showFailMissionConfirmation by rememberSaveable { mutableStateOf(false) }

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

                            if (uiState.canCompleteToday) item {
                                Button(
                                    onClick = viewModel::completeTodayOccurrence,
                                    enabled = !uiState.isValidating,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(if (uiState.isValidating) "Mise à jour..." else "Terminer")
                                }
                            }

                            if (uiState.canValidateToday) item {
                                Button(
                                    onClick = viewModel::validateTodayOccurrence,
                                    enabled = !uiState.isValidating,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(if (uiState.isValidating) "Validation..." else "Valider")
                                }
                            }

                            if (uiState.canManageOverdueMission) item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = {
                                            val nextDay = LocalDate.now().plusDays(1)
                                            DatePickerDialog(
                                                context, R.style.Theme_Taskoday_FormPicker,
                                                { _, year, month, day -> viewModel.rescheduleMission(LocalDate.of(year, month + 1, day).toString()) },
                                                nextDay.year,
                                                nextDay.monthValue - 1,
                                                nextDay.dayOfMonth,
                                            ).apply { datePicker.minDate = System.currentTimeMillis() }.show()
                                        },
                                        enabled = !uiState.isValidating,
                                        modifier = Modifier.weight(1f),
                                    ) { Text("Reporter") }
                                    Button(
                                        onClick = { showFailMissionConfirmation = true },
                                        enabled = !uiState.isValidating,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = ParchmentLight),
                                        modifier = Modifier.weight(1f),
                                    ) { Text("Ratée") }
                                }
                            }

                            if (uiState.access.canManage) item {
                                FamilyTaskDetailActions(
                                    task = task,
                                    isDeleting = uiState.isDeleting,
                                    onEdit = onEdit,
                                    onDuplicate = onDuplicate,
                                    onDelete = viewModel::requestDelete,
                                )
                            }
                        }

                        uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
                            item {
                                ErrorCard(error)
                            }
                        }
                        uiState.successMessage?.takeIf { it.isNotBlank() }?.let { message ->
                            item { Text(message, color = ParchmentLight) }
                        }
                    }
                }
            }
        }
    }

    if (showFailMissionConfirmation) {
        AlertDialog(
            onDismissRequest = { showFailMissionConfirmation = false },
            title = { Text("Marquer cette Mission comme ratée ?") },
            text = { Text("Cette Mission sera clôturée comme ratée.") },
            confirmButton = {
                TextButton(onClick = {
                    showFailMissionConfirmation = false
                    viewModel.failMission()
                }) { Text("Marquer ratée") }
            },
            dismissButton = { TextButton(onClick = { showFailMissionConfirmation = false }) { Text("Annuler") } },
        )
    }

    if (uiState.access.canManage && uiState.showDeleteConfirmation) {
        DeleteFamilyTaskDialog(
            isDeleting = uiState.isDeleting,
            onConfirm = viewModel::deleteTask,
            onDismiss = viewModel::dismissDelete,
        )
    }
}

@Composable
private fun FamilyTaskDetailHeader(onBack: () -> Unit) {
    FantasyFormHeader("Détail de l’action", "Les informations utiles pour passer à l’action.", onBack)
}

@Composable
private fun FamilyTaskDefinitionCard(
    task: FamilyTaskDefinition,
    todayOccurrence: FamilyTaskTodayItem?,
) {
    FantasyFormPanel() {
        Column(
            modifier = Modifier,
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
            DetailLine(label = "Répétition", value = familyTaskRecurrenceSummary(task.recurrence, task.selectedWeekdays, task.recurrenceInterval))
            DetailLine(label = "Validation parent", value = task.validationRequired.yesNoLabel())
            DetailLine(label = "Gamification", value = task.gamificationEnabled.yesNoLabel())
            DetailLine(
                label = "État de l’action",
                value = todayOccurrence?.let { familyTaskStatusLabel(it.status) } ?: "Aucune occurrence active",
            )
        }
    }
}

@Composable
private fun FamilyTaskDetailActions(
    task: FamilyTaskDefinition,
    isDeleting: Boolean,
    onEdit: (Long) -> Unit,
    onDuplicate: (FamilyTaskDefinition) -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    FantasyFormPanel() {
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(modifier = Modifier.align(Alignment.End)) {
                IconButton(onClick = { menuExpanded = true }, enabled = !isDeleting) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Actions de la tâche")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Dupliquer") }, onClick = {
                        menuExpanded = false
                        onDuplicate(task)
                    })
                }
            }
            Button(
                onClick = { onEdit(task.id) },
                enabled = !isDeleting,
                shape = RoundedCornerShape(8.dp),
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
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isDeleting) "Retrait..." else "Désactiver / Supprimer", color = MaterialTheme.colorScheme.error)
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
                Text(if (isDeleting) "Retrait..." else "Retirer", color = MaterialTheme.colorScheme.error)
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
        contentColor = MaterialTheme.colorScheme.error,
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

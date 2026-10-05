package com.example.taskoday.features.exploration

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.theme.DangerGlow
import com.example.taskoday.core.ui.theme.ParchmentCream
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.features.familyhome.familyTaskStatusLabel
import com.example.taskoday.features.familyhome.familyTaskActionLabel

@Composable
fun ExplorationScreen(
    viewModel: ExplorationViewModel,
    onOpenJournal: () -> Unit = {},
    onOpenFamilyTask: (Long) -> Unit = {},
    onCreatePersonal: (FamilyActionType) -> Unit = {},
    openCreateChoices: Boolean = false,
    onCreateChoicesOpened: () -> Unit = {},
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateChoices by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(openCreateChoices) {
        if (openCreateChoices) {
            showCreateChoices = true
            onCreateChoicesOpened()
        }
    }
    val actions = explorationDayActions(state, viewModel)
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 28.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Exploration", style = MaterialTheme.typography.headlineSmall, color = ParchmentLight)
                    Text("Mon parcours · ${state.dateLabel}", color = ParchmentCream)
                }
                Row {
                    TextButton(onClick = onOpenJournal) { Text("Journal") }
                    if (state.access.canManage) TextButton(onClick = { showCreateChoices = !showCreateChoices }) { Text("Ajouter") }
                }
            }
        }
        if (showCreateChoices && state.access.canManage) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { showCreateChoices = false; onCreatePersonal(FamilyActionType.PERSONAL_ROUTINE) }) { Text("Routine") }
                    TextButton(onClick = { showCreateChoices = false; onCreatePersonal(FamilyActionType.PERSONAL_MISSION) }) { Text("Mission") }
                }
            }
        }
        if (state.isLoading) {
            item { CircularProgressIndicator() }
        } else {
        if (!state.errorMessage.isNullOrBlank()) item { Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error) }
        val overdue = actions.filter { it.overdue && !it.completed }
        val today = actions.filter { !it.overdue && !it.completed }
        val completed = actions.filter { it.completed }
        if (overdue.isNotEmpty()) {
            item { SectionTitle("En retard") }
            items(overdue, key = { "overdue-${it.key}" }) { action -> ActionRow(action, onOpenFamilyTask) }
        }
        item { SectionTitle("Aujourd'hui") }
        if (today.isEmpty()) item { Text("Rien à faire aujourd'hui.", color = ParchmentCream) }
        items(today, key = { "today-${it.key}" }) { action -> ActionRow(action, onOpenFamilyTask) }
        if (completed.isNotEmpty()) {
            item { SectionTitle("Terminées") }
            items(completed, key = { "done-${it.key}" }) { action -> ActionRow(action, onOpenFamilyTask) }
        }
        }
    }
}

private data class ExplorationDayAction(
    val key: String,
    val title: String,
    val kind: String,
    val status: String,
    val completed: Boolean,
    val overdue: Boolean,
    val taskId: Long? = null,
    val actionLabel: String?,
    val onToggle: () -> Unit,
)

private fun explorationDayActions(state: ExplorationUiState, viewModel: ExplorationViewModel): List<ExplorationDayAction> =
    buildList {
        state.personalTasks.forEach { item ->
            val task = item.occurrence
            add(ExplorationDayAction("mission-${task.occurrenceId}", task.title, "Mission", familyTaskStatusLabel(task.status), task.status.countsAsDone, item.overdue,
                task.taskId, state.access.quickAction(task)?.let(::familyTaskActionLabel), { viewModel.toggleFamilyTask(item) }))
        }
        state.routines.forEach { item ->
            val task = item.familyTask?.occurrence
            add(ExplorationDayAction("routine-${task?.occurrenceId ?: item.legacyTask?.task?.id}", item.title, "Routine",
                task?.let { familyTaskStatusLabel(it.status) } ?: if (item.completed) "Terminée" else "À faire",
                item.completed, item.familyTask?.overdue == true, task?.taskId,
                task?.let { state.access.quickAction(it)?.let(::familyTaskActionLabel) }
                    ?: if (task == null && (state.access.canManage || !item.completed)) (if (item.completed) "Rouvrir" else "Terminer") else null,
                { viewModel.toggleRoutineItem(item) }))
        }
        state.missions.forEach { task ->
            add(ExplorationDayAction("legacy-mission-${task.task.id}", task.task.title, "Mission", if (task.isCompleted) "Terminée" else "À faire",
                task.isCompleted, false, null, if (state.access.canManage || !task.isCompleted) (if (task.isCompleted) "Rouvrir" else "Terminer") else null,
                { viewModel.toggleRoutineItem(ExplorationRoutineItem(task.task.title, "", task.isCompleted, legacyTask = task)) }))
        }
    }

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = ParchmentLight, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ActionRow(action: ExplorationDayAction, onOpenFamilyTask: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f).clickable(enabled = action.taskId != null) { action.taskId?.let(onOpenFamilyTask) }) {
            Text(action.title, color = ParchmentLight)
            Text("${action.kind} · ${action.status}", color = if (action.overdue) DangerGlow else ParchmentCream, style = MaterialTheme.typography.bodySmall)
        }
        action.actionLabel?.let { label -> TextButton(onClick = action.onToggle) { Text(label) } }
    }
}

package com.example.taskoday.features.exploration

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.example.taskoday.features.taskodayv4.*
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.FantasyButton
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
import com.example.taskoday.features.familyhome.isOpenUndatedMission

@Composable
fun ExplorationScreen(
    viewModel: ExplorationViewModel,
    onOpenJournal: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenFamilyTask: (Long) -> Unit = {},
    onCreatePersonal: (FamilyActionType) -> Unit = {},
    openCreateChoices: Boolean = false,
    onCreateChoicesOpened: () -> Unit = {},
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenResumed() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateChoices by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(openCreateChoices) {
        if (openCreateChoices) {
            showCreateChoices = true
            onCreateChoicesOpened()
        }
    }
    val actions = explorationDayActions(state, viewModel)
    FantasyScreenBackground {
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 28.dp),
    ) {
        item { FantasyPageHeader("Exploration", "Mon parcours · ${state.selectedMemberName}", onOpenProfile) }
        item {
            FantasySummaryPanel {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mon aventure du jour", color = JourneyGold, style = MaterialTheme.typography.titleMedium)
                    Text(if (state.isLoading) "Préparation de ton parcours…" else "${actions.count { it.completed }} / ${actions.size} actions accomplies",
                        color = JourneyParchment, style = MaterialTheme.typography.titleLarge)
                    Text("Mes missions et mes routines personnelles", color = JourneyParchment, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FantasyFramedButton(label = "Mon journal", onClick = onOpenJournal, modifier = Modifier.weight(1f))
                if (state.access.canManage) FantasyFramedButton(label = "Ajouter", onClick = { showCreateChoices = !showCreateChoices }, modifier = Modifier.weight(1f))
            }
        }
        if (showCreateChoices && state.access.canManage) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FantasyFramedButton(label = "Routine", onClick = { showCreateChoices = false; onCreatePersonal(FamilyActionType.PERSONAL_ROUTINE) }, modifier = Modifier.weight(1f))
                    FantasyFramedButton(label = "Mission", onClick = { showCreateChoices = false; onCreatePersonal(FamilyActionType.PERSONAL_MISSION) }, modifier = Modifier.weight(1f))
                }
            }
        }
        if (state.isLoading) {
            item { CircularProgressIndicator() }
        } else {
        if (!state.errorMessage.isNullOrBlank()) item { Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error) }
        if (!state.userMessage.isNullOrBlank()) item { Text(state.userMessage.orEmpty(), color = ParchmentLight) }
        val overdue = actions.filter { it.overdue && !it.completed }
        val today = actions.filter { !it.overdue && !it.completed && !it.undated }
        val undated = actions.filter { it.undated && !it.completed }
        val completed = actions.filter { it.completed }
        if (overdue.isNotEmpty()) {
            item { SectionTitle("En retard") }
            items(overdue, key = { "overdue-${it.key}" }) { action -> ActionRow(action, onOpenFamilyTask, state.actingKey != null) }
        }
        item { SectionTitle("Aujourd'hui") }
        if (today.isEmpty()) item {
            FantasyParchmentCard {
                Text("Une journée à ton rythme", color = JourneyInk, style = MaterialTheme.typography.titleMedium)
                Text("Rien à faire aujourd'hui. Tes missions et routines apparaîtront ici.", color = JourneyMuted)
            }
        }
        items(today, key = { "today-${it.key}" }) { action -> ActionRow(action, onOpenFamilyTask, state.actingKey != null) }
        if (undated.isNotEmpty()) {
            item { SectionTitle("À faire") }
            items(undated, key = { "undated-${it.key}" }) { action -> ActionRow(action, onOpenFamilyTask, state.actingKey != null) }
        }
        if (completed.isNotEmpty()) {
            item { SectionTitle("Terminées") }
            items(completed, key = { "done-${it.key}" }) { action -> ActionRow(action, onOpenFamilyTask, state.actingKey != null) }
        }
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
    val undated: Boolean = false,
)

private fun explorationDayActions(state: ExplorationUiState, viewModel: ExplorationViewModel): List<ExplorationDayAction> =
    buildList {
        state.personalTasks.forEach { item ->
            val task = item.occurrence
            add(ExplorationDayAction("mission-${task.occurrenceId}", task.title, "Mission", familyTaskStatusLabel(task.status), task.status.countsAsDone, item.overdue,
                task.taskId, state.access.quickAction(task)?.let(::familyTaskActionLabel), { viewModel.toggleFamilyTask(item) }, task.isOpenUndatedMission()))
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
    FantasySectionHeading(text)
}

@Composable
private fun ActionRow(action: ExplorationDayAction, onOpenFamilyTask: (Long) -> Unit, isActing: Boolean) {
    FantasyParchmentCard {
        Column(Modifier.fillMaxWidth().clickable(enabled = action.taskId != null) { action.taskId?.let(onOpenFamilyTask) }) {
            Text(action.title, color = JourneyInk, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("${action.kind} · ${action.status}", color = if (action.overdue) JourneyDangerInk else JourneyMuted, style = MaterialTheme.typography.bodySmall)
        }
        action.actionLabel?.let { label ->
            FantasyButton(text = label, onClick = action.onToggle,
                modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp), enabled = !isActing)
        }
    }
}

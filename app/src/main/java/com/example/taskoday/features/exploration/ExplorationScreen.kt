package com.example.taskoday.features.exploration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.theme.DangerGlow
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.features.familyhome.familyTaskDueLabel

@Composable
fun ExplorationScreen(viewModel: ExplorationViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 28.dp),
    ) {
        item { Text("Exploration", style = MaterialTheme.typography.headlineSmall, color = ParchmentLight) }
        item { Text("Aujourd'hui · ${state.dateLabel}", style = MaterialTheme.typography.bodyMedium, color = InkMuted) }
        if (state.members.size > 1) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.members.forEach { member ->
                        FilterChip(selected = member.id == state.selectedMemberId, onClick = { viewModel.selectMember(member.id) }, label = { Text(member.displayName, color = ParchmentLight) })
                    }
                }
            }
        }
        if (!state.errorMessage.isNullOrBlank()) item { Text(state.errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error) }
        item { ExplorationSectionTitle("À faire pour ${state.selectedMemberName}") }
        if (state.personalTasks.isEmpty()) item { EmptyLine("Rien à faire pour l'instant.") }
        items(state.personalTasks, key = { "personal-${it.occurrence.occurrenceId}" }) { item ->
            ExplorationFamilyTaskRow(item, state.actingKey == "family-${item.occurrence.occurrenceId}", viewModel::toggleFamilyTask)
        }
        items(state.missions, key = { "mission-${it.task.id}" }) { item ->
            val display = ExplorationRoutineItem(item.task.title, item.task.dayPart.name.lowercase().replace('_', ' '), item.isCompleted, legacyTask = item)
            ExplorationRoutineRow(display, state.actingKey == "task-${item.task.id}", viewModel::toggleRoutineItem)
        }
        item { ExplorationSectionTitle("Mes routines") }
        if (state.routines.isEmpty()) item { EmptyLine("Aucune routine prévue aujourd'hui.") }
        items(state.routines, key = { item -> "routine-${item.title}-${item.familyTask?.occurrence?.occurrenceId ?: item.legacyTask?.task?.id}" }) { item ->
            val key = item.familyTask?.let { "family-${it.occurrence.occurrenceId}" } ?: item.legacyTask?.let { "task-${it.task.id}" }
            ExplorationRoutineRow(item, state.actingKey == key, viewModel::toggleRoutineItem)
        }
        item { ExplorationSectionTitle("À faire dans la maison") }
        if (state.houseTasks.isEmpty()) item { EmptyLine("Rien à faire dans la maison aujourd'hui.") }
        items(state.houseTasks, key = { "house-${it.occurrence.occurrenceId}" }) { item -> ExplorationFamilyTaskRow(item, state.actingKey == "family-${item.occurrence.occurrenceId}", viewModel::toggleFamilyTask) }
        if (state.objectives.isNotEmpty()) {
            item { ExplorationSectionTitle("Objectifs") }
            items(state.objectives, key = { "quest-${it.quest.id}" }) { quest -> Text("• ${quest.quest.title}${if (quest.isCompletedForDay) " · Terminée" else ""}", color = ParchmentLight) }
        }
    }
}

@Composable private fun ExplorationSectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleMedium, color = ParchmentLight, modifier = Modifier.padding(top = 8.dp)) }
@Composable private fun EmptyLine(text: String) { Text(text, style = MaterialTheme.typography.bodyMedium, color = InkMuted) }

@Composable private fun ExplorationFamilyTaskRow(item: ExplorationTask, acting: Boolean, onToggle: (ExplorationTask) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(item.occurrence.title, color = ParchmentLight)
            Text((if (item.overdue) "En retard · " else "") + (familyTaskDueLabel(item.occurrence.dueDate, item.occurrence.dueTime, item.occurrence.hasDueTime, item.occurrence.dueAt) ?: "Aujourd'hui"), style = MaterialTheme.typography.bodySmall, color = if (item.overdue) DangerGlow else InkMuted)
        }
        TextButton(enabled = !acting, onClick = { onToggle(item) }) { Text(if (item.occurrence.status.countsAsDone) "Rouvrir" else "Terminer") }
    }
}

@Composable private fun ExplorationRoutineRow(item: ExplorationRoutineItem, acting: Boolean, onToggle: (ExplorationRoutineItem) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) { Text(item.title, color = ParchmentLight); Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = InkMuted) }
        TextButton(enabled = !acting, onClick = { onToggle(item) }) { Text(if (item.completed) "Rouvrir" else "Terminer") }
    }
}

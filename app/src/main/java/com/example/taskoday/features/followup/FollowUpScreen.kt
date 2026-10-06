package com.example.taskoday.features.followup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import com.example.taskoday.core.ui.theme.DangerGlow
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.ParchmentCream
import com.example.taskoday.core.ui.component.DarkSurfaceFilterChip
import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.features.familyhome.familyTaskStatusLabel

@Composable
fun FollowUpScreen(
    viewModel: FollowUpViewModel,
    onOpenJournal: () -> Unit,
    onOpenFamilyTask: (Long) -> Unit,
    onOpenLegacyTask: (Long) -> Unit,
    onCreatePersonal: (FamilyActionType, Long) -> Unit = { _, _ -> },
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selected = state.members.firstOrNull { it.memberId == state.selectedMemberId }
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 28.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Suivi", style = MaterialTheme.typography.headlineSmall, color = ParchmentLight)
                TextButton(onClick = onOpenJournal) { Text("Journal") }
            }
        }
        item { Text("Aujourd’hui · ${java.time.LocalDate.now()}", style = MaterialTheme.typography.bodyMedium, color = ParchmentCream.copy(alpha = 0.72f)) }
        if (state.members.size > 1) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.members, key = { it.memberId }) { member ->
                        DarkSurfaceFilterChip(selected = member.memberId == state.selectedMemberId, onClick = { viewModel.selectMember(member.memberId) }, label = member.displayName)
                    }
                }
            }
        }
        state.errorMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        item { Text("Membres du foyer", style = MaterialTheme.typography.titleMedium, color = ParchmentLight) }
        items(state.members, key = { "member-${it.memberId}" }) { member -> MemberSummaryCard(member, member.memberId == state.selectedMemberId) { viewModel.selectMember(member.memberId) } }
        state.house?.let { house ->
            item { Text("Actions Maison", style = MaterialTheme.typography.titleMedium, color = ParchmentLight) }
            item { MemberSummaryCard(house, false) {} }
            items(house.items, key = { "house-${it.key}" }) { item -> FollowUpItemRow(item, onOpenFamilyTask, onOpenLegacyTask) }
        }
        selected?.let { member ->
            item {
                Column {
                    Text("Détail · ${member.displayName}", style = MaterialTheme.typography.titleMedium, color = ParchmentLight)
                    Row {
                        TextButton(onClick = { onCreatePersonal(FamilyActionType.PERSONAL_ROUTINE, member.memberId) }) { Text("Ajouter une routine") }
                        TextButton(onClick = { onCreatePersonal(FamilyActionType.PERSONAL_MISSION, member.memberId) }) { Text("Ajouter une mission") }
                    }
                }
            }
            if (member.items.isEmpty()) item { Text("Aucune tâche prévue aujourd’hui.", color = ParchmentCream.copy(alpha = 0.72f)) }
            val overdue = member.items.filter { it.overdue }
            val pending = member.items.filter { !it.completed && !it.overdue && !it.undated }
            val undated = member.items.filter { it.undated && !it.completed }
            val completed = member.items.filter { it.completed }
            if (pending.isNotEmpty()) {
                item { Text("Aujourd'hui", style = MaterialTheme.typography.titleSmall, color = ParchmentLight) }
                items(pending, key = { "pending-${it.key}" }) { item -> FollowUpItemRow(item, onOpenFamilyTask, onOpenLegacyTask) }
            }
            if (undated.isNotEmpty()) {
                item { Text("À faire", style = MaterialTheme.typography.titleSmall, color = ParchmentLight) }
                items(undated, key = { "undated-${it.key}" }) { item -> FollowUpItemRow(item, onOpenFamilyTask, onOpenLegacyTask) }
            }
            if (completed.isNotEmpty()) {
                item { Text("Terminées", style = MaterialTheme.typography.titleSmall, color = ParchmentLight) }
                items(completed, key = { "done-${it.key}" }) { item -> FollowUpItemRow(item, onOpenFamilyTask, onOpenLegacyTask) }
            }
            if (overdue.isNotEmpty()) {
                item { Text("En retard", style = MaterialTheme.typography.titleSmall, color = ParchmentLight) }
                items(overdue, key = { "overdue-${it.key}" }) { item -> FollowUpItemRow(item, onOpenFamilyTask, onOpenLegacyTask) }
            }
        }
    }
}

@Composable
private fun MemberSummaryCard(summary: FollowUpMemberSummary, selected: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = ParchmentLight.copy(alpha = if (selected) .98f else .90f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(summary.displayName, style = MaterialTheme.typography.titleMedium)
            Text("${summary.completed} / ${summary.total} terminées · ${summary.remaining} restantes", color = InkMuted)
            if (summary.overdue > 0) Text("${summary.overdue} en retard", color = DangerGlow)
            if (summary.total == 0) Text("Aucune tâche prévue aujourd’hui.", color = InkMuted)
        }
    }
}

@Composable
private fun FollowUpItemRow(item: FollowUpItem, onOpenFamilyTask: (Long) -> Unit, onOpenLegacyTask: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable {
        item.familyTask?.let { onOpenFamilyTask(it.taskId) }
        item.legacyTask?.let { onOpenLegacyTask(it.task.id) }
    }.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(item.title, color = ParchmentLight)
            item.familyTask?.let { task ->
                Text(task.followUpIdentityLabel(), color = ParchmentCream, style = MaterialTheme.typography.bodySmall)
                familyTaskCompletionActorLabels(task).forEach { actorLabel ->
                    Text(actorLabel, color = ParchmentCream.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(when { item.overdue -> "En retard"; item.familyTask != null -> familyTaskStatusLabel(item.familyTask.status); item.completed -> "Terminée"; else -> "À faire" }, color = if (item.overdue) DangerGlow else ParchmentCream.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

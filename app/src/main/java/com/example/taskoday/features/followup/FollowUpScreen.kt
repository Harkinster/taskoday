package com.example.taskoday.features.followup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.theme.DangerGlow
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.ParchmentLight

@Composable
fun FollowUpScreen(
    viewModel: FollowUpViewModel,
    onOpenFamilyTask: (Long) -> Unit,
    onOpenLegacyTask: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selected = state.members.firstOrNull { it.memberId == state.selectedMemberId }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 28.dp),
    ) {
        item { Text("Suivi", style = MaterialTheme.typography.headlineSmall, color = ParchmentLight) }
        item { Text("Aujourd’hui · ${java.time.LocalDate.now()}", style = MaterialTheme.typography.bodyMedium, color = InkMuted) }
        if (state.members.size > 1) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.members.forEach { member ->
                        FilterChip(selected = member.memberId == state.selectedMemberId, onClick = { viewModel.selectMember(member.memberId) }, label = { Text(member.displayName) })
                    }
                }
            }
        }
        state.errorMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        item { Text("Membres du foyer", style = MaterialTheme.typography.titleMedium, color = ParchmentLight) }
        items(state.members, key = { "member-${it.memberId}" }) { member -> MemberSummaryCard(member, member.memberId == state.selectedMemberId) { viewModel.selectMember(member.memberId) } }
        state.house?.let { house ->
            item { Text("Maison", style = MaterialTheme.typography.titleMedium, color = ParchmentLight) }
            item { MemberSummaryCard(house, false) {} }
        }
        selected?.let { member ->
            item { Text("Détail · ${member.displayName}", style = MaterialTheme.typography.titleMedium, color = ParchmentLight) }
            if (member.items.isEmpty()) item { Text("Aucune tâche prévue aujourd’hui.", color = InkMuted) }
            val overdue = member.items.filter { it.overdue }
            val pending = member.items.filter { !it.completed && !it.overdue }
            val completed = member.items.filter { it.completed }
            if (pending.isNotEmpty()) {
                item { Text("À faire", style = MaterialTheme.typography.titleSmall, color = ParchmentLight) }
                items(pending, key = { "pending-${it.key}" }) { item -> FollowUpItemRow(item, onOpenFamilyTask, onOpenLegacyTask) }
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
            Text(when { item.overdue -> "En retard"; item.completed -> "Terminée"; else -> "À faire" }, color = if (item.overdue) DangerGlow else InkMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

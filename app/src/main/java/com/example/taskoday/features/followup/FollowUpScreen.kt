package com.example.taskoday.features.followup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.example.taskoday.features.taskodayv4.*
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.FantasyButton
import com.example.taskoday.core.ui.component.fantasy.FantasyButtonStyle
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
    onOpenProfile: () -> Unit = {},
    onOpenAllTasks: () -> Unit = {},
    onOpenFamilyTask: (Long) -> Unit,
    onOpenLegacyTask: (Long) -> Unit,
    onCreatePersonal: (FamilyActionType, Long) -> Unit = { _, _ -> },
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenResumed() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selected = state.members.firstOrNull { it.memberId == state.selectedMemberId }
    val globalItems = (state.members.flatMap { it.items } + state.house?.items.orEmpty()).distinctBy { it.key }
    val validationCount = globalItems.count { it.familyTask?.status == com.example.taskoday.domain.model.FamilyTaskStatus.PENDING_VALIDATION }
    FantasyScreenBackground {
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 28.dp),
    ) {
        item { FantasyPageHeader("Suivi", "Le tableau de bord de ma famille", onOpenProfile) }
        item {
            FantasySummaryPanel {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vue d'ensemble · Aujourd'hui", color = JourneyGold, style = MaterialTheme.typography.titleMedium)
                    if (state.isLoading) CircularProgressIndicator(color = JourneyGold)
                    else if (state.errorMessage != null) Text("Bilan indisponible", color = JourneyParchment)
                    else {
                        Text("${globalItems.count { it.completed }} / ${globalItems.size} actions accomplies", color = JourneyParchment, style = MaterialTheme.typography.titleLarge)
                        Text("${globalItems.count { !it.completed }} restantes · $validationCount à valider · ${globalItems.count { it.overdue }} en retard",
                            color = JourneyParchment, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FantasyButton(text = "Toutes les actions", onClick = onOpenAllTasks, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
                FantasyButton(text = "Journal familial", onClick = onOpenJournal, modifier = Modifier.weight(1f).heightIn(min = 48.dp), style = FantasyButtonStyle.Outline)
            }
        }
        state.errorMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        item { FantasySectionHeading("Membres du foyer", "Choisir un parcours") }
        state.members.chunked(2).forEach { pair ->
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { member ->
                        MemberSummaryCard(member, member.memberId == state.selectedMemberId, Modifier.weight(1f).heightIn(min = 48.dp)) { viewModel.selectMember(member.memberId) }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f).heightIn(min = 48.dp))
                }
            }
        }
        state.house?.let { house ->
            item { FantasySectionHeading("Actions Maison", "Collectif") }
            item { MemberSummaryCard(house, false) {} }
            items(house.items, key = { "house-${it.key}" }) { item -> FollowUpItemRow(item, onOpenFamilyTask, onOpenLegacyTask) }
        }
        selected?.let { member ->
            item {
                Column {
                    FantasySectionHeading("Parcours · ${member.displayName}")
                    Text("Créer pour ${member.displayName}", color = JourneyParchment, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FantasyButton(text = "Ajouter une routine", onClick = { onCreatePersonal(FamilyActionType.PERSONAL_ROUTINE, member.memberId) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
                        FantasyButton(text = "Ajouter une mission", onClick = { onCreatePersonal(FamilyActionType.PERSONAL_MISSION, member.memberId) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
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
}

@Composable
private fun MemberSummaryCard(summary: FollowUpMemberSummary, selected: Boolean, modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    Column(modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
        .background(if (selected) androidx.compose.ui.graphics.Color(0xFF4C2B60) else androidx.compose.ui.graphics.Color(0xFF302039))
        .border(1.dp, JourneyGold.copy(alpha = if (selected) .9f else .3f), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
        .clickable(onClick = onClick).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(summary.displayName, color = JourneyParchment, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text("${summary.completed} / ${summary.total} terminées", color = JourneyGold, style = MaterialTheme.typography.bodySmall)
        Text("${summary.remaining} restantes", color = JourneyParchment, style = MaterialTheme.typography.bodySmall)
        if (summary.overdue > 0) Text("${summary.overdue} en retard", color = androidx.compose.ui.graphics.Color(0xFFFFB5A0), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun FollowUpItemRow(item: FollowUpItem, onOpenFamilyTask: (Long) -> Unit, onOpenLegacyTask: (Long) -> Unit) {
    FantasyParchmentCard {
    Row(Modifier.fillMaxWidth().clickable {
        item.familyTask?.let { onOpenFamilyTask(it.taskId) }
        item.legacyTask?.let { onOpenLegacyTask(it.task.id) }
    }.padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f).heightIn(min = 48.dp)) {
            Text(item.title, color = JourneyInk)
            item.familyTask?.let { task ->
                Text(task.followUpIdentityLabel(), color = JourneyMuted, style = MaterialTheme.typography.bodySmall)
                familyTaskCompletionActorLabels(task).forEach { actorLabel ->
                    Text(actorLabel, color = JourneyMuted, style = MaterialTheme.typography.bodySmall)
                }
                if (task.status == com.example.taskoday.domain.model.FamilyTaskStatus.IN_PROGRESS && task.contributors.isNotEmpty()) {
                    Text("En cours · ${task.contributors.joinToString(", ") { it.displayName }}", color = JourneyMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(when { item.overdue -> "En retard"; item.familyTask != null -> familyTaskStatusLabel(item.familyTask.status); item.completed -> "Terminée"; else -> "À faire" }, color = if (item.overdue) JourneyDangerInk else JourneyMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
    }

package com.example.taskoday.features.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.DarkSurfaceFilterChip
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.theme.InkBrown
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.ParchmentCream
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.domain.model.FamilyActionType
import com.example.taskoday.domain.model.FamilyTaskEvent

@Composable
fun ActivityJournalScreen(
    viewModel: ActivityJournalViewModel,
    isLocalChildMode: Boolean = false,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sections = journalSections(state.visibleEvents)
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
        FantasyScreenBackground(modifier = Modifier.statusBarsPadding().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = onBack) { Text("Retour") }
                        TextButton(onClick = viewModel::refresh) { Text("Actualiser") }
                    }
                    Text(if (state.isParent && !isLocalChildMode) "Journal familial" else "Mon journal", style = MaterialTheme.typography.headlineSmall, color = ParchmentLight)
                    Text("Ce qui s'est passé dans les 7 derniers jours", color = ParchmentCream)
                }
                if (isLocalChildMode && state.isParent) {
                    item { Text("Ouvre ton compte enfant pour consulter ton journal.", color = ParchmentCream) }
                } else if (state.isLoading) {
                    item { CircularProgressIndicator() }
                } else {
                    state.errorMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
                    if (state.isParent && state.members.isNotEmpty()) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item { DarkSurfaceFilterChip(selected = state.selectedMemberId == null, onClick = { viewModel.selectMember(null) }, label = "Tous") }
                                items(state.members, key = { it.userId }) { member ->
                                    DarkSurfaceFilterChip(selected = state.selectedMemberId == member.userId, onClick = { viewModel.selectMember(member.userId) }, label = member.displayName)
                                }
                            }
                        }
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { DarkSurfaceFilterChip(selected = state.selectedType == null, onClick = { viewModel.selectType(null) }, label = "Tous les types") }
                            items(FamilyActionType.entries) { type ->
                                DarkSurfaceFilterChip(selected = state.selectedType == type, onClick = { viewModel.selectType(type) }, label = type.journalLabel())
                            }
                        }
                    }
                    if (sections.isEmpty() && state.errorMessage == null) {
                        item { Text(if (state.isParent) "Aucune activité récente." else "Tu n'as encore rien terminé récemment.", color = ParchmentCream) }
                    }
                    sections.forEach { section ->
                        item { Text(section.period.label, style = MaterialTheme.typography.titleMedium, color = ParchmentLight) }
                        items(section.events, key = { it.id }) { event -> JournalEventCard(event) }
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalEventCard(event: FamilyTaskEvent) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ParchmentLight)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(event.category.journalLabel(), style = MaterialTheme.typography.labelMedium, color = InkMuted)
                Text(event.occurredAt.journalTimeLabel(), style = MaterialTheme.typography.labelMedium, color = InkMuted)
            }
            Text(event.title, style = MaterialTheme.typography.titleMedium, color = InkBrown)
            Text(event.journalEventLabel(), color = InkBrown)
            Text(event.journalActorLabel(), style = MaterialTheme.typography.bodySmall, color = InkMuted)
        }
    }
}

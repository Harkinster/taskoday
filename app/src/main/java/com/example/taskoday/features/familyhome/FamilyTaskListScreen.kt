package com.example.taskoday.features.familyhome

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.domain.model.FamilyTaskDefinition
import com.example.taskoday.features.taskodayv4.*
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.widthIn

@Composable
fun FamilyTaskListScreen(
    viewModel: FamilyTaskListViewModel,
    onBack: () -> Unit,
    onAddTask: () -> Unit,
    onOpenTask: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
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
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SoftGold)
                }
                return@FantasyScreenBackground
            }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.medium),
                contentPadding = PaddingValues(top = spacing.large, bottom = spacing.xxLarge),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                item {
                    FamilyTaskListHeader(
                        totalTasks = uiState.tasks.count { task -> task.active },
                        onBack = onBack,
                        onAddTask = onAddTask.takeIf { uiState.access.canManage },
                    )
                }

                if (uiState.filters.isNotEmpty()) {
                    item {
                        FamilyTaskListFilters(
                            filters = uiState.filters,
                            selectedFilterKey = uiState.selectedFilterKey,
                            onSelectFilter = viewModel::selectFilter,
                        )
                    }
                }

                item { FantasySectionHeading("Actions de la famille", "${uiState.visibleTasks.size} affichée${if(uiState.visibleTasks.size>1) "s" else ""}") }

                uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
                    item {
                        FamilyTaskListMessage(
                            message = error,
                            onRetry = viewModel::refresh,
                        )
                    }
                }

                if (uiState.visibleTasks.isEmpty()) {
                    item {
                        EmptyFamilyTaskListCard(onAddTask = onAddTask.takeIf { uiState.access.canManage })
                    }
                } else {
                    items(
                        items = uiState.visibleTasks,
                        key = { task -> task.id },
                    ) { task ->
                        FamilyTaskDefinitionListCard(
                            task = task,
                            onOpenTask = onOpenTask,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun FamilyTaskListHeader(
    totalTasks: Int,
    onBack: () -> Unit,
    onAddTask: (() -> Unit)?,
) {
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        FantasyCompactHeader(title="Actions globales")
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            FantasyFramedButton("Retour",onBack,Modifier.widthIn(min=96.dp))
            Text("$totalTasks définition${if (totalTasks > 1) "s" else ""} familiale${if (totalTasks > 1) "s" else ""}",
                Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,color=JourneyParchment)
        }
        if(onAddTask!=null) FantasyFramedButton("Ajouter une tâche",onAddTask,Modifier.fillMaxWidth())
    }
}

@Composable
private fun FamilyTaskListFilters(
    filters: List<FamilyTaskListFilterOption>,
    selectedFilterKey: String,
    onSelectFilter: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filters.forEach { filter ->
            FantasyTrailTab(
                selected = filter.key == selectedFilterKey,
                onClick = { onSelectFilter(filter.key) },
                label = filter.label,
            )
        }
    }
}

@Composable
private fun FamilyTaskDefinitionListCard(
    task: FamilyTaskDefinition,
    onOpenTask: (Long) -> Unit,
) {
    FantasyParchmentCard(Modifier.clickable(enabled=task.id>0L,role=Role.Button) {onOpenTask(task.id)}) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = JourneyInk,
            )
            Text(
                text = task.assignmentLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = JourneyMuted,
            )
            Text(
                text = task.scheduleLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = JourneyMuted,
            )
            val metadata = task.listMetadataLabels()
            if (metadata.isNotEmpty()) {
                Text(
                    text = metadata.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = JourneyMuted,
                )
            }
    }
}

@Composable
private fun EmptyFamilyTaskListCard(onAddTask: (() -> Unit)?) {
    FantasyParchmentCard {
            Text(
                text = "Aucune tâche familiale pour le moment.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = JourneyInk,
            )
            Text(
                text = if (onAddTask != null) "Crée une tâche pour organiser la maison sans passer par une occurrence du jour." else "Aucune tâche accessible pour le moment.",
                style = MaterialTheme.typography.bodyMedium,
                color = JourneyMuted,
            )
            if(onAddTask!=null) FantasyFramedButton("Ajouter une tâche",onAddTask,Modifier.fillMaxWidth())
    }
}

@Composable
private fun FamilyTaskListMessage(
    message: String,
    onRetry: () -> Unit,
) {
    FantasyParchmentCard {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color=JourneyDangerInk,
            )
            FantasyFramedButton("Réessayer",onRetry)
    }
}

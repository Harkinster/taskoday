package com.example.taskoday.features.familyhome

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.taskoday.core.ui.theme.DangerGlow
import com.example.taskoday.core.ui.theme.InkBrown
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.ParchmentCream
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.WoodBrown
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.domain.model.FamilyTaskDefinition

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
                        onAddTask = onAddTask,
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
                        EmptyFamilyTaskListCard(onAddTask = onAddTask)
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
private fun FamilyTaskListHeader(
    totalTasks: Int,
    onBack: () -> Unit,
    onAddTask: () -> Unit,
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
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        text = "Toutes les tâches",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = InkBrown,
                    )
                    Text(
                        text = "$totalTasks définition${if (totalTasks > 1) "s" else ""} familiale${if (totalTasks > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted,
                    )
                }
                Icon(Icons.Outlined.Home, contentDescription = null, tint = WoodBrown, modifier = Modifier.size(28.dp))
            }

            Button(
                onClick = onAddTask,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WoodBrown, contentColor = ParchmentLight),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajouter une tâche")
            }
        }
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
            FilterChip(
                selected = filter.key == selectedFilterKey,
                onClick = { onSelectFilter(filter.key) },
                label = { Text(filter.label) },
            )
        }
    }
}

@Composable
private fun FamilyTaskDefinitionListCard(
    task: FamilyTaskDefinition,
    onOpenTask: (Long) -> Unit,
) {
    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = task.id > 0L) { onOpenTask(task.id) },
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = ParchmentLight.copy(alpha = 0.97f),
                contentColor = InkBrown,
            ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkBrown,
            )
            Text(
                text = task.assignmentLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )
            Text(
                text = task.scheduleLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
            val metadata = task.listMetadataLabels()
            if (metadata.isNotEmpty()) {
                Text(
                    text = metadata.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
            }
        }
    }
}

@Composable
private fun EmptyFamilyTaskListCard(onAddTask: () -> Unit) {
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
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Aucune tâche familiale pour le moment.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkBrown,
            )
            Text(
                text = "Crée une tâche pour organiser la maison sans passer par une occurrence du jour.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )
            OutlinedButton(
                onClick = onAddTask,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajouter une tâche")
            }
        }
    }
}

@Composable
private fun FamilyTaskListMessage(
    message: String,
    onRetry: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = ParchmentCream.copy(alpha = 0.95f),
        contentColor = DangerGlow,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onRetry) {
                Text("Réessayer")
            }
        }
    }
}


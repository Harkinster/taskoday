package com.example.taskoday.features.quests

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.NeonButton
import com.example.taskoday.core.ui.component.fantasy.NeonButtonStyle
import com.example.taskoday.core.ui.component.fantasy.NeonCard
import com.example.taskoday.core.ui.component.fantasy.NeonTone
import com.example.taskoday.core.ui.component.fantasy.ProgressHeroCard
import com.example.taskoday.core.ui.component.fantasy.QuestCard
import com.example.taskoday.core.ui.component.fantasy.QuestLevelBadge
import com.example.taskoday.core.ui.component.fantasy.TaskodayDragonWatermark
import com.example.taskoday.core.ui.component.fantasy.TaskodayHeader
import com.example.taskoday.core.ui.component.fantasy.XpProgressBar
import com.example.taskoday.core.ui.theme.NeonCyan
import com.example.taskoday.core.ui.theme.StarWhite
import com.example.taskoday.core.ui.theme.TextMuted
import com.example.taskoday.core.ui.theme.WarningGlow
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.QuestForDay

private const val XP_PER_LEVEL: Int = 1000

@Composable
fun QuestsScreen(viewModel: QuestsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    val completedCount = uiState.quests.count { it.isCompletedForDay }
    val questProgress = if (uiState.quests.isEmpty()) 0f else completedCount.toFloat() / uiState.quests.size.toFloat()
    val level = (uiState.pointsBalance / XP_PER_LEVEL) + 1
    val levelXp = uiState.pointsBalance % XP_PER_LEVEL
    val levelProgress = levelXp.toFloat() / XP_PER_LEVEL.toFloat()

    var formTitle by rememberSaveable { mutableStateOf("") }
    var formDescription by rememberSaveable { mutableStateOf("") }
    var formPoints by rememberSaveable { mutableStateOf("3") }
    var formDayPart by rememberSaveable { mutableStateOf(DayPart.APRES_MIDI) }
    var editingQuestId by rememberSaveable { mutableStateOf<Long?>(null) }

    fun resetForm() {
        formTitle = ""
        formDescription = ""
        formPoints = "3"
        formDayPart = DayPart.APRES_MIDI
        editingQuestId = null
    }

    fun startEdit(item: QuestForDay) {
        editingQuestId = item.quest.id
        formTitle = item.quest.title
        formDescription = item.quest.description.orEmpty()
        formPoints = item.quest.pointsReward.toString()
        formDayPart = item.quest.dayPart
    }

    Scaffold(
        containerColor = Color.Transparent,
    ) { innerPadding ->
        FantasyScreenBackground(modifier = Modifier.padding(innerPadding)) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = NeonCyan)
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
                    TaskodayHeader(
                        title = "Quetes",
                        subtitle = "Accomplis des actions, gagne de l XP et deviens legendaire.",
                        avatarInitials = "AB",
                    )
                }

                item {
                    DateControlsCard(
                        dateLabel = uiState.dateLabel,
                        onPrevious = viewModel::goToPreviousDay,
                        onNext = viewModel::goToNextDay,
                        onToday = viewModel::goToToday,
                    )
                }

                item {
                    NeonCard(
                        tone = NeonTone.Violet,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            TaskodayDragonWatermark(
                                modifier =
                                    Modifier
                                        .size(130.dp)
                                        .align(Alignment.TopEnd),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                QuestLevelBadge(
                                    level = level,
                                    modifier = Modifier.size(72.dp),
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "Ton aventure continue !",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = StarWhite,
                                    )
                                    Text(
                                        text = "Encore ${XP_PER_LEVEL - levelXp} XP pour passer au niveau ${level + 1}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                    )
                                }
                            }
                        }
                        XpProgressBar(progress = levelProgress, modifier = Modifier.fillMaxWidth())
                        Text(
                            text = "$levelXp / $XP_PER_LEVEL XP",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan,
                        )
                    }
                }

                item {
                    ProgressHeroCard(
                        title = "Quetes du jour",
                        completed = completedCount,
                        total = uiState.quests.size,
                        progress = questProgress,
                        subtitle = "Continue pour gagner de l XP.",
                        accent = NeonTone.Cyan,
                    )
                }

                if (uiState.canCreateQuest) {
                    item {
                        QuestFormCard(
                            formTitle = formTitle,
                            onFormTitleChange = {
                                formTitle = it
                                viewModel.clearError()
                            },
                            formDescription = formDescription,
                            onFormDescriptionChange = {
                                formDescription = it
                                viewModel.clearError()
                            },
                            formPoints = formPoints,
                            onFormPointsChange = {
                                formPoints = it.filter { ch -> ch.isDigit() }.take(2)
                                viewModel.clearError()
                            },
                            formDayPart = formDayPart,
                            onFormDayPartChange = {
                                formDayPart = it
                                viewModel.clearError()
                            },
                            isEditing = editingQuestId != null,
                            isSubmitting = uiState.isSubmittingQuest,
                            onSubmit = {
                                val parsedPoints = formPoints.toIntOrNull() ?: 3
                                val editingItem =
                                    editingQuestId?.let { id -> uiState.quests.firstOrNull { it.quest.id == id } }
                                if (editingItem == null) {
                                    viewModel.createQuest(
                                        title = formTitle,
                                        description = formDescription,
                                        dayPart = formDayPart,
                                        pointsReward = parsedPoints,
                                    )
                                } else {
                                    viewModel.updateQuest(
                                        questForDay = editingItem,
                                        title = formTitle,
                                        description = formDescription,
                                        dayPart = formDayPart,
                                        pointsReward = parsedPoints,
                                    )
                                }
                                resetForm()
                            },
                            onCancelEdit = { resetForm() },
                        )
                    }
                }

                if (!uiState.errorMessage.isNullOrBlank()) {
                    item {
                        NeonCard(tone = NeonTone.Warning) {
                            Text(
                                text = uiState.errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarningGlow,
                            )
                            NeonButton(
                                text = "Fermer",
                                onClick = viewModel::clearError,
                                style = NeonButtonStyle.Outline,
                            )
                        }
                    }
                }

                if (uiState.quests.isEmpty()) {
                    item {
                        NeonCard(tone = NeonTone.Blue) {
                            Text(
                                text = "Aucune quete active.",
                                style = MaterialTheme.typography.titleMedium,
                                color = StarWhite,
                            )
                            Text(
                                text = "Passe dans l onglet Missions pour preparer de nouveaux objectifs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                    }
                } else {
                    items(uiState.quests, key = { it.quest.id }) { item ->
                        val checked = item.isCompletedForDay
                        QuestCard(
                            title = item.quest.title,
                            description = item.quest.description,
                            emoji = item.quest.emoji,
                            xpLabel = "+${item.quest.pointsReward} XP",
                            progress = if (checked) 1f else 0.12f,
                            actionLabel = if (checked) "Recuperer" else "Commencer",
                            dayPartLabel = "${item.quest.dayPart.emoji()} ${item.quest.dayPart.label()}",
                            done = checked,
                            canManage = uiState.canManageQuests,
                            onAction = { viewModel.setQuestCompleted(item, !checked) },
                            onEdit = if (uiState.canManageQuests) ({ startEdit(item) }) else null,
                            onDelete = if (uiState.canManageQuests) ({ viewModel.deleteQuest(item) }) else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateControlsCard(
    dateLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    NeonCard(tone = NeonTone.Blue) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Jour precedent",
                    tint = StarWhite,
                )
            }
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleMedium,
                color = StarWhite,
            )
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Jour suivant",
                    tint = StarWhite,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onToday) {
                Icon(
                    imageVector = Icons.Outlined.Today,
                    contentDescription = "Aujourd hui",
                    tint = NeonCyan,
                )
            }
        }
    }
}

@Composable
private fun QuestFormCard(
    formTitle: String,
    onFormTitleChange: (String) -> Unit,
    formDescription: String,
    onFormDescriptionChange: (String) -> Unit,
    formPoints: String,
    onFormPointsChange: (String) -> Unit,
    formDayPart: DayPart,
    onFormDayPartChange: (DayPart) -> Unit,
    isEditing: Boolean,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    NeonCard(tone = NeonTone.Violet) {
        Text(
            text = if (isEditing) "Modifier la quete" else "Ajouter une quete",
            style = MaterialTheme.typography.titleMedium,
            color = StarWhite,
        )
        OutlinedTextField(
            value = formTitle,
            onValueChange = onFormTitleChange,
            label = { Text("Titre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = formDescription,
            onValueChange = onFormDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = formPoints,
            onValueChange = onFormPointsChange,
            label = { Text("Points XP") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DayPart.entries.forEach { dayPart ->
                FilterChip(
                    selected = formDayPart == dayPart,
                    onClick = { onFormDayPartChange(dayPart) },
                    label = { Text("${dayPart.emoji()} ${dayPart.label()}") },
                )
            }
        }
        NeonButton(
            text = if (isEditing) "Mettre a jour" else "Creer la quete",
            onClick = onSubmit,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
        if (isEditing) {
            NeonButton(
                text = "Annuler la modification",
                onClick = onCancelEdit,
                style = NeonButtonStyle.Outline,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

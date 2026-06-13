package com.example.taskoday.features.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.FantasyConfirmationDialog
import com.example.taskoday.core.ui.component.fantasy.NeonButton
import com.example.taskoday.core.ui.component.fantasy.NeonButtonStyle
import com.example.taskoday.core.ui.component.fantasy.NeonCard
import com.example.taskoday.core.ui.component.fantasy.NeonTone
import com.example.taskoday.core.ui.component.fantasy.ProgressHeroCard
import com.example.taskoday.core.ui.component.fantasy.QuestCard
import com.example.taskoday.core.ui.component.fantasy.TaskodayHeader
import com.example.taskoday.core.ui.theme.MagicViolet
import com.example.taskoday.core.ui.theme.NeonCyan
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.StarWhite
import com.example.taskoday.core.ui.theme.TextMuted
import com.example.taskoday.core.ui.theme.WarningGlow
import com.example.taskoday.core.ui.theme.WoodBrownDark
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.QuestForDay

@Composable
fun QuestsScreen(
    viewModel: QuestsViewModel,
    onOpenProfile: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    val completedCount = uiState.quests.count { it.isCompletedForDay }
    val questProgress = if (uiState.quests.isEmpty()) 0f else completedCount.toFloat() / uiState.quests.size.toFloat()

    var formTitle by rememberSaveable { mutableStateOf("") }
    var formDescription by rememberSaveable { mutableStateOf("") }
    var formPoints by rememberSaveable { mutableStateOf("3") }
    var formDayPart by rememberSaveable { mutableStateOf(DayPart.APRES_MIDI) }
    var editingQuestId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDeleteQuestId by rememberSaveable { mutableStateOf<Long?>(null) }

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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        FantasyScreenBackground(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(innerPadding),
        ) {
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
                contentPadding = PaddingValues(top = spacing.large, bottom = 148.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                item {
                    TaskodayHeader(
                        title = "Quête",
                        subtitle = "Accomplis des actions, gagne de l XP et deviens legendaire.",
                        avatarInitials = "AB",
                        onAvatarClick = onOpenProfile,
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
                    ProgressHeroCard(
                        title = "Quête du jour",
                        completed = completedCount,
                        total = uiState.quests.size,
                        progress = questProgress,
                        subtitle = "Progression des quêtes du jour.",
                        accent = NeonTone.Cyan,
                    )
                }

                if (uiState.canCreateQuest && editingQuestId != null) {
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
                                text = "Aucune quête active.",
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
                            dayPartLabel = item.quest.dayPart.label(),
                            done = checked,
                            canManage = uiState.canManageQuests,
                            onAction = { viewModel.setQuestCompleted(item, !checked) },
                            onEdit = if (uiState.canManageQuests) ({ startEdit(item) }) else null,
                            onDelete = if (uiState.canManageQuests) ({ pendingDeleteQuestId = item.quest.id }) else null,
                        )
                    }
                }
            }
        }
    }

    pendingDeleteQuestId
        ?.let { id -> uiState.quests.firstOrNull { it.quest.id == id } }
        ?.let { item ->
            FantasyConfirmationDialog(
                title = "Supprimer la quête",
                message = "Supprimer « ${item.quest.title} » ? Cette action est définitive.",
                confirmLabel = "Supprimer",
                confirmEnabled = !uiState.isSubmittingQuest,
                onDismiss = { pendingDeleteQuestId = null },
                onConfirm = {
                    pendingDeleteQuestId = null
                    viewModel.deleteQuest(item)
                },
            )
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
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Jour suivant",
                    tint = StarWhite,
                )
            }
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
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Brush.verticalGradient(listOf(WoodBrownDark, MagicViolet, Color(0xFF261238))))
                .border(1.5.dp, SoftGold.copy(alpha = 0.82f), shape)
                .padding(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
        Text(
            text = if (isEditing) "Modifier la quête" else "Ajouter une quête",
            style = MaterialTheme.typography.titleMedium,
            color = SoftGold,
        )
        OutlinedTextField(
            value = formTitle,
            onValueChange = onFormTitleChange,
            label = { Text("Titre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ParchmentLight.copy(alpha = 0.92f)),
        )
        OutlinedTextField(
            value = formDescription,
            onValueChange = onFormDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ParchmentLight.copy(alpha = 0.92f)),
        )
        OutlinedTextField(
            value = formPoints,
            onValueChange = onFormPointsChange,
            label = { Text("Points XP") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ParchmentLight.copy(alpha = 0.92f)),
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
                    label = { Text(dayPart.label()) },
                )
            }
        }
        NeonButton(
            text = if (isEditing) "Mettre à jour" else "Créer la quête",
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
}

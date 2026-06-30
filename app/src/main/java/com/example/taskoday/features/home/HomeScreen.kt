package com.example.taskoday.features.home

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.FantasyAssetBubble
import com.example.taskoday.core.ui.component.fantasy.FantasyButton
import com.example.taskoday.core.ui.component.fantasy.FantasyButtonStyle
import com.example.taskoday.core.ui.component.fantasy.FantasyCard
import com.example.taskoday.core.ui.component.fantasy.FantasyConfirmationDialog
import com.example.taskoday.core.ui.component.fantasy.FantasyProgressBar
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.FantasyStateCard
import com.example.taskoday.core.ui.component.fantasy.FantasyTone
import com.example.taskoday.core.ui.component.fantasy.NestAssets
import com.example.taskoday.core.ui.component.fantasy.ParentPinDialog
import com.example.taskoday.core.ui.component.fantasy.RewardToast
import com.example.taskoday.core.ui.component.fantasy.RoutineItemRow
import com.example.taskoday.core.ui.component.fantasy.TaskodayTopBar
import com.example.taskoday.core.ui.theme.CrystalBlue
import com.example.taskoday.core.ui.theme.EmberOrange
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.MossGreen
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.WoodBrownDark
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.PlanningItemType
import com.example.taskoday.domain.model.QuestForDay
import com.example.taskoday.domain.model.TaskForDay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    isLocalChildMode: Boolean = false,
    onOpenTask: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    onOpenProfile: () -> Unit,
    onAddAction: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenWishes: () -> Unit,
    onOpenNest: () -> Unit,
    onEnterLocalChildMode: () -> Unit = {},
    onExitLocalChildMode: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showReturnParentPinDialog by rememberSaveable { mutableStateOf(false) }
    var returnParentPinError by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeleteRoutine by remember { mutableStateOf<TaskForDay?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshDashboard()
        viewModel.refreshParentPinState()
    }

    val visibleTasksForDay =
        if (uiState.usingRemoteData) {
            uiState.tasksForDay.filter { it.task.id < 0L }
        } else {
            uiState.tasksForDay.filter { it.task.id > 0L }
        }
    val visibleQuestsForDay =
        if (uiState.usingRemoteData) {
            uiState.questsForDay.filter { it.quest.id < 0L }
        } else {
            uiState.questsForDay.filter { it.quest.id > 0L }
        }
    val planningItems = buildPlanningItems(visibleTasksForDay, visibleQuestsForDay)
    val completedCount = planningItems.count { it.isCompleted }
    val todoCount = planningItems.size - completedCount
    val progress = if (planningItems.isEmpty()) 0f else completedCount.toFloat() / planningItems.size.toFloat()
    val sections = buildActionSections(planningItems)
    val showParentDashboard = uiState.isParentUser && uiState.usingRemoteData && !isLocalChildMode
    val canManageActions = uiState.canManageActions && !isLocalChildMode
    val emptyActionsTitle = if (showParentDashboard) "Aucune action pour l’instant" else "Rien à faire pour le moment"
    val emptyActionsMessage =
        if (showParentDashboard) {
            "Ajoutez une routine, une mission ou une quête pour lancer la journée."
        } else {
            "Demande à ton parent d’ajouter une routine ou une mission."
        }
    val parentOnboardingStep =
        if (showParentDashboard) {
            parentOnboardingStep(
                hasActiveChild = !uiState.activeChildLabel.isNullOrBlank(),
                hasParentPin = uiState.hasParentPin,
                hasAnyAction = planningItems.isNotEmpty(),
            )
        } else {
            null
        }

    val selectedDate =
        if (uiState.selectedDayStartMillis == 0L) {
            LocalDate.now()
        } else {
            DateTimeUtils.toLocalDate(uiState.selectedDayStartMillis)
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
                    FantasyStateCard(
                        title = "Le Nid se réveille",
                        message = "La Routine du Gardien se prépare doucement.",
                        loading = true,
                        tone = FantasyTone.Gold,
                    )
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
                    TaskodayTopBar(
                        avatarInitials = "AB",
                        compact = true,
                        showNotification = false,
                        onAvatarClick = onOpenProfile,
                    )
                }

                if (isLocalChildMode) {
                    item {
                        LocalChildModeCard(
                            childLabel = uiState.activeChildLabel,
                            onExit = {
                                returnParentPinError = null
                                showReturnParentPinDialog = true
                            },
                        )
                    }
                }

                item {
                    RoutineDateHeader(
                        dateLabel =
                            if (selectedDate == LocalDate.now()) {
                                "Aujourd'hui — ${uiState.dateLabel}"
                            } else {
                                uiState.dateLabel
                            },
                        onOpenCalendar = { showDatePicker = true },
                    )
                }

                if (showParentDashboard) {
                    item {
                        ParentDashboardCard(
                            childLabel = uiState.activeChildLabel ?: "Enfant selectionne",
                            xp = uiState.remoteXp ?: 0,
                            flammeches = uiState.remoteFlammeches ?: 0,
                            crystals = uiState.remoteCrystals ?: 0,
                            todoCount = todoCount,
                            completedCount = completedCount,
                            pendingWishCount = uiState.pendingWishCount,
                            availableScrollCount = uiState.availableScrollCount,
                        )
                    }

                    parentOnboardingStep?.let { step ->
                        item {
                            ParentOnboardingCard(
                                step = step,
                                onOpenProfile = onOpenProfile,
                                onAddAction = onAddAction,
                            )
                        }
                    }

                    item {
                        ParentShortcutsCard(
                            onAddAction = onAddAction,
                            onOpenJournal = onOpenJournal,
                            onOpenWishes = onOpenWishes,
                            onOpenNest = onOpenNest,
                            onEnterLocalChildMode = onEnterLocalChildMode,
                            canEnterLocalChildMode = uiState.hasParentPin,
                        )
                    }
                } else if (uiState.usingRemoteData) {
                    item {
                        ChildJournalCard(onOpenJournal = onOpenJournal)
                    }
                }

                item {
                    DailyProgressCard(
                        title = "Actions du jour",
                        completed = completedCount,
                        total = planningItems.size,
                        progress = progress,
                        subtitle =
                            if (planningItems.isEmpty()) {
                                emptyActionsTitle
                            } else {
                                "Chaque action nourrit Le Nid."
                            },
                        badgeLabel = walletSummaryLabel(uiState),
                    )
                }

                uiState.completionFeedback?.let { feedback ->
                    item {
                        RewardToast(
                            message = feedbackMessage(feedback),
                            tone = FantasyTone.Gold,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                uiState.successMessage?.let { message ->
                    item {
                        RewardToast(message = message, tone = FantasyTone.Moss)
                    }
                }

                if (!uiState.errorMessage.isNullOrBlank()) {
                    item {
                        RewardToast(message = "Alerte réseau : ${uiState.errorMessage.orEmpty()}", tone = FantasyTone.Ember)
                    }
                }

                if (planningItems.isEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                            FantasyStateCard(
                                title = emptyActionsTitle,
                                message = emptyActionsMessage,
                                assetResId = NestAssets.interfaceAsset("nid"),
                                assetDescription = null,
                                tone = FantasyTone.Moss,
                            )
                            if (showParentDashboard) {
                                FantasyButton(
                                    text = "Ajouter une action",
                                    onClick = onAddAction,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = FantasyButtonStyle.Filled,
                                )
                            }
                        }
                    }
                } else {
                    sections.filter { section -> section.items.isNotEmpty() }.forEach { section ->
                        item(key = "section-${section.itemType.name}") {
                            TodayActionSection(
                                section = section,
                                onOpenTask = onOpenTask,
                                onEditTask = onEditTask,
                                onRequestDeleteRoutine = { pendingDeleteRoutine = it },
                                onToggleCheck = { item, checked ->
                                    item.taskForDay?.let { taskForDay ->
                                        viewModel.setTaskChecked(taskForDay, checked)
                                    }
                                    item.questForDay?.let { questForDay ->
                                        viewModel.setQuestChecked(questForDay, checked)
                                    }
                                },
                                pendingCompletionKeys = uiState.pendingCompletionKeys,
                                pendingManagementKeys = uiState.pendingManagementKeys,
                                canManageActions = canManageActions,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialUtcMillis =
            selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedUtcMillis ->
                            viewModel.selectDay(datePickerSelectionToDayStartMillis(selectedUtcMillis))
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(text = "Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = "Annuler")
                }
            },
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    pendingDeleteRoutine?.let { routine ->
        FantasyConfirmationDialog(
            title = "Désactiver la routine",
            message = "Désactiver « ${routine.task.title} » ? Elle ne sera plus proposée à l'enfant.",
            confirmLabel = "Désactiver",
            onDismiss = { pendingDeleteRoutine = null },
            onConfirm = {
                pendingDeleteRoutine = null
                viewModel.deleteRoutine(routine)
            },
        )
    }

    if (showReturnParentPinDialog) {
        ParentPinDialog(
            title = "Retour parent",
            message = "Saisis le PIN parent pour revenir au tableau de bord.",
            errorMessage = returnParentPinError,
            onDismiss = {
                showReturnParentPinDialog = false
                returnParentPinError = null
            },
            onConfirm = { pin ->
                if (viewModel.verifyParentPin(pin)) {
                    showReturnParentPinDialog = false
                    returnParentPinError = null
                    onExitLocalChildMode()
                } else {
                    returnParentPinError = "PIN incorrect."
                }
            },
        )
    }
}

@Composable
private fun LocalChildModeCard(
    childLabel: String?,
    onExit: () -> Unit,
) {
    FantasyCard(tone = FantasyTone.Gold) {
        Text(
            text = "Mode enfant local",
            style = MaterialTheme.typography.titleMedium,
            color = WoodBrownDark,
        )
        Text(
            text = childLabel?.let { "Tu joues avec $it sur cet appareil." } ?: "Tu joues avec l'enfant actif.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
        )
        FantasyButton(
            text = "Retour parent",
            onClick = onExit,
            style = FantasyButtonStyle.Outline,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun RoutineDateHeader(
    dateLabel: String,
    onOpenCalendar: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.titleMedium,
            color = SoftGold,
            maxLines = 1,
        )
        IconButton(onClick = onOpenCalendar) {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = "Calendrier",
                tint = SoftGold,
            )
        }
    }
}

@Composable
private fun ParentOnboardingCard(
    step: ParentOnboardingStep,
    onOpenProfile: () -> Unit,
    onAddAction: () -> Unit,
) {
    FantasyCard(tone = FantasyTone.Gold) {
        Text(
            text = "Premiers pas",
            style = MaterialTheme.typography.labelLarge,
            color = EmberOrange,
            maxLines = 1,
        )
        Text(
            text = step.title,
            style = MaterialTheme.typography.titleMedium,
            color = WoodBrownDark,
            maxLines = 2,
        )
        Text(
            text = step.message,
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            maxLines = 2,
        )
        FantasyButton(
            text = step.buttonLabel,
            onClick =
                when (step.action) {
                    ParentOnboardingAction.AddAction -> onAddAction
                    ParentOnboardingAction.AddChild,
                    ParentOnboardingAction.DefinePin,
                    -> onOpenProfile
                },
            modifier = Modifier.fillMaxWidth(),
            style = FantasyButtonStyle.Filled,
        )
    }
}

@Composable
private fun ParentDashboardCard(
    childLabel: String,
    xp: Int,
    flammeches: Int,
    crystals: Int,
    todoCount: Int,
    completedCount: Int,
    pendingWishCount: Int,
    availableScrollCount: Int,
) {
    FantasyCard(tone = FantasyTone.Violet) {
        Text(
            text = "Tableau de bord parent",
            style = MaterialTheme.typography.titleLarge,
            color = WoodBrownDark,
            maxLines = 1,
        )
        Text(
            text = childLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            maxLines = 1,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ParentMetric(label = "XP", value = xp.toString(), color = MossGreen, modifier = Modifier.weight(1f))
            ParentMetric(label = "Flammèches", value = flammeches.toString(), color = EmberOrange, modifier = Modifier.weight(1f))
            ParentMetric(label = "Cristaux", value = crystals.toString(), color = CrystalBlue, modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ParentMetric(label = "À faire", value = todoCount.toString(), color = EmberOrange, modifier = Modifier.weight(1f))
            ParentMetric(label = "Terminées", value = completedCount.toString(), color = MossGreen, modifier = Modifier.weight(1f))
        }
        Text(
            text = wishSummaryText(pendingWishCount, availableScrollCount),
            style = MaterialTheme.typography.bodyMedium,
            color = WoodBrownDark,
            maxLines = 2,
        )
    }
}

private data class ParentOnboardingStep(
    val title: String,
    val message: String,
    val buttonLabel: String,
    val action: ParentOnboardingAction,
)

private enum class ParentOnboardingAction {
    AddChild,
    DefinePin,
    AddAction,
}

private fun parentOnboardingStep(
    hasActiveChild: Boolean,
    hasParentPin: Boolean,
    hasAnyAction: Boolean,
): ParentOnboardingStep? =
    when {
        !hasActiveChild ->
            ParentOnboardingStep(
                title = "Créer mon premier enfant",
                message = "Ajoutez son prénom pour préparer ses routines et son Nid.",
                buttonLabel = "Créer mon premier enfant",
                action = ParentOnboardingAction.AddChild,
            )

        !hasParentPin ->
            ParentOnboardingStep(
                title = "Définir le PIN parent",
                message = "Ce code protège le retour au tableau de bord parent.",
                buttonLabel = "Définir le PIN parent",
                action = ParentOnboardingAction.DefinePin,
            )

        !hasAnyAction ->
            ParentOnboardingStep(
                title = "Créer une première routine",
                message = "Commencez par une action simple pour aujourd’hui.",
                buttonLabel = "Créer une première routine",
                action = ParentOnboardingAction.AddAction,
            )

        else -> null
    }

@Composable
private fun ParentMetric(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = InkMuted,
            maxLines = 1,
        )
    }
}

@Composable
private fun ParentShortcutsCard(
    onAddAction: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenWishes: () -> Unit,
    onOpenNest: () -> Unit,
    onEnterLocalChildMode: () -> Unit,
    canEnterLocalChildMode: Boolean,
) {
    FantasyCard(tone = FantasyTone.Night) {
        Text(
            text = "Raccourcis parent",
            style = MaterialTheme.typography.titleMedium,
            color = WoodBrownDark,
            maxLines = 1,
        )
        FantasyButton(
            text = "Ajouter une action",
            onClick = onAddAction,
            modifier = Modifier.fillMaxWidth(),
            style = FantasyButtonStyle.Filled,
        )
        FantasyButton(
            text = "Journal",
            onClick = onOpenJournal,
            modifier = Modifier.fillMaxWidth(),
            style = FantasyButtonStyle.Outline,
        )
        FantasyButton(
            text = "Voir les souhaits",
            onClick = onOpenWishes,
            modifier = Modifier.fillMaxWidth(),
            style = FantasyButtonStyle.Outline,
        )
        FantasyButton(
            text = "Voir Le Nid",
            onClick = onOpenNest,
            modifier = Modifier.fillMaxWidth(),
            style = FantasyButtonStyle.Quiet,
        )
        FantasyButton(
            text = "Passer en mode enfant",
            onClick = onEnterLocalChildMode,
            modifier = Modifier.fillMaxWidth(),
            style = FantasyButtonStyle.Outline,
            enabled = canEnterLocalChildMode,
        )
        if (!canEnterLocalChildMode) {
            Text(
                text = "Définissez le PIN parent dans Profil avant le mode enfant.",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun ChildJournalCard(onOpenJournal: () -> Unit) {
    FantasyCard(tone = FantasyTone.Violet) {
        Text(
            text = "Mes dernières réussites",
            style = MaterialTheme.typography.titleMedium,
            color = WoodBrownDark,
            maxLines = 1,
        )
        Text(
            text = "Retrouve les actions terminées et tes souhaits.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
            maxLines = 2,
        )
        FantasyButton(
            text = "Ouvrir le journal",
            onClick = onOpenJournal,
            modifier = Modifier.fillMaxWidth(),
            style = FantasyButtonStyle.Outline,
        )
    }
}

@Composable
private fun DailyProgressCard(
    title: String,
    completed: Int,
    total: Int,
    progress: Float,
    subtitle: String,
    badgeLabel: String,
) {
    FantasyCard(tone = FantasyTone.Moss) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = WoodBrownDark)
        Text(text = "$completed/$total terminées", style = MaterialTheme.typography.headlineSmall, color = MossGreen)
        FantasyProgressBar(progress = progress, modifier = Modifier.fillMaxWidth())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = InkMuted, modifier = Modifier.weight(1f))
            Text(text = badgeLabel, style = MaterialTheme.typography.labelLarge, color = EmberOrange)
        }
    }
}

@Composable
private fun TodayActionSection(
    section: HomeSection,
    onOpenTask: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    onRequestDeleteRoutine: (TaskForDay) -> Unit,
    onToggleCheck: (HomePlanningItem, Boolean) -> Unit,
    pendingCompletionKeys: Set<String>,
    pendingManagementKeys: Set<String>,
    canManageActions: Boolean,
) {
    val doneCount = section.items.count { it.isCompleted }

    FantasyCard(tone = section.tone) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                FantasyAssetBubble(
                    assetResId = actionTypeAsset(section.itemType),
                    contentDescription = section.title,
                    size = 34.dp,
                )
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = WoodBrownDark,
                    maxLines = 1,
                )
            }
            Text(
                text = "$doneCount/${section.items.size}",
                style = MaterialTheme.typography.titleMedium,
                color = EmberOrange,
            )
        }
        section.items.forEach { item ->
            val isSubmitting = pendingCompletionKeys.contains(item.key)
            val isManaging = pendingManagementKeys.contains(item.key)
            val openTaskAction =
                item.taskForDay?.task
                    ?.takeIf { task -> task.id > 0L }
                    ?.let { task -> { onOpenTask(task.id) } }

            RoutineItemRow(
                title = item.title,
                emoji = item.emoji,
                done = item.isCompleted,
                subtitle = item.description ?: "${item.dayPart.label()} • ${formatPlanningTime(item)}",
                statusLabel =
                    when {
                        isSubmitting -> "Validation..."
                        item.isCompleted -> "Terminé"
                        else -> "À faire"
                    },
                rewardLabel = "Récompense : ${item.reward.compactLabel()}",
                actionEnabled = !item.isCompleted && !isSubmitting,
                isSubmitting = isSubmitting,
                onClick = openTaskAction,
                onEdit =
                    item.taskForDay
                        ?.takeIf { canManageActions && item.itemType == PlanningItemType.ROUTINE && !isManaging }
                        ?.let { taskForDay -> { onEditTask(taskForDay.task.id) } },
                onDelete =
                    item.taskForDay
                        ?.takeIf { canManageActions && item.itemType == PlanningItemType.ROUTINE && !isManaging }
                        ?.let { taskForDay -> { onRequestDeleteRoutine(taskForDay) } },
                onToggleDone = { onToggleCheck(item, true) },
            )
        }
    }
}

private data class HomeSection(
    val itemType: PlanningItemType,
    val title: String,
    val tone: FantasyTone,
    val items: List<HomePlanningItem>,
)

private data class HomePlanningItem(
    val key: String,
    val itemType: PlanningItemType,
    val dayPart: DayPart,
    val title: String,
    val description: String?,
    val emoji: String,
    val dueDate: Long?,
    val isCompleted: Boolean,
    val reward: com.example.taskoday.domain.model.CompletionReward,
    val taskForDay: TaskForDay? = null,
    val questForDay: QuestForDay? = null,
)

private fun buildPlanningItems(
    tasks: List<TaskForDay>,
    quests: List<QuestForDay>,
): List<HomePlanningItem> {
    val taskItems =
        tasks.map { item ->
            val itemType = if (item.task.isDaily) PlanningItemType.ROUTINE else PlanningItemType.MISSION
            HomePlanningItem(
                key = "${itemType.apiValue}-${item.task.id}",
                itemType = itemType,
                dayPart = item.task.dayPart,
                title = item.task.title,
                description = item.task.description,
                emoji = item.task.emoji,
                dueDate = item.task.dueDate,
                isCompleted = item.isCompleted,
                reward = rewardPreviewFor(itemType),
                taskForDay = item,
            )
        }
    val questItems =
        quests.map { item ->
            HomePlanningItem(
                key = "quest-${item.quest.id}",
                itemType = PlanningItemType.QUEST,
                dayPart = item.quest.dayPart,
                title = item.quest.title,
                description = item.quest.description,
                emoji = item.quest.emoji,
                dueDate = null,
                isCompleted = item.isCompletedForDay,
                reward = rewardPreviewFor(PlanningItemType.QUEST),
                questForDay = item,
            )
        }
    return (taskItems + questItems).sortedWith(compareBy<HomePlanningItem> { it.itemType.ordinal }.thenBy { it.dueDate ?: Long.MAX_VALUE })
}

private fun buildActionSections(items: List<HomePlanningItem>): List<HomeSection> =
    listOf(
        HomeSection(
            itemType = PlanningItemType.ROUTINE,
            title = "Routines",
            tone = FantasyTone.Moss,
            items = items.filter { it.itemType == PlanningItemType.ROUTINE },
        ),
        HomeSection(
            itemType = PlanningItemType.MISSION,
            title = "Missions",
            tone = FantasyTone.Gold,
            items = items.filter { it.itemType == PlanningItemType.MISSION },
        ),
        HomeSection(
            itemType = PlanningItemType.QUEST,
            title = "Quêtes",
            tone = FantasyTone.Violet,
            items = items.filter { it.itemType == PlanningItemType.QUEST },
        ),
    )

private fun actionTypeAsset(itemType: PlanningItemType): Int =
    when (itemType) {
        PlanningItemType.ROUTINE -> NestAssets.interfaceAsset("flammeche")
        PlanningItemType.MISSION -> NestAssets.interfaceAsset("crystal")
        PlanningItemType.QUEST -> NestAssets.interfaceAsset("nid")
    }

private fun formatPlanningTime(item: HomePlanningItem): String {
    return item.dueDate?.let { DateTimeUtils.formatTimeOnly(it) } ?: defaultTimeForPart(item.dayPart)
}

private fun defaultTimeForPart(dayPart: DayPart): String =
    when (dayPart) {
        DayPart.MATIN -> "07:30"
        DayPart.MATINEE -> "09:00"
        DayPart.MIDI -> "12:30"
        DayPart.APRES_MIDI -> "15:00"
        DayPart.SOIR -> "18:30"
        DayPart.SOIREE -> "20:30"
    }

private fun datePickerSelectionToDayStartMillis(selectedUtcMillis: Long): Long {
    val selectedDate = Instant.ofEpochMilli(selectedUtcMillis).atOffset(ZoneOffset.UTC).toLocalDate()
    return DateTimeUtils.startOfDayMillis(selectedDate)
}

private fun walletSummaryLabel(uiState: HomeUiState): String =
    if (uiState.usingRemoteData) {
        listOfNotNull(
            uiState.remoteXp?.let { "$it XP" },
            uiState.remoteFlammeches?.let { "$it Flammèches" },
            uiState.remoteCrystals?.let { "$it Cristaux" },
        ).joinToString(" • ")
    } else {
        "${uiState.pointsBalance} points"
    }

private fun wishSummaryText(
    pendingWishCount: Int,
    availableScrollCount: Int,
): String =
    when {
        pendingWishCount > 0 && availableScrollCount > 0 ->
            "$pendingWishCount souhait(s) en attente parent, $availableScrollCount parchemin(s) disponible(s)."
        pendingWishCount > 0 ->
            "$pendingWishCount souhait(s) en attente de validation parent."
        availableScrollCount > 0 ->
            "$availableScrollCount parchemin(s) accepte(s) disponible(s)."
        else ->
            "Aucune demande de souhait en attente."
    }

private fun feedbackMessage(feedback: CompletionFeedback): String {
    val rewardLabel = feedback.reward?.compactLabel().orEmpty()
    return if (rewardLabel.isBlank()) {
        "Bravo ! ${feedback.actionTitle} est terminée."
    } else {
        "Bravo ! $rewardLabel"
    }
}

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.FantasyAssetBubble
import com.example.taskoday.core.ui.component.fantasy.FantasyCard
import com.example.taskoday.core.ui.component.fantasy.FantasyConfirmationDialog
import com.example.taskoday.core.ui.component.fantasy.FantasyProgressBar
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.FantasyStateCard
import com.example.taskoday.core.ui.component.fantasy.FantasyTone
import com.example.taskoday.core.ui.component.fantasy.NestAssets
import com.example.taskoday.core.ui.component.fantasy.RewardToast
import com.example.taskoday.core.ui.component.fantasy.RoutineItemRow
import com.example.taskoday.core.ui.component.fantasy.TaskodayTopBar
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
    onOpenTask: (Long) -> Unit,
    onEditTask: (Long) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteRoutine by remember { mutableStateOf<TaskForDay?>(null) }

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
    val progress = if (planningItems.isEmpty()) 0f else completedCount.toFloat() / planningItems.size.toFloat()
    val sections = buildActionSections(planningItems)

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

                item {
                    DailyProgressCard(
                        title = "Actions du jour",
                        completed = completedCount,
                        total = planningItems.size,
                        progress = progress,
                        subtitle =
                            if (planningItems.isEmpty()) {
                                "Rien de prévu aujourd'hui."
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
                        FantasyStateCard(
                            title = "Journée libre",
                            message = "Aucune routine, mission ou quête aujourd'hui.",
                            assetResId = NestAssets.interfaceAsset("nid"),
                            assetDescription = null,
                            tone = FantasyTone.Moss,
                        )
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
                                canManageActions = uiState.canManageActions,
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

private fun feedbackMessage(feedback: CompletionFeedback): String {
    val rewardLabel = feedback.reward?.compactLabel().orEmpty()
    return if (rewardLabel.isBlank()) {
        "Bravo ! ${feedback.actionTitle} est terminée."
    } else {
        "Bravo ! $rewardLabel"
    }
}

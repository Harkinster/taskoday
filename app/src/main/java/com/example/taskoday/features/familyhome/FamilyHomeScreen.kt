package com.example.taskoday.features.familyhome

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.taskoday.core.ui.component.fantasy.TaskodayTopBar
import com.example.taskoday.core.ui.theme.DangerGlow
import com.example.taskoday.core.ui.theme.InkBrown
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.MossGreen
import com.example.taskoday.core.ui.theme.ParchmentCream
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.SoftRed
import com.example.taskoday.core.ui.theme.WarningGlow
import com.example.taskoday.core.ui.theme.WoodBrown
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskTodayItem

@Composable
fun FamilyHomeScreen(
    viewModel: FamilyHomeViewModel,
    onOpenProfile: () -> Unit,
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = SoftGold)
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
                        avatarInitials = "MM",
                        compact = true,
                        showNotification = false,
                        onAvatarClick = onOpenProfile,
                    )
                }

                item {
                    FamilyHomeHeader(
                        mode = uiState.mode,
                        dateLabel = uiState.dateLabel,
                        weekRangeLabel = uiState.weekRangeLabel,
                        completedTasks = uiState.completedTasks,
                        totalTasks = uiState.totalTasks,
                        onAddTask = onAddTask,
                        onShowToday = viewModel::showToday,
                        onShowWeek = viewModel::showWeek,
                    )
                }

                if (uiState.mode == FamilyHomeMode.WEEK) {
                    item {
                        FamilyHomeWeekControls(
                            weekRangeLabel = uiState.weekRangeLabel,
                            selectedDateLabel = uiState.selectedWeekDateLabel,
                            days = uiState.weekDays,
                            isCurrentWeek = uiState.isCurrentWeek,
                            onPreviousWeek = viewModel::previousWeek,
                            onNextWeek = viewModel::nextWeek,
                            onCurrentWeek = viewModel::showCurrentWeek,
                            onSelectDate = viewModel::selectWeekDate,
                        )
                    }
                }

                uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
                    item {
                        MessagePanel(
                            message = error,
                            tone = MessageTone.Error,
                            actionLabel = "Réessayer",
                            onAction = viewModel::refresh,
                            onDismiss = viewModel::clearMessages,
                        )
                    }
                }

                uiState.userMessage?.takeIf { it.isNotBlank() }?.let { message ->
                    item {
                        MessagePanel(
                            message = message,
                            tone = MessageTone.Success,
                            actionLabel = "OK",
                            onAction = viewModel::clearMessages,
                            onDismiss = viewModel::clearMessages,
                        )
                    }
                }

                if (uiState.sections.isEmpty()) {
                    item {
                        when {
                            uiState.mode == FamilyHomeMode.WEEK && uiState.isWeekEmpty ->
                                EmptyFamilyWeekCard(
                                    title = "Rien de prévu cette semaine.",
                                    message = "Ajoute une tâche si la maison a besoin d'un repère.",
                                    onAddTask = onAddTask,
                                )
                            uiState.mode == FamilyHomeMode.WEEK ->
                                EmptyFamilyWeekCard(
                                    title = "Rien de prévu ce jour-là.",
                                    message = "Les autres jours de la semaine restent accessibles juste au-dessus.",
                                    onAddTask = onAddTask,
                                )
                            else -> EmptyFamilyHomeCard(onAddTask = onAddTask)
                        }
                    }
                } else {
                    items(
                        items = uiState.sections,
                        key = { section -> section.key },
                    ) { section ->
                        FamilyMemberSectionCard(
                            section = section,
                            actingOccurrenceId = uiState.actingOccurrenceId,
                            onQuickAction = viewModel::runQuickAction,
                            onOpenTask = onOpenTask,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyHomeHeader(
    mode: FamilyHomeMode,
    dateLabel: String,
    weekRangeLabel: String,
    completedTasks: Int,
    totalTasks: Int,
    onAddTask: () -> Unit,
    onShowToday: () -> Unit,
    onShowWeek: () -> Unit,
) {
    val progress = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks.toFloat()
    val subtitle =
        when (mode) {
            FamilyHomeMode.TODAY -> dateLabel
            FamilyHomeMode.WEEK -> weekRangeLabel
        }
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
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Ma maison",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = InkBrown,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkMuted,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Home,
                    contentDescription = null,
                    tint = WoodBrown,
                    modifier = Modifier.size(30.dp),
                )
            }

            FamilyHomeModeSwitch(
                mode = mode,
                onShowToday = onShowToday,
                onShowWeek = onShowWeek,
            )

            if (totalTasks > 0) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MossGreen,
                    trackColor = ParchmentCream,
                )
                Text(
                    text = "$completedTasks / $totalTasks tâches terminées",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
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
            } else {
                Text(
                    text =
                        if (mode == FamilyHomeMode.TODAY) {
                            "Aucune tâche planifiée aujourd'hui."
                        } else {
                            "S\u00e9lectionne un jour pour suivre la semaine."
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                )
            }
        }
    }
}

@Composable
private fun FamilyHomeModeSwitch(
    mode: FamilyHomeMode,
    onShowToday: () -> Unit,
    onShowWeek: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FamilyHomeModeButton(
            label = "Aujourd'hui",
            selected = mode == FamilyHomeMode.TODAY,
            onClick = onShowToday,
            modifier = Modifier.weight(1f),
        )
        FamilyHomeModeButton(
            label = "Semaine",
            selected = mode == FamilyHomeMode.WEEK,
            onClick = onShowWeek,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FamilyHomeModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = WoodBrown, contentColor = ParchmentLight),
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(label)
        }
    }
}

@Composable
private fun FamilyHomeWeekControls(
    weekRangeLabel: String,
    selectedDateLabel: String,
    days: List<FamilyTaskWeekDaySummary>,
    isCurrentWeek: Boolean,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onCurrentWeek: () -> Unit,
    onSelectDate: (String) -> Unit,
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onPreviousWeek) {
                    Text("<")
                }
                Text(
                    text = weekRangeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBrown,
                )
                TextButton(onClick = onNextWeek) {
                    Text(">")
                }
            }

            if (!isCurrentWeek) {
                OutlinedButton(
                    onClick = onCurrentWeek,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Semaine actuelle")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                days.forEach { day ->
                    FamilyHomeWeekDayButton(
                        day = day,
                        onClick = { onSelectDate(day.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Text(
                text = selectedDateLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkBrown,
            )
        }
    }
}

@Composable
private fun FamilyHomeWeekDayButton(
    day: FamilyTaskWeekDaySummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background =
        when {
            day.isSelected -> WoodBrown
            day.isToday -> SoftGold.copy(alpha = 0.28f)
            else -> ParchmentCream.copy(alpha = 0.82f)
        }
    val content =
        if (day.isSelected) {
            ParchmentLight
        } else {
            InkBrown
        }
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = background,
        contentColor = content,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = day.weekdayLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = day.dayNumberLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = day.progressLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = content.copy(alpha = 0.82f),
            )
        }
    }
}

@Composable
private fun FamilyMemberSectionCard(
    section: FamilyTaskMemberSection,
    actingOccurrenceId: Long?,
    onQuickAction: (FamilyTaskTodayItem) -> Unit,
    onOpenTask: (Long) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = ParchmentLight.copy(alpha = 0.96f),
                contentColor = InkBrown,
            ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = section.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBrown,
                )
                Text(
                    text = "${section.completedCount} / ${section.totalCount}",
                    style = MaterialTheme.typography.labelLarge,
                    color = InkMuted,
                )
            }

            section.tasks.forEach { row ->
                FamilyTaskRowCard(
                    task = row.task,
                    isActing = actingOccurrenceId == row.task.occurrenceId,
                    onQuickAction = onQuickAction,
                    onOpenTask = onOpenTask,
                )
            }
        }
    }
}

@Composable
private fun FamilyTaskRowCard(
    task: FamilyTaskTodayItem,
    isActing: Boolean,
    onQuickAction: (FamilyTaskTodayItem) -> Unit,
    onOpenTask: (Long) -> Unit,
) {
    val action = quickActionFor(task)
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = task.taskId > 0L) { onOpenTask(task.taskId) },
        shape = RoundedCornerShape(8.dp),
        color = ParchmentCream.copy(alpha = 0.78f),
        contentColor = InkBrown,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = InkBrown,
                    )
                    StatusChip(status = task.status)
                }
                familyTaskPriorityLabel(task.priority)?.let { priorityLabel ->
                    PriorityChip(priority = task.priority, label = priorityLabel)
                }
            }

            val details = task.details()
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
            }

            if (action != null) {
                OutlinedButton(
                    onClick = { onQuickAction(task) },
                    enabled = !isActing && canRunQuickAction(task),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = action.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isActing) "Mise à jour..." else familyTaskActionLabel(action))
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: FamilyTaskStatus) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = status.containerColor(),
        contentColor = status.contentColor(),
    ) {
        Text(
            text = familyTaskStatusLabel(status),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PriorityChip(
    priority: FamilyTaskPriority,
    label: String,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color =
            when (priority) {
                FamilyTaskPriority.URGENT -> SoftRed.copy(alpha = 0.18f)
                else -> WarningGlow.copy(alpha = 0.20f)
            },
        contentColor = if (priority == FamilyTaskPriority.URGENT) SoftRed else WoodBrown,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EmptyFamilyWeekCard(
    title: String,
    message: String,
    onAddTask: () -> Unit,
) {
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkBrown,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )
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
private fun EmptyFamilyHomeCard(onAddTask: () -> Unit) {
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
                text = "Tout est calme à la maison aujourd'hui.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkBrown,
            )
            Text(
                text = "Ajoute une tâche quand la journée a besoin d'un petit repère.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )
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
private fun MessagePanel(
    message: String,
    tone: MessageTone,
    actionLabel: String,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    val color = if (tone == MessageTone.Error) DangerGlow else MossGreen
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = ParchmentLight.copy(alpha = 0.97f),
        contentColor = InkBrown,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onAction) {
                    Text(actionLabel)
                }
                TextButton(onClick = onDismiss) {
                    Text("Fermer")
                }
            }
        }
    }
}

private enum class MessageTone {
    Error,
    Success,
}

private fun FamilyTaskTodayItem.details(): List<String> =
    buildList {
        familyTaskDueLabel(
            dueDate = dueDate,
            dueTime = dueTime,
            hasDueTime = hasDueTime,
            dueAt = dueAt,
        )?.let { add("Échéance $it") }
        recurrenceLabel?.let { add(it) }
        if (validationRequired) add("Validation requise")
        if (gamificationEnabled) add("Gamification active")
    }

private fun FamilyTaskQuickAction.icon() =
    when (this) {
        FamilyTaskQuickAction.COMPLETE -> Icons.Outlined.Check
        FamilyTaskQuickAction.VALIDATE -> Icons.Outlined.Check
        FamilyTaskQuickAction.REOPEN -> Icons.AutoMirrored.Outlined.Undo
    }

private fun FamilyTaskWeekDaySummary.progressLabel(): String =
    if (totalCount == 0) "-" else "$completedCount/$totalCount"


@Composable
private fun FamilyTaskStatus.containerColor(): Color =
    when (this) {
        FamilyTaskStatus.TODO -> ParchmentLight
        FamilyTaskStatus.COMPLETED -> MossGreen.copy(alpha = 0.18f)
        FamilyTaskStatus.PENDING_VALIDATION -> WarningGlow.copy(alpha = 0.22f)
        FamilyTaskStatus.VALIDATED -> MossGreen.copy(alpha = 0.24f)
        FamilyTaskStatus.SKIPPED -> InkMuted.copy(alpha = 0.12f)
        FamilyTaskStatus.UNKNOWN -> ParchmentCream
    }

@Composable
private fun FamilyTaskStatus.contentColor(): Color =
    when (this) {
        FamilyTaskStatus.COMPLETED,
        FamilyTaskStatus.VALIDATED,
        -> MossGreen
        FamilyTaskStatus.PENDING_VALIDATION -> WoodBrown
        FamilyTaskStatus.SKIPPED,
        FamilyTaskStatus.UNKNOWN,
        -> InkMuted
        FamilyTaskStatus.TODO -> InkBrown
    }

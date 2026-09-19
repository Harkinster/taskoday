package com.example.taskoday.features.familyhome

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onAddTask: (String?) -> Unit,
    onOpenAllTasks: () -> Unit,
    onOpenTask: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    val creationDate =
        familyTaskCreationDateForMode(
            mode = uiState.mode,
            selectedWeekDate = uiState.selectedWeekDate,
            todayDate = uiState.todayDate,
        )
    var hasObservedInitialResume by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (hasObservedInitialResume) {
            viewModel.refresh()
        } else {
            hasObservedInitialResume = true
        }
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
                contentPadding = PaddingValues(top = 10.dp, bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
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
                        pendingValidationTasks = uiState.pendingValidationTasks,
                        overdueTotalTasks = uiState.overdueTotalTasks,
                        creationDate = creationDate,
                        onAddTask = onAddTask,
                        onOpenAllTasks = onOpenAllTasks,
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

                uiState.secondaryErrorMessage?.takeIf { it.isNotBlank() }?.let { error ->
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

                if (uiState.mode == FamilyHomeMode.TODAY) {
                    if (uiState.overdueTotalTasks > 0) {
                        item {
                            FamilyOverdueSectionCard(
                                totalCount = uiState.overdueTotalTasks,
                                rows = uiState.overdueTasks,
                                todayDate = uiState.todayDate,
                                actingOccurrenceId = uiState.actingOccurrenceId,
                                onQuickAction = viewModel::runQuickAction,
                                onOpenTask = onOpenTask,
                            )
                        }
                    }

                    if (uiState.sections.isEmpty() && uiState.overdueTotalTasks == 0) {
                        item { EmptyFamilyHomeCard() }
                    } else {
                        item {
                            FamilyHomeSectionTitle(
                                title = "Aujourd'hui",
                                detail = "${uiState.completedTasks} / ${uiState.totalTasks} terminées",
                            )
                        }
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

                    if (uiState.upcomingSections.isNotEmpty()) {
                        item {
                            FamilyUpcomingSectionCard(
                                sections = uiState.upcomingSections,
                                totalCount = uiState.upcomingTotalTasks,
                                hasMore = uiState.hasMoreUpcomingTasks,
                                onShowWeek = viewModel::showWeek,
                                onOpenTask = onOpenTask,
                            )
                        }
                    }
                } else {
                    if (uiState.sections.isEmpty()) {
                        item {
                            if (uiState.isWeekEmpty) {
                                EmptyFamilyWeekCard(
                                    title = "Rien de prévu cette semaine.",
                                    message = "Ajoute une tâche si la maison a besoin d'un repère.",
                                )
                            } else {
                                EmptyFamilyWeekCard(
                                    title = "Rien de prévu ce jour-là.",
                                    message = "Les autres jours de la semaine restent accessibles juste au-dessus.",
                                )
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
}

@Composable
private fun FamilyHomeHeader(
    mode: FamilyHomeMode,
    dateLabel: String,
    weekRangeLabel: String,
    completedTasks: Int,
    totalTasks: Int,
    pendingValidationTasks: Int,
    overdueTotalTasks: Int,
    creationDate: String?,
    onAddTask: (String?) -> Unit,
    onOpenAllTasks: () -> Unit,
    onShowToday: () -> Unit,
    onShowWeek: () -> Unit,
) {
    val subtitle =
        when (mode) {
            FamilyHomeMode.TODAY -> dateLabel
            FamilyHomeMode.WEEK -> weekRangeLabel
        }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "Ma maison",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ParchmentLight,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ParchmentCream.copy(alpha = 0.72f),
                )
            }
            TextButton(
                onClick = { onAddTask(creationDate) },
                colors = ButtonDefaults.textButtonColors(contentColor = ParchmentLight),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ajouter")
            }
        }

        if (mode == FamilyHomeMode.TODAY && (totalTasks > 0 || overdueTotalTasks > 0 || pendingValidationTasks > 0)) {
            Text(
                text =
                    buildList {
                        if (totalTasks > 0) add("$completedTasks / $totalTasks terminées")
                        if (pendingValidationTasks > 0) add("$pendingValidationTasks à valider")
                        if (overdueTotalTasks > 0) add("$overdueTotalTasks en retard")
                    }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = ParchmentCream.copy(alpha = 0.72f),
            )
        }

        FamilyHomeModeSwitch(
            mode = mode,
            onShowToday = onShowToday,
            onShowWeek = onShowWeek,
            onOpenAllTasks = onOpenAllTasks,
        )
    }
}

@Composable
private fun FamilyHomeModeSwitch(
    mode: FamilyHomeMode,
    onShowToday: () -> Unit,
    onShowWeek: () -> Unit,
    onOpenAllTasks: () -> Unit,
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
            label = "À venir",
            selected = mode == FamilyHomeMode.WEEK,
            onClick = onShowWeek,
            modifier = Modifier.weight(1f),
        )
        FamilyHomeModeButton(
            label = "Toutes",
            selected = false,
            onClick = onOpenAllTasks,
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
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(if (selected) ParchmentLight else Color.White.copy(alpha = 0.05f))
                .border(1.dp, ParchmentLight.copy(alpha = if (selected) 0f else 0.12f), shape)
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) InkBrown else ParchmentCream.copy(alpha = 0.74f),
            maxLines = 1,
        )
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
private fun FamilyHomeSectionTitle(
    title: String,
    detail: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = ParchmentLight,
        )
        detail?.takeIf { !it.startsWith("0 / 0") }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = ParchmentCream.copy(alpha = 0.70f),
            )
        }
    }
}

@Composable
private fun FamilyOverdueSectionCard(
    totalCount: Int,
    rows: List<FamilyTaskRow>,
    todayDate: String?,
    actingOccurrenceId: Long?,
    onQuickAction: (FamilyTaskTodayItem) -> Unit,
    onOpenTask: (Long) -> Unit,
) {
    val today = parseFamilyTaskDateInput(todayDate.orEmpty()) ?: java.time.LocalDate.now()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "En retard · $totalCount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBrown,
                )
                if (rows.size < totalCount) {
                    Text(
                        text = "${rows.size} affichées",
                        style = MaterialTheme.typography.labelMedium,
                        color = InkMuted,
                    )
                }
            }
            rows.forEach { row ->
                FamilyOverdueRow(
                    task = row.task,
                    today = today,
                    isActing = actingOccurrenceId == row.task.occurrenceId,
                    onQuickAction = onQuickAction,
                    onOpenTask = onOpenTask,
                )
            }
        }
}

@Composable
private fun FamilyOverdueRow(
    task: FamilyTaskTodayItem,
    today: java.time.LocalDate,
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
        color = ParchmentLight.copy(alpha = 0.92f),
        contentColor = InkBrown,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = InkBrown,
                    )
                    Text(
                        text = familyTaskOverdueMetaLabel(task = task, today = today),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkMuted,
                    )
                }
                familyTaskPriorityLabel(task.priority)?.let { priorityLabel ->
                    PriorityChip(priority = task.priority, label = priorityLabel)
                }
            }
            if (action == FamilyTaskQuickAction.COMPLETE) {
                OutlinedButton(
                    onClick = { onQuickAction(task) },
                    enabled = !isActing && canRunQuickAction(task),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isActing) "Mise à jour..." else "Terminer")
                }
            }
        }
    }
}

@Composable
private fun FamilyUpcomingSectionCard(
    sections: List<FamilyTaskUpcomingDaySection>,
    totalCount: Int,
    hasMore: Boolean,
    onShowWeek: () -> Unit,
    onOpenTask: (Long) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "À venir",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ParchmentLight,
            )
            Text(
                text = "$totalCount sur 7 jours",
                style = MaterialTheme.typography.labelMedium,
                color = ParchmentCream.copy(alpha = 0.70f),
            )
        }
        sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = section.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = ParchmentCream.copy(alpha = 0.82f),
                )
                section.tasks.forEach { row ->
                    FamilyUpcomingRow(
                        task = row.task,
                        onOpenTask = onOpenTask,
                    )
                }
            }
        }
        if (hasMore) {
            TextButton(onClick = onShowWeek) {
                Text("Voir la semaine")
            }
        }
    }
}

@Composable
private fun FamilyUpcomingRow(
    task: FamilyTaskTodayItem,
    onOpenTask: (Long) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = task.taskId > 0L) { onOpenTask(task.taskId) },
        shape = RoundedCornerShape(8.dp),
        color = ParchmentLight.copy(alpha = 0.96f),
        contentColor = InkBrown,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBrown,
                )
                Text(
                    text = familyTaskUpcomingMetaLabel(task),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                )
            }
            familyTaskPriorityLabel(task.priority)?.let { priorityLabel ->
                PriorityChip(priority = task.priority, label = priorityLabel)
            }
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
        }
    }
}

@Composable
private fun EmptyFamilyHomeCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = ParchmentLight.copy(alpha = 0.96f),
        contentColor = InkBrown,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Aucune tâche aujourd'hui",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = InkBrown,
            )
            Text(
                text = "Utilise + pour ajouter un repère à la journée.",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
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
        familyTaskOccurrenceRecurrenceLabel(recurrenceLabel)?.let { add(it) }
        if (validationRequired) add("Validation requise")
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

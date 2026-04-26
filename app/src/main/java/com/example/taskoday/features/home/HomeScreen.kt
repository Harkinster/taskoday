package com.example.taskoday.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.NeonButton
import com.example.taskoday.core.ui.component.fantasy.NeonButtonStyle
import com.example.taskoday.core.ui.component.fantasy.NeonCard
import com.example.taskoday.core.ui.component.fantasy.NeonTone
import com.example.taskoday.core.ui.component.fantasy.ProgressHeroCard
import com.example.taskoday.core.ui.component.fantasy.RoutineItemRow
import com.example.taskoday.core.ui.component.fantasy.RoutineSectionCard
import com.example.taskoday.core.ui.component.fantasy.TaskodayHeader
import com.example.taskoday.core.ui.theme.ArcaneViolet
import com.example.taskoday.core.ui.theme.NeonBlue
import com.example.taskoday.core.ui.theme.NeonCyan
import com.example.taskoday.core.ui.theme.StarWhite
import com.example.taskoday.core.ui.theme.TextMuted
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.QuestForDay
import com.example.taskoday.domain.model.TaskForDay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenTasks: () -> Unit,
    onOpenWeek: () -> Unit,
    onOpenTask: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val tasksForDay =
        if (uiState.usingRemoteData) {
            uiState.tasksForDay.filter { it.task.id < 0L }
        } else {
            uiState.tasksForDay.filter { it.task.id > 0L }
        }
    val questsForDay =
        if (uiState.usingRemoteData) {
            uiState.questsForDay.filter { it.quest.id < 0L }
        } else {
            uiState.questsForDay.filter { it.quest.id > 0L }
        }
    val planningItems = buildPlanningItems(tasksForDay, questsForDay)
    val completedCount = planningItems.count { it.isCompleted }
    val progress = if (planningItems.isEmpty()) 0f else completedCount.toFloat() / planningItems.size.toFloat()
    val sections = buildSections(planningItems)

    val selectedDate =
        if (uiState.selectedDayStartMillis == 0L) {
            LocalDate.now()
        } else {
            DateTimeUtils.toLocalDate(uiState.selectedDayStartMillis)
        }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                        title = "Bonjour, Alex !",
                        subtitle = "Organise ta journee, etape par etape.",
                        avatarInitials = "AB",
                    )
                }

                item {
                    HomeDateCard(
                        dateLabel = uiState.dateLabel,
                        onPrevious = viewModel::goToPreviousDay,
                        onNext = viewModel::goToNextDay,
                        onToday = viewModel::goToToday,
                        onOpenCalendar = { showDatePicker = true },
                    )
                }

                item {
                    ProgressHeroCard(
                        title = "Routines du jour",
                        completed = completedCount,
                        total = planningItems.size,
                        progress = progress,
                        subtitle =
                            if (planningItems.isEmpty()) {
                                "Aucune routine pour ce jour."
                            } else {
                                "Continue comme ca !"
                            },
                        badgeLabel = "${uiState.pointsBalance} points",
                    )
                }

                if (uiState.usingRemoteData) {
                    item {
                        NeonCard(tone = NeonTone.Blue) {
                            Text(
                                text = "Mode synchronise",
                                style = MaterialTheme.typography.titleSmall,
                                color = StarWhite,
                            )
                            Text(
                                text = "Les routines du jour viennent du backend.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                    }
                }

                if (!uiState.errorMessage.isNullOrBlank()) {
                    item {
                        NeonCard(tone = NeonTone.Danger) {
                            Text(
                                text = "Alerte reseau",
                                style = MaterialTheme.typography.titleSmall,
                                color = StarWhite,
                            )
                            Text(
                                text = uiState.errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                            NeonButton(
                                text = "Fermer",
                                onClick = viewModel::clearError,
                                style = NeonButtonStyle.Outline,
                            )
                        }
                    }
                }

                sections.forEach { section ->
                    item(key = "section-${section.dayPart.name}") {
                        DayPartCard(
                            section = section,
                            onOpenTask = onOpenTask,
                            onToggleCheck = { item, checked ->
                                when {
                                    item.taskForDay != null -> viewModel.setTaskChecked(item.taskForDay, checked)
                                    item.questForDay != null -> viewModel.setQuestChecked(item.questForDay, checked)
                                }
                                if (checked) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(POSITIVE_MESSAGES.random())
                                    }
                                }
                            },
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    ) {
                        NeonButton(
                            text = "Semaine",
                            onClick = onOpenWeek,
                            style = NeonButtonStyle.Outline,
                            modifier = Modifier.weight(1f),
                        )
                        NeonButton(
                            text = "Missions",
                            onClick = onOpenTasks,
                            style = NeonButtonStyle.Filled,
                            modifier = Modifier.weight(1f),
                        )
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
}

@Composable
private fun HomeDateCard(
    dateLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onOpenCalendar: () -> Unit,
) {
    NeonCard(tone = NeonTone.Violet) {
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToday) {
                Icon(
                    imageVector = Icons.Outlined.Today,
                    contentDescription = "Aujourd hui",
                    tint = NeonCyan,
                )
            }
            IconButton(onClick = onOpenCalendar) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = "Calendrier",
                    tint = NeonCyan,
                )
            }
        }
    }
}

@Composable
private fun DayPartCard(
    section: HomeSection,
    onOpenTask: (Long) -> Unit,
    onToggleCheck: (HomePlanningItem, Boolean) -> Unit,
) {
    val doneCount = section.items.count { it.isCompleted }

    RoutineSectionCard(
        title = "${section.dayPart.emoji()} ${section.dayPart.label()}",
        progressLabel = "$doneCount/${section.items.size}",
        tone = section.tone,
    ) {
        if (section.items.isEmpty()) {
            Text(
                text = "Rien de prevu pour ce moment.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            return@RoutineSectionCard
        }

        section.items.forEach { item ->
            val openTaskAction =
                item.taskForDay?.task
                    ?.takeIf { task -> task.id > 0L }
                    ?.let { task -> { onOpenTask(task.id) } }

            RoutineItemRow(
                title = item.title,
                emoji = item.emoji,
                done = item.isCompleted,
                subtitle = formatPlanningTime(item),
                onClick = openTaskAction,
                onToggleDone = { onToggleCheck(item, !item.isCompleted) },
            )
        }
    }
}

private data class HomeSection(
    val dayPart: DayPart,
    val tone: NeonTone,
    val items: List<HomePlanningItem>,
)

private data class HomePlanningItem(
    val key: String,
    val dayPart: DayPart,
    val title: String,
    val emoji: String,
    val dueDate: Long?,
    val isCompleted: Boolean,
    val taskForDay: TaskForDay? = null,
    val questForDay: QuestForDay? = null,
)

private fun buildPlanningItems(
    tasks: List<TaskForDay>,
    quests: List<QuestForDay>,
): List<HomePlanningItem> {
    val taskItems =
        tasks.map { item ->
            HomePlanningItem(
                key = "task-${item.task.id}",
                dayPart = item.task.dayPart,
                title = item.task.title,
                emoji = item.task.emoji,
                dueDate = item.task.dueDate,
                isCompleted = item.isCompleted,
                taskForDay = item,
            )
        }
    val questItems =
        quests.map { item ->
            HomePlanningItem(
                key = "quest-${item.quest.id}",
                dayPart = item.quest.dayPart,
                title = item.quest.title,
                emoji = item.quest.emoji,
                dueDate = null,
                isCompleted = item.isCompletedForDay,
                questForDay = item,
            )
        }
    return (taskItems + questItems).sortedBy { it.dueDate ?: Long.MAX_VALUE }
}

private fun buildSections(items: List<HomePlanningItem>): List<HomeSection> {
    val tones =
        mapOf(
            DayPart.MATIN to NeonTone.Cyan,
            DayPart.MATINEE to NeonTone.Blue,
            DayPart.MIDI to NeonTone.Blue,
            DayPart.APRES_MIDI to NeonTone.Blue,
            DayPart.SOIR to NeonTone.Violet,
            DayPart.SOIREE to NeonTone.Violet,
        )
    return DayPart.entries.map { dayPart ->
        HomeSection(
            dayPart = dayPart,
            tone = tones.getValue(dayPart),
            items = items.filter { it.dayPart == dayPart },
        )
    }
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

private val POSITIVE_MESSAGES: List<String> =
    listOf(
        "Bravo !",
        "Excellent travail !",
        "Mission validee !",
        "Continue ton aventure !",
    )

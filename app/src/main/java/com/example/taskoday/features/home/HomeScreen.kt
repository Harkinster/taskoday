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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.FantasyAssetBubble
import com.example.taskoday.core.ui.component.fantasy.FantasyCard
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
import com.example.taskoday.domain.model.TaskForDay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenTask: (Long) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val visibleTasksForDay =
        if (uiState.usingRemoteData) {
            uiState.tasksForDay.filter { it.task.id < 0L }
        } else {
            uiState.tasksForDay.filter { it.task.id > 0L }
        }
    val routinesForDay = visibleTasksForDay.filter { it.task.isDaily }
    val planningItems = buildPlanningItems(routinesForDay)
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                        title = "Routines du jour",
                        completed = completedCount,
                        total = planningItems.size,
                        progress = progress,
                        subtitle =
                            if (planningItems.isEmpty()) {
                                "Rien de prévu pour cette routine."
                            } else {
                                "Chaque geste nourrit Le Nid."
                            },
                        badgeLabel = "${uiState.pointsBalance} Flammèches",
                    )
                }

                if (uiState.usingRemoteData) {
                    item {
                        FantasyCard(tone = FantasyTone.Night) {
                            Text(
                                text = "Mode synchronisé",
                                style = MaterialTheme.typography.titleSmall,
                                color = WoodBrownDark,
                            )
                            Text(
                                text = "Les routines viennent du backend.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkMuted,
                            )
                        }
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
                            title = "Routine vide",
                            message = "Rien de prévu pour cette routine.",
                            assetResId = NestAssets.interfaceAsset("nid"),
                            assetDescription = null,
                            tone = FantasyTone.Moss,
                        )
                    }
                } else {
                    sections.filter { section -> section.items.isNotEmpty() }.forEach { section ->
                        item(key = "section-${section.dayPart.name}") {
                            DayPartCard(
                                section = section,
                                onOpenTask = onOpenTask,
                                onToggleCheck = { item, checked ->
                                    item.taskForDay?.let { taskForDay ->
                                        viewModel.setTaskChecked(taskForDay, checked)
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
private fun DayPartCard(
    section: HomeSection,
    onOpenTask: (Long) -> Unit,
    onToggleCheck: (HomePlanningItem, Boolean) -> Unit,
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
                    assetResId = dayPartAsset(section.dayPart),
                    contentDescription = section.dayPart.label(),
                    size = 34.dp,
                )
                Text(
                    text = section.dayPart.label(),
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
        if (section.items.isEmpty()) {
            Text(
                text = "Rien de prévu pour ce moment.",
                style = MaterialTheme.typography.bodyMedium,
                color = InkMuted,
            )
            return@FantasyCard
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
    val tone: FantasyTone,
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
)

private fun buildPlanningItems(tasks: List<TaskForDay>): List<HomePlanningItem> =
    tasks
        .map { item ->
            HomePlanningItem(
                key = "routine-${item.task.id}",
                dayPart = item.task.dayPart,
                title = item.task.title,
                emoji = item.task.emoji,
                dueDate = item.task.dueDate,
                isCompleted = item.isCompleted,
                taskForDay = item,
            )
        }.sortedBy { it.dueDate ?: Long.MAX_VALUE }

private fun buildSections(items: List<HomePlanningItem>): List<HomeSection> {
    val tones =
        mapOf(
            DayPart.MATIN to FantasyTone.Gold,
            DayPart.MATINEE to FantasyTone.Moss,
            DayPart.MIDI to FantasyTone.Wood,
            DayPart.APRES_MIDI to FantasyTone.Ember,
            DayPart.SOIR to FantasyTone.Violet,
            DayPart.SOIREE to FantasyTone.Night,
        )
    return DayPart.entries.map { dayPart ->
        HomeSection(
            dayPart = dayPart,
            tone = tones.getValue(dayPart),
            items = items.filter { it.dayPart == dayPart },
        )
    }
}

private fun dayPartAsset(dayPart: DayPart): Int =
    when (dayPart) {
        DayPart.MATIN,
        DayPart.MATINEE,
        -> NestAssets.interfaceAsset("flammeche")
        DayPart.MIDI,
        DayPart.APRES_MIDI,
        -> NestAssets.interfaceAsset("crystal")
        DayPart.SOIR,
        DayPart.SOIREE,
        -> NestAssets.interfaceAsset("nid")
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
        "Routine validée !",
        "Continue ton aventure !",
    )

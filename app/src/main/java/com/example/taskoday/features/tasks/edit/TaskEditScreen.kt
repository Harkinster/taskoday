package com.example.taskoday.features.tasks.edit

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.testing.TaskodayTestTags
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.core.util.DateTimeUtils
import com.example.taskoday.domain.model.DayPart
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    viewModel: TaskEditViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            viewModel.consumeSaveCompletion()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.taskId == null) "Nouvelle mission" else "Modifier mission",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::saveTask,
                        enabled = !uiState.isSaving,
                        modifier = Modifier.testTag(TaskodayTestTags.TaskEditSaveButton),
                    ) {
                        Text(text = if (uiState.isSaving) "..." else "Enregistrer")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
            ) {
                Text(
                    text = "Titre + rubrique + routine: simple et rapide.",
                    modifier = Modifier.padding(spacing.medium),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChanged,
                label = { Text(text = "Titre") },
                placeholder = { Text(text = "Ex: Se brosser les dents") },
                modifier = Modifier.fillMaxWidth().testTag(TaskodayTestTags.TaskEditTitleField),
                singleLine = true,
                supportingText = {
                    uiState.errorMessage?.let { error ->
                        Text(text = error, color = MaterialTheme.colorScheme.error)
                    }
                },
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChanged,
                label = { Text(text = "Détail (optionnel)") },
                modifier = Modifier.fillMaxWidth().testTag(TaskodayTestTags.TaskEditDescriptionField),
                maxLines = 2,
            )

            Text(text = "Icône", style = MaterialTheme.typography.titleMedium)
            EmojiPickerRow(
                selected = uiState.emoji,
                onSelected = viewModel::onEmojiChanged,
            )

            Text(text = "Rubrique de la journée", style = MaterialTheme.typography.titleMedium)
            DayPartPickerRow(
                selected = uiState.dayPart,
                onSelected = viewModel::onDayPartChanged,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Routine récurrente", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = uiState.isRoutine,
                    onCheckedChange = viewModel::onRoutineChanged,
                )
            }

            if (uiState.isRoutine) {
                Text(text = "Fréquence", style = MaterialTheme.typography.titleMedium)
                RoutineDaysPickerRow(
                    selectedDays = uiState.routineDays,
                    onEveryDay = viewModel::onEveryDayRoutineSelected,
                    onToggleDay = viewModel::onRoutineDayToggled,
                )
            } else {
                Text(text = "Jour exceptionnel", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    QuickDateChip(
                        text = "Aujourd'hui",
                        selected = isSelectedDay(uiState.scheduledDate, LocalDate.now()),
                        onClick = { viewModel.onScheduledDateChanged(DateTimeUtils.startOfDayMillis(LocalDate.now())) },
                    )
                    QuickDateChip(
                        text = "Demain",
                        selected = isSelectedDay(uiState.scheduledDate, LocalDate.now().plusDays(1)),
                        onClick = { viewModel.onScheduledDateChanged(DateTimeUtils.startOfDayMillis(LocalDate.now().plusDays(1))) },
                    )
                    QuickDateChip(
                        text = "Hier",
                        selected = isSelectedDay(uiState.scheduledDate, LocalDate.now().minusDays(1)),
                        onClick = { viewModel.onScheduledDateChanged(DateTimeUtils.startOfDayMillis(LocalDate.now().minusDays(1))) },
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    Icon(imageVector = Icons.Outlined.CalendarMonth, contentDescription = null)
                    Text(
                        text = DateTimeUtils.formatDayLabel(uiState.scheduledDate),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmojiPickerRow(
    selected: String,
    onSelected: (String) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        CHILD_TASK_EMOJIS.forEach { emoji ->
            FilterChip(
                selected = selected == emoji,
                onClick = { onSelected(emoji) },
                label = { Text(text = emoji) },
            )
        }
    }
}

@Composable
private fun DayPartPickerRow(
    selected: DayPart,
    onSelected: (DayPart) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        DayPart.entries.forEach { dayPart ->
            FilterChip(
                selected = selected == dayPart,
                onClick = { onSelected(dayPart) },
                label = { Text(text = "${dayPart.emoji()} ${dayPart.label()}") },
            )
        }
    }
}

@Composable
private fun RoutineDaysPickerRow(
    selectedDays: Set<Int>,
    onEveryDay: () -> Unit,
    onToggleDay: (Int) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        FilterChip(
            selected = selectedDays.isEmpty(),
            onClick = onEveryDay,
            label = { Text(text = "Tous les jours") },
        )
        WEEKDAY_LABELS.forEach { (iso, label) ->
            FilterChip(
                selected = selectedDays.contains(iso),
                onClick = { onToggleDay(iso) },
                label = { Text(text = label) },
            )
        }
    }
}

@Composable
private fun QuickDateChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = text) },
    )
}

private fun isSelectedDay(
    selectedDayStartMillis: Long,
    date: LocalDate,
): Boolean = selectedDayStartMillis == DateTimeUtils.startOfDayMillis(date)

private val WEEKDAY_LABELS: List<Pair<Int, String>> =
    listOf(
        1 to "Lun",
        2 to "Mar",
        3 to "Mer",
        4 to "Jeu",
        5 to "Ven",
        6 to "Sam",
        7 to "Dim",
    )

private val CHILD_TASK_EMOJIS: List<String> =
    listOf(
        "\uD83E\uDE65",
        "\uD83C\uDF92",
        "\uD83D\uDCDA",
        "\uD83C\uDF7D\uFE0F",
        "\uD83C\uDFC3",
        "\uD83E\uDDE9",
        "\uD83C\uDFA8",
        "\uD83E\uDDF8",
        "\uD83C\uDF1F",
        "\uD83C\uDFB5",
        "\uD83C\uDF19",
    )

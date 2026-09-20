package com.example.taskoday.features.familyhome

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.FormBottomBar
import com.example.taskoday.core.ui.theme.DangerGlow
import com.example.taskoday.core.ui.theme.InkBrown
import com.example.taskoday.core.ui.theme.InkMuted
import com.example.taskoday.core.ui.theme.ParchmentLight
import com.example.taskoday.core.ui.theme.SoftGold
import com.example.taskoday.core.ui.theme.WoodBrown
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.domain.model.FamilyTaskMember
import com.example.taskoday.domain.model.FamilyTaskMemberRole
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskRecurrence
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

@Composable
fun FamilyTaskCreateScreen(
    viewModel: FamilyTaskCreateViewModel,
    onBack: () -> Unit,
    onCreated: () -> Unit,
    quickMode: Boolean = false,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    LaunchedEffect(uiState.created) {
        if (uiState.created) {
            onCreated()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!uiState.isLoadingTask) {
                FormBottomBar(
                    onBack = onBack,
                    onPrimary = viewModel::submit,
                    primaryEnabled = !uiState.isSubmitting,
                    primaryLabel = when {
                        uiState.isSubmitting && uiState.isEditing -> "Enregistrement..."
                        uiState.isSubmitting -> "Création..."
                        uiState.isEditing -> "Enregistrer"
                        else -> "Créer la tâche"
                    },
                    backLabel = "Annuler",
                )
            }
        },
    ) { innerPadding ->
        FantasyScreenBackground(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .padding(innerPadding),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.medium),
                contentPadding = PaddingValues(top = spacing.large, bottom = spacing.xxLarge),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                item {
                    FamilyTaskCreateHeader(isEditing = uiState.isEditing, quickMode = quickMode, onBack = onBack)
                }

                if (uiState.isLoadingTask) {
                    item {
                        LoadingTaskCard()
                    }
                } else {
                    if (quickMode) {
                        item {
                            QuickTaskForm(
                                uiState = uiState,
                                onTitleChanged = viewModel::onTitleChanged,
                                onDateChanged = viewModel::onDateChanged,
                                onTimeChanged = viewModel::onTimeChanged,
                                onClearTime = viewModel::clearTime,
                                onSelectHouse = viewModel::selectHouseTask,
                                onToggleAssignee = viewModel::toggleAssignee,
                                onRecurrenceChanged = viewModel::onRecurrenceChanged,
                                onToggleWeekday = viewModel::toggleWeekday,
                                onCustomizeRecurrence = viewModel::customizeRecurrence,
                                onIntervalChanged = viewModel::onRecurrenceIntervalChanged,
                                onCustomUnitChanged = viewModel::onCustomRecurrenceUnitChanged,
                                onDescriptionChanged = viewModel::onDescriptionChanged,
                                onValidationRequiredChanged = viewModel::onValidationRequiredChanged,
                                onGamificationEnabledChanged = viewModel::onGamificationEnabledChanged,
                                onPriorityChanged = viewModel::onPriorityChanged,
                            )
                        }
                    } else {
                        item { FamilyTaskCreateFormCard(uiState, viewModel::onTitleChanged, viewModel::onDescriptionChanged, viewModel::onDateChanged, viewModel::onTimeChanged, viewModel::clearTime) }
                        item { AssigneesCard(uiState, viewModel::selectHouseTask, viewModel::toggleAssignee) }
                        item { RecurrenceCard(uiState, viewModel::onRecurrenceChanged, viewModel::toggleWeekday, viewModel::customizeRecurrence, viewModel::onRecurrenceIntervalChanged, viewModel::onCustomRecurrenceUnitChanged) }
                        item { TaskOptionsCard(uiState, viewModel::onValidationRequiredChanged, viewModel::onGamificationEnabledChanged, viewModel::onPriorityChanged) }
                    }
                }

                uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
                    item {
                        ErrorCard(error)
                    }
                }

            }
        }
    }
}

@Composable
private fun QuickTaskForm(
    uiState: FamilyTaskCreateUiState,
    onTitleChanged: (String) -> Unit,
    onDateChanged: (String) -> Unit,
    onTimeChanged: (String) -> Unit,
    onClearTime: () -> Unit,
    onSelectHouse: () -> Unit,
    onToggleAssignee: (Long) -> Unit,
    onRecurrenceChanged: (FamilyTaskRecurrence) -> Unit,
    onToggleWeekday: (Int) -> Unit,
    onCustomizeRecurrence: () -> Unit,
    onIntervalChanged: (Int) -> Unit,
    onCustomUnitChanged: (CustomRecurrenceUnit) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onValidationRequiredChanged: (Boolean) -> Unit,
    onGamificationEnabledChanged: (Boolean) -> Unit,
    onPriorityChanged: (FamilyTaskPriority) -> Unit,
) {
    var showMore by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        requester.requestFocus()
        keyboard?.show()
    }
    val today = LocalDate.now()
    FamilyTaskCreateCard(title = "Que faut-il faire ?") {
        OutlinedTextField(
            value = uiState.title,
            onValueChange = onTitleChanged,
            placeholder = { Text("Ex. Ranger sa chambre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(requester),
        )
        Text("Pour qui ?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = InkBrown)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = uiState.isHouseTask, onClick = onSelectHouse, label = { Text("Maison") })
            uiState.members.forEach { member ->
                FilterChip(selected = member.userId in uiState.selectedAssigneeUserIds, onClick = { onToggleAssignee(member.userId) }, label = { Text(member.displayName) })
            }
        }
        Text("Quand ?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = InkBrown)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = uiState.date == today.toString(), onClick = { onDateChanged(today.toString()) }, label = { Text("Aujourd'hui") })
            FilterChip(selected = uiState.date == today.plusDays(1).toString(), onClick = { onDateChanged(today.plusDays(1).toString()) }, label = { Text("Demain") })
            FilterChip(selected = false, onClick = { onDateChanged(today.toString()) }, label = { Text("Cette semaine") })
            FilterChip(selected = false, onClick = {
                val selected = parseFamilyTaskDateInput(uiState.date) ?: today
                DatePickerDialog(context, { _, year, month, day -> onDateChanged(LocalDate.of(year, month + 1, day).toString()) }, selected.year, selected.monthValue - 1, selected.dayOfMonth).show()
            }, label = { Text("Choisir") })
        }
        TextButton(onClick = {
            val selected = parseFamilyTaskTimeInput(uiState.time) ?: LocalTime.now()
            TimePickerDialog(context, { _, hour, minute -> onTimeChanged(String.format(Locale.US, "%02d:%02d", hour, minute)) }, selected.hour, selected.minute, true).show()
        }) { Text(if (uiState.time.isBlank()) "+ Ajouter une heure" else "Heure : ${uiState.time}") }
        TextButton(onClick = { showMore = !showMore }) { Text(if (showMore) "Moins d'options" else "Plus d'options") }
        if (showMore) {
            OutlinedTextField(value = uiState.description, onValueChange = onDescriptionChanged, label = { Text("Note (facultatif)") }, minLines = 2, maxLines = 3, modifier = Modifier.fillMaxWidth())
            RecurrenceCard(uiState, onRecurrenceChanged, onToggleWeekday, onCustomizeRecurrence, onIntervalChanged, onCustomUnitChanged)
            TaskOptionsCard(uiState, onValidationRequiredChanged, onGamificationEnabledChanged, onPriorityChanged)
        }
    }
}

@Composable
private fun FamilyTaskCreateHeader(
    isEditing: Boolean,
    quickMode: Boolean = false,
    onBack: () -> Unit,
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
        Row(
            modifier = Modifier.padding(14.dp),
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
                    text = if (isEditing) "Modifier la tâche familiale" else "Nouvelle tâche familiale",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBrown,
                )
                Text(
                    text =
                        if (isEditing) {
                            "Ajuste l'organisation sans toucher aux occurrences à la main."
                        } else {
                            "Organise la journée sans mélanger la partie Chronodria."
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                )
            }
            Icon(Icons.Outlined.Home, contentDescription = null, tint = WoodBrown, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun FamilyTaskCreateFormCard(
    uiState: FamilyTaskCreateUiState,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onDateChanged: (String) -> Unit,
    onTimeChanged: (String) -> Unit,
    onClearTime: () -> Unit,
) {
    val context = LocalContext.current
    val selectedDate = parseFamilyTaskDateInput(uiState.date) ?: LocalDate.now()
    val selectedTime = parseFamilyTaskTimeInput(uiState.time) ?: LocalTime.now()

    FamilyTaskCreateCard(title = "Détails") {
        OutlinedTextField(
            value = uiState.title,
            onValueChange = onTitleChanged,
            label = { Text("Titre obligatoire") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.description,
            onValueChange = onDescriptionChanged,
            label = { Text("Description facultative") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            onDateChanged(LocalDate.of(year, month + 1, dayOfMonth).toString())
                        },
                        selectedDate.year,
                        selectedDate.monthValue - 1,
                        selectedDate.dayOfMonth,
                    ).show()
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(formatFamilyTaskDateLabel(uiState.date))
            }
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            onTimeChanged(String.format(Locale.US, "%02d:%02d", hourOfDay, minute))
                        },
                        selectedTime.hour,
                        selectedTime.minute,
                        true,
                    ).show()
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(formatFamilyTaskTimeLabel(uiState.time))
            }
        }
        if (uiState.time.isNotBlank()) {
            TextButton(onClick = onClearTime) {
                Text("Retirer l'heure")
            }
        }
    }
}

@Composable
private fun LoadingTaskCard() {
    FamilyTaskCreateCard(title = "Chargement") {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = SoftGold)
            Text("Chargement de la tâche familiale...", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
        }
    }
}

@Composable
private fun AssigneesCard(
    uiState: FamilyTaskCreateUiState,
    onSelectHouse: () -> Unit,
    onToggleAssignee: (Long) -> Unit,
) {
    FamilyTaskCreateCard(title = "Assignation") {
        Text(
            text = "Choisis un ou plusieurs membres, ou laisse la tâche pour la Maison.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkMuted,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = uiState.isHouseTask,
                onClick = onSelectHouse,
                label = { Text("Maison") },
            )
            uiState.members.forEach { member ->
                FilterChip(
                    selected = member.userId in uiState.selectedAssigneeUserIds,
                    onClick = { onToggleAssignee(member.userId) },
                    label = { Text(member.memberLabel()) },
                )
            }
        }
        if (uiState.isLoadingMembers) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = SoftGold)
                Text("Chargement des membres...", style = MaterialTheme.typography.bodySmall, color = InkMuted)
            }
        } else if (uiState.members.isEmpty()) {
            Text(
                text = "Aucun membre chargé. La tâche peut rester non assignée.",
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
        }
    }
}

@Composable
private fun RecurrenceCard(
    uiState: FamilyTaskCreateUiState,
    onRecurrenceChanged: (FamilyTaskRecurrence) -> Unit,
    onToggleWeekday: (Int) -> Unit,
    onCustomizeRecurrence: () -> Unit,
    onIntervalChanged: (Int) -> Unit,
    onCustomUnitChanged: (CustomRecurrenceUnit) -> Unit,
) {
    FamilyTaskCreateCard(title = "Répéter") {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FamilyTaskRecurrence.entries.forEach { recurrence ->
                FilterChip(
                    selected = !uiState.isCustomRecurrence && uiState.recurrence == recurrence,
                    onClick = { onRecurrenceChanged(recurrence) },
                    label = { Text(familyTaskRecurrenceLabel(recurrence)) },
                )
            }
            FilterChip(selected = uiState.isCustomRecurrence, onClick = onCustomizeRecurrence, label = { Text("Personnaliser") })
        }
        if (uiState.isCustomRecurrence) {
            Text("Répéter toutes les", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = InkBrown)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onIntervalChanged(uiState.recurrenceInterval - 1) }) { Text("−") }
                Text(uiState.recurrenceInterval.toString(), style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { onIntervalChanged(uiState.recurrenceInterval + 1) }) { Text("+") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = uiState.customRecurrenceUnit == CustomRecurrenceUnit.DAYS, onClick = { onCustomUnitChanged(CustomRecurrenceUnit.DAYS) }, label = { Text("jours") })
                FilterChip(selected = uiState.customRecurrenceUnit == CustomRecurrenceUnit.WEEKS, onClick = { onCustomUnitChanged(CustomRecurrenceUnit.WEEKS) }, label = { Text("semaines") })
            }
        }
        if (uiState.recurrence == FamilyTaskRecurrence.SELECTED_WEEKDAYS && (!uiState.isCustomRecurrence || uiState.customRecurrenceUnit == CustomRecurrenceUnit.WEEKS)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                familyTaskWeekdays.forEach { (day, label) ->
                    FilterChip(
                        selected = day in uiState.selectedWeekdays,
                        onClick = { onToggleWeekday(day) },
                        label = { Text(label.take(3)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskOptionsCard(
    uiState: FamilyTaskCreateUiState,
    onValidationRequiredChanged: (Boolean) -> Unit,
    onGamificationEnabledChanged: (Boolean) -> Unit,
    onPriorityChanged: (FamilyTaskPriority) -> Unit,
) {
    FamilyTaskCreateCard(title = "Options") {
        ToggleRow(
            title = "Validation parent",
            subtitle = "La tâche passera par une validation avant d'être terminée.",
            checked = uiState.validationRequired,
            onCheckedChange = onValidationRequiredChanged,
        )
        ToggleRow(
            title = "Gamification",
            subtitle = "Active seulement le lien simple avec la progression Taskoday.",
            checked = uiState.gamificationEnabled,
            onCheckedChange = onGamificationEnabledChanged,
        )
        Text(
            text = "Priorité",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = InkBrown,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FamilyTaskPriority.entries
                .filter { it.allowedCreatePriorities }
                .forEach { priority ->
                    FilterChip(
                        selected = uiState.priority == priority,
                        onClick = { onPriorityChanged(priority) },
                        label = { Text(familyTaskPriorityFormLabel(priority)) },
                    )
                }
        }
    }
}

@Composable
private fun FamilyTaskCreateCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkBrown,
                )
                content()
            },
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = InkBrown,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = InkMuted,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ErrorCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = ParchmentLight.copy(alpha = 0.98f),
        contentColor = DangerGlow,
    ) {
        Box(modifier = Modifier.padding(14.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun FamilyTaskMember.memberLabel(): String =
    "$displayName - ${role.memberRoleLabel()}"

private fun FamilyTaskMemberRole.memberRoleLabel(): String =
    when (this) {
        FamilyTaskMemberRole.PARENT -> "Parent"
        FamilyTaskMemberRole.CHILD -> "Enfant"
    }

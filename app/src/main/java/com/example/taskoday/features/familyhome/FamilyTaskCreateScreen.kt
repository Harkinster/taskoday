package com.example.taskoday.features.familyhome

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
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
                    FamilyTaskCreateHeader(isEditing = uiState.isEditing, onBack = onBack)
                }

                if (uiState.isLoadingTask) {
                    item {
                        LoadingTaskCard()
                    }
                } else {
                    item {
                        FamilyTaskCreateFormCard(
                            uiState = uiState,
                            onTitleChanged = viewModel::onTitleChanged,
                            onDescriptionChanged = viewModel::onDescriptionChanged,
                            onDateChanged = viewModel::onDateChanged,
                            onTimeChanged = viewModel::onTimeChanged,
                            onClearTime = viewModel::clearTime,
                        )
                    }

                    item {
                        AssigneesCard(
                            uiState = uiState,
                            onSelectHouse = viewModel::selectHouseTask,
                            onToggleAssignee = viewModel::toggleAssignee,
                        )
                    }

                    item {
                        RecurrenceCard(
                            uiState = uiState,
                            onRecurrenceChanged = viewModel::onRecurrenceChanged,
                            onToggleWeekday = viewModel::toggleWeekday,
                        )
                    }

                    item {
                        TaskOptionsCard(
                            uiState = uiState,
                            onValidationRequiredChanged = viewModel::onValidationRequiredChanged,
                            onGamificationEnabledChanged = viewModel::onGamificationEnabledChanged,
                            onPriorityChanged = viewModel::onPriorityChanged,
                        )
                    }
                }

                uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { error ->
                    item {
                        ErrorCard(error)
                    }
                }

                if (!uiState.isLoadingTask) {
                    item {
                        Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onBack,
                            enabled = !uiState.isSubmitting,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Annuler")
                        }
                        Button(
                            onClick = viewModel::submit,
                            enabled = !uiState.isSubmitting,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WoodBrown, contentColor = ParchmentLight),
                            modifier = Modifier.weight(1f),
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = ParchmentLight,
                                )
                            } else {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                when {
                                    uiState.isSubmitting && uiState.isEditing -> "Enregistrement..."
                                    uiState.isSubmitting -> "Création..."
                                    uiState.isEditing -> "Enregistrer"
                                    else -> "Créer la tâche"
                                },
                            )
                        }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyTaskCreateHeader(
    isEditing: Boolean,
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
) {
    FamilyTaskCreateCard(title = "Récurrence") {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FamilyTaskRecurrence.entries.forEach { recurrence ->
                FilterChip(
                    selected = uiState.recurrence == recurrence,
                    onClick = { onRecurrenceChanged(recurrence) },
                    label = { Text(familyTaskRecurrenceLabel(recurrence)) },
                )
            }
        }
        if (uiState.recurrence == FamilyTaskRecurrence.SELECTED_WEEKDAYS) {
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

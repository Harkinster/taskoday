package com.example.taskoday.features.parent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.ParentChild
import com.example.taskoday.domain.model.PlanningFormType
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentPlanningScreen(
    viewModel: ParentPlanningViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    var formType by rememberSaveable { mutableStateOf(PlanningFormType.ROUTINE) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var pointsText by rememberSaveable { mutableStateOf("1") }
    var selectedDayPart by rememberSaveable { mutableStateOf(DayPart.MATIN) }
    var routineWeekdays by rememberSaveable { mutableStateOf(setOf<Int>()) }
    var missionDate by rememberSaveable { mutableStateOf(LocalDate.now()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mode parent") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.isLoading) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (!uiState.hasParentAccess) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
            ) {
                Text("Acces refuse: cette section est reservee au parent.")
                uiState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = onBack) { Text("Retour") }
            }
            return@Scaffold
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Text("Choisir un enfant", style = MaterialTheme.typography.titleMedium)
            ChildrenSelector(
                children = uiState.children,
                selectedChildId = uiState.selectedChildId,
                onSelect = viewModel::selectChild,
            )

            FormTypeSelector(
                selected = formType,
                onSelect = { selected ->
                    formType = selected
                    pointsText =
                        when (selected) {
                            PlanningFormType.ROUTINE -> "1"
                            PlanningFormType.MISSION -> "2"
                            PlanningFormType.QUEST -> "3"
                        }
                },
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    viewModel.clearMessages()
                },
                label = { Text("Titre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    viewModel.clearMessages()
                },
                label = { Text("Description (optionnelle)") },
                modifier = Modifier.fillMaxWidth(),
            )

            DayPartSelector(
                selected = selectedDayPart,
                onSelect = {
                    selectedDayPart = it
                    viewModel.clearMessages()
                },
            )

            when (formType) {
                PlanningFormType.ROUTINE -> {
                    WeekdaysSelector(
                        selectedDays = routineWeekdays,
                        onToggle = { day ->
                            routineWeekdays =
                                if (routineWeekdays.contains(day)) {
                                    routineWeekdays - day
                                } else {
                                    routineWeekdays + day
                                }
                            viewModel.clearMessages()
                        },
                    )
                    Text(
                        "Aucun jour selectionne = routine quotidienne.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                PlanningFormType.MISSION -> {
                    OutlinedTextField(
                        value = missionDate.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date mission") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                        TextButton(onClick = {
                            missionDate = missionDate.minusDays(1)
                            viewModel.clearMessages()
                        }) {
                            Text("Jour precedent")
                        }
                        TextButton(onClick = {
                            missionDate = missionDate.plusDays(1)
                            viewModel.clearMessages()
                        }) {
                            Text("Jour suivant")
                        }
                    }
                }

                PlanningFormType.QUEST -> Unit
            }

            OutlinedTextField(
                value = pointsText,
                onValueChange = {
                    pointsText = it.filter { c -> c.isDigit() }.take(3)
                    viewModel.clearMessages()
                },
                label = { Text("Points") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            uiState.successMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    val parsedPoints = pointsText.toIntOrNull() ?: defaultPoints(formType)
                    when (formType) {
                        PlanningFormType.ROUTINE ->
                            viewModel.createRoutine(
                                title = title,
                                description = description,
                                dayPart = selectedDayPart,
                                selectedWeekdays = routineWeekdays,
                                points = parsedPoints,
                            )

                        PlanningFormType.MISSION ->
                            viewModel.createMission(
                                title = title,
                                description = description,
                                dayPart = selectedDayPart,
                                scheduledDate = missionDate,
                                points = parsedPoints,
                            )

                        PlanningFormType.QUEST ->
                            viewModel.createQuest(
                                title = title,
                                description = description,
                                dayPart = selectedDayPart,
                                points = parsedPoints,
                            )
                    }
                },
                enabled = !uiState.isSubmitting && uiState.selectedChildId != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isSubmitting) "Envoi..." else "Ajouter")
            }
        }
    }
}

@Composable
private fun ChildrenSelector(
    children: List<ParentChild>,
    selectedChildId: Long?,
    onSelect: (Long) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    if (children.isEmpty()) {
        Text(
            "Aucun enfant disponible pour ce parent.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        items(children, key = { it.id }) { child ->
            val selected = child.id == selectedChildId
            Card(
                shape = RoundedCornerShape(16.dp),
                border =
                    if (selected) {
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    },
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                    ),
                modifier = Modifier.clickable { onSelect(child.id) },
            ) {
                Column(modifier = Modifier.padding(spacing.small)) {
                    Text(child.displayName, style = MaterialTheme.typography.titleSmall)
                    Text(child.email, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun FormTypeSelector(
    selected: PlanningFormType,
    onSelect: (PlanningFormType) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    val items =
        listOf(
            PlanningFormType.ROUTINE to "Routine",
            PlanningFormType.MISSION to "Mission",
            PlanningFormType.QUEST to "Quete",
        )
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        items.forEach { (type, label) ->
            val selectedType = selected == type
            Card(
                shape = RoundedCornerShape(14.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            if (selectedType) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    ),
                border = BorderStroke(if (selectedType) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.weight(1f).clickable { onSelect(type) },
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(vertical = spacing.small, horizontal = spacing.small),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun DayPartSelector(
    selected: DayPart,
    onSelect: (DayPart) -> Unit,
) {
    val spacing = MaterialTheme.spacing
    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        items(DayPart.entries, key = { it.name }) { dayPart ->
            val isSelected = dayPart == selected
            Card(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                    ),
                modifier = Modifier.clickable { onSelect(dayPart) },
            ) {
                Text(
                    text = "${dayPart.emoji()} ${dayPart.label()}",
                    modifier = Modifier.padding(horizontal = spacing.small, vertical = spacing.xSmall),
                )
            }
        }
    }
}

@Composable
private fun WeekdaysSelector(
    selectedDays: Set<Int>,
    onToggle: (Int) -> Unit,
) {
    val labels = listOf(1 to "L", 2 to "M", 3 to "M", 4 to "J", 5 to "V", 6 to "S", 7 to "D")
    val spacing = MaterialTheme.spacing
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        labels.forEach { (day, label) ->
            val selected = selectedDays.contains(day)
            Card(
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(if (selected) 2.dp else 1.dp, MaterialTheme.colorScheme.outline),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            if (selected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
                    ),
                modifier = Modifier.clickable { onToggle(day) },
            ) {
                Text(text = label, modifier = Modifier.padding(horizontal = spacing.small, vertical = spacing.xSmall))
            }
        }
    }
}

private fun defaultPoints(formType: PlanningFormType): Int =
    when (formType) {
        PlanningFormType.ROUTINE -> 1
        PlanningFormType.MISSION -> 2
        PlanningFormType.QUEST -> 3
    }

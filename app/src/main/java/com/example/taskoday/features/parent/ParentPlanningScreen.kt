package com.example.taskoday.features.parent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.core.plan.TaskodayPlanFeature
import com.example.taskoday.core.plan.TaskodayPlanPolicy
import com.example.taskoday.core.ui.component.fantasy.FantasyScreenBackground
import com.example.taskoday.core.ui.component.fantasy.NeonButton
import com.example.taskoday.core.ui.component.fantasy.NeonButtonStyle
import com.example.taskoday.core.ui.component.fantasy.NeonCard
import com.example.taskoday.core.ui.component.fantasy.NeonTone
import com.example.taskoday.core.ui.component.fantasy.TaskodayTopBar
import com.example.taskoday.core.ui.theme.ArcaneViolet
import com.example.taskoday.core.ui.theme.DangerGlow
import com.example.taskoday.core.ui.theme.NeonBlue
import com.example.taskoday.core.ui.theme.NeonCyan
import com.example.taskoday.core.ui.theme.NeonCyanSoft
import com.example.taskoday.core.ui.theme.StarWhite
import com.example.taskoday.core.ui.theme.SuccessGlow
import com.example.taskoday.core.ui.theme.SurfacePanel
import com.example.taskoday.core.ui.theme.TextMuted
import com.example.taskoday.core.ui.theme.WarningGlow
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.domain.model.DayPart
import com.example.taskoday.domain.model.ParentChild
import com.example.taskoday.domain.model.ParentPlanUsage
import com.example.taskoday.domain.model.PlanningFormType
import java.time.LocalDate
import kotlinx.coroutines.delay

@Composable
fun ParentPlanningScreen(
    viewModel: ParentPlanningViewModel,
    onBack: () -> Unit,
    onCreated: (PlanningFormType) -> Unit,
    initialFormType: PlanningFormType = PlanningFormType.ROUTINE,
    onOpenPremium: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing

    var formType by rememberSaveable(initialFormType) { mutableStateOf(initialFormType) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var pointsText by rememberSaveable(initialFormType) { mutableStateOf(defaultPoints(initialFormType).toString()) }
    var selectedDayPart by rememberSaveable { mutableStateOf(DayPart.MATIN) }
    var routineWeekdays by rememberSaveable { mutableStateOf(setOf<Int>()) }
    var missionDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    val selectedChild = uiState.children.firstOrNull { child -> child.id == uiState.selectedChildId }
    val selectedPlanFeature = formType.toPlanFeature()
    val selectedPlanCount = uiState.planUsage.countFor(formType)
    val selectedPlanLimitReached =
        !TaskodayPlanPolicy.canCreate(selectedPlanFeature, selectedPlanCount)

    LaunchedEffect(uiState.createdFormType) {
        uiState.createdFormType?.let { createdType ->
            delay(CREATION_CONFIRMATION_DELAY_MILLIS)
            viewModel.consumeCreationResult()
            onCreated(createdType)
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
                    ParentPlanningHeader(
                        subtitle = formType.subtitle(),
                        onBack = onBack,
                    )
                }

                if (!uiState.hasParentAccess) {
                    item {
                        NeonCard(tone = NeonTone.Warning) {
                            Text(
                                text = "Accès refusé",
                                style = MaterialTheme.typography.titleMedium,
                                color = WarningGlow,
                            )
                            Text(
                                text = "Cette section est réservée au parent.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted,
                            )
                            uiState.errorMessage?.let { message ->
                                Text(message, color = DangerGlow, style = MaterialTheme.typography.bodyMedium)
                            }
                            NeonButton(
                                text = "Retour",
                                onClick = onBack,
                                style = NeonButtonStyle.Outline,
                            )
                        }
                    }
                    return@LazyColumn
                }

                item {
                    NeonCard(tone = NeonTone.Blue) {
                        SectionTitle(if (uiState.children.size == 1) "Enfant ciblé" else "Choisir un enfant")
                        if (uiState.children.size == 1 && selectedChild != null) {
                            SelectedChildSummary(selectedChild)
                        } else {
                            ChildrenSelector(
                                children = uiState.children,
                                selectedChildId = uiState.selectedChildId,
                                onSelect = viewModel::selectChild,
                            )
                        }
                    }
                }

                item {
                    NeonCard(tone = NeonTone.Violet) {
                        SectionTitle("Type d'ajout")
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

                        QuickActionIdeas(
                            onSelect = { template ->
                                title = template.title
                                description = template.description
                                viewModel.clearMessages()
                            },
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                viewModel.clearMessages()
                            },
                            label = { Text("Titre obligatoire") },
                            singleLine = true,
                            colors = fantasyTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = {
                                description = it
                                viewModel.clearMessages()
                            },
                            label = { Text("Description optionnelle") },
                            colors = fantasyTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        SectionTitle("Moment")
                        DayPartSelector(
                            selected = selectedDayPart,
                            onSelect = {
                                selectedDayPart = it
                                viewModel.clearMessages()
                            },
                        )

                        when (formType) {
                            PlanningFormType.ROUTINE -> {
                                SectionTitle("Jours")
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
                                    text = "Aucun jour sélectionné = routine quotidienne.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                )
                            }

                            PlanningFormType.MISSION -> {
                                OutlinedTextField(
                                    value = missionDate.toString(),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Date mission") },
                                    colors = fantasyTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                                    NeonButton(
                                        text = "Jour précédent",
                                        onClick = {
                                            missionDate = missionDate.minusDays(1)
                                            viewModel.clearMessages()
                                        },
                                        style = NeonButtonStyle.Outline,
                                        modifier = Modifier.weight(1f),
                                    )
                                    NeonButton(
                                        text = "Jour suivant",
                                        onClick = {
                                            missionDate = missionDate.plusDays(1)
                                            viewModel.clearMessages()
                                        },
                                        style = NeonButtonStyle.Outline,
                                        modifier = Modifier.weight(1f),
                                    )
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
                            colors = fantasyTextFieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text(
                            text = TaskodayPlanPolicy.usageLabel(selectedPlanFeature, selectedPlanCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (selectedPlanLimitReached) WarningGlow else TextMuted,
                        )
                        if (selectedPlanLimitReached) {
                            Text(
                                text = TaskodayPlanPolicy.limitReachedMessage(),
                                style = MaterialTheme.typography.bodySmall,
                                color = WarningGlow,
                            )
                            NeonButton(
                                text = "Voir Premium",
                                onClick = onOpenPremium,
                                modifier = Modifier.fillMaxWidth(),
                                style = NeonButtonStyle.Outline,
                            )
                        }

                        uiState.successMessage?.let {
                            Text(it, color = SuccessGlow, style = MaterialTheme.typography.bodyMedium)
                        }
                        uiState.errorMessage?.let {
                            Text(it, color = DangerGlow, style = MaterialTheme.typography.bodyMedium)
                        }

                        NeonButton(
                            text =
                                when {
                                    uiState.createdFormType != null -> "Créé"
                                    uiState.isSubmitting -> "Envoi…"
                                    else -> "Ajouter"
                                },
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
                            enabled =
                                !uiState.isSubmitting &&
                                    uiState.createdFormType == null &&
                                    uiState.selectedChildId != null &&
                                    !selectedPlanLimitReached &&
                                    title.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParentPlanningHeader(
    subtitle: String,
    onBack: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        TaskodayTopBar(
            avatarInitials = "AB",
            showNotification = false,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Retour",
                    tint = StarWhite,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xSmall)) {
                Text(
                    text = "Mes enfants",
                    style = MaterialTheme.typography.headlineMedium,
                    color = StarWhite,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = StarWhite,
    )
}

@Composable
private fun SelectedChildSummary(child: ParentChild) {
    SelectableCard(selected = true) {
        Text(child.displayName, style = MaterialTheme.typography.titleSmall, color = StarWhite)
        Text(child.email, style = MaterialTheme.typography.bodySmall, color = TextMuted)
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
            text = "Créer un enfant",
            color = StarWhite,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "Crée un compte enfant, puis associe-le depuis le Profil avec son code temporaire.",
            color = TextMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }

    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        items(children, key = { it.id }) { child ->
            val selected = child.id == selectedChildId
            SelectableCard(
                selected = selected,
                modifier = Modifier.clickable { onSelect(child.id) },
            ) {
                Text(child.displayName, style = MaterialTheme.typography.titleSmall, color = StarWhite)
                Text(child.email, style = MaterialTheme.typography.bodySmall, color = TextMuted)
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
            PlanningFormType.QUEST to "Quête",
        )
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
        items.forEach { (type, label) ->
            SelectableChip(
                label = label,
                selected = selected == type,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(type) },
            )
        }
    }
}

@Composable
private fun QuickActionIdeas(onSelect: (QuickActionTemplate) -> Unit) {
    val spacing = MaterialTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xSmall)) {
        SectionTitle("Idées rapides")
        Text(
            text = "Choisis une idée pour préremplir le formulaire, puis ajuste avant de valider.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            items(quickActionTemplates, key = { template -> template.title }) { template ->
                QuickActionTemplateCard(
                    template = template,
                    onClick = { onSelect(template) },
                )
            }
        }
    }
}

@Composable
private fun QuickActionTemplateCard(
    template: QuickActionTemplate,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.58f)),
        colors = CardDefaults.cardColors(containerColor = SurfacePanel.copy(alpha = 0.72f)),
        modifier =
            Modifier
                .width(220.dp)
                .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xSmall),
        ) {
            Text(
                text = template.title,
                style = MaterialTheme.typography.titleSmall,
                color = StarWhite,
                maxLines = 2,
            )
            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 3,
            )
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
            SelectableChip(
                label = "${dayPart.emoji()} ${dayPart.label()}",
                selected = dayPart == selected,
                onClick = { onSelect(dayPart) },
            )
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
            SelectableChip(
                label = label,
                selected = selectedDays.contains(day),
                modifier = Modifier.weight(1f),
                onClick = { onToggle(day) },
            )
        }
    }
}

@Composable
private fun SelectableCard(
    selected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border =
            BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) NeonCyan else ArcaneViolet.copy(alpha = 0.52f),
            ),
        colors =
            CardDefaults.cardColors(
                containerColor = if (selected) NeonBlue.copy(alpha = 0.28f) else SurfacePanel.copy(alpha = 0.72f),
            ),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xSmall),
            content = { content() },
        )
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) NeonBlue.copy(alpha = 0.28f) else SurfacePanel.copy(alpha = 0.58f))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) NeonCyan else ArcaneViolet.copy(alpha = 0.52f),
                    shape = RoundedCornerShape(12.dp),
                )
                .clickable(onClick = onClick)
                .padding(horizontal = MaterialTheme.spacing.small, vertical = MaterialTheme.spacing.xSmall),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) NeonCyanSoft else StarWhite,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun fantasyTextFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = StarWhite,
        unfocusedTextColor = StarWhite,
        focusedBorderColor = NeonCyan,
        unfocusedBorderColor = TextMuted.copy(alpha = 0.7f),
        focusedLabelColor = NeonCyan,
        unfocusedLabelColor = TextMuted,
        cursorColor = NeonCyan,
    )

private fun PlanningFormType.subtitle(): String =
    when (this) {
        PlanningFormType.ROUTINE -> "Prépare une routine régulière."
        PlanningFormType.MISSION -> "Planifie une mission obligatoire."
        PlanningFormType.QUEST -> "Ajoute une quête facultative."
    }

private fun defaultPoints(formType: PlanningFormType): Int =
    when (formType) {
        PlanningFormType.ROUTINE -> 1
        PlanningFormType.MISSION -> 2
        PlanningFormType.QUEST -> 3
    }

private fun PlanningFormType.toPlanFeature(): TaskodayPlanFeature =
    when (this) {
        PlanningFormType.ROUTINE -> TaskodayPlanFeature.Routine
        PlanningFormType.MISSION -> TaskodayPlanFeature.Mission
        PlanningFormType.QUEST -> TaskodayPlanFeature.Quest
    }

private fun ParentPlanUsage.countFor(formType: PlanningFormType): Int =
    when (formType) {
        PlanningFormType.ROUTINE -> activeRoutines
        PlanningFormType.MISSION -> activeMissions
        PlanningFormType.QUEST -> activeQuests
    }

private data class QuickActionTemplate(
    val title: String,
    val description: String,
)

private val quickActionTemplates =
    listOf(
        QuickActionTemplate(
            title = "Brosser les dents",
            description = "Se brosser les dents sans rappel.",
        ),
        QuickActionTemplate(
            title = "Préparer le cartable",
            description = "Mettre les affaires utiles pour demain.",
        ),
        QuickActionTemplate(
            title = "Ranger la chambre",
            description = "Remettre les jouets et vêtements à leur place.",
        ),
        QuickActionTemplate(
            title = "Mettre le linge au panier",
            description = "Déposer le linge sale au bon endroit.",
        ),
        QuickActionTemplate(
            title = "Faire les devoirs",
            description = "Terminer les devoirs prévus aujourd’hui.",
        ),
        QuickActionTemplate(
            title = "Lire 10 minutes",
            description = "Lire calmement pendant dix minutes.",
        ),
        QuickActionTemplate(
            title = "Aider à mettre la table",
            description = "Participer à la préparation du repas.",
        ),
    )

private const val CREATION_CONFIRMATION_DELAY_MILLIS = 700L

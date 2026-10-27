package com.example.taskoday.features.familyhome

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskoday.R
import com.example.taskoday.features.taskodayv4.FantasyProfileMedallion
import com.example.taskoday.features.taskodayv4.FantasyTrailTab
import com.example.taskoday.features.taskodayv4.FantasyCompactHeader
import com.example.taskoday.features.taskodayv4.FantasyPurpleButton
import com.example.taskoday.features.taskodayv4.FantasySummaryPanel
import com.example.taskoday.features.taskodayv4.FantasyTaskCardBackground
import com.example.taskoday.features.taskodayv4.FantasyTaskMedallion
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3ActionKind
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3ActionTypeBadge
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3FamilyAvatar
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3InfoBlock
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3PrimaryButton
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3SelectableChip
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3SecondaryButton
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3StatusBadge
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3TaskStatus
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3TaskCard
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3TertiaryButton
import com.example.taskoday.core.ui.theme.TaskodayV3Colors
import com.example.taskoday.core.ui.theme.TaskodayV3UtilityTheme
import com.example.taskoday.core.ui.theme.spacing
import com.example.taskoday.core.ui.theme.taskodayV3Palette
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import java.time.LocalDate

@Composable
fun FamilyHomeScreen(
    viewModel: FamilyHomeViewModel,
    onOpenProfile: () -> Unit,
    onAddTask: (String?) -> Unit,
    onOpenAllTasks: () -> Unit,
    onOpenTask: (Long) -> Unit,
) {
    TaskodayV3UtilityTheme {
        FamilyHomeScreenContent(
            viewModel = viewModel,
            onOpenProfile = onOpenProfile,
            onAddTask = onAddTask,
            onOpenAllTasks = onOpenAllTasks,
            onOpenTask = onOpenTask,
        )
    }
}

@Composable
private fun FamilyHomeScreenContent(
    viewModel: FamilyHomeViewModel,
    onOpenProfile: () -> Unit,
    onAddTask: (String?) -> Unit,
    onOpenAllTasks: () -> Unit,
    onOpenTask: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = MaterialTheme.spacing
    val todaySections = uiState.todayDisplaySections()
    val creationDate =
        familyTaskCreationDateForMode(
            mode = uiState.mode,
            selectedWeekDate = uiState.selectedWeekDate,
            todayDate = uiState.todayDate,
        )
    var hasObservedInitialResume by rememberSaveable { mutableStateOf(false) }
    var failConfirmationOccurrenceId by rememberSaveable { mutableStateOf<Long?>(null) }
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (hasObservedInitialResume) {
            viewModel.refresh()
        } else {
            hasObservedInitialResume = true
        }
    }

    Scaffold(
        containerColor = Color(0xFF21162F),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF21162F),
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = TaskodayV3Colors.brandPrimary)
                }
                return@Surface
            }

            if (!uiState.hasResolvedHouseholdAccess()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    MessagePanel(
                        message = uiState.errorMessage ?: "Impossible de vérifier votre session. Réessayez.",
                        tone = MessageTone.Error,
                        actionLabel = "Réessayer",
                        onAction = viewModel::refresh,
                        onDismiss = viewModel::clearMessages,
                    )
                }
                return@Surface
            }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(innerPadding)
                        .padding(horizontal = spacing.medium),
                contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    FamilyHomeHeader(
                        mode = uiState.mode,
                        avatarInitials = uiState.avatarInitials,
                        dateLabel = uiState.dateLabel,
                        weekRangeLabel = uiState.weekRangeLabel,
                        completedTasks = uiState.completedTasks,
                        totalTasks = uiState.totalTasks,
                        pendingValidationTasks = uiState.pendingValidationTasks,
                        overdueTotalTasks = uiState.overdueTotalTasks,
                        hasLoadError = uiState.errorMessage != null || uiState.secondaryErrorMessage != null,
                        creationDate = creationDate,
                        onAddTask = onAddTask.takeIf { uiState.access.canManage && uiState.errorMessage == null },
                        onOpenAllTasks = onOpenAllTasks,
                        onShowToday = viewModel::showToday,
                        onShowWeek = viewModel::showWeek,
                        onOpenProfile = onOpenProfile,
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
                    if (uiState.shouldShowTodayEmptyState()) {
                        item {
                            EmptyFamilyHomeCard()
                        }
                    } else {
                        if (todaySections.toDo.isNotEmpty()) {
                            item { FamilyHomeSectionTitle(title = "À faire aujourd'hui", detail = todaySections.toDo.size.toString()) }
                            items(todaySections.toDo, key = { "todo-${it.task.occurrenceId}" }) { row ->
                                FamilyTaskRowCard(uiState.access, row.task, uiState.actingOccurrenceId == row.task.occurrenceId, viewModel::runQuickAction, onOpenTask)
                            }
                        }
                        if (todaySections.withoutDueDate.isNotEmpty()) {
                            item { FamilyHomeSectionTitle(title = "Missions sans échéance", detail = todaySections.withoutDueDate.size.toString()) }
                            items(todaySections.withoutDueDate, key = { "undated-${it.task.occurrenceId}" }) { row ->
                                FamilyTaskRowCard(uiState.access, row.task, uiState.actingOccurrenceId == row.task.occurrenceId, viewModel::runQuickAction, onOpenTask)
                            }
                        }
                        if (todaySections.inProgress.isNotEmpty()) {
                            item { FamilyHomeSectionTitle(title = "En cours", detail = todaySections.inProgress.size.toString()) }
                            items(todaySections.inProgress, key = { "progress-${it.task.occurrenceId}" }) { row ->
                                FamilyTaskRowCard(uiState.access, row.task, uiState.actingOccurrenceId == row.task.occurrenceId, viewModel::runQuickAction, onOpenTask)
                            }
                        }
                        if (todaySections.pendingValidation.isNotEmpty()) {
                            item { FamilyHomeSectionTitle(title = "À valider", detail = todaySections.pendingValidation.size.toString()) }
                            items(todaySections.pendingValidation, key = { "validation-${it.task.occurrenceId}" }) { row ->
                                FamilyTaskRowCard(uiState.access, row.task, uiState.actingOccurrenceId == row.task.occurrenceId, viewModel::runQuickAction, onOpenTask)
                            }
                        }
                        if (todaySections.overdue.isNotEmpty()) {
                            item {
                                FamilyOverdueSectionCard(
                                    access = uiState.access,
                                    totalCount = uiState.overdueTotalTasks,
                                    rows = todaySections.overdue,
                                    todayDate = uiState.todayDate,
                                    actingOccurrenceId = uiState.actingOccurrenceId,
                                    onQuickAction = viewModel::runQuickAction,
                                    onOpenTask = onOpenTask,
                                    onReschedule = { task ->
                                        val initialDate = LocalDate.now().plusDays(1)
                                        DatePickerDialog(context, { _, year, month, day ->
                                            viewModel.rescheduleMission(task.occurrenceId, LocalDate.of(year, month + 1, day).toString())
                                        }, initialDate.year, initialDate.monthValue - 1, initialDate.dayOfMonth).apply {
                                            datePicker.minDate = System.currentTimeMillis()
                                        }.show()
                                    },
                                    onFail = { task -> failConfirmationOccurrenceId = task.occurrenceId },
                                )
                            }
                        }
                        if (todaySections.completed.isNotEmpty()) {
                            item { FamilyHomeSectionTitle(title = "Terminées", detail = todaySections.completed.size.toString()) }
                            items(todaySections.completed, key = { "done-${it.task.occurrenceId}" }) { row ->
                                FamilyTaskRowCard(uiState.access, row.task, uiState.actingOccurrenceId == row.task.occurrenceId, viewModel::runQuickAction, onOpenTask)
                            }
                        }
                    }                } else {
                    if (uiState.sections.isEmpty()) {
                        item {
                            if (uiState.isWeekEmpty) {
                                EmptyFamilyWeekCard(
                                    title = "Rien de prévu cette semaine.",
                                    message = "Rien de prévu pour la maison aujourd'hui.",
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
                                access = uiState.access,
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
        failConfirmationOccurrenceId?.let { occurrenceId ->
            AlertDialog(
                onDismissRequest = { failConfirmationOccurrenceId = null },
                title = { Text("Marquer cette Mission comme ratée ?") },
                text = { Text("Cette Mission sera clôturée comme ratée.") },
                confirmButton = {
                    TextButton(onClick = {
                        failConfirmationOccurrenceId = null
                        viewModel.failMission(occurrenceId)
                    }) { Text("Marquer ratée") }
                },
                dismissButton = {
                    TextButton(onClick = { failConfirmationOccurrenceId = null }) { Text("Annuler") }
                },
            )
        }
    }
}

@Composable
internal fun FamilyHomeHeader(
    mode: FamilyHomeMode,
    avatarInitials: String,
    dateLabel: String,
    weekRangeLabel: String,
    completedTasks: Int,
    totalTasks: Int,
    pendingValidationTasks: Int,
    overdueTotalTasks: Int,
    hasLoadError: Boolean = false,
    creationDate: String?,
    onAddTask: ((String?) -> Unit)?,
    onOpenAllTasks: () -> Unit,
    onShowToday: () -> Unit,
    onShowWeek: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val subtitle = if (mode == FamilyHomeMode.TODAY) dateLabel else weekRangeLabel
    val unavailable = hasLoadError
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FantasyCompactHeader(
            trailingContent = {
                FantasyProfileMedallion(onOpenProfile,
                    Modifier.align(Alignment.CenterEnd).padding(end = 16.dp))
            },
        )
        Text(subtitle, modifier = Modifier.padding(start = 22.dp), color = Color(0xFFF1DEB9), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        Box(Modifier.fillMaxWidth()) {
            FantasySummaryPanel(Modifier.fillMaxWidth().heightIn(min = 126.dp)) {
                Column(Modifier.padding(top = 9.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        if (unavailable) "Informations indisponibles" else if (mode == FamilyHomeMode.TODAY) "Aujourd'hui" else "Cette semaine",
                        color = Color(0xFFE4BB77),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        familyHomeSummaryCountText(totalTasks, unavailable),
                        color = Color(0xFFF1DEB9),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (!unavailable && totalTasks > 0 && completedTasks > 0) {
                        Text("$completedTasks sur $totalTasks termin\u00e9es", color = Color(0xFFE0D1E1), style = MaterialTheme.typography.bodySmall)
                    }
                    val alerts = buildList {
                        if (pendingValidationTasks > 0) add("$pendingValidationTasks \u00e0 valider")
                        if (overdueTotalTasks > 0) add("$overdueTotalTasks en retard")
                    }
                    if (alerts.isNotEmpty() && !unavailable) {
                        Text(alerts.joinToString("  \u00b7  "), color = Color(0xFFF1DEB9), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Image(
                painter = painterResource(R.drawable.v4_top_ornament),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopCenter).width(112.dp).height(45.dp).offset(y = (-17).dp),
            )
        }
        FamilyHomeModeSwitch(mode, onShowToday, onShowWeek, onOpenAllTasks)
    }
}

@Composable
private fun FamilyHomeModeSwitch(
    mode: FamilyHomeMode,
    onShowToday: () -> Unit,
    onShowWeek: () -> Unit,
    onOpenAllTasks: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FamilyHomeFilter("Aujourd'hui", mode == FamilyHomeMode.TODAY, onShowToday, Modifier.weight(1f))
        FamilyHomeFilter("\u00c0 venir", mode == FamilyHomeMode.WEEK, onShowWeek, Modifier.weight(1f))
    }
}

@Composable
private fun FamilyHomeFilter(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FantasyTrailTab(label, selected, onClick, modifier)
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
    FantasySummaryPanel(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onPreviousWeek) {
                    Text("\u2039", color = Color(0xFFE4BB77), style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = weekRangeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF1DEB9),
                )
                TextButton(onClick = onNextWeek) {
                    Text("\u203a", color = Color(0xFFE4BB77), style = MaterialTheme.typography.titleLarge)
                }
            }

            if (!isCurrentWeek) {
                FantasyPurpleButton("Semaine actuelle", Modifier.fillMaxWidth(), onClick = onCurrentWeek)
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
                color = Color(0xFFF1DEB9),
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
    val background = if (day.isSelected) Color(0xFF4C2B60) else Color(0xFF35243F)
    val content = if (day.isSelected) Color(0xFFE4BB77) else Color(0xFFF1DEB9)
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = background,
        contentColor = content,
        border = BorderStroke(if (day.isSelected || day.isToday) 1.dp else 0.dp, if (day.isSelected) Color(0xFFE4BB77) else Color(0xFF785B82)),
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
internal fun FamilyHomeSectionTitle(
    title: String,
    detail: String?,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF1DEB9))
            detail?.takeIf { !it.startsWith("0 / 0") }?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = Color(0xFFE4BB77)) }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFA97735).copy(alpha = 0.82f)))
    }
}

@Composable
internal fun FamilyOverdueSectionCard(
    access: FamilyTaskAccessPolicy,
    totalCount: Int,
    rows: List<FamilyTaskRow>,
    todayDate: String?,
    actingOccurrenceId: Long?,
    onQuickAction: (FamilyTaskTodayItem) -> Unit,
    onOpenTask: (Long) -> Unit,
    onReschedule: (FamilyTaskTodayItem) -> Unit,
    onFail: (FamilyTaskTodayItem) -> Unit,
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
                    color = Color(0xFFF1DEB9),
                )
                if (rows.size < totalCount) {
                    Text(
                        text = "${rows.size} affichées",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFE4BB77),
                    )
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFA97735).copy(alpha = 0.82f)))
            rows.forEach { row ->
                FamilyOverdueRow(
                    access = access,
                    task = row.task,
                    today = today,
                    isActing = actingOccurrenceId == row.task.occurrenceId,
                    onQuickAction = onQuickAction,
                    onOpenTask = onOpenTask,
                    onReschedule = onReschedule,
                    onFail = onFail,
                )
            }
        }
}

@Composable
private fun FamilyOverdueRow(
    access: FamilyTaskAccessPolicy,
    task: FamilyTaskTodayItem,
    today: java.time.LocalDate,
    isActing: Boolean,
    onQuickAction: (FamilyTaskTodayItem) -> Unit,
    onOpenTask: (Long) -> Unit,
    onReschedule: (FamilyTaskTodayItem) -> Unit,
    onFail: (FamilyTaskTodayItem) -> Unit,
) {
    val action = access.quickAction(task)
    val icon = when (task.kind) {
        com.example.taskoday.domain.model.FamilyActionKind.ROUTINE -> R.drawable.v4_task_routine
        com.example.taskoday.domain.model.FamilyActionKind.MISSION -> R.drawable.v4_task_mission
        com.example.taskoday.domain.model.FamilyActionKind.QUEST -> R.drawable.v4_task_quest
    }
    FantasyTaskCardBackground(
        modifier = Modifier.fillMaxWidth().clickable(enabled = task.taskId > 0L) { if (task.taskId > 0L) onOpenTask(task.taskId) },
        contentStartPadding = 55.dp,
        verticalContentPadding = 5.dp,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(task.title, color = Color(0xFF39263F), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text("EN RETARD  \u00b7  ${familyTaskKindLabel(task).uppercase()}", color = Color(0xFF9D3D3D), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            Text(familyTaskOverdueMetaLabel(task, today), color = Color(0xFF695D65), style = MaterialTheme.typography.bodySmall)
            FamilyTaskPeopleLine("Pr\u00e9vu", task.assignees.map { it.id to it.displayName })
            if (action != null) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    FantasyPurpleButton(
                        label = if (isActing) "Mise \u00e0 jour\u2026" else familyTaskActionLabel(action),
                        enabled = !isActing && canRunQuickAction(task),
                        onClick = { onQuickAction(task) },
                    )
                    if (access.canManage && task.kind == com.example.taskoday.domain.model.FamilyActionKind.MISSION) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(enabled = !isActing, onClick = { onReschedule(task) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Reporter") }
                            TextButton(enabled = !isActing, onClick = { onFail(task) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Rat\u00e9e") }
                        }
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.TopStart).offset(x = (-40).dp, y = 1.dp)) { FantasyTaskMedallion(icon, "Mission en retard", medallionSize = 40.dp, iconSize = 20.dp) }
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
                color = Color(0xFFF1DEB9),
            )
            Text(
                text = "$totalCount sur 7 jours",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFE4BB77),
            )
        }
        sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = section.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE4BB77),
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
private fun FamilyUpcomingRow(task: FamilyTaskTodayItem, onOpenTask: (Long) -> Unit) {
    val icon = when (task.kind) {
        com.example.taskoday.domain.model.FamilyActionKind.ROUTINE -> R.drawable.v4_task_routine
        com.example.taskoday.domain.model.FamilyActionKind.MISSION -> R.drawable.v4_task_mission
        com.example.taskoday.domain.model.FamilyActionKind.QUEST -> R.drawable.v4_task_quest
    }
    FantasyTaskCardBackground(
        Modifier.fillMaxWidth().clickable(enabled = task.taskId > 0L) { if (task.taskId > 0L) onOpenTask(task.taskId) },
        contentStartPadding = 55.dp,
        verticalContentPadding = 5.dp,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(task.title, color = Color(0xFF39263F), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(familyTaskKindLabel(task).uppercase(), color = Color(0xFF80602C), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Text(familyTaskStatusLabel(task.status), color = Color(0xFF514653), style = MaterialTheme.typography.labelMedium)
            }
            val due = familyTaskDueLabel(task.dueDate, task.dueTime, task.hasDueTime, task.dueAt)
            if (due != null) Text("\u00c9ch\u00e9ance \u00b7 $due", color = Color(0xFF695D65), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            FamilyTaskPeopleLine("Pr\u00e9vu", task.assignees.map { it.id to it.displayName })
            familyTaskOccurrenceRecurrenceLabel(task.recurrenceLabel)?.let { Text(it, color = Color(0xFF695D65), style = MaterialTheme.typography.bodySmall, maxLines = 1) }
        }
        Box(Modifier.align(Alignment.TopStart).offset(x = (-40).dp, y = 1.dp)) { FantasyTaskMedallion(icon, familyTaskKindLabel(task), medallionSize = 40.dp, iconSize = 20.dp) }
    }
}

@Composable
private fun FamilyMemberSectionCard(
    access: FamilyTaskAccessPolicy,
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
                    color = Color(0xFFF1DEB9),
                )
                Text(
                    text = "${section.completedCount} / ${section.totalCount}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFE4BB77),
                )
            }

            section.tasks.forEach { row ->
                FamilyTaskRowCard(
                    access = access,
                    task = row.task,
                    isActing = actingOccurrenceId == row.task.occurrenceId,
                    onQuickAction = onQuickAction,
                    onOpenTask = onOpenTask,
                )
            }
    }
}

@Composable
internal fun FamilyTaskRowCard(
    access: FamilyTaskAccessPolicy,
    task: FamilyTaskTodayItem,
    isActing: Boolean,
    onQuickAction: (FamilyTaskTodayItem) -> Unit,
    onOpenTask: (Long) -> Unit,
) {
    val action = access.quickAction(task)
    val icon = when (task.kind) {
        com.example.taskoday.domain.model.FamilyActionKind.ROUTINE -> R.drawable.v4_task_routine
        com.example.taskoday.domain.model.FamilyActionKind.MISSION -> R.drawable.v4_task_mission
        com.example.taskoday.domain.model.FamilyActionKind.QUEST -> R.drawable.v4_task_quest
    }
    val kindColor = when (task.kind) {
        com.example.taskoday.domain.model.FamilyActionKind.ROUTINE -> Color(0xFF397A74)
        com.example.taskoday.domain.model.FamilyActionKind.MISSION -> Color(0xFFA94F37)
        com.example.taskoday.domain.model.FamilyActionKind.QUEST -> Color(0xFF80602C)
    }
    val statusColor = when (task.status) {
        FamilyTaskStatus.PENDING_VALIDATION -> Color(0xFF4C2B60)
        FamilyTaskStatus.IN_PROGRESS -> Color(0xFF3C6C86)
        FamilyTaskStatus.COMPLETED, FamilyTaskStatus.VALIDATED -> Color(0xFF34744D)
        FamilyTaskStatus.FAILED -> Color(0xFF9D3D3D)
        else -> Color(0xFF514653)
    }
    FantasyTaskCardBackground(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = task.taskId > 0L) { if (task.taskId > 0L) onOpenTask(task.taskId) },
        contentStartPadding = 55.dp,
        verticalContentPadding = 5.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(task.title, color = Color(0xFF39263F), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(familyTaskKindLabel(task).uppercase(), color = kindColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Text("\u00b7", color = Color(0xFF8B6A47))
                Text(familyTaskStatusLabel(task.status), color = statusColor, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                familyTaskPriorityLabel(task.priority)?.let { Text("\u00b7 $it", color = Color(0xFF80602C), style = MaterialTheme.typography.labelSmall) }
                val due = familyTaskDueLabel(task.dueDate, task.dueTime, task.hasDueTime, task.dueAt)
                when {
                    due != null -> Text("\u00b7 $due", color = Color(0xFF695D65), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    task.isOpenUndatedMission() -> Text("\u00b7 Sans \u00e9ch\u00e9ance", color = Color(0xFF695D65), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
            Box(Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Column(
                    modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth().padding(end = if (action != null) 170.dp else 0.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    if (task.assignees.isEmpty()) {
                        Text("Pour toute la maison", color = Color(0xFF695D65), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    } else {
                        FamilyTaskPeopleAvatarLine("Pour", task.assignees.map { it.id to it.displayName })
                    }
                    if (task.contributors.isNotEmpty()) {
                        FamilyTaskPeopleAvatarLine("Avec", task.contributors.map { it.userId to it.displayName })
                    }
                }
                if (action != null) {
                    FantasyPurpleButton(
                        label = if (isActing) "Mise \u00e0 jour\u2026" else familyTaskActionLabel(action),
                        modifier = Modifier.align(Alignment.BottomEnd).offset(x = 48.dp).heightIn(min = 48.dp),
                        enabled = !isActing && canRunQuickAction(task),
                        onClick = { onQuickAction(task) },
                    )
                }
            }
        }
        Box(Modifier.align(Alignment.TopStart).offset(x = (-40).dp, y = 1.dp)) {
            FantasyTaskMedallion(icon, familyTaskKindLabel(task), medallionSize = 40.dp, iconSize = 20.dp)
        }
    }
}

@Composable
private fun FamilyTaskPeopleAvatarLine(label: String, people: List<Pair<Long?, String>>) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 22.dp).semantics {
            this.contentDescription = "$label : ${people.joinToString { it.second }}"
        },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        people.take(1).forEach { (id, name) ->
            TaskodayV3FamilyAvatar(
                initials = name.trim().split(Regex("\\s+")).mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString(""),
                contentDescription = name,
                color = familyMemberAvatarColor(id ?: name.hashCode().toLong()),
                size = 20.dp,
            )
        }
        if (people.size > 3) {
            Text("+${people.size - 3}", color = Color(0xFF695D65), style = MaterialTheme.typography.labelSmall)
        } else if (people.size > 1) {
            Text("+${people.size - 1}", color = Color(0xFF695D65), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FamilyTaskPeopleLine(label: String, people: List<Pair<Long?, String>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        people.take(3).forEach { (id, name) ->
            TaskodayV3FamilyAvatar(
                initials = name.trim().split(Regex("\\s+")).mapNotNull { it.firstOrNull()?.uppercaseChar() }.take(2).joinToString(""),
                contentDescription = name,
                color = familyMemberAvatarColor(id ?: name.hashCode().toLong()),
                size = 24.dp,
            )
        }
        val names = people.joinToString { it.second }
        Text(
            text = "$label · $names${if (people.size > 3) " +${people.size - 3}" else ""}",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = TaskodayV3Colors.textSecondary,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

private fun familyMemberAvatarColor(id: Long): Color = when (Math.floorMod(id, 5L).toInt()) {
    0 -> Color(0xFFD9EEF4)
    1 -> Color(0xFFEAE3FF)
    2 -> Color(0xFFF8D8C6)
    3 -> Color(0xFFDDF0E3)
    else -> Color(0xFFF8EDC8)
}

private fun FamilyTaskTodayItem.v3ActionKind(): TaskodayV3ActionKind = when (kind) {
    com.example.taskoday.domain.model.FamilyActionKind.ROUTINE -> TaskodayV3ActionKind.ROUTINE
    com.example.taskoday.domain.model.FamilyActionKind.MISSION -> TaskodayV3ActionKind.MISSION
    com.example.taskoday.domain.model.FamilyActionKind.QUEST -> TaskodayV3ActionKind.QUEST
}

private fun FamilyTaskStatus.v3Status(): TaskodayV3TaskStatus = when (this) {
    FamilyTaskStatus.TODO -> TaskodayV3TaskStatus.TODO
    FamilyTaskStatus.IN_PROGRESS -> TaskodayV3TaskStatus.IN_PROGRESS
    FamilyTaskStatus.PENDING_VALIDATION -> TaskodayV3TaskStatus.PENDING_VALIDATION
    FamilyTaskStatus.COMPLETED, FamilyTaskStatus.VALIDATED -> TaskodayV3TaskStatus.COMPLETED
    FamilyTaskStatus.FAILED -> TaskodayV3TaskStatus.ERROR
    FamilyTaskStatus.SKIPPED, FamilyTaskStatus.UNKNOWN -> TaskodayV3TaskStatus.TODO
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
                FamilyTaskPriority.URGENT -> TaskodayV3Colors.errorInk.copy(alpha = 0.18f)
                else -> TaskodayV3Colors.questInk.copy(alpha = 0.20f)
            },
        contentColor = if (priority == FamilyTaskPriority.URGENT) TaskodayV3Colors.errorInk else TaskodayV3Colors.brandDeep,
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
    com.example.taskoday.features.taskodayv4.FantasyParchmentCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            color = com.example.taskoday.features.taskodayv4.JourneyInk)
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = com.example.taskoday.features.taskodayv4.JourneyMuted)
    }
}

@Composable
private fun EmptyFamilyHomeCard() {
    FantasySummaryPanel(Modifier.fillMaxWidth().heightIn(min = 104.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Le calme r\u00e8gne aujourd'hui", color = Color(0xFFE4BB77), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text("Rien n'est pr\u00e9vu pour la maison aujourd'hui.", color = Color(0xFFF1DEB9), style = MaterialTheme.typography.bodyMedium)
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
    FantasySummaryPanel(Modifier.fillMaxWidth().heightIn(min = 112.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(if (tone == MessageTone.Error) "Informations indisponibles" else "C'est enregistr\u00e9", color = Color(0xFFE4BB77), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(message, color = Color(0xFFF1DEB9), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (tone == MessageTone.Error) FantasyPurpleButton(actionLabel, enabled = true, onClick = onAction)
                else TextButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) { Text(actionLabel) }
                if (tone == MessageTone.Error) TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text("Fermer") }
            }
        }
    }
}

private enum class MessageTone {
    Error,
    Success,
}

private fun FamilyTaskWeekDaySummary.progressLabel(): String =
    if (totalCount == 0) "-" else "$completedCount/$totalCount"

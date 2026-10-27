package com.example.taskoday.features.familyhome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3InfoBlock
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3PrimaryButton
import com.example.taskoday.core.ui.component.taskodayv3.TaskodayV3SelectableChip
import com.example.taskoday.features.taskodayv4.FantasyHomeNavigationBar
import com.example.taskoday.features.taskodayv4.FantasyPurpleButton
import com.example.taskoday.features.taskodayv4.FantasySummaryPanel
import com.example.taskoday.navigation.TaskodayDestination
import com.example.taskoday.navigation.visibleAccountDestinations
import com.example.taskoday.core.ui.theme.TaskodayV3UtilityTheme
import com.example.taskoday.core.ui.theme.taskodayV3Palette
import com.example.taskoday.domain.model.FamilyActionKind
import com.example.taskoday.domain.model.FamilyActionScope
import com.example.taskoday.domain.model.FamilyTaskActor
import com.example.taskoday.domain.model.FamilyTaskAssignee
import com.example.taskoday.domain.model.FamilyTaskPriority
import com.example.taskoday.domain.model.FamilyTaskStatus
import com.example.taskoday.domain.model.FamilyTaskTodayItem
import java.time.LocalDate

/** Debug-only visual fixture. No ViewModel, account, repository, or network call is used. */
class FamilyHomeV3FixtureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TaskodayV3UtilityTheme { FamilyHomeFixture(role = intent.getStringExtra("role") ?: "PARENT") } }
    }
}

/** Clean visual host for screenshot review; still uses labeled fictitious data. */
class FamilyHomeV3CleanPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialState = runCatching {
            FixtureState.valueOf(intent.getStringExtra("state") ?: FixtureState.LOADED.name)
        }.getOrDefault(FixtureState.LOADED)
        val role = intent.getStringExtra("role") ?: "PARENT"
        setContent {
            TaskodayV3UtilityTheme {
                FamilyHomeFixture(showStateControls = false, initialState = initialState, role = role)
            }
        }
    }
}

private enum class FixtureState { LOADED, EMPTY, ERROR }

@Composable
private fun FamilyHomeFixture(
    showStateControls: Boolean = true,
    initialState: FixtureState = FixtureState.LOADED,
    role: String = "PARENT",
) {
    var state by remember(initialState) { mutableStateOf(initialState) }
    val palette = MaterialTheme.taskodayV3Palette
    val today = LocalDate.now().toString()
    val yesterday = LocalDate.now().minusDays(1).toString()
    val access = FamilyTaskAccessPolicy(userId = 20L, role = role)
    val tasks = remember(today) {
        listOf(
            fixtureTask(201, "Préparer le dîner", FamilyTaskStatus.TODO, FamilyActionKind.MISSION, today),
            fixtureTask(202, "Ranger l'entrée", FamilyTaskStatus.IN_PROGRESS, FamilyActionKind.ROUTINE, today).copy(
                contributors = listOf(FamilyTaskActor(21L, "Léo")),
            ),
            fixtureTask(203, "Mettre la table", FamilyTaskStatus.PENDING_VALIDATION, FamilyActionKind.MISSION, today),
        )
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color(0xFF21162F),
        bottomBar = {
            FantasyHomeNavigationBar(
                destinations = visibleAccountDestinations(isChild = access.role.equals("CHILD", ignoreCase = true)),
                currentRoute = TaskodayDestination.FamilyHome.route,
                quickAddEnabled = access.canManage,
                onNavigate = {},
                onCreate = {},
            )
        },
    ) { insets ->
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF21162F)).padding(insets).padding(horizontal = 16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showStateControls) {
            item {
                Text("Ma maison · aperçu DEBUG", style = MaterialTheme.typography.titleMedium, color = palette.brandDeep)
                Text("Données fictives — aucun compte ni service utilisé", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
            }
            item {
                androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskodayV3SelectableChip("Chargé", state == FixtureState.LOADED, { state = FixtureState.LOADED })
                    TaskodayV3SelectableChip("Vide", state == FixtureState.EMPTY, { state = FixtureState.EMPTY })
                    TaskodayV3SelectableChip("Erreur", state == FixtureState.ERROR, { state = FixtureState.ERROR })
                }
            }
        } else {
            item {
                Text("Aperçu visuel · données fictives", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color(0xFFF1DEB9))
            }
        }
        item {
            FamilyHomeHeader(
                mode = FamilyHomeMode.TODAY,
                avatarInitials = "LM",
                dateLabel = "Aujourd'hui · aperçu",
                weekRangeLabel = "Cette semaine",
                completedTasks = 0,
                totalTasks = if (state == FixtureState.LOADED) 3 else 0,
                pendingValidationTasks = if (state == FixtureState.LOADED) 1 else 0,
                overdueTotalTasks = if (state == FixtureState.LOADED) 1 else 0,
                hasLoadError = state == FixtureState.ERROR,
                creationDate = today,
                onAddTask = if (state == FixtureState.ERROR) null else ({}),
                onOpenAllTasks = {},
                onShowToday = {},
                onShowWeek = {},
                onOpenProfile = {},
            )
        }
        when (state) {
            FixtureState.LOADED -> {
                item { FamilyHomeSectionTitle("À faire aujourd'hui", "1") }
                item { FamilyTaskRowCard(access, tasks[0], false, {}, {}) }
                item { FamilyHomeSectionTitle("Missions sans échéance", "1") }
                item { FamilyTaskRowCard(access, fixtureTask(205, "Préparer les décorations", FamilyTaskStatus.TODO, FamilyActionKind.MISSION, ""), false, {}, {}) }
                item { FamilyHomeSectionTitle("En cours", "1") }
                item { FamilyTaskRowCard(access, tasks[1], false, {}, {}) }
                item { FamilyHomeSectionTitle("À valider", "1") }
                item { FamilyTaskRowCard(access, tasks[2], false, {}, {}) }
                item {
                    FamilyOverdueSectionCard(
                        access = access,
                        totalCount = 1,
                        rows = listOf(FamilyTaskRow(fixtureTask(204, "Sortir le recyclage", FamilyTaskStatus.TODO, FamilyActionKind.MISSION, yesterday))),
                        todayDate = today,
                        actingOccurrenceId = null,
                        onQuickAction = {},
                        onOpenTask = {},
                        onReschedule = {},
                        onFail = {},
                    )
                }
            }
            FixtureState.EMPTY -> item {
                FantasySummaryPanel(Modifier.fillMaxWidth().heightIn(min = 104.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Le calme r\u00e8gne aujourd'hui", color = androidx.compose.ui.graphics.Color(0xFFE4BB77), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("Rien n'est pr\u00e9vu pour la maison aujourd'hui.", color = androidx.compose.ui.graphics.Color(0xFFF1DEB9))
                    }
                }
            }
            FixtureState.ERROR -> item {
                FantasySummaryPanel(Modifier.fillMaxWidth().heightIn(min = 116.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Informations indisponibles", color = androidx.compose.ui.graphics.Color(0xFFE4BB77), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("Impossible de charger Ma maison. Cet aper\u00e7u ne remplace pas l'erreur par un \u00e9tat vide.", color = androidx.compose.ui.graphics.Color(0xFFF1DEB9))
                        FantasyPurpleButton("R\u00e9essayer \u00b7 aper\u00e7u", onClick = { state = FixtureState.LOADED })
                    }
                }
            }
        }
    }
    }
}

private fun fixtureTask(
    id: Long,
    title: String,
    status: FamilyTaskStatus,
    kind: FamilyActionKind,
    date: String,
) = FamilyTaskTodayItem(
    taskId = id,
    occurrenceId = id,
    title = title,
    assignees = listOf(FamilyTaskAssignee(21L, "Léo"), FamilyTaskAssignee(22L, "Mila")),
    scheduledDate = date,
    dueDate = date,
    dueTime = null,
    hasDueTime = false,
    dueAt = null,
    status = status,
    validationRequired = status == FamilyTaskStatus.PENDING_VALIDATION,
    gamificationEnabled = false,
    priority = FamilyTaskPriority.NORMAL,
    scope = FamilyActionScope.HOUSE,
    kind = kind,
)

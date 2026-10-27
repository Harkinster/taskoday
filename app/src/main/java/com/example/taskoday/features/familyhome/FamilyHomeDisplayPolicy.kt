package com.example.taskoday.features.familyhome

import com.example.taskoday.domain.model.FamilyTaskStatus

/** UI-only projection: preserves the repository-backed rows and their occurrence identities. */
data class FamilyHomeTodayDisplaySections(
    val toDo: List<FamilyTaskRow>,
    val withoutDueDate: List<FamilyTaskRow>,
    val inProgress: List<FamilyTaskRow>,
    val pendingValidation: List<FamilyTaskRow>,
    val overdue: List<FamilyTaskRow>,
    val completed: List<FamilyTaskRow>,
) {
    val isEmpty: Boolean
        get() = toDo.isEmpty() && withoutDueDate.isEmpty() && inProgress.isEmpty() && pendingValidation.isEmpty() && overdue.isEmpty() && completed.isEmpty()
}

fun FamilyHomeUiState.todayDisplaySections(): FamilyHomeTodayDisplaySections {
    val activeTasks = todayTasks
    val toDo = activeTasks.filter { it.task.status == FamilyTaskStatus.TODO }
    val withoutDueDate = openUndatedTasks.filter { it.task.status == FamilyTaskStatus.TODO }
    val pendingValidation = (activeTasks.filter { it.task.status == FamilyTaskStatus.PENDING_VALIDATION } +
        openUndatedTasks.filter { it.task.status == FamilyTaskStatus.PENDING_VALIDATION })
        .distinctBy { it.task.occurrenceId }
    return FamilyHomeTodayDisplaySections(
        toDo = toDo,
        withoutDueDate = withoutDueDate,
        inProgress = activeTasks.filter { it.task.status == FamilyTaskStatus.IN_PROGRESS },
        pendingValidation = pendingValidation,
        overdue = overdueTasks,
        completed = completedTodayTasks,
    )
}

fun FamilyHomeUiState.hasResolvedHouseholdAccess(): Boolean =
    access.userId != null && (access.canManage || access.role.equals("CHILD", ignoreCase = true))

fun FamilyHomeUiState.shouldShowTodayEmptyState(): Boolean =
    !isLoading &&
        hasResolvedHouseholdAccess() &&
        errorMessage == null &&
        secondaryErrorMessage == null &&
        todayDisplaySections().isEmpty

fun familyHomeSummaryText(totalTasks: Int, mode: FamilyHomeMode, dataUnavailable: Boolean): String {
    if (dataUnavailable) return "Informations indisponibles"
    val period = if (mode == FamilyHomeMode.TODAY) "aujourd'hui" else "ce jour-là"
    return when (totalTasks) {
        0 -> "Aucune action prévue $period"
        1 -> "1 action prévue $period"
        else -> "$totalTasks actions prévues $period"
    }
}

/** Compact count copy for the fantasy summary panel; unavailable data is never described as empty. */
fun familyHomeSummaryCountText(totalTasks: Int, dataUnavailable: Boolean): String = when {
    dataUnavailable -> "Les compteurs sont indisponibles."
    totalTasks <= 0 -> "Aucune action pr\u00e9vue"
    totalTasks == 1 -> "1 action pr\u00e9vue"
    else -> "$totalTasks actions pr\u00e9vues"
}

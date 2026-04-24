package com.example.taskoday.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class TaskodayDestination(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
) {
    data object Splash : TaskodayDestination(route = "splash", label = "Démarrage")

    data object Home : TaskodayDestination(route = "home", label = "Accueil", icon = Icons.Outlined.Home)

    data object Tasks :
        TaskodayDestination(route = "tasks", label = "Tâches", icon = Icons.Outlined.CheckCircle)

    data object Projects :
        TaskodayDestination(route = "projects", label = "Projets", icon = Icons.Outlined.Folder)

    data object Routines :
        TaskodayDestination(route = "routines", label = "Routines", icon = Icons.Outlined.Repeat)

    data object Settings :
        TaskodayDestination(route = "settings", label = "Paramètres", icon = Icons.Outlined.Settings)

    data object Week : TaskodayDestination(route = "week", label = "Semaine")

    data object TaskDetail : TaskodayDestination(route = "task_detail/{taskId}", label = "Détail tâche") {
        const val ARG_TASK_ID: String = "taskId"

        fun createRoute(taskId: Long): String = "task_detail/$taskId"
    }

    data object TaskEdit : TaskodayDestination(route = "task_edit?taskId={taskId}", label = "Édition tâche") {
        const val ARG_TASK_ID: String = "taskId"

        fun createRoute(taskId: Long?): String = if (taskId == null) "task_edit" else "task_edit?taskId=$taskId"
    }
}

val TopLevelDestinations: List<TaskodayDestination> =
    listOf(
        TaskodayDestination.Home,
        TaskodayDestination.Tasks,
        TaskodayDestination.Projects,
        TaskodayDestination.Routines,
        TaskodayDestination.Settings,
    )

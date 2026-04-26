package com.example.taskoday.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.ui.graphics.vector.ImageVector

sealed class TaskodayDestination(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
) {
    data object Splash : TaskodayDestination(route = "splash", label = "Demarrage")

    data object Login : TaskodayDestination(route = "auth/login", label = "Connexion")

    data object RegisterParent : TaskodayDestination(route = "auth/register-parent", label = "Inscription parent")

    data object Home : TaskodayDestination(route = "home", label = "Accueil", icon = Icons.Outlined.Home)

    data object Tasks :
        TaskodayDestination(route = "tasks", label = "Missions", icon = Icons.Outlined.CheckCircle)

    data object Quests :
        TaskodayDestination(route = "quests", label = "Quetes", icon = Icons.Outlined.AutoAwesome)

    data object Shop :
        TaskodayDestination(route = "shop", label = "Recompenses", icon = Icons.Outlined.Redeem)

    data object Settings :
        TaskodayDestination(route = "settings", label = "Profil", icon = Icons.Outlined.Person)

    data object ParentPlanning : TaskodayDestination(route = "parent/planning", label = "Mode parent")

    data object Week : TaskodayDestination(route = "week", label = "Semaine")

    data object TaskDetail : TaskodayDestination(route = "task_detail/{taskId}", label = "Detail mission") {
        const val ARG_TASK_ID: String = "taskId"

        fun createRoute(taskId: Long): String = "task_detail/$taskId"
    }

    data object TaskEdit : TaskodayDestination(route = "task_edit?taskId={taskId}", label = "Edition mission") {
        const val ARG_TASK_ID: String = "taskId"

        fun createRoute(taskId: Long?): String = if (taskId == null) "task_edit" else "task_edit?taskId=$taskId"
    }
}

val TopLevelDestinations: List<TaskodayDestination> =
    listOf(
        TaskodayDestination.Home,
        TaskodayDestination.Tasks,
        TaskodayDestination.Quests,
        TaskodayDestination.Settings,
    )

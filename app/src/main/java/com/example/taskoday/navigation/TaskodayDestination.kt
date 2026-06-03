package com.example.taskoday.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector

sealed class TaskodayDestination(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
) {
    data object Splash : TaskodayDestination(route = "splash", label = "Demarrage")

    data object Login : TaskodayDestination(route = "auth/login", label = "Connexion")

    data object RegisterParent : TaskodayDestination(route = "auth/register", label = "Inscription")

    data object Nest : TaskodayDestination(route = "nest", label = "Le Nid", icon = Icons.Outlined.Home)

    data object Home : TaskodayDestination(route = "home", label = "Journée", icon = Icons.Outlined.Today)

    data object Tasks :
        TaskodayDestination(route = "tasks", label = "Missions", icon = Icons.Outlined.CheckCircle)

    data object Quests :
        TaskodayDestination(route = "quests", label = "Quêtes", icon = Icons.Outlined.AutoAwesome)

    data object Shop :
        TaskodayDestination(route = "shop", label = "Souhaits", icon = Icons.Outlined.Redeem)

    data object Inventory : TaskodayDestination(route = "inventory", label = "Inventaire")

    data object Eggs : TaskodayDestination(route = "eggs", label = "Œufs")

    data object Dragons : TaskodayDestination(route = "dragons", label = "Dragons")

    data object Scrolls : TaskodayDestination(route = "scrolls", label = "Parchemins")

    data object ParentRewards : TaskodayDestination(route = "parent/rewards", label = "Souhaits parent")

    data object Settings :
        TaskodayDestination(route = "settings", label = "Profil", icon = Icons.Outlined.Person)

    data object ParentPlanning : TaskodayDestination(route = "parent/planning?type={type}", label = "Mode parent") {
        const val ARG_TYPE: String = "type"

        fun createRoute(type: String? = null): String = type?.let { "parent/planning?type=$it" } ?: "parent/planning"
    }

    data object Week : TaskodayDestination(route = "week", label = "Semaine")

    data object TaskDetail : TaskodayDestination(route = "task_detail/{taskId}", label = "Detail mission") {
        const val ARG_TASK_ID: String = "taskId"

        fun createRoute(taskId: Long): String = "task_detail/$taskId"
    }

    data object TaskEdit :
        TaskodayDestination(route = "task_edit?taskId={taskId}&mode={mode}", label = "Edition mission") {
        const val ARG_TASK_ID: String = "taskId"
        const val ARG_MODE: String = "mode"
        const val MODE_ROUTINE: String = "routine"
        const val MODE_MISSION: String = "mission"

        fun createRoute(
            taskId: Long?,
            mode: String? = null,
        ): String {
            val params = buildList {
                if (taskId != null) add("taskId=$taskId")
                if (!mode.isNullOrBlank()) add("mode=$mode")
            }
            return if (params.isEmpty()) "task_edit" else "task_edit?${params.joinToString("&")}"
        }

        fun createRoutineRoute(): String = createRoute(taskId = null, mode = MODE_ROUTINE)

        fun createMissionRoute(): String = createRoute(taskId = null, mode = MODE_MISSION)
    }
}

val TopLevelDestinations: List<TaskodayDestination> =
    listOf(
        TaskodayDestination.Home,
        TaskodayDestination.Nest,
        TaskodayDestination.Tasks,
        TaskodayDestination.Quests,
        TaskodayDestination.Shop,
    )

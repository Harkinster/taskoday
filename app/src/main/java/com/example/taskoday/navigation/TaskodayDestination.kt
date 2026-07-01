package com.example.taskoday.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Repeat
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

    data object Home : TaskodayDestination(route = "home", label = "Routine", icon = Icons.Outlined.Repeat)

    data object Tasks :
        TaskodayDestination(route = "tasks", label = "Mission", icon = Icons.Outlined.Flag)

    data object Quests :
        TaskodayDestination(route = "quests", label = "Quête", icon = Icons.Outlined.AutoAwesome)

    data object Shop :
        TaskodayDestination(route = "shop?section={section}", label = "Caverne", icon = Icons.Outlined.Redeem) {
        const val ARG_SECTION: String = "section"
        const val SECTION_WISHES: String = "wishes"
        const val SECTION_CHESTS: String = "chests"

        fun createRoute(section: String = SECTION_WISHES): String = "shop?section=$section"
    }

    data object Inventory : TaskodayDestination(route = "inventory", label = "Inventaire")

    data object Eggs : TaskodayDestination(route = "eggs", label = "Œufs")

    data object Dragons : TaskodayDestination(route = "dragons", label = "Dragons")

    data object Scrolls : TaskodayDestination(route = "scrolls", label = "Parchemins")

    data object Settings :
        TaskodayDestination(route = "settings", label = "Profil", icon = Icons.Outlined.Person)

    data object Premium : TaskodayDestination(route = "premium", label = "Premium")

    data object ActivityJournal : TaskodayDestination(route = "activity_journal", label = "Journal")

    data object ParentPlanning : TaskodayDestination(route = "parent/planning?type={type}", label = "Mode parent") {
        const val ARG_TYPE: String = "type"

        fun createRoute(type: String? = null): String = type?.let { "parent/planning?type=$it" } ?: "parent/planning"
    }

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
        TaskodayDestination.Tasks,
        TaskodayDestination.Quests,
        TaskodayDestination.Nest,
    )

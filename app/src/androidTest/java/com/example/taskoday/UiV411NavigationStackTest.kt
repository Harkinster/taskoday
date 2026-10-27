package com.example.taskoday

import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.taskoday.navigation.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Real NavController regression for the removed Splash anchor; no business fixture or network. */
class UiV411NavigationStackTest {
    @get:Rule val ui=createComposeRule()
    private lateinit var nav: NavHostController
    private fun graph() {
        ui.setContent {
            nav=rememberNavController()
            NavHost(nav,TaskodayDestination.Splash.route) {
                for (d in listOf(TaskodayDestination.Splash,TaskodayDestination.FamilyHome,TaskodayDestination.Exploration,TaskodayDestination.FollowUp,TaskodayDestination.Nest)) {
                    composable(d.route) { Text(d.label) }
                }
            }
        }
    }
    private fun login(root: TaskodayDestination) {
        ui.runOnIdle { nav.navigate(root.route) { popUpTo(TaskodayDestination.Splash.route) { inclusive=true } } }
    }
    @Test fun repeated_parent_tabs_dispose_old_entries_and_back_returns_to_actual_account_root() {
        graph();login(TaskodayDestination.FamilyHome)
        repeat(5) {
            for (d in listOf(TaskodayDestination.Exploration,TaskodayDestination.Nest,TaskodayDestination.FollowUp)) {
                ui.runOnIdle { nav.navigateToAccountDestination(d,TaskodayDestination.FamilyHome) }
            }
        }
        ui.runOnIdle {
            assertEquals(TaskodayDestination.FamilyHome.route,nav.previousBackStackEntry!!.destination.route)
            assertTrue(runCatching {nav.getBackStackEntry(TaskodayDestination.Exploration.route)}.isFailure)
            assertTrue(nav.popBackStack())
            assertEquals(TaskodayDestination.FamilyHome.route,nav.currentDestination!!.route)
        }
    }
    @Test fun child_tabs_return_to_personal_root_without_retaining_house_history() {
        graph();login(TaskodayDestination.Exploration)
        ui.runOnIdle {nav.navigateToAccountDestination(TaskodayDestination.FamilyHome,TaskodayDestination.Exploration)}
        ui.runOnIdle {nav.navigateToAccountDestination(TaskodayDestination.Nest,TaskodayDestination.Exploration)}
        ui.runOnIdle {
            assertEquals(TaskodayDestination.Exploration.route,nav.previousBackStackEntry!!.destination.route)
            assertTrue(runCatching {nav.getBackStackEntry(TaskodayDestination.FamilyHome.route)}.isFailure)
            assertTrue(nav.popBackStack())
            assertEquals(TaskodayDestination.Exploration.route,nav.currentDestination!!.route)
        }
    }
    @Test fun absent_account_root_uses_graph_instead_of_ignoring_pop() {
        graph()
        ui.runOnIdle {nav.navigateToAccountDestination(TaskodayDestination.FamilyHome,TaskodayDestination.Home)}
        ui.runOnIdle {
            assertEquals(TaskodayDestination.FamilyHome.route,nav.currentDestination!!.route)
            assertNull(nav.previousBackStackEntry)
        }
    }
}

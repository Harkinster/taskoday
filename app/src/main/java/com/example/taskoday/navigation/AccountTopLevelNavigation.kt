package com.example.taskoday.navigation

import androidx.navigation.NavHostController

/** Splash is removed after login. Use the actual account root and dispose the tab being left.
 * Keeping no saved tab snapshots also avoids introducing account/family scoped screen caches.
 */
internal fun NavHostController.navigateToAccountDestination(
    destination: TaskodayDestination,
    accountRoot: TaskodayDestination,
) {
    val rootPresent = runCatching { getBackStackEntry(accountRoot.route) }.isSuccess
    navigate(destination.route) {
        if (rootPresent) popUpTo(accountRoot.route) { saveState = false }
        else popUpTo(graph.id) { saveState = false }
        launchSingleTop = true
        restoreState = false
    }
}

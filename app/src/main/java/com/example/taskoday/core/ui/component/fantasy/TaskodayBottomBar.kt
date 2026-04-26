package com.example.taskoday.core.ui.component.fantasy

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavDestination
import com.example.taskoday.navigation.TaskodayDestination

@Composable
fun TaskodayBottomBar(
    destinations: List<TaskodayDestination>,
    currentDestination: NavDestination?,
    onNavigate: (TaskodayDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    FantasyBottomNavigation(
        destinations = destinations,
        currentDestination = currentDestination,
        onNavigate = onNavigate,
        modifier = modifier.padding(horizontal = 0.dp),
    )
}

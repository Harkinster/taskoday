package com.example.taskoday.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.taskoday.core.ui.testing.TaskodayTestTags
import com.example.taskoday.features.home.HomeScreen
import com.example.taskoday.features.home.HomeViewModel
import com.example.taskoday.features.projects.ProjectsScreen
import com.example.taskoday.features.projects.ProjectsViewModel
import com.example.taskoday.features.routines.RoutinesScreen
import com.example.taskoday.features.routines.RoutinesViewModel
import com.example.taskoday.features.settings.SettingsScreen
import com.example.taskoday.features.settings.SettingsViewModel
import com.example.taskoday.features.splash.SplashScreen
import com.example.taskoday.features.tasks.TasksScreen
import com.example.taskoday.features.tasks.TasksViewModel
import com.example.taskoday.features.tasks.detail.TaskDetailScreen
import com.example.taskoday.features.tasks.detail.TaskDetailViewModel
import com.example.taskoday.features.tasks.edit.TaskEditScreen
import com.example.taskoday.features.tasks.edit.TaskEditViewModel
import com.example.taskoday.features.week.WeekScreen

@Composable
fun TaskodayApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar =
        TopLevelDestinations.any { destination ->
            currentDestination?.hierarchy?.any { it.route == destination.route } == true
        }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestinations.forEach { destination ->
                        val selected =
                            currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        val itemTestTag =
                            when (destination) {
                                TaskodayDestination.Home -> TaskodayTestTags.NavHome
                                TaskodayDestination.Tasks -> TaskodayTestTags.NavTasks
                                TaskodayDestination.Projects -> TaskodayTestTags.NavProjects
                                TaskodayDestination.Routines -> TaskodayTestTags.NavRoutines
                                TaskodayDestination.Settings -> TaskodayTestTags.NavSettings
                                else -> ""
                            }
                        NavigationBarItem(
                            modifier = Modifier.testTag(itemTestTag),
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                destination.icon?.let {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = destination.label,
                                    )
                                }
                            },
                            label = { Text(text = destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TaskodayDestination.Splash.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TaskodayDestination.Splash.route) {
                SplashScreen(
                    onFinished = {
                        navController.navigate(TaskodayDestination.Home.route) {
                            popUpTo(TaskodayDestination.Splash.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(TaskodayDestination.Home.route) {
                val viewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = viewModel,
                    onOpenTasks = { navController.navigate(TaskodayDestination.Tasks.route) },
                    onOpenWeek = { navController.navigate(TaskodayDestination.Week.route) },
                    onOpenTask = { taskId -> navController.navigate(TaskodayDestination.TaskDetail.createRoute(taskId)) },
                )
            }

            composable(TaskodayDestination.Tasks.route) {
                val viewModel: TasksViewModel = hiltViewModel()
                TasksScreen(
                    viewModel = viewModel,
                    onTaskClick = { taskId -> navController.navigate(TaskodayDestination.TaskDetail.createRoute(taskId)) },
                    onCreateTask = { navController.navigate(TaskodayDestination.TaskEdit.createRoute(null)) },
                    onEditTask = { taskId -> navController.navigate(TaskodayDestination.TaskEdit.createRoute(taskId)) },
                )
            }

            composable(
                route = TaskodayDestination.TaskDetail.route,
                arguments = listOf(navArgument(TaskodayDestination.TaskDetail.ARG_TASK_ID) { type = NavType.LongType }),
            ) {
                val viewModel: TaskDetailViewModel = hiltViewModel()
                TaskDetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onEditTask = { taskId -> navController.navigate(TaskodayDestination.TaskEdit.createRoute(taskId)) },
                )
            }

            composable(
                route = TaskodayDestination.TaskEdit.route,
                arguments =
                    listOf(
                        navArgument(TaskodayDestination.TaskEdit.ARG_TASK_ID) {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                    ),
            ) {
                val viewModel: TaskEditViewModel = hiltViewModel()
                TaskEditScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(TaskodayDestination.Projects.route) {
                val viewModel: ProjectsViewModel = hiltViewModel()
                ProjectsScreen(viewModel = viewModel)
            }

            composable(TaskodayDestination.Routines.route) {
                val viewModel: RoutinesViewModel = hiltViewModel()
                RoutinesScreen(viewModel = viewModel)
            }

            composable(TaskodayDestination.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(viewModel = viewModel)
            }

            composable(TaskodayDestination.Week.route) {
                WeekScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

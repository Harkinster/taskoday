package com.example.taskoday.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.taskoday.core.ui.component.fantasy.TaskodayBottomBar
import com.example.taskoday.features.auth.AuthViewModel
import com.example.taskoday.features.auth.LoginScreen
import com.example.taskoday.features.auth.RegisterParentScreen
import com.example.taskoday.features.auth.SessionEventsViewModel
import com.example.taskoday.features.home.HomeScreen
import com.example.taskoday.features.home.HomeViewModel
import com.example.taskoday.features.parent.ParentPlanningScreen
import com.example.taskoday.features.parent.ParentPlanningViewModel
import com.example.taskoday.features.quests.QuestsScreen
import com.example.taskoday.features.quests.QuestsViewModel
import com.example.taskoday.features.settings.SettingsScreen
import com.example.taskoday.features.settings.SettingsViewModel
import com.example.taskoday.features.shop.ShopScreen
import com.example.taskoday.features.shop.ShopViewModel
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
    val sessionEventsViewModel: SessionEventsViewModel = hiltViewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    LaunchedEffect(sessionEventsViewModel, navController) {
        sessionEventsViewModel.unauthorizedEvents.collect {
            navController.navigate(TaskodayDestination.Login.route) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    val showBottomBar =
        TopLevelDestinations.any { destination ->
            currentDestination?.hierarchy?.any { it.route == destination.route } == true
        }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                TaskodayBottomBar(
                    destinations = TopLevelDestinations,
                    currentDestination = currentDestination,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TaskodayDestination.Splash.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TaskodayDestination.Splash.route) {
                val viewModel: AuthViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                SplashScreen(
                    message =
                        if (uiState.isCheckingSession) {
                            "Verification de la session..."
                        } else {
                            "Preparation de l'application..."
                        },
                )

                LaunchedEffect(uiState.isCheckingSession, uiState.isAuthenticated, uiState.isLocalMode) {
                    if (!uiState.isCheckingSession) {
                        if (uiState.isAuthenticated || uiState.isLocalMode) {
                            navController.navigate(TaskodayDestination.Home.route) {
                                popUpTo(TaskodayDestination.Splash.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        } else {
                            navController.navigate(TaskodayDestination.Login.route) {
                                popUpTo(TaskodayDestination.Splash.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }

            composable(TaskodayDestination.Login.route) {
                val viewModel: AuthViewModel = hiltViewModel()
                LoginScreen(
                    viewModel = viewModel,
                    onOpenRegisterParent = { navController.navigate(TaskodayDestination.RegisterParent.route) },
                    onOpenApp = {
                        navController.navigate(TaskodayDestination.Home.route) {
                            popUpTo(TaskodayDestination.Login.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(TaskodayDestination.RegisterParent.route) {
                val viewModel: AuthViewModel = hiltViewModel()
                RegisterParentScreen(
                    viewModel = viewModel,
                    onBackToLogin = { navController.popBackStack() },
                    onOpenApp = {
                        navController.navigate(TaskodayDestination.Home.route) {
                            popUpTo(TaskodayDestination.Login.route) {
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

            composable(TaskodayDestination.Quests.route) {
                val viewModel: QuestsViewModel = hiltViewModel()
                QuestsScreen(viewModel = viewModel)
            }

            composable(TaskodayDestination.Shop.route) {
                val viewModel: ShopViewModel = hiltViewModel()
                ShopScreen(viewModel = viewModel)
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

            composable(TaskodayDestination.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onOpenParentMode = { navController.navigate(TaskodayDestination.ParentPlanning.route) },
                )
            }

            composable(TaskodayDestination.ParentPlanning.route) {
                val viewModel: ParentPlanningViewModel = hiltViewModel()
                ParentPlanningScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(TaskodayDestination.Week.route) {
                WeekScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

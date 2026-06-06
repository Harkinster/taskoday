package com.example.taskoday.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
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
import com.example.taskoday.core.ui.theme.BackgroundBottom
import com.example.taskoday.core.ui.theme.BackgroundTop
import com.example.taskoday.domain.model.PlanningFormType
import com.example.taskoday.features.add.QuickAddFab
import com.example.taskoday.features.add.QuickAddViewModel
import com.example.taskoday.features.auth.AuthViewModel
import com.example.taskoday.features.auth.LoginScreen
import com.example.taskoday.features.auth.RegisterParentScreen
import com.example.taskoday.features.auth.SessionEventsViewModel
import com.example.taskoday.features.gamification.DragonsScreen
import com.example.taskoday.features.gamification.EggsScreen
import com.example.taskoday.features.gamification.InventoryScreen
import com.example.taskoday.features.gamification.NestScreen
import com.example.taskoday.features.gamification.ScrollsScreen
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

@Composable
fun TaskodayApp() {
    val navController = rememberNavController()
    val sessionEventsViewModel: SessionEventsViewModel = hiltViewModel()
    val quickAddViewModel: QuickAddViewModel = hiltViewModel()
    val quickAddUiState by quickAddViewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentTopLevelIndex =
        TopLevelDestinations.indexOfFirst { destination ->
            currentDestination?.hierarchy?.any { it.route == destination.route } == true
        }
    val isProfileDestination =
        currentDestination?.hierarchy?.any { it.route == TaskodayDestination.Settings.route } == true

    val navigateToTopLevel: (TaskodayDestination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
    val navigateToProfile: () -> Unit = {
        navController.navigate(TaskodayDestination.Settings.route) {
            launchSingleTop = true
        }
    }

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

    LaunchedEffect(navBackStackEntry?.destination?.route) {
        quickAddViewModel.refresh()
    }

    val showBottomBar = currentTopLevelIndex >= 0 || isProfileDestination
    val swipeModifier =
        if (currentTopLevelIndex >= 0) {
            Modifier.pointerInput(currentTopLevelIndex) {
                val swipeThresholdPx = MENU_SWIPE_THRESHOLD_DP.toPx()
                var accumulatedDrag = 0f

                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        accumulatedDrag += dragAmount
                    },
                    onDragEnd = {
                        val targetIndex =
                            when {
                                accumulatedDrag > swipeThresholdPx -> currentTopLevelIndex - 1
                                accumulatedDrag < -swipeThresholdPx -> currentTopLevelIndex + 1
                                else -> -1
                            }
                        if (targetIndex in TopLevelDestinations.indices) {
                            navigateToTopLevel(TopLevelDestinations[targetIndex])
                        }
                        accumulatedDrag = 0f
                    },
                    onDragCancel = {
                        accumulatedDrag = 0f
                    },
                )
            }
        } else {
            Modifier
        }
    val openCreateRoutine: () -> Unit = {
        if (quickAddUiState.hasRemoteSession && quickAddUiState.isParent) {
            navController.navigate(TaskodayDestination.ParentPlanning.createRoute("routine"))
        } else {
            navController.navigate(TaskodayDestination.TaskEdit.createRoutineRoute())
        }
    }
    val openCreateMission: () -> Unit = {
        if (quickAddUiState.hasRemoteSession && quickAddUiState.isParent) {
            navController.navigate(TaskodayDestination.ParentPlanning.createRoute("mission"))
        } else {
            navController.navigate(TaskodayDestination.TaskEdit.createMissionRoute())
        }
    }
    val openCreateQuest: () -> Unit = {
        if (quickAddUiState.hasRemoteSession && quickAddUiState.isParent) {
            navController.navigate(TaskodayDestination.ParentPlanning.createRoute("quest"))
        } else {
            navigateToTopLevel(TaskodayDestination.Quests)
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(listOf(BackgroundTop, BackgroundBottom)),
                ),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                if (showBottomBar) {
                    Box {
                        TaskodayBottomBar(
                            destinations = TopLevelDestinations,
                            currentDestination = currentDestination,
                            onNavigate = navigateToTopLevel,
                        )
                        QuickAddFab(
                            uiState = quickAddUiState,
                            onRefresh = quickAddViewModel::refresh,
                            onCreateRoutine = openCreateRoutine,
                            onCreateMission = openCreateMission,
                            onCreateQuest = openCreateQuest,
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 10.dp),
                        )
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = TaskodayDestination.Splash.route,
                modifier = swipeModifier.padding(innerPadding),
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

            composable(TaskodayDestination.Nest.route) {
                NestScreen(
                    onOpenInventory = { navController.navigate(TaskodayDestination.Inventory.route) },
                    onOpenEggs = { navController.navigate(TaskodayDestination.Eggs.route) },
                    onOpenDragons = { navController.navigate(TaskodayDestination.Dragons.route) },
                    onOpenWishes = { navController.navigate(TaskodayDestination.Shop.route) },
                    onOpenScrolls = { navController.navigate(TaskodayDestination.Scrolls.route) },
                    onOpenProfile = navigateToProfile,
                )
            }

            composable(TaskodayDestination.Home.route) {
                val viewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = viewModel,
                    onOpenTasks = { navigateToTopLevel(TaskodayDestination.Tasks) },
                    onOpenTask = { taskId -> navController.navigate(TaskodayDestination.TaskDetail.createRoute(taskId)) },
                    onOpenProfile = navigateToProfile,
                )
            }

            composable(TaskodayDestination.Tasks.route) {
                val viewModel: TasksViewModel = hiltViewModel()
                TasksScreen(
                    viewModel = viewModel,
                    onTaskClick = { taskId -> navController.navigate(TaskodayDestination.TaskDetail.createRoute(taskId)) },
                    onEditTask = { taskId -> navController.navigate(TaskodayDestination.TaskEdit.createRoute(taskId)) },
                    onOpenProfile = navigateToProfile,
                )
            }

            composable(TaskodayDestination.Quests.route) {
                val viewModel: QuestsViewModel = hiltViewModel()
                QuestsScreen(viewModel = viewModel, onOpenProfile = navigateToProfile)
            }

            composable(TaskodayDestination.Shop.route) {
                val viewModel: ShopViewModel = hiltViewModel()
                ShopScreen(viewModel = viewModel, onOpenProfile = navigateToProfile)
            }

            composable(TaskodayDestination.Inventory.route) {
                InventoryScreen(onOpenProfile = navigateToProfile)
            }

            composable(TaskodayDestination.Eggs.route) {
                EggsScreen(onOpenProfile = navigateToProfile)
            }

            composable(TaskodayDestination.Dragons.route) {
                DragonsScreen(onOpenProfile = navigateToProfile)
            }

            composable(TaskodayDestination.Scrolls.route) {
                ScrollsScreen(onOpenProfile = navigateToProfile)
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
                        navArgument(TaskodayDestination.TaskEdit.ARG_MODE) {
                            type = NavType.StringType
                            defaultValue = ""
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
                    onOpenParentMode = { navController.navigate(TaskodayDestination.ParentPlanning.createRoute()) },
                )
            }

            composable(
                route = TaskodayDestination.ParentPlanning.route,
                arguments =
                    listOf(
                        navArgument(TaskodayDestination.ParentPlanning.ARG_TYPE) {
                            type = NavType.StringType
                            defaultValue = PlanningFormType.ROUTINE.name.lowercase()
                        },
                    ),
            ) { entry ->
                val viewModel: ParentPlanningViewModel = hiltViewModel()
                val initialFormType = entry.arguments?.getString(TaskodayDestination.ParentPlanning.ARG_TYPE).toPlanningFormType()
                ParentPlanningScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    initialFormType = initialFormType,
                )
            }
            }
        }
    }
}

private val MENU_SWIPE_THRESHOLD_DP = 90.dp

private fun String?.toPlanningFormType(): PlanningFormType =
    when (this?.lowercase()) {
        "mission" -> PlanningFormType.MISSION
        "quest" -> PlanningFormType.QUEST
        else -> PlanningFormType.ROUTINE
    }

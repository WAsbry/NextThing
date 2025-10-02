package com.example.nextthingb1.presentation.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.nextthingb1.presentation.theme.BgPrimary
import com.example.nextthingb1.presentation.theme.Primary
import com.example.nextthingb1.presentation.screens.today.TodayScreen
import com.example.nextthingb1.presentation.screens.today.TodayViewModel
import com.example.nextthingb1.presentation.screens.tasks.TasksScreen
import com.example.nextthingb1.presentation.screens.tasks.TasksViewModel
import com.example.nextthingb1.presentation.screens.stats.StatsScreen
import com.example.nextthingb1.presentation.screens.stats.StatsViewModel
import com.example.nextthingb1.presentation.screens.settings.SettingsScreen
import com.example.nextthingb1.presentation.screens.settings.SettingsViewModel
import com.example.nextthingb1.presentation.screens.focus.FocusScreen
import com.example.nextthingb1.presentation.screens.focus.FocusViewModel
import com.example.nextthingb1.presentation.screens.create.CreateTaskScreen
import com.example.nextthingb1.presentation.screens.create.CreateTaskViewModel
import com.example.nextthingb1.presentation.screens.createlocation.CreateLocationScreen
import com.example.nextthingb1.presentation.screens.createlocation.CreateLocationViewModel
import com.example.nextthingb1.presentation.screens.createnotificationstrategy.CreateNotificationStrategyScreen
import com.example.nextthingb1.presentation.screens.createnotificationstrategy.CreateNotificationStrategyViewModel
import com.example.nextthingb1.presentation.screens.taskdetail.TaskDetailScreen
import com.example.nextthingb1.presentation.screens.taskdetail.TaskDetailViewModel
import com.example.nextthingb1.presentation.screens.userinfo.UserInfoScreen
import com.example.nextthingb1.presentation.screens.login.LoginScreen
import com.example.nextthingb1.presentation.components.BottomNavigationBar
import androidx.compose.runtime.LaunchedEffect
import com.example.nextthingb1.domain.usecase.UserUseCases
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.first

@Composable
fun NextThingNavigation(
    navController: NavHostController,
    userUseCases: UserUseCases
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 检查是否有用户登录
    val startDestination = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val currentUser = userUseCases.getCurrentUser().first()
        startDestination.value = if (currentUser == null) {
            Screen.Login.route
        } else {
            Screen.Today.route
        }
    }

    // 等待确定起始目的地
    if (startDestination.value == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPrimary),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }
        return
    }

    Scaffold(
        bottomBar = {
            if (currentRoute != Screen.Focus.route && currentRoute != Screen.Login.route) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Today.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination.value!!,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Today.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Today.route) {
                val viewModel: TodayViewModel = hiltViewModel()
                TodayScreen(
                    viewModel = viewModel,
                    onNavigateToFocus = {
                        navController.navigate(Screen.Focus.route)
                    },
                    onNavigateToTaskDetail = { taskId ->
                        navController.navigate("task_detail/$taskId")
                    }
                )
            }
            
            composable(Screen.Tasks.route) {
                val viewModel: TasksViewModel = hiltViewModel()
                TasksScreen(
                    viewModel = viewModel,
                    onNavigateToTaskDetail = { taskId ->
                        navController.navigate("task_detail/$taskId")
                    }
                )
            }
            
            composable(Screen.CreateTask.route) {
                val viewModel: CreateTaskViewModel = hiltViewModel()
                CreateTaskScreen(
                    viewModel = viewModel,
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onNavigateToCreateLocation = {
                        navController.navigate(Screen.CreateLocation.route)
                    },
                    onNavigateToCreateNotificationStrategy = {
                        navController.navigate(Screen.CreateNotificationStrategy.route)
                    }
                )
            }

            composable(Screen.CreateLocation.route) {
                val viewModel: CreateLocationViewModel = hiltViewModel()
                CreateLocationScreen(
                    viewModel = viewModel,
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.CreateNotificationStrategy.route) {
                val viewModel: CreateNotificationStrategyViewModel = hiltViewModel()
                CreateNotificationStrategyScreen(
                    viewModel = viewModel,
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.Stats.route) {
                val viewModel: StatsViewModel = hiltViewModel()
                StatsScreen(viewModel = viewModel)
            }
            
            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToUserInfo = {
                        navController.navigate(Screen.UserInfo.route)
                    }
                )
            }

            composable(Screen.UserInfo.route) {
                UserInfoScreen(
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.Focus.route) {
                val viewModel: FocusViewModel = hiltViewModel()
                FocusScreen(
                    viewModel = viewModel,
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.TaskDetail.route) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                val viewModel: TaskDetailViewModel = hiltViewModel()
                TaskDetailScreen(
                    taskId = taskId,
                    viewModel = viewModel,
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onEditTask = {
                        // TODO: 导航到编辑任务页面
                    }
                )
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: String) {
    object Login : Screen("login", "登录", "login")
    object Today : Screen("today", "首页", "home")
    object Tasks : Screen("tasks", "任务", "list")
    object CreateTask : Screen("create_task", "创建", "add")
    object CreateLocation : Screen("create_location", "新建地点", "location")
    object CreateNotificationStrategy : Screen("create_notification_strategy", "新建通知策略", "notification")
    object Stats : Screen("stats", "统计", "chart-pie")
    object Settings : Screen("settings", "我的", "user")
    object UserInfo : Screen("user_info", "用户信息", "user-info")
    object Focus : Screen("focus", "专注", "clock")
    object TaskDetail : Screen("task_detail/{taskId}", "任务详情", "detail")
} 
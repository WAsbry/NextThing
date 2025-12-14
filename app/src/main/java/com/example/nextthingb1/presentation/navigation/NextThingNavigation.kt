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
import com.example.nextthingb1.presentation.screens.geofence.config.GeofenceConfigScreen
import com.example.nextthingb1.presentation.screens.geofence.detail.GeofenceLocationDetailScreen
import com.example.nextthingb1.presentation.screens.geofence.add.AddGeofenceLocationScreen
import com.example.nextthingb1.presentation.screens.mappicker.MapPickerScreen
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

    // 这个好像是什么异步的东西吧
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
                    },
                    onNavigateToGeofenceAdd = {
                        navController.navigate("geofence_location_add")
                    }
                )
            }

            composable(Screen.CreateLocation.route) {
                val viewModel: CreateLocationViewModel = hiltViewModel()

                // 监听从地图选择返回的数据
                val savedStateHandle = it.savedStateHandle
                LaunchedEffect(Unit) {
                    // 检查是否有从地图选择返回的数据
                    savedStateHandle.get<Double>("selected_latitude")?.let { lat ->
                        savedStateHandle.get<Double>("selected_longitude")?.let { lng ->
                            val address = savedStateHandle.get<String>("selected_address") ?: ""

                            // 更新ViewModel
                            viewModel.updateFromMapPicker(
                                latitude = lat,
                                longitude = lng,
                                address = address
                            )

                            // 清除数据，避免重复处理
                            savedStateHandle.remove<Double>("selected_latitude")
                            savedStateHandle.remove<Double>("selected_longitude")
                            savedStateHandle.remove<String>("selected_address")
                        }
                    }
                }

                CreateLocationScreen(
                    viewModel = viewModel,
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onNavigateToMapPicker = {
                        navController.navigate("map_picker")
                    }
                )
            }

            composable("map_picker") { backStackEntry ->
                // 从导航参数中获取初始位置（如果有的话）
                val initialLat = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("initial_latitude")
                val initialLng = navController.previousBackStackEntry?.savedStateHandle?.get<Double>("initial_longitude")

                // 如果有初始位置，设置到当前页面的 savedStateHandle 供 ViewModel 读取
                if (initialLat != null && initialLng != null) {
                    backStackEntry.savedStateHandle["initial_latitude"] = initialLat
                    backStackEntry.savedStateHandle["initial_longitude"] = initialLng
                    // 清除 previous 的数据
                    navController.previousBackStackEntry?.savedStateHandle?.remove<Double>("initial_latitude")
                    navController.previousBackStackEntry?.savedStateHandle?.remove<Double>("initial_longitude")
                }

                MapPickerScreen(
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onLocationSelected = { latitude, longitude, address ->
                        // 将选择的位置数据传回CreateLocationScreen
                        navController.previousBackStackEntry?.savedStateHandle?.set("selected_latitude", latitude)
                        navController.previousBackStackEntry?.savedStateHandle?.set("selected_longitude", longitude)
                        navController.previousBackStackEntry?.savedStateHandle?.set("selected_address", address)
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
                    },
                    onNavigateToGeofence = {
                        navController.navigate("geofence_config")
                    }
                )
            }

            composable("geofence_config") {
                GeofenceConfigScreen(navController = navController)
            }

            composable("geofence_location_detail/{locationId}") { backStackEntry ->
                val locationId = backStackEntry.arguments?.getString("locationId") ?: ""
                GeofenceLocationDetailScreen(navController = navController)
            }

            composable("geofence_location_add") {
                AddGeofenceLocationScreen(navController = navController)
            }

            composable("geofence_related_tasks/{locationId}") { backStackEntry ->
                val locationId = backStackEntry.arguments?.getString("locationId") ?: ""
                com.example.nextthingb1.presentation.screens.geofence.relatedtasks.RelatedTasksScreen(
                    navController = navController
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
                        // 暂不支持编辑,可以在详情页直接编辑各个字段
                        // 如需独立编辑页面,可导航到 "edit_task/$taskId"
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
package com.nextthing.app.presentation.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.nextthing.app.presentation.theme.BgPrimary
import com.nextthing.app.presentation.screens.today.TodayScreen
import com.nextthing.app.presentation.screens.today.TodayViewModel
import com.nextthing.app.presentation.screens.aiassistant.AiAssistantScreen
import com.nextthing.app.presentation.screens.aiassistant.AiAssistantViewModel
import com.nextthing.app.presentation.screens.tasks.TasksScreen
import com.nextthing.app.presentation.screens.tasks.TasksViewModel
import com.nextthing.app.presentation.screens.tasks.TaskView
import com.nextthing.app.presentation.screens.stats.StatsScreen
import com.nextthing.app.presentation.screens.stats.StatsAIReportScreen
import com.nextthing.app.presentation.screens.stats.StatsEfficiencyScreen
import com.nextthing.app.presentation.screens.stats.StatsStructureScreen
import com.nextthing.app.presentation.screens.stats.StatsTrendScreen
import com.nextthing.app.presentation.screens.stats.StatsViewModel
import com.nextthing.app.presentation.screens.settings.SettingsScreen
import com.nextthing.app.presentation.screens.settings.SettingsViewModel
import com.nextthing.app.presentation.screens.settings.AIConfigScreen
import com.nextthing.app.presentation.screens.settings.BriefingSettingsScreen
import com.nextthing.app.presentation.screens.settings.ExportDataScreen
import com.nextthing.app.presentation.screens.settings.ReminderStrategyScreen
import com.nextthing.app.presentation.screens.settings.ThemeSettingsScreen
import com.nextthing.app.presentation.screens.focus.FocusScreen
import com.nextthing.app.presentation.screens.focus.FocusViewModel
import com.nextthing.app.presentation.screens.create.CreateTaskScreen
import com.nextthing.app.presentation.screens.create.CreateTaskViewModel
import com.nextthing.app.presentation.screens.createlocation.CreateLocationScreen
import com.nextthing.app.presentation.screens.createlocation.CreateLocationViewModel
import com.nextthing.app.presentation.screens.createnotificationstrategy.CreateNotificationStrategyScreen
import com.nextthing.app.presentation.screens.createnotificationstrategy.CreateNotificationStrategyViewModel
import com.nextthing.app.presentation.screens.taskdetail.TaskDetailScreen
import com.nextthing.app.presentation.screens.taskdetail.TaskDetailViewModel
import com.nextthing.app.presentation.screens.userinfo.UserInfoScreen
import com.nextthing.app.presentation.screens.achievement.AchievementScreen
import com.nextthing.app.presentation.screens.login.LoginScreen
import com.nextthing.app.presentation.screens.splash.SplashScreen
import com.nextthing.app.presentation.screens.geofence.config.GeofenceConfigScreen
import com.nextthing.app.presentation.screens.geofence.detail.GeofenceLocationDetailScreen
import com.nextthing.app.presentation.screens.geofence.add.AddGeofenceLocationScreen
import com.nextthing.app.presentation.screens.mappicker.MapPickerScreen
import com.nextthing.app.presentation.screens.categorymanagement.CategoryManagementScreen
import com.nextthing.app.presentation.screens.categorymanagement.CategoryManagementViewModel
import com.nextthing.app.presentation.screens.categorymanagement.CategoryEditScreen
import com.nextthing.app.presentation.screens.categorymanagement.CategoryEditViewModel
import com.nextthing.app.presentation.screens.repeatcustom.RepeatCustomScreen
import com.nextthing.app.presentation.screens.sync.SyncConflictScreen
import com.nextthing.app.presentation.components.BottomNavigationBar
import androidx.compose.runtime.LaunchedEffect
import com.nextthing.app.domain.model.RepeatFrequency
import com.nextthing.app.domain.model.RepeatFrequencyType
import com.nextthing.app.domain.usecase.UserUseCases
import com.nextthing.app.util.ToastHelper

@Composable
fun NextThingNavigation(
    navController: NavHostController,
    userUseCases: UserUseCases
) {
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isMineSubPage = currentRoute == Screen.UserInfo.route
        || currentRoute == Screen.Achievement.route
        || currentRoute == Screen.ViewPreferences.route
        || currentRoute == Screen.ThemeSettings.route
        || currentRoute == Screen.AIConfig.route
        || currentRoute == Screen.BriefingSettings.route
        || currentRoute == Screen.ReminderStrategy.route
        || currentRoute == Screen.ExportData.route
        || currentRoute == Screen.Sync.route
        || currentRoute == Screen.SyncConflicts.route
        || currentRoute == "geofence_config"
        || currentRoute == Screen.CreateLocation.route
        || currentRoute == "map_picker"
        || currentRoute?.startsWith("geofence_location") == true
        || currentRoute?.startsWith("geofence_related_tasks") == true
        || currentRoute?.startsWith("create_notification_strategy") == true

    Scaffold(
        bottomBar = {
            val hideBottomBar = currentRoute == Screen.Focus.route
                || currentRoute == Screen.Login.route
                || currentRoute == Screen.Splash.route
                || currentRoute == Screen.CreateTask.route
                || currentRoute == Screen.AIAssistant.route
                || currentRoute == Screen.CategoryManagement.route
                || isMineSubPage
                || currentRoute?.startsWith("stats_structure") == true
                || currentRoute?.startsWith("stats_trend") == true
                || currentRoute?.startsWith("stats_efficiency") == true
                || currentRoute == "stats_ai"
                || currentRoute?.startsWith("task_detail") == true
                || currentRoute?.startsWith("repeat_custom") == true
            if (!hideBottomBar) {
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
        val navHostModifier = if (isMineSubPage) {
            Modifier
        } else {
            Modifier.padding(paddingValues)
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = navHostModifier
        ) {
            // 启动页：完成权限申请后跳转
            composable(Screen.Splash.route) {
                SplashScreen(
                    userUseCases = userUseCases,
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Today.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

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
                    },
                    onNavigateToCalendar = {
                        navController.navigate(Screen.TasksCalendar.route)
                    }
                )
            }

            composable(Screen.AIAssistant.route) {
                val viewModel: AiAssistantViewModel = hiltViewModel()
                AiAssistantScreen(
                    viewModel = viewModel,
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onNavigateToAIConfig = {
                        navController.navigate(Screen.AIConfig.route)
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

            composable(Screen.TasksCalendar.route) {
                val viewModel: TasksViewModel = hiltViewModel()
                TasksScreen(
                    viewModel = viewModel,
                    onNavigateToTaskDetail = { taskId ->
                        navController.navigate("task_detail/$taskId")
                    },
                    initialView = TaskView.CALENDAR
                )
            }
            
            composable(Screen.CreateTask.route) { backStackEntry ->
                val viewModel: CreateTaskViewModel = hiltViewModel()

                // 监听从 RepeatCustomScreen 返回的自定义重复配置
                LaunchedEffect(Unit) {
                    backStackEntry.savedStateHandle.getStateFlow<String?>("repeat_type", null)
                        .collect { type ->
                            if (type == null) return@collect
                            val weekdaysStr = backStackEntry.savedStateHandle.get<String>("repeat_weekdays") ?: ""
                            val monthDaysStr = backStackEntry.savedStateHandle.get<String>("repeat_month_days") ?: ""
                            val weekdays = if (weekdaysStr.isBlank()) emptySet()
                                else weekdaysStr.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                            val monthDays = if (monthDaysStr.isBlank()) emptySet()
                                else monthDaysStr.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                            viewModel.updateRepeatFrequency(
                                RepeatFrequency(
                                    type = RepeatFrequencyType.valueOf(type),
                                    weekdays = weekdays,
                                    monthDays = monthDays
                                )
                            )
                            backStackEntry.savedStateHandle.remove<String>("repeat_type")
                            backStackEntry.savedStateHandle.remove<String>("repeat_weekdays")
                            backStackEntry.savedStateHandle.remove<String>("repeat_month_days")
                        }
                }

                LaunchedEffect(Unit) {
                    backStackEntry.savedStateHandle
                        .getStateFlow<String?>("created_geofence_location_id", null)
                        .collect { locationId ->
                            if (locationId == null) return@collect
                            viewModel.updateSelectedGeofenceLocation(locationId)
                            backStackEntry.savedStateHandle.remove<String>("created_geofence_location_id")
                        }
                }

                CreateTaskScreen(
                    viewModel = viewModel,
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onNavigateToCreateNotificationStrategy = {
                        navController.navigate(Screen.CreateNotificationStrategy.route)
                    },
                    onEditNotificationStrategy = { strategyId ->
                        navController.navigate(Screen.CreateNotificationStrategy.createRoute(strategyId))
                    },
                    onNavigateToGeofenceAdd = {
                        navController.navigate(Screen.CreateLocation.route)
                    },
                    onNavigateToManageCategories = {
                        navController.navigate(Screen.CategoryManagement.route)
                    },
                    onNavigateToRepeatCustom = {
                        val freq = viewModel.uiState.value.repeatFrequency
                        val typeStr = if (freq.type == RepeatFrequencyType.WEEKLY || freq.type == RepeatFrequencyType.MONTHLY)
                            freq.type.name else RepeatFrequencyType.WEEKLY.name
                        val weekdaysStr = freq.weekdays.joinToString(",")
                        val monthDaysStr = freq.monthDays.joinToString(",")
                        navController.navigate("repeat_custom?type=$typeStr&weekdays=$weekdaysStr&monthDays=$monthDaysStr")
                    },
                    onNavigateToAIConfig = {
                        navController.navigate(Screen.AIConfig.route)
                    }
                )
            }

            composable(Screen.CreateLocation.route) {
                val viewModel: CreateLocationViewModel = hiltViewModel()

                // 监听从地图选择返回的数据
                val savedStateHandle = it.savedStateHandle
                LaunchedEffect(Unit) {
                    viewModel.setBindTaskId(savedStateHandle.get<String>("bind_task_id"))
                    savedStateHandle.remove<String>("bind_task_id")
                }
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
                    onNavigateToMapPicker = { latitude, longitude ->
                        if (latitude != null && longitude != null) {
                            it.savedStateHandle["initial_latitude"] = latitude
                            it.savedStateHandle["initial_longitude"] = longitude
                        }
                        navController.navigate("map_picker")
                    },
                    onLocationSaved = { result ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            "created_geofence_location_id",
                            result.geofenceLocationId
                        )
                        ToastHelper.showToast(
                            context,
                            if (result.boundTaskId != null) "已创建并绑定「${result.locationName}」"
                            else "已创建「${result.locationName}」"
                        )
                        navController.popBackStack()
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

            composable(
                route = "create_notification_strategy?strategyId={strategyId}",
                arguments = listOf(
                    navArgument("strategyId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val strategyId = backStackEntry.arguments?.getString("strategyId")
                val viewModel: CreateNotificationStrategyViewModel = hiltViewModel()
                CreateNotificationStrategyScreen(
                    viewModel = viewModel,
                    strategyId = strategyId,
                    onBackPressed = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(Screen.Stats.route) {
                val viewModel: StatsViewModel = hiltViewModel()
                StatsScreen(
                    viewModel = viewModel,
                    onNavigateToStatsSection = { section, timeRange ->
                        when (section) {
                            "structure" -> navController.navigate("stats_structure/${timeRange.name}")
                            "trend" -> navController.navigate("stats_trend/${timeRange.name}")
                            "efficiency" -> navController.navigate("stats_efficiency/${timeRange.name}")
                            else -> navController.navigate("stats_$section")
                        }
                    }
                )
            }

            composable(
                route = "stats_structure/{timeRange}",
                arguments = listOf(navArgument("timeRange") { type = NavType.StringType })
            ) { backStackEntry ->
                val timeRange = runCatching {
                    com.nextthing.app.presentation.screens.stats.OverviewTimeRange.valueOf(
                        backStackEntry.arguments?.getString("timeRange")
                            ?: com.nextthing.app.presentation.screens.stats.OverviewTimeRange.TODAY.name
                    )
                }.getOrDefault(com.nextthing.app.presentation.screens.stats.OverviewTimeRange.TODAY)
                StatsStructureScreen(
                    initialTimeRange = timeRange,
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable(
                route = "stats_trend/{timeRange}",
                arguments = listOf(navArgument("timeRange") { type = NavType.StringType })
            ) { backStackEntry ->
                val timeRange = runCatching {
                    com.nextthing.app.presentation.screens.stats.OverviewTimeRange.valueOf(
                        backStackEntry.arguments?.getString("timeRange")
                            ?: com.nextthing.app.presentation.screens.stats.OverviewTimeRange.TODAY.name
                    )
                }.getOrDefault(com.nextthing.app.presentation.screens.stats.OverviewTimeRange.TODAY)
                StatsTrendScreen(
                    initialTimeRange = timeRange,
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable(
                route = "stats_efficiency/{timeRange}",
                arguments = listOf(navArgument("timeRange") { type = NavType.StringType })
            ) { backStackEntry ->
                val timeRange = runCatching {
                    com.nextthing.app.presentation.screens.stats.OverviewTimeRange.valueOf(
                        backStackEntry.arguments?.getString("timeRange")
                            ?: com.nextthing.app.presentation.screens.stats.OverviewTimeRange.TODAY.name
                    )
                }.getOrDefault(com.nextthing.app.presentation.screens.stats.OverviewTimeRange.TODAY)
                StatsEfficiencyScreen(
                    initialTimeRange = timeRange,
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable("stats_ai") {
                StatsAIReportScreen(
                    onBackPressed = { navController.popBackStack() },
                    onNavigateToAIConfig = { navController.navigate(Screen.AIConfig.route) }
                )
            }

            composable(
                route = "trend_detail/{trendType}",
                arguments = listOf(navArgument("trendType") { type = NavType.StringType })
            ) { backStackEntry ->
                val trendType = backStackEntry.arguments?.getString("trendType") ?: "completion"
                com.nextthing.app.presentation.screens.stats.TrendDetailScreen(
                    trendType = trendType,
                    onBackPressed = { navController.popBackStack() }
                )
            }
            
            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToUserInfo = {
                        navController.navigate(Screen.UserInfo.route)
                    },
                    onNavigateToAIConfig = {
                        navController.navigate(Screen.AIConfig.route)
                    },
                    onNavigateToGeofence = {
                        navController.navigate("geofence_config")
                    },
                    onNavigateToAchievement = {
                        navController.navigate(Screen.Achievement.route)
                    },
                    onNavigateToViewPreferences = {
                        navController.navigate(Screen.ViewPreferences.route)
                    },
                    onNavigateToThemeSettings = {
                        navController.navigate(Screen.ThemeSettings.route)
                    },
                    onNavigateToSync = {
                        navController.navigate(Screen.Sync.route)
                    },
                    onNavigateToBriefing = {
                        navController.navigate(Screen.BriefingSettings.route)
                    },
                    onNavigateToReminderStrategy = {
                        navController.navigate(Screen.ReminderStrategy.route)
                    },
                    onNavigateToExportData = {
                        navController.navigate(Screen.ExportData.route)
                    }
                )
            }

            composable(Screen.ThemeSettings.route) {
                ThemeSettingsScreen(
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable(Screen.AIConfig.route) {
                AIConfigScreen(
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable(Screen.BriefingSettings.route) {
                BriefingSettingsScreen(
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable(Screen.ReminderStrategy.route) {
                ReminderStrategyScreen(
                    onBackPressed = { navController.popBackStack() },
                    onCreateStrategy = {
                        navController.navigate(Screen.CreateNotificationStrategy.route)
                    },
                    onEditStrategy = { strategyId ->
                        navController.navigate(Screen.CreateNotificationStrategy.createRoute(strategyId))
                    }
                )
            }

            composable(Screen.ExportData.route) {
                ExportDataScreen(
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable(Screen.Calendar.route) {
                com.nextthing.app.presentation.screens.calendar.CalendarScreen(
                    onNavigateToTaskDetail = { taskId ->
                        navController.navigate("task_detail/$taskId")
                    },
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable(Screen.Sync.route) {
                com.nextthing.app.presentation.screens.sync.SyncScreen(
                    onBackPressed = { navController.popBackStack() },
                    onOpenConflicts = { navController.navigate(Screen.SyncConflicts.route) }
                )
            }

            composable(Screen.SyncConflicts.route) {
                SyncConflictScreen(onBackPressed = { navController.popBackStack() })
            }

            composable(Screen.Achievement.route) {
                AchievementScreen(
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable("geofence_config") {
                GeofenceConfigScreen(navController = navController)
            }

            composable(
                route = "geofence_location_detail/{locationId}",
                arguments = listOf(navArgument("locationId") { type = NavType.StringType })
            ) {
                GeofenceLocationDetailScreen(navController = navController)
            }

            composable(
                route = "geofence_location_edit/{locationId}",
                arguments = listOf(navArgument("locationId") { type = NavType.StringType })
            ) {
                GeofenceLocationDetailScreen(
                    navController = navController,
                    startInEditMode = true
                )
            }

            composable("geofence_location_add") {
                AddGeofenceLocationScreen(navController = navController)
            }

            composable(Screen.CategoryManagement.route) {
                val viewModel: CategoryManagementViewModel = hiltViewModel()
                CategoryManagementScreen(
                    viewModel = viewModel,
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onNavigateToEditCategory = { categoryId ->
                        val route = if (categoryId != null) "category_edit/$categoryId" else "category_edit/new"
                        navController.navigate(route)
                    }
                )
            }

            composable(
                route = "category_edit/{categoryId}",
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId")
                    ?.takeIf { it != "new" }
                val viewModel: CategoryEditViewModel = hiltViewModel()
                CategoryEditScreen(
                    categoryId = categoryId,
                    viewModel = viewModel,
                    onBackPressed = { navController.popBackStack() }
                )
            }

            composable(Screen.ViewPreferences.route) {
                com.nextthing.app.presentation.screens.settings.ViewPreferencesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "geofence_related_tasks/{locationId}",
                arguments = listOf(navArgument("locationId") { type = NavType.StringType })
            ) {
                com.nextthing.app.presentation.screens.geofence.relatedtasks.RelatedTasksScreen(
                    navController = navController
                )
            }

            composable(Screen.UserInfo.route) {
                UserInfoScreen(
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToAchievement = {
                        navController.navigate(Screen.Achievement.route)
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

                // 监听从 RepeatCustomScreen 返回的自定义重复配置
                LaunchedEffect(Unit) {
                    backStackEntry.savedStateHandle.getStateFlow<String?>("repeat_type", null)
                        .collect { type ->
                            if (type == null) return@collect
                            val weekdaysStr = backStackEntry.savedStateHandle.get<String>("repeat_weekdays") ?: ""
                            val monthDaysStr = backStackEntry.savedStateHandle.get<String>("repeat_month_days") ?: ""
                            val weekdays = if (weekdaysStr.isBlank()) emptySet()
                                else weekdaysStr.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                            val monthDays = if (monthDaysStr.isBlank()) emptySet()
                                else monthDaysStr.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                            viewModel.updateEditedRepeatFrequency(
                                RepeatFrequency(
                                    type = RepeatFrequencyType.valueOf(type),
                                    weekdays = weekdays,
                                    monthDays = monthDays
                                )
                            )
                            backStackEntry.savedStateHandle.remove<String>("repeat_type")
                            backStackEntry.savedStateHandle.remove<String>("repeat_weekdays")
                            backStackEntry.savedStateHandle.remove<String>("repeat_month_days")
                        }
                }

                LaunchedEffect(Unit) {
                    backStackEntry.savedStateHandle
                        .getStateFlow<String?>("created_geofence_location_id", null)
                        .collect { locationId ->
                            if (locationId == null) return@collect
                            viewModel.updateEditedGeofenceLocation(locationId)
                            backStackEntry.savedStateHandle.remove<String>("created_geofence_location_id")
                        }
                }

                TaskDetailScreen(
                    taskId = taskId,
                    viewModel = viewModel,
                    onBackPressed = {
                        navController.popBackStack()
                    },
                    onNavigateToManageCategories = {
                        navController.navigate(Screen.CategoryManagement.route)
                    },
                    onNavigateToRepeatCustom = {
                        val freq = viewModel.uiState.value.editedRepeatFrequency
                        val typeStr = if (freq.type == RepeatFrequencyType.WEEKLY || freq.type == RepeatFrequencyType.MONTHLY)
                            freq.type.name else RepeatFrequencyType.WEEKLY.name
                        val weekdaysStr = freq.weekdays.joinToString(",")
                        val monthDaysStr = freq.monthDays.joinToString(",")
                        navController.navigate("repeat_custom?type=$typeStr&weekdays=$weekdaysStr&monthDays=$monthDaysStr")
                    },
                    onNavigateToGeofenceConfig = {
                        navController.navigate("geofence_config")
                    },
                    onNavigateToGeofenceAdd = {
                        backStackEntry.savedStateHandle["bind_task_id"] = taskId
                        navController.navigate(Screen.CreateLocation.route)
                    },
                    onNavigateToNotificationStrategies = {
                        navController.navigate(Screen.ReminderStrategy.route)
                    },
                    onEditTask = {
                        // 暂不支持编辑,可以在详情页直接编辑各个字段
                        // 如需独立编辑页面,可导航到 "edit_task/$taskId"
                    }
                )
            }

            composable(
                route = "repeat_custom?type={type}&weekdays={weekdays}&monthDays={monthDays}",
                arguments = listOf(
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = "WEEKLY"
                    },
                    navArgument("weekdays") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("monthDays") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "WEEKLY"
                val weekdays = backStackEntry.arguments?.getString("weekdays") ?: ""
                val monthDays = backStackEntry.arguments?.getString("monthDays") ?: ""
                RepeatCustomScreen(
                    initialType = type,
                    initialWeekdays = weekdays,
                    initialMonthDays = monthDays,
                    onBackPressed = { navController.popBackStack() },
                    onSave = { newType, newWeekdays, newMonthDays ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("repeat_type", newType.name)
                        navController.previousBackStackEntry?.savedStateHandle?.set("repeat_weekdays", newWeekdays.joinToString(","))
                        navController.previousBackStackEntry?.savedStateHandle?.set("repeat_month_days", newMonthDays.joinToString(","))
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: String) {
    object Splash : Screen("splash", "启动", "splash")
    object Login : Screen("login", "登录", "login")
    object Today : Screen("today", "首页", "home")
    object Tasks : Screen("tasks", "任务", "list")
    object TasksCalendar : Screen("tasks_calendar", "任务日历", "calendar")
    object AIAssistant : Screen("ai_assistant", "AI 助手", "ai")
    object CreateTask : Screen("create_task", "创建", "add")
    object CreateLocation : Screen("create_location", "新建地点", "location")
    object CreateNotificationStrategy : Screen("create_notification_strategy", "新建通知策略", "notification") {
        fun createRoute(strategyId: String? = null): String {
            return if (strategyId != null) {
                "create_notification_strategy?strategyId=$strategyId"
            } else {
                "create_notification_strategy"
            }
        }
    }
    object Stats : Screen("stats", "统计", "chart-pie")
    object Settings : Screen("settings", "我的", "user")
    object Achievement : Screen("achievement", "成就", "trophy")
    object UserInfo : Screen("user_info", "用户信息", "user-info")
    object Focus : Screen("focus", "专注", "clock")
    object TaskDetail : Screen("task_detail/{taskId}", "任务详情", "detail")
    object CategoryManagement : Screen("category_management", "管理分类", "category")
    object CategoryEdit : Screen("category_edit/{categoryId}", "编辑分类", "edit")
    object ViewPreferences : Screen("view_preferences", "视图偏好", "view")
    object ThemeSettings : Screen("theme_settings", "主题设置", "theme")
    object AIConfig : Screen("ai_config", "AI 智能助手", "ai")
    object BriefingSettings : Screen("briefing_settings", "智能早晚报", "briefing")
    object ReminderStrategy : Screen("reminder_strategy", "提醒策略", "reminder")
    object ExportData : Screen("export_data", "导出数据", "export")
    object Calendar : Screen("calendar", "日历", "calendar")
    object Sync : Screen("sync", "数据同步", "sync")
    object SyncConflicts : Screen("sync_conflicts", "处理冲突", "sync")
}

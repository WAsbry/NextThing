package com.nextthing.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.SavedStateHandle
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nextthing.app.domain.repository.LocationRepository
import com.nextthing.app.domain.usecase.GeofenceUseCases
import com.nextthing.app.domain.usecase.TaskUseCases
import com.nextthing.app.domain.usecase.UserUseCases
import com.nextthing.app.presentation.screens.aiassistant.AiAssistantScreen
import com.nextthing.app.presentation.screens.calendar.CalendarScreen
import com.nextthing.app.presentation.screens.categorymanagement.CategoryEditScreen
import com.nextthing.app.presentation.screens.categorymanagement.CategoryManagementScreen
import com.nextthing.app.presentation.screens.focus.FocusScreen
import com.nextthing.app.presentation.screens.geofence.detail.GeofenceLocationDetailScreen
import com.nextthing.app.presentation.screens.geofence.detail.GeofenceLocationDetailViewModel
import com.nextthing.app.presentation.screens.geofence.relatedtasks.RelatedTasksScreen
import com.nextthing.app.presentation.screens.geofence.relatedtasks.RelatedTasksViewModel
import com.nextthing.app.presentation.screens.login.LoginScreen
import com.nextthing.app.presentation.screens.splash.SplashScreen
import com.nextthing.app.presentation.theme.NextThingB1Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Debug-only launcher used to capture otherwise unreachable production screens on a real device. */
@AndroidEntryPoint
class UiCaptureActivity : ComponentActivity() {
    @Inject lateinit var userUseCases: UserUseCases
    @Inject lateinit var geofenceUseCases: GeofenceUseCases
    @Inject lateinit var locationRepository: LocationRepository
    @Inject lateinit var taskUseCases: TaskUseCases

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val screen = intent.getStringExtra("screen") ?: "focus"
        setContent {
            NextThingB1Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (screen) {
                        "splash" -> SplashScreen(userUseCases, {}, {})
                        "login" -> LoginScreen(onLoginSuccess = {})
                        "focus" -> FocusScreen(onBackPressed = {})
                        "ai_assistant" -> AiAssistantScreen(
                            onBackPressed = {},
                            onNavigateToAIConfig = {}
                        )
                        "calendar" -> CalendarScreen(
                            onNavigateToTaskDetail = {},
                            onBackPressed = {}
                        )
                        "category_management" -> CategoryManagementScreen(
                            onBackPressed = {},
                            onNavigateToEditCategory = {}
                        )
                        "category_edit" -> CategoryEditScreen(
                            categoryId = "preset_work",
                            onBackPressed = {}
                        )
                        "geofence_detail" -> GeofenceDetailCapture(
                            geofenceUseCases = geofenceUseCases,
                            locationRepository = locationRepository
                        )
                        "related_tasks" -> RelatedTasksCapture(
                            geofenceUseCases = geofenceUseCases,
                            taskUseCases = taskUseCases
                        )
                        else -> FocusScreen(onBackPressed = {})
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun GeofenceDetailCapture(
    geofenceUseCases: GeofenceUseCases,
    locationRepository: LocationRepository
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "geofence_location_detail/ui_capture_location"
    ) {
        composable(
            route = "geofence_location_detail/{locationId}",
            arguments = listOf(navArgument("locationId") { type = NavType.StringType })
        ) {
            val viewModel = remember {
                GeofenceLocationDetailViewModel(
                    geofenceUseCases = geofenceUseCases,
                    locationRepository = locationRepository,
                    savedStateHandle = SavedStateHandle(mapOf("locationId" to "ui_capture_location"))
                )
            }
            GeofenceLocationDetailScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun RelatedTasksCapture(
    geofenceUseCases: GeofenceUseCases,
    taskUseCases: TaskUseCases
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "geofence_related_tasks/ui_capture_location"
    ) {
        composable(
            route = "geofence_related_tasks/{locationId}",
            arguments = listOf(navArgument("locationId") { type = NavType.StringType })
        ) {
            val viewModel = remember {
                RelatedTasksViewModel(
                    geofenceUseCases = geofenceUseCases,
                    taskUseCases = taskUseCases,
                    savedStateHandle = SavedStateHandle(mapOf("locationId" to "ui_capture_location"))
                )
            }
            RelatedTasksScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}

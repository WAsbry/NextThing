package com.nextthing.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.nextthing.app.data.preferences.ThemePreferences
import com.nextthing.app.domain.model.ThemeMode
import com.nextthing.app.domain.model.WeatherCondition
import com.nextthing.app.presentation.navigation.NextThingNavigation
import com.nextthing.app.performance.StartupTracker
import com.nextthing.app.presentation.theme.NextThingB1Theme
import com.nextthing.app.util.PermissionHelper
import com.nextthing.app.util.PermissionManager
import com.nextthing.app.presentation.components.PermissionRequestDialog
import com.nextthing.app.domain.usecase.UserUseCases
import com.nextthing.app.data.local.dao.StartupTraceDao
import com.nextthing.app.performance.StartupTraceCollector
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

// 用于在Compose中访问权限请求器的CompositionLocal
val LocalPermissionLauncher = staticCompositionLocalOf<ActivityResultLauncher<Array<String>>?> { null }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userUseCases: UserUseCases

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var themePreferences: ThemePreferences

    @Inject
    lateinit var startupTraceDao: StartupTraceDao

    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private var notificationPermissionLauncher: ActivityResultLauncher<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StartupTracker.record("activity_onCreate")

        // 初始化位置权限请求器
        locationPermissionLauncher = PermissionHelper.createLocationPermissionLauncher(this) { isGranted ->
            if (isGranted) {
                Timber.d("位置权限已授予")
                // 通知应用权限状态已更改
                sendBroadcast(android.content.Intent("com.nextthing.app.LOCATION_PERMISSION_GRANTED"))
            } else {
                Timber.w("位置权限被拒绝")
            }
        }

        // 用于在权限回调中刷新UI的标志
        var permissionCallbackTrigger by mutableStateOf(0)

        // 初始化通知权限请求器
        notificationPermissionLauncher = permissionManager.createNotificationPermissionLauncher(this) { isGranted ->
            if (isGranted) {
                Timber.tag("NotificationTask").d("✅ 通知权限已授予")
                // 触发重新检查权限
                permissionCallbackTrigger++
            } else {
                Timber.tag("NotificationTask").w("⚠️ 通知权限被拒绝")
            }
        }

        enableEdgeToEdge()
        setContent {
            StartupTracker.record("setContent")
            // 首帧用默认值快速渲染，异步收集 DataStore Flow 避免阻塞
            var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
            var weatherCondition by remember { mutableStateOf(WeatherCondition.UNKNOWN) }
            var weatherCustomPrimaries by remember { mutableStateOf<Map<WeatherCondition, Long>>(emptyMap()) }

            LaunchedEffect(Unit) { themePreferences.themeMode.collect { themeMode = it } }
            LaunchedEffect(Unit) { themePreferences.currentWeatherCondition.collect { weatherCondition = it } }
            LaunchedEffect(Unit) { themePreferences.weatherCustomPrimaries.collect { weatherCustomPrimaries = it } }

            NextThingB1Theme(
                themeMode = themeMode,
                weatherCondition = weatherCondition,
                weatherCustomPrimaries = weatherCustomPrimaries
            ) {
                CompositionLocalProvider(LocalPermissionLauncher provides locationPermissionLauncher) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NextThingApp(
                            userUseCases = userUseCases,
                            permissionManager = permissionManager,
                            notificationPermissionLauncher = notificationPermissionLauncher,
                            startupTraceDao = startupTraceDao,
                            activity = this,
                            permissionCallbackTrigger = permissionCallbackTrigger
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NextThingApp(
    userUseCases: UserUseCases,
    permissionManager: PermissionManager,
    notificationPermissionLauncher: ActivityResultLauncher<String>?,
    startupTraceDao: StartupTraceDao,
    activity: ComponentActivity,
    permissionCallbackTrigger: Int = 0
) {
    val navController = rememberNavController()

    // 第二次 flush：写入 Activity + Splash 阶段的探针
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        try {
            StartupTraceCollector.flushNewToDatabase(startupTraceDao)
        } catch (error: Exception) {
            Timber.w(error, "启动打点写入失败")
        }
    }

    // 权限检查和请求
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionStatus by remember { mutableStateOf(permissionManager.checkAllPermissions()) }

    // 在应用启动时检查权限
    LaunchedEffect(Unit) {
        permissionStatus = permissionManager.checkAllPermissions()
        if (!permissionStatus.allGranted) {
            Timber.tag("NotificationTask").w("检测到缺少必要权限，显示权限请求对话框")
            showPermissionDialog = true
        }
    }

    // 监听权限回调，当权限被授予时刷新状态
    LaunchedEffect(permissionCallbackTrigger) {
        if (permissionCallbackTrigger > 0) {
            Timber.tag("NotificationTask").d("检测到权限回调触发，重新检查权限状态")
            permissionStatus = permissionManager.checkAllPermissions()
            if (permissionStatus.allGranted) {
                showPermissionDialog = false
                Timber.tag("NotificationTask").d("✅ 所有权限已授予，自动关闭权限对话框")
            }
        }
    }

    // 显示权限请求对话框
    if (showPermissionDialog && permissionStatus.missingPermissions.isNotEmpty()) {
        PermissionRequestDialog(
            missingPermissions = permissionStatus.missingPermissions,
            onRequestNotification = {
                Timber.tag("NotificationTask").d("用户点击授予通知权限")
                permissionManager.requestNotificationPermission(notificationPermissionLauncher)
                // 权限状态会在回调触发时自动刷新，不需要在这里立即检查
            },
            onRequestExactAlarm = {
                Timber.tag("NotificationTask").d("用户点击打开精确闹钟设置")
                permissionManager.openExactAlarmSettings(activity)
                showPermissionDialog = false
            },
            onDismiss = {
                Timber.tag("NotificationTask").d("用户关闭权限对话框")
                showPermissionDialog = false
            },
            showDismissButton = true
        )
    }

    // 页面就在这里
    NextThingNavigation(
        navController = navController,
        userUseCases = userUseCases
    )
}

// Preview removed as it requires dependency injection

package com.example.nextthingb1.presentation.screens.geofence.config

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextthingb1.domain.model.GeofenceLocation
import com.example.nextthingb1.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceConfigScreen(
    navController: NavController,
    viewModel: GeofenceConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 位置权限请求launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onLocationPermissionResult(
            granted = granted,
            shouldShowRationale = !granted
        )
        // 如果位置权限授予成功，自动请求后台权限（Android 10+）
        if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            viewModel.requestBackgroundLocationPermission()
        }
    }

    // 后台位置权限请求launcher（Android 10+）
    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onBackgroundLocationPermissionResult(
            granted = granted,
            shouldShowRationale = !granted
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("地理围栏") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Text("←", fontSize = 24.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("geofence_location_add") }) {
                        Text("➕", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgCard
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPrimary)
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 权限请求卡片（如果需要）
            if (uiState.shouldShowPermissionRequest) {
                item {
                    PermissionRequestCard(
                        uiState = uiState,
                        onRequestLocationPermission = {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        },
                        onRequestBackgroundPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }
                        },
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 全局开关卡片
            item {
                GlobalEnableCard(
                    uiState = uiState,
                    onToggle = { viewModel.toggleGlobalEnabled() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 统计信息卡片
            item {
                StatisticsCard(
                    totalLocations = uiState.totalLocationsCount,
                    frequentLocations = uiState.frequentLocationsCount,
                    activeTasks = uiState.activeTasksCount,
                    monthlyChecks = uiState.monthlyCheckCount,
                    hitRate = uiState.averageHitRate
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 常用地点卡片
            if (uiState.frequentLocations.isNotEmpty()) {
                item {
                    Text(
                        text = "⭐ 常用地点",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(uiState.frequentLocations) { location ->
                    LocationListItem(
                        location = location,
                        onToggleFrequent = { viewModel.toggleFrequent(location) },
                        onDelete = { viewModel.showDeleteConfirmation(location) },
                        onClick = { navController.navigate("geofence_location_detail/${location.id}") }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // 所有地点列表
            item {
                Text(
                    text = "📍 所有地点 (${uiState.totalLocationsCount})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (uiState.locations.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyLocationState(
                        onAddClick = { navController.navigate("geofence_location_add") }
                    )
                }
            } else {
                items(uiState.locations) { location ->
                    LocationListItem(
                        location = location,
                        onToggleFrequent = { viewModel.toggleFrequent(location) },
                        onDelete = { viewModel.showDeleteConfirmation(location) },
                        onClick = { navController.navigate("geofence_location_detail/${location.id}") }
                    )
                }
            }

            // 高级设置
            item {
                Spacer(modifier = Modifier.height(16.dp))
                AdvancedSettingsCard(
                    isExpanded = uiState.showAdvancedSettings,
                    onToggle = { viewModel.toggleAdvancedSettings() },
                    defaultRadius = uiState.defaultRadius,
                    onRadiusChange = { viewModel.updateDefaultRadius(it) },
                    accuracyThreshold = uiState.locationAccuracyThreshold,
                    onAccuracyChange = { viewModel.updateLocationAccuracyThreshold(it) },
                    batteryOptimization = uiState.batteryOptimization,
                    onBatteryToggle = { viewModel.toggleBatteryOptimization() },
                    notifyWhenOutside = uiState.notifyWhenOutside,
                    onNotifyToggle = { viewModel.toggleNotifyWhenOutside() }
                )
            }
        }

        // 加载指示器
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgPrimary.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // 成功消息
        uiState.successMessage?.let { message ->
            LaunchedEffect(message) {
                // 自动清除消息由 ViewModel 处理
            }
            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = Color(0xFF4CAF50)
            ) {
                Text(message)
            }
        }

        // 错误消息
        uiState.errorMessage?.let { message ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearErrorMessage() }) {
                        Text("关闭")
                    }
                },
                containerColor = Color(0xFFF44336)
            ) {
                Text(message)
            }
        }

        // 删除确认对话框
        if (uiState.showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDelete() },
                title = { Text("确认删除") },
                text = {
                    Text("确定要删除此地点吗?关联的任务地理围栏也会被删除。")
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmDelete() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF44336))
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDelete() }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

// ========== 组件 ==========

@Composable
private fun GlobalEnableCard(
    uiState: GeofenceConfigUiState,
    onToggle: () -> Unit
) {
    val isEnabled = uiState.isGlobalEnabled
    val hasPermission = uiState.hasLocationPermission
    val canToggle = hasPermission

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !hasPermission -> Color(0xFFFFEBEE) // 浅红色 - 无权限
                !isEnabled -> Color(0xFFFFF3E0)     // 浅橙色 - 已禁用
                else -> Color(0xFFE8F5E9)           // 浅绿色 - 已启用
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 主要开关行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🛡️",
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "地理围栏全局开关",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            !hasPermission -> "⚠️ 缺少位置权限"
                            !isEnabled -> "地理围栏已禁用"
                            else -> "✅ 地理围栏运行中"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            !hasPermission -> Color(0xFFC62828)
                            !isEnabled -> Color(0xFFE65100)
                            else -> Color(0xFF2E7D32)
                        },
                        fontWeight = FontWeight.Medium
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { if (canToggle) onToggle() },
                    enabled = canToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF4CAF50),
                        disabledCheckedThumbColor = Color.White,
                        disabledCheckedTrackColor = Color(0xFFBDBDBD),
                        disabledUncheckedThumbColor = Color.White,
                        disabledUncheckedTrackColor = Color(0xFFE0E0E0)
                    )
                )
            }

            // 系统地理围栏状态
            if (hasPermission) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Border)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "系统地理围栏",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (uiState.systemGeofencesActive) "●" else "○",
                                color = if (uiState.systemGeofencesActive) Color(0xFF4CAF50) else Color(0xFFBDBDBD),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.systemGeofencesActive) "活跃" else "未活跃",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (uiState.systemGeofencesActive) Color(0xFF4CAF50) else TextSecondary,
                                fontWeight = if (uiState.systemGeofencesActive) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "已注册围栏",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${uiState.systemGeofencesRegistered} 个",
                            style = MaterialTheme.typography.titleMedium,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 无权限提示
            if (!hasPermission) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "💡 开关已禁用，请先授予位置权限",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC62828),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun StatisticsCard(
    totalLocations: Int,
    frequentLocations: Int,
    activeTasks: Int,
    monthlyChecks: Int,
    hitRate: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 第一行：3个统计项
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("📍", totalLocations.toString(), "总地点")
                StatItem("⭐", frequentLocations.toString(), "常用")
                StatItem("✅", activeTasks.toString(), "活跃任务")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Border)
            Spacer(modifier = Modifier.height(16.dp))

            // 第二行：2个统计项
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("📊", monthlyChecks.toString(), "本月检查")
                StatItem(
                    "🎯",
                    if (monthlyChecks > 0) "${(hitRate * 100).toInt()}%" else "无数据",
                    "命中率"
                )
            }
        }
    }
}

@Composable
private fun StatItem(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 24.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun LocationListItem(
    location: GeofenceLocation,
    onToggleFrequent: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = location.locationInfo.locationName.ifEmpty { "未命名地点" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    if (location.isFrequent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "⭐", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append("📍 ${location.locationInfo.address.take(30)}")
                        if (location.locationInfo.address.length > 30) append("...")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = "半径: ${location.customRadius ?: "默认"}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "使用: ${location.usageCount}次",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLocationState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "📍", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "还没有地理围栏地点",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击右上角 ➕ 添加常用地点",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("添加地点")
        }
    }
}

@Composable
private fun AdvancedSettingsCard(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    defaultRadius: Int,
    onRadiusChange: (Int) -> Unit,
    accuracyThreshold: Int,
    onAccuracyChange: (Int) -> Unit,
    batteryOptimization: Boolean,
    onBatteryToggle: () -> Unit,
    notifyWhenOutside: Boolean,
    onNotifyToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚙️ 高级设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (isExpanded) "▲" else "▼",
                    color = TextSecondary
                )
            }

            // 展开的内容
            if (isExpanded) {
                Divider(color = Border)

                // 默认半径
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "默认半径",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${defaultRadius}米",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = defaultRadius.toFloat(),
                        onValueChange = { onRadiusChange(it.toInt()) },
                        valueRange = 50f..5000f,
                        steps = 98, // (5000-50)/50 - 1
                        colors = SliderDefaults.colors(
                            thumbColor = Primary,
                            activeTrackColor = Primary
                        )
                    )
                }

                Divider(color = Border)

                // 精度阈值
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "位置精度阈值",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${accuracyThreshold}米",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = accuracyThreshold.toFloat(),
                        onValueChange = { onAccuracyChange(it.toInt()) },
                        valueRange = 10f..500f,
                        steps = 48, // (500-10)/10 - 1
                        colors = SliderDefaults.colors(
                            thumbColor = Primary,
                            activeTrackColor = Primary
                        )
                    )
                }

                Divider(color = Border)

                // 省电模式
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBatteryToggle() }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "省电模式",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "降低位置检查频率",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = batteryOptimization,
                        onCheckedChange = { onBatteryToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Primary
                        )
                    )
                }

                Divider(color = Border)

                // 离开通知
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNotifyToggle() }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "离开地点时通知",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "在围栏外也发送提醒",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = notifyWhenOutside,
                        onCheckedChange = { onNotifyToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Primary
                        )
                    )
                }
            }
        }
    }
}

// ========== 权限请求组件 ==========

/**
 * 权限请求卡片
 */
@Composable
private fun PermissionRequestCard(
    uiState: GeofenceConfigUiState,
    onRequestLocationPermission: () -> Unit,
    onRequestBackgroundPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0) // 淡橙色背景，表示需要注意
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⚠️",
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "需要位置权限",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 说明文字
            Text(
                text = "地理围栏功能需要访问您的位置信息，以便在您进入或离开特定区域时发送提醒。",
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 位置权限按钮
            if (!uiState.hasLocationPermission) {
                Button(
                    onClick = {
                        when (uiState.locationPermissionState) {
                            PermissionState.PERMANENTLY_DENIED -> onOpenSettings()
                            else -> onRequestLocationPermission()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Text(
                        text = if (uiState.locationPermissionState == PermissionState.PERMANENTLY_DENIED) {
                            "去设置中开启"
                        } else {
                            "授予位置权限"
                        }
                    )
                }

                if (uiState.locationPermissionState == PermissionState.DENIED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 位置权限被拒绝，地理围栏功能将无法使用",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100)
                    )
                } else if (uiState.locationPermissionState == PermissionState.PERMANENTLY_DENIED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 权限被永久拒绝，请在系统设置中手动开启",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD32F2F)
                    )
                }
            }

            // 后台位置权限按钮（Android 10+）
            if (uiState.hasLocationPermission && !uiState.hasBackgroundLocationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFFFFCC80))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "后台位置权限",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "为了在应用在后台运行时也能监控地理围栏，需要授予\"始终允许\"访问位置的权限。",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        when (uiState.backgroundLocationPermissionState) {
                            PermissionState.PERMANENTLY_DENIED -> onOpenSettings()
                            else -> onRequestBackgroundPermission()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Text(
                        text = if (uiState.backgroundLocationPermissionState == PermissionState.PERMANENTLY_DENIED) {
                            "去设置中开启"
                        } else {
                            "授予后台位置权限"
                        }
                    )
                }

                if (uiState.backgroundLocationPermissionState == PermissionState.DENIED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 没有后台权限，应用在后台时无法监控地理围栏",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100)
                    )
                } else if (uiState.backgroundLocationPermissionState == PermissionState.PERMANENTLY_DENIED) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 权限被永久拒绝，请在系统设置中选择\"始终允许\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD32F2F)
                    )
                }
            }

            // 权限说明
            if (uiState.hasFullPermissions) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "✅", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "所有必需权限已授予",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


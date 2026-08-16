package com.nextthing.app.presentation.screens.geofence.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
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
import com.nextthing.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceLocationDetailScreen(
    navController: NavController,
    viewModel: GeofenceLocationDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val location = uiState.location

    // 监听从地图选择器返回的数据
    LaunchedEffect(Unit) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
        savedStateHandle?.let { handle ->
            // 监听地图选择器返回的数据
            handle.getStateFlow<Double?>("selected_latitude", null).collect { lat ->
                if (lat != null) {
                    val lng = handle.get<Double>("selected_longitude") ?: 0.0
                    val address = handle.get<String>("selected_address") ?: ""
                    viewModel.updateEditPosition(lat, lng, address)

                    // 清除数据，避免重复处理
                    handle.remove<Double>("selected_latitude")
                    handle.remove<Double>("selected_longitude")
                    handle.remove<String>("selected_address")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "编辑地点" else "地点详情", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", Modifier.size(24.dp), tint = Color(0xFF0F1726))
                    }
                },
                actions = {
                    if (!uiState.isEditMode && location != null) {
                        IconButton(onClick = { viewModel.enterEditMode() }) {
                            Icon(Icons.Outlined.Edit, "编辑", Modifier.size(22.dp), tint = Color(0xFF1A7DFA))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgCard
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        bottomBar = {
            if (uiState.isEditMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = BgCard
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.exitEditMode() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消")
                        }
                        Button(
                            onClick = { viewModel.saveChanges() },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("保存")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (location == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("地点不存在", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgPrimary)
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 位置信息卡片
                item {
                    LocationInfoCard(
                        locationName = location.locationInfo.locationName,
                        address = location.locationInfo.address,
                        latitude = location.locationInfo.latitude,
                        longitude = location.locationInfo.longitude,
                        isEditMode = uiState.isEditMode,
                        editLocationName = uiState.editLocationName,
                        editLatitude = uiState.editLatitude,
                        editLongitude = uiState.editLongitude,
                        editAddress = uiState.editAddress,
                        onNameChange = { viewModel.updateEditLocationName(it) },
                        onEditPositionClick = {
                            // 保存到 SavedStateHandle 并跳转到地图选择器
                            navController.navigate("map_picker")
                        }
                    )
                }

                // 地理围栏配置卡片
                item {
                    GeofenceConfigCard(
                        customRadius = location.customRadius,
                        defaultRadius = uiState.defaultRadius,
                        onRadiusChange = { viewModel.updateCustomRadius(it) },
                        latitude = location.locationInfo.latitude,
                        longitude = location.locationInfo.longitude,
                        locationName = location.locationInfo.locationName,
                        onViewOnMap = {
                            // 跳转到地图选择器，并设置初始位置
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("initial_latitude", location.locationInfo.latitude)
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("initial_longitude", location.locationInfo.longitude)
                            navController.navigate("map_picker")
                        }
                    )
                }

                // 使用统计卡片
                item {
                    UsageStatisticsCard(
                        usageCount = location.usageCount,
                        lastUsed = location.lastUsed?.toString() ?: "从未使用",
                        relatedTasksCount = uiState.relatedTasksCount,
                        monthlyCheckCount = uiState.monthlyCheckCount,
                        hitRate = uiState.hitRate,
                        isFrequent = location.isFrequent,
                        onToggleFrequent = { viewModel.toggleFrequent() },
                        onViewRelatedTasks = {
                            // 导航到关联任务列表
                            navController.navigate("geofence_related_tasks/${location.id}")
                        }
                    )
                }

                // 删除按钮（编辑模式下隐藏）
                if (!uiState.isEditMode) {
                    item {
                        DeleteLocationCard(
                            relatedTasksCount = uiState.relatedTasksCount,
                            onClick = { viewModel.showDeleteDialog() }
                        )
                    }
                }
            }
        }

        // 删除确认对话框
        if (uiState.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.hideDeleteDialog() },
                title = { Text("确认删除") },
                text = {
                    Text(
                        buildString {
                            append("确定要删除此地点吗?")
                            if (uiState.relatedTasksCount > 0) {
                                append("\n\n关联的 ${uiState.relatedTasksCount} 个任务的地理围栏也会被删除。")
                            }
                        }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.hideDeleteDialog()
                            viewModel.deleteLocation { navController.popBackStack() }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF44336))
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

// ========== 组件 ==========

@Composable
private fun LocationInfoCard(
    locationName: String,
    address: String,
    latitude: Double,
    longitude: Double,
    isEditMode: Boolean,
    editLocationName: String,
    editLatitude: Double,
    editLongitude: Double,
    editAddress: String,
    onNameChange: (String) -> Unit,
    onEditPositionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "📍 位置信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isEditMode) {
                // 编辑模式：显示输入框
                OutlinedTextField(
                    value = editLocationName,
                    onValueChange = onNameChange,
                    label = { Text("地点名称") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        focusedLabelColor = Primary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 位置编辑按钮
                Button(
                    onClick = onEditPositionClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("🗺️ 在地图上选择位置")
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Border)
                Spacer(modifier = Modifier.height(16.dp))

                // 显示当前编辑的坐标
                Text(
                    text = "当前选择",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (editAddress.isNotEmpty()) {
                    InfoRow("地址", editAddress)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                InfoRow("经度", String.format("%.6f", editLongitude))
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow("纬度", String.format("%.6f", editLatitude))
            } else {
                // 查看模式：显示信息
                InfoRow("名称", locationName.ifEmpty { "未命名" })
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow("地址", address.ifEmpty { "无" })
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow("经度", String.format("%.6f", longitude))
                Spacer(modifier = Modifier.height(12.dp))
                InfoRow("纬度", String.format("%.6f", latitude))
            }
        }
    }
}

@Composable
private fun GeofenceConfigCard(
    customRadius: Int?,
    defaultRadius: Int,
    onRadiusChange: (Int?) -> Unit,
    latitude: Double,
    longitude: Double,
    locationName: String,
    onViewOnMap: () -> Unit = {}
) {
    val context = LocalContext.current
    var sliderValue by remember(customRadius, defaultRadius) {
        mutableStateOf((customRadius ?: defaultRadius).toFloat())
    }
    var useCustom by remember(customRadius) {
        mutableStateOf(customRadius != null)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎯 地理围栏配置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 半径配置
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "自定义半径",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (useCustom) "使用自定义半径" else "使用全局默认半径",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = useCustom,
                    onCheckedChange = {
                        useCustom = it
                        onRadiusChange(if (it) sliderValue.toInt() else null)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Primary
                    )
                )
            }

            if (useCustom) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "当前半径",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${sliderValue.toInt()} 米",
                        style = MaterialTheme.typography.titleMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = {
                        onRadiusChange(sliderValue.toInt())
                    },
                    valueRange = 50f..5000f,
                    steps = 98,
                    colors = SliderDefaults.colors(
                        thumbColor = Primary,
                        activeTrackColor = Primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Border)
            Spacer(modifier = Modifier.height(16.dp))

            // 在地图上查看按钮
            Button(
                onClick = onViewOnMap,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("🗺️ 在地图上查看")
            }
        }
    }
}

@Composable
private fun UsageStatisticsCard(
    usageCount: Int,
    lastUsed: String,
    relatedTasksCount: Int,
    monthlyCheckCount: Int,
    hitRate: Float,
    isFrequent: Boolean,
    onToggleFrequent: () -> Unit,
    onViewRelatedTasks: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "📊 使用统计",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 基础统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "使用次数",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$usageCount 次",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "关联任务",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$relatedTasksCount 个",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "最后使用",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lastUsed,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Border)
            Spacer(modifier = Modifier.height(16.dp))

            // 月度统计
            Text(
                text = "📈 本月统计",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 检查次数
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = monthlyCheckCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                    Text(
                        text = "检查次数",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // 命中率
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (monthlyCheckCount > 0) {
                            "${(hitRate * 100).toInt()}%"
                        } else {
                            "无数据"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (monthlyCheckCount > 0) {
                            when {
                                hitRate >= 0.8f -> Color(0xFF4CAF50) // 高命中率-绿色
                                hitRate >= 0.5f -> Color(0xFFFF9800) // 中命中率-橙色
                                else -> Color(0xFFF44336) // 低命中率-红色
                            }
                        } else {
                            TextSecondary
                        }
                    )
                    Text(
                        text = "命中率",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // 查看关联任务按钮
            if (relatedTasksCount > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onViewRelatedTasks,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary)
                ) {
                    Text("📋 查看关联任务 ($relatedTasksCount)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Border)
            Spacer(modifier = Modifier.height(16.dp))

            // 常用标记
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "⭐ 标记为常用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "常用地点将优先显示",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isFrequent,
                    onCheckedChange = { onToggleFrequent() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Primary
                    )
                )
            }
        }
    }
}

@Composable
private fun DeleteLocationCard(
    relatedTasksCount: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3F3))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🗑️", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "删除此地点",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF44336)
                )
                if (relatedTasksCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "将影响 $relatedTasksCount 个任务",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE57373)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontSize = 15.sp,
            lineHeight = 20.sp
        )
    }
}

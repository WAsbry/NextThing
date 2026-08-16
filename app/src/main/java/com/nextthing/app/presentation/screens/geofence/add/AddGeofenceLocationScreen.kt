package com.nextthing.app.presentation.screens.geofence.add

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nextthing.app.domain.model.LocationInfo
import com.nextthing.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGeofenceLocationScreen(
    navController: NavController,
    viewModel: AddGeofenceLocationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRadiusDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加地理围栏地点", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFF0F1726)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgCard
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = BgCard
            ) {
                Button(
                    onClick = {
                        viewModel.saveGeofenceLocation {
                            navController.popBackStack()
                        }
                    },
                    enabled = uiState.selectedLocation != null && !uiState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("保存", fontSize = 16.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPrimary)
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 选择地点提示
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "从现有地点中选择一个添加为地理围栏",
                            color = Primary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // 可用地点列表
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                        .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.availableLocations.isEmpty()) {
                item {
                    EmptyLocationsCard(
                        onCreateLocation = {
                            // 跳转到创建地点页面
                            navController.navigate("create_location")
                        }
                    )
                }
            } else {
                items(uiState.availableLocations) { location ->
                    LocationSelectionItem(
                        location = location,
                        isSelected = uiState.selectedLocation?.id == location.id,
                        radius = if (uiState.useCustomRadius) uiState.customRadius ?: uiState.defaultRadius else uiState.defaultRadius,
                        onClick = { viewModel.selectLocation(location) },
                        onRadiusClick = { showRadiusDialog = true }
                    )
                }

                // 创建新地点按钮（放在列表底部）
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { navController.navigate("create_location") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Primary
                        ),
                        border = BorderStroke(1.5.dp, Primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "创建新地点",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // 错误提示
        uiState.errorMessage?.let { message ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("关闭")
                    }
                },
                containerColor = Color(0xFFF44336)
            ) {
                Text(message)
            }
        }
    }

    // 半径调整对话框
    if (showRadiusDialog) {
        RadiusAdjustmentDialog(
            currentRadius = if (uiState.useCustomRadius) uiState.customRadius ?: uiState.defaultRadius else uiState.defaultRadius,
            onDismiss = { showRadiusDialog = false },
            onConfirm = { newRadius ->
                viewModel.toggleUseCustomRadius(true)
                viewModel.updateCustomRadius(newRadius)
                showRadiusDialog = false
            },
            onSetDefault = {
                viewModel.toggleUseCustomRadius(false)
                showRadiusDialog = false
            }
        )
    }
}

// ========== 组件 ==========

@Composable
private fun RadiusConfigCard(
    useCustom: Boolean,
    customRadius: Int,
    onToggleCustom: (Boolean) -> Unit,
    onRadiusChange: (Int) -> Unit
) {
    var sliderValue by remember(customRadius) { mutableStateOf(customRadius.toFloat()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎯 自定义半径",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (useCustom) "使用自定义半径" else "使用全局默认半径",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
                Switch(
                    checked = useCustom,
                    onCheckedChange = onToggleCustom,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Primary
                    )
                )
            }

            // 滑块区域（启用自定义时显示）
            if (useCustom) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Border.copy(alpha = 0.5f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // 当前值显示
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Primary.copy(alpha = 0.1f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "${sliderValue.toInt()} 米",
                        style = MaterialTheme.typography.titleLarge,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 滑块
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onRadiusChange(sliderValue.toInt()) },
                    valueRange = 50f..5000f,
                    steps = 98,
                    colors = SliderDefaults.colors(
                        thumbColor = Primary,
                        activeTrackColor = Primary,
                        inactiveTrackColor = Border
                    )
                )

                // 范围提示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "50m",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "5000m",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationSelectionItem(
    location: LocationInfo,
    isSelected: Boolean,
    radius: Int,
    onClick: () -> Unit,
    onRadiusClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F5FF) else BgCard
        ),
        border = BorderStroke(1.dp, if (isSelected) Primary.copy(alpha = 0.45f) else Color(0xFFD6E0ED)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // 顶部：地点名称和选中标记
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 地点名称
                Text(
                    text = location.locationName.ifEmpty { "未命名地点" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Primary else TextPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )

                // 选中指示器 - 改进的圆形勾选标记
                if (isSelected) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Primary,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "✓",
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(2.dp, Border),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 中间：地址信息
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "📍",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = location.address.take(50) + if (location.address.length > 50) "..." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    if (location.city.isNotEmpty() || location.district.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildString {
                                if (location.city.isNotEmpty()) append(location.city)
                                if (location.district.isNotEmpty()) {
                                    if (location.city.isNotEmpty()) append(" · ")
                                    append(location.district)
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 底部：坐标信息和半径（仅选中时显示半径）
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Border.copy(alpha = 0.5f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌐 ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )

                if (isSelected) {
                    Text(
                        text = "半径：${radius}m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable(onClick = onRadiusClick)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RadiusAdjustmentDialog(
    currentRadius: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    onSetDefault: () -> Unit
) {
    val radiusOptions = listOf(50, 100, 200, 500)
    var selectedRadius by remember { mutableStateOf(
        radiusOptions.minByOrNull { kotlin.math.abs(it - currentRadius) } ?: 200
    ) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        shape = RoundedCornerShape(20.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 顶部：标题和默认按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "请滑动调整半径",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    )

                    TextButton(
                        onClick = onSetDefault,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Primary
                        )
                    ) {
                        Text("默认", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 中部：当前选中的半径显示
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Primary.copy(alpha = 0.1f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "${selectedRadius}m",
                        style = MaterialTheme.typography.displaySmall,
                        color = Primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 中部：四档刻度尺（美观布局）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    // 刻度标签
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        radiusOptions.forEachIndexed { index, radius ->
                            Box(
                                modifier = Modifier.weight(
                                    when (index) {
                                        0 -> 1f      // 50m
                                        1 -> 1.6f    // 100m
                                        2 -> 2.3f    // 200m
                                        3 -> 3.5f    // 500m
                                        else -> 1f
                                    }
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${radius}m",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selectedRadius == radius) Primary else TextMuted,
                                    fontSize = if (selectedRadius == radius) 15.sp else 13.sp,
                                    fontWeight = if (selectedRadius == radius) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 连接线和刻度点
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    ) {
                        // 背景连接线
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .align(Alignment.Center)
                                .background(
                                    color = Border.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )

                        // 刻度点
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            radiusOptions.forEachIndexed { index, radius ->
                                Box(
                                    modifier = Modifier.weight(
                                        when (index) {
                                            0 -> 1f      // 50m
                                            1 -> 1.6f    // 100m
                                            2 -> 2.3f    // 200m
                                            3 -> 3.5f    // 500m
                                            else -> 1f
                                        }
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (selectedRadius == radius) Primary else Color.White,
                                        border = if (selectedRadius == radius) null else BorderStroke(2.dp, Border),
                                        modifier = Modifier
                                            .size(if (selectedRadius == radius) 28.dp else 18.dp)
                                            .clickable { selectedRadius = radius }
                                    ) {}
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedRadius) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    "确认",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = null
    )
}

@Composable
private fun EmptyLocationsCard(onCreateLocation: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Color(0xFFD6E0ED)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 大图标
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Primary.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 标题
            Text(
                text = "还没有可用的地点",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 描述
            Text(
                text = "所有地点都已添加为地理围栏",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "或者您还没有创建任何地点",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 按钮
            Button(
                onClick = onCreateLocation,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("创建新地点", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

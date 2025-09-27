package com.example.nextthingb1.presentation.screens.createlocation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.nextthingb1.presentation.theme.*

@Composable
fun CreateLocationScreen(
    onBackPressed: () -> Unit,
    viewModel: CreateLocationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    // 错误消息处理
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            // 3秒后自动清除错误消息
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 顶部导航区
        TopNavigationSection(
            screenHeight = screenHeight,
            screenWidth = screenWidth,
            onBackPressed = onBackPressed
        )

        // 主要内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 地点名称输入区
            LocationNameSection(
                locationName = uiState.locationName,
                onLocationNameChange = { viewModel.updateLocationName(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 位置选择区
            LocationSelectionSection(
                uiState = uiState,
                onModeSelected = { viewModel.updateSelectedMode(it) },
                onGetCurrentLocation = { viewModel.getCurrentLocation() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 错误消息显示
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFD32F2F),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底部保存按钮
            BottomActionSection(
                onSave = {
                    viewModel.saveLocation(onSuccess = onBackPressed)
                },
                onCancel = onBackPressed,
                isEnabled = uiState.locationName.isNotBlank() &&
                           uiState.latitude != null &&
                           uiState.longitude != null,
                isSaving = uiState.isSaving
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TopNavigationSection(
    screenHeight: androidx.compose.ui.unit.Dp,
    screenWidth: androidx.compose.ui.unit.Dp,
    onBackPressed: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFF71CBF4))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 页面标题
            Text(
                text = "新建地点",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            // 占位空间
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun LocationNameSection(
    locationName: String,
    onLocationNameChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "地点名称",
                color = Color(0xFF9E9E9E),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            BasicTextField(
                value = locationName,
                onValueChange = onLocationNameChange,
                textStyle = TextStyle(
                    color = Color(0xFF424242),
                    fontSize = 16.sp
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (locationName.isEmpty()) {
                        Text(
                            text = "输入地点名称（如：公司、家、健身房）",
                            color = Color(0xFF9E9E9E),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun LocationSelectionSection(
    uiState: CreateLocationUiState,
    onModeSelected: (LocationSelectionMode) -> Unit,
    onGetCurrentLocation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "位置选择",
                color = Color(0xFF9E9E9E),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 实时位置选项
            RealTimeLocationCard(
                title = "实时位置",
                icon = Icons.Default.Place,
                isSelected = uiState.selectedMode == LocationSelectionMode.REAL_TIME,
                onClick = {
                    onModeSelected(LocationSelectionMode.REAL_TIME)
                    onGetCurrentLocation()
                },
                isLoading = uiState.isLoadingLocation && uiState.selectedMode == LocationSelectionMode.REAL_TIME,
                locationName = uiState.address,
                latitude = uiState.latitude,
                longitude = uiState.longitude
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 地图选择选项
            LocationModeCard(
                title = "地图选择",
                subtitle = "从地图中选择位置",
                icon = Icons.Default.LocationOn,
                isSelected = uiState.selectedMode == LocationSelectionMode.MAP_SELECT,
                onClick = { onModeSelected(LocationSelectionMode.MAP_SELECT) },
                enabled = false // 暂时禁用，后续可以集成地图选择功能
            )

            // 删除底部位置信息卡片，位置信息现在显示在实时位置卡片上
        }
    }
}

@Composable
private fun RealTimeLocationCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    locationName: String = "",
    latitude: Double? = null,
    longitude: Double? = null,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color(0xFF2196F3) else Color(0xFFE0E0E0)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 主要内容区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) {
                        if (isSelected) Color(0xFF2196F3) else Color(0xFF666666)
                    } else {
                        Color(0xFFBDBDBD)
                    },
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = if (enabled) Color(0xFF424242) else Color(0xFFBDBDBD),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // 经纬度信息显示在标题下方
                    if (latitude != null && longitude != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "经度: ${String.format("%.6f", longitude)}",
                            color = Color(0xFF666666),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "纬度: ${String.format("%.6f", latitude)}",
                            color = Color(0xFF666666),
                            fontSize = 12.sp
                        )
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF2196F3)
                    )
                }
            }

            // 地点名称显示在右上角
            if (locationName.isNotBlank()) {
                Text(
                    text = locationName,
                    color = Color(0xFF2196F3),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.TopEnd),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LocationModeCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color(0xFF2196F3) else Color(0xFFE0E0E0)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    if (isSelected) Color(0xFF2196F3) else Color(0xFF666666)
                } else {
                    Color(0xFFBDBDBD)
                },
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) Color(0xFF424242) else Color(0xFFBDBDBD),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = if (enabled) subtitle else "暂不可用",
                    color = if (enabled) Color(0xFF666666) else Color(0xFFBDBDBD),
                    fontSize = 14.sp
                )
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
private fun BottomActionSection(
    onSave: () -> Unit,
    onCancel: () -> Unit,
    isEnabled: Boolean,
    isSaving: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 取消按钮
        Text(
            text = "取消",
            color = Color(0xFF424242),
            fontSize = 16.sp,
            modifier = Modifier.clickable { onCancel() }
        )

        // 保存按钮
        Button(
            onClick = onSave,
            enabled = isEnabled && !isSaving,
            modifier = Modifier
                .width(120.dp)
                .height(40.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isEnabled) Color(0xFF2196F3) else Color(0xFFB3D9F2),
                contentColor = if (isEnabled) Color.White else Color.White.copy(alpha = 0.8f)
            )
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text(
                    text = "保存",
                    fontSize = 16.sp
                )
            }
        }
    }
}
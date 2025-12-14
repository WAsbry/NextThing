package com.example.nextthingb1.presentation.screens.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextthingb1.domain.model.GeofenceLocation
import com.example.nextthingb1.presentation.theme.Primary
import com.example.nextthingb1.presentation.theme.TextMuted
import com.example.nextthingb1.presentation.theme.TextPrimary
import com.example.nextthingb1.presentation.theme.TextSecondary

/**
 * 任务地理围栏配置卡片（简洁版）
 * 点击后弹出底部对话框进行配置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskGeofenceCard(
    geofenceEnabled: Boolean,
    onGeofenceEnabledChange: (Boolean) -> Unit,
    availableLocations: List<GeofenceLocation>,
    selectedLocationId: String?,
    onLocationSelected: (String?) -> Unit,
    onNavigateToAddLocation: () -> Unit,
    onNavigateToGeofenceSettings: () -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }
    val selectedLocation = availableLocations.find { it.id == selectedLocationId }

    // 简洁的主卡片
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = isEditMode) { showDialog = true },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(0.5.dp, Color(0xFFE0E0E0)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 左上角标签
            Text(
                text = "地理围栏",
                color = Color(0xFF9E9E9E),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.TopStart)
            )

            // 主要内容行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
                    .padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🛡️",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            !geofenceEnabled -> "未启用"
                            selectedLocation != null -> selectedLocation.locationInfo.locationName.ifEmpty { "未命名地点" }
                            else -> "点击选择地点"
                        },
                        color = if (geofenceEnabled && selectedLocation != null) Color(0xFF424242) else Color(0xFF9E9E9E),
                        fontSize = 14.sp
                    )
                    // 显示选中地点的半径
                    if (geofenceEnabled && selectedLocation != null) {
                        Text(
                            text = "半径: ${selectedLocation.customRadius ?: 200}m",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                if (isEditMode) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // 底部弹出对话框
    if (showDialog && isEditMode) {
        val sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = false // 禁止拖动到全屏展开
        )

        ModalBottomSheet(
            onDismissRequest = { showDialog = false },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            dragHandle = {
                // 自定义拖动手柄，防止过度拖动
                Surface(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 4.dp)
                            .background(
                                color = Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxHeight(0.85f)) { // 限制最大高度为屏幕85%
                GeofenceConfigBottomSheet(
                    geofenceEnabled = geofenceEnabled,
                    onGeofenceEnabledChange = onGeofenceEnabledChange,
                    availableLocations = availableLocations,
                    selectedLocationId = selectedLocationId,
                    onLocationSelected = onLocationSelected,
                    onNavigateToAddLocation = onNavigateToAddLocation,
                    onNavigateToGeofenceSettings = onNavigateToGeofenceSettings,
                    onDismiss = { showDialog = false }
                )
            }
        }
    }
}

/**
 * 地理围栏配置底部对话框
 */
@Composable
private fun GeofenceConfigBottomSheet(
    geofenceEnabled: Boolean,
    onGeofenceEnabledChange: (Boolean) -> Unit,
    availableLocations: List<GeofenceLocation>,
    selectedLocationId: String?,
    onLocationSelected: (String?) -> Unit,
    onNavigateToAddLocation: () -> Unit,
    onNavigateToGeofenceSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 固定的标题栏（不滚动）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🛡️ 地理围栏配置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            TextButton(onClick = onDismiss) {
                Text("完成", color = Primary)
            }
        }

        Divider(color = Color(0xFFE0E0E0))

        // 可滚动的内容区域
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 启用/禁用开关
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "启用地理围栏",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "到达指定地点时自动提醒",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = geofenceEnabled,
                        onCheckedChange = onGeofenceEnabledChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Primary
                        )
                    )
                }
            }

            // 地点选择器（仅在启用时显示）
            if (geofenceEnabled) {
                item {
                    Divider(color = Color(0xFFE0E0E0))
                }

                if (availableLocations.isEmpty()) {
                    // 无可用地点提示
                    item {
                        NoLocationsHint(
                            onNavigateToAddLocation = {
                                onNavigateToAddLocation()
                                onDismiss()
                            }
                        )
                    }
                } else {
                    // 地点选择器标题和设置入口
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "选择地点",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "⚙️ 地理围栏设置",
                                modifier = Modifier.clickable {
                                    onNavigateToGeofenceSettings()
                                    onDismiss()
                                },
                                color = Primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 可用地点列表
                    items(availableLocations) { location ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLocationSelected(location.id) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLocationId == location.id,
                                onClick = { onLocationSelected(location.id) },
                                colors = RadioButtonDefaults.colors(selectedColor = Primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (location.isFrequent) {
                                        Text(text = "⭐", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = location.locationInfo.locationName.ifEmpty { "未命名地点" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (selectedLocationId == location.id) Primary else TextPrimary,
                                        fontWeight = if (selectedLocationId == location.id) FontWeight.Medium else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                                if (location.locationInfo.address.isNotEmpty()) {
                                    Text(
                                        text = location.locationInfo.address.take(30) + if (location.locationInfo.address.length > 30) "..." else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "半径: ${location.customRadius ?: 200}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                    if (location.usageCount > 0) {
                                        Text(
                                            text = "• 使用${location.usageCount}次",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 不选择选项（放在列表最后）
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLocationSelected(null) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLocationId == null,
                                onClick = { onLocationSelected(null) },
                                colors = RadioButtonDefaults.colors(selectedColor = Primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "不选择",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selectedLocationId == null) Primary else TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // 添加新地点按钮
                    item {
                        Divider(color = Color(0xFFE0E0E0))
                    }

                    item {
                        OutlinedButton(
                            onClick = {
                                onNavigateToAddLocation()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Primary
                            ),
                            border = BorderStroke(1.dp, Primary)
                        ) {
                            Text("➕ 添加新地点", fontSize = 14.sp)
                        }
                    }

                    // 底部安全区域
                    item {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        }
    }
}

/**
 * 无可用地点提示组件
 */
@Composable
private fun NoLocationsHint(
    onNavigateToAddLocation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "📍", fontSize = 36.sp)

            Text(
                text = "还没有可用的地理围栏地点",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "请先添加地理围栏地点，以便在到达时自动提醒",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp
            )

            Button(
                onClick = onNavigateToAddLocation,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("前往添加地点", fontSize = 14.sp)
            }
        }
    }
}

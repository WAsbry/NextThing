package com.nextthing.app.presentation.screens.geofence.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nextthing.app.R
import com.nextthing.app.presentation.theme.BgCard
import com.nextthing.app.presentation.theme.BgPrimary
import com.nextthing.app.presentation.theme.Border
import com.nextthing.app.presentation.theme.Primary
import com.nextthing.app.presentation.theme.TextMuted
import com.nextthing.app.presentation.theme.TextPrimary
import com.nextthing.app.presentation.theme.TextSecondary
import com.nextthing.app.presentation.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceLocationDetailScreen(
    navController: NavController,
    startInEditMode: Boolean = false,
    viewModel: GeofenceLocationDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val location = uiState.location

    LaunchedEffect(Unit) {
        navController.currentBackStackEntry?.savedStateHandle?.let { handle ->
            handle.getStateFlow<Double?>("selected_latitude", null).collect { latitude ->
                if (latitude != null) {
                    viewModel.updateEditPosition(
                        latitude,
                        handle.get<Double>("selected_longitude") ?: 0.0,
                        handle.get<String>("selected_address") ?: ""
                    )
                    handle.remove<Double>("selected_latitude")
                    handle.remove<Double>("selected_longitude")
                    handle.remove<String>("selected_address")
                }
            }
        }
    }

    LaunchedEffect(startInEditMode, location?.id) {
        if (startInEditMode && location != null && !uiState.isEditMode) {
            viewModel.enterEditMode()
        }
    }

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditMode || startInEditMode) "编辑地点" else "地点详情",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navController::popBackStack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary)
                    }
                },
                actions = {
                    if (!startInEditMode && !uiState.isEditMode && location != null) {
                        TextButton(onClick = {
                            navController.navigate("geofence_location_edit/${location.id}")
                        }) {
                            Text("编辑", color = Primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        bottomBar = {
            if (uiState.isEditMode) {
                SurfaceSaveBar(
                    saving = uiState.isSaving,
                    onCancel = navController::popBackStack,
                    onSave = { viewModel.saveChanges(navController::popBackStack) }
                )
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> LoadingState(paddingValues)
            location == null -> MissingLocationState(paddingValues)
            else -> LocationDetailContent(
                location = location,
                uiState = uiState,
                paddingValues = paddingValues,
                onNameChange = viewModel::updateEditLocationName,
                onEditPosition = {
                    navController.navigate("map_picker")
                },
                onViewMap = {
                    navController.currentBackStackEntry?.savedStateHandle?.apply {
                        set("initial_latitude", location.locationInfo.latitude)
                        set("initial_longitude", location.locationInfo.longitude)
                    }
                    navController.navigate("map_picker")
                },
                onRadiusChange = viewModel::updateCustomRadius,
                onGeofenceEnabledChange = viewModel::updateGeofenceEnabled,
                onToggleFrequent = viewModel::toggleFrequent,
                onViewRelatedTasks = {
                    navController.navigate("geofence_related_tasks/${location.id}")
                },
                onDelete = viewModel::showDeleteDialog
            )
        }

        if (uiState.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = viewModel::hideDeleteDialog,
                shape = RoundedCornerShape(8.dp),
                containerColor = BgCard,
                title = { Text("删除此地点？", color = TextPrimary) },
                text = {
                    Text(
                        text = if (uiState.relatedTasksCount > 0) {
                            "删除后将解除 ${uiState.relatedTasksCount} 个任务的地点围栏。"
                        } else {
                            "删除后不可恢复。"
                        },
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.hideDeleteDialog()
                            viewModel.deleteLocation(navController::popBackStack)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF44336))
                    ) { Text("删除") }
                },
                dismissButton = { TextButton(onClick = viewModel::hideDeleteDialog) { Text("取消") } }
            )
        }
    }
}

@Composable
private fun LocationDetailContent(
    location: com.nextthing.app.domain.model.GeofenceLocation,
    uiState: GeofenceLocationDetailUiState,
    paddingValues: PaddingValues,
    onNameChange: (String) -> Unit,
    onEditPosition: () -> Unit,
    onViewMap: () -> Unit,
    onRadiusChange: (Int?) -> Unit,
    onGeofenceEnabledChange: (Boolean) -> Unit,
    onToggleFrequent: () -> Unit,
    onViewRelatedTasks: () -> Unit,
    onDelete: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgPrimary).padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        item { SectionLabel("地点信息") }
        item {
            LocationInfoCard(
                location = location,
                uiState = uiState,
                onNameChange = onNameChange,
                onEditPosition = onEditPosition,
                onViewMap = onViewMap
            )
        }
        if (uiState.isEditMode || (uiState.isGlobalEnabled && location.isEnabled)) {
            item { SectionLabel("围栏设置") }
            item {
                if (uiState.isEditMode) {
                GeofenceSettingsCard(
                    enabled = uiState.editGeofenceEnabled,
                    customRadius = uiState.editCustomRadius,
                    defaultRadius = uiState.defaultRadius,
                    isGlobalEnabled = uiState.isGlobalEnabled,
                    relatedTasksCount = uiState.relatedTasksCount,
                    onRadiusChange = onRadiusChange,
                    onEnabledChange = onGeofenceEnabledChange,
                    onViewRelatedTasks = onViewRelatedTasks
                )
                } else {
                GeofenceSettingsSummary(
                    customRadius = location.customRadius,
                    defaultRadius = uiState.defaultRadius,
                    relatedTasksCount = uiState.relatedTasksCount,
                    onViewRelatedTasks = onViewRelatedTasks
                )
                }
            }
        }
        if (uiState.isEditMode) {
            item { SectionLabel("其他设置") }
            item {
                FrequentSettingCard(
                    checked = uiState.editIsFrequent,
                    onCheckedChange = onToggleFrequent
                )
            }
            item { Spacer(Modifier.height(14.dp)) }
            item { DeleteLocationCard(onClick = onDelete) }
        } else {
            item { SectionLabel("其他设置") }
            item { FrequentSettingSummary(isFrequent = location.isFrequent) }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
        modifier = Modifier.padding(top = 8.dp, bottom = 3.dp)
    )
}

@Composable
private fun LocationInfoCard(
    location: com.nextthing.app.domain.model.GeofenceLocation,
    uiState: GeofenceLocationDetailUiState,
    onNameChange: (String) -> Unit,
    onEditPosition: () -> Unit,
    onViewMap: () -> Unit
) {
    StandardCard {
        if (uiState.isEditMode) {
            OutlinedTextField(
                value = uiState.editLocationName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("地点名称") },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    focusedLabelColor = Primary,
                    unfocusedBorderColor = Border,
                    focusedContainerColor = BgCard,
                    unfocusedContainerColor = BgCard
                )
            )
            Spacer(Modifier.height(10.dp))
            LocationActionRow("在地图上重新选点", onEditPosition)
            Spacer(Modifier.height(12.dp))
            AddressAndCoordinates(
                address = uiState.editAddress,
                latitude = uiState.editLatitude,
                longitude = uiState.editLongitude
            )
        } else {
            Text(location.locationInfo.locationName.ifBlank { "未命名地点" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            AddressAndCoordinates(
                address = location.locationInfo.address,
                latitude = location.locationInfo.latitude,
                longitude = location.locationInfo.longitude
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Border)
            LocationActionRow("在地图中查看", onViewMap)
        }
    }
}

@Composable
private fun AddressAndCoordinates(address: String, latitude: Double, longitude: Double) {
    Text("详细地址", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    Spacer(Modifier.height(3.dp))
    Text(
        address.ifBlank { "未获取详细地址" },
        color = TextPrimary,
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth()) {
        CoordinateItem("经度", String.format("%.6f", longitude), Modifier.weight(1f))
        CoordinateItem("纬度", String.format("%.6f", latitude), Modifier.weight(1f))
    }
}

@Composable
private fun CoordinateItem(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

@Composable
private fun LocationActionRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Map, null, tint = Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = Primary, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "打开地图", tint = TextMuted)
    }
}

@Composable
private fun GeofenceSettingsCard(
    enabled: Boolean,
    customRadius: Int?,
    defaultRadius: Int,
    isGlobalEnabled: Boolean,
    relatedTasksCount: Int,
    onRadiusChange: (Int?) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onViewRelatedTasks: () -> Unit
) {
    val suggestedRadius = GeofenceRadiusPreset.entries
        .firstOrNull { it.meters == defaultRadius }
        ?.meters
        ?: GeofenceRadiusPreset.Recommended.meters
    StandardCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("启用此地点地理围栏", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(
                    if (isGlobalEnabled) {
                        "开启后，可用于任务的到达和离开提醒"
                    } else {
                        "请先在地理围栏页面开启服务"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Switch(
                checked = enabled,
                enabled = isGlobalEnabled,
                onCheckedChange = { onEnabledChange(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary)
            )
        }
        if (enabled && isGlobalEnabled) {
            Spacer(Modifier.height(14.dp))
            Text("此地点半径", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text("用于判断到达和离开，不等同于定位精度", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(10.dp))
            RadiusPresetSelector(
                selectedRadius = customRadius ?: suggestedRadius,
                onRadiusSelected = onRadiusChange
            )
        }
        if (relatedTasksCount > 0) {
            HorizontalDivider(color = Border)
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onViewRelatedTasks).padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("已关联任务", color = TextPrimary, modifier = Modifier.weight(1f))
                Text("$relatedTasksCount 个", color = TextSecondary)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "查看关联任务", tint = TextMuted)
            }
        }
    }
}

@Composable
private fun GeofenceSettingsSummary(
    customRadius: Int?,
    defaultRadius: Int,
    relatedTasksCount: Int,
    onViewRelatedTasks: () -> Unit
) {
    StandardCard {
        Text("此地点半径", color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(
            text = if (customRadius == null) {
                "使用全局默认 · ${formatRadius(defaultRadius)}"
            } else {
                "已单独设置 · ${formatRadius(customRadius)}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        if (relatedTasksCount > 0) {
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = Border)
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onViewRelatedTasks).padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("关联任务", color = TextPrimary, modifier = Modifier.weight(1f))
                Text("$relatedTasksCount 个", color = TextSecondary)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "查看关联任务", tint = TextMuted)
            }
        }
    }
}

private enum class GeofenceRadiusPreset(val meters: Int, val label: String) {
    Precise(100, "100m"),
    Recommended(200, "200m"),
    Nearby(500, "500m"),
    Area(1000, "1km")
}

@Composable
private fun RadiusPresetSelector(
    selectedRadius: Int,
    onRadiusSelected: (Int) -> Unit
) {
    val hasLegacyRadius = GeofenceRadiusPreset.entries.none { it.meters == selectedRadius }
    if (hasLegacyRadius) {
        Text(
            text = "当前旧设置 ${formatRadius(selectedRadius)}，请选择新的半径档位",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Spacer(Modifier.height(10.dp))
    }

    Box(modifier = Modifier.fillMaxWidth().height(26.dp)) {
        HorizontalDivider(
            modifier = Modifier.align(Alignment.Center),
            color = Border,
            thickness = 1.dp
        )
        Row(modifier = Modifier.fillMaxSize()) {
            GeofenceRadiusPreset.entries.forEach { preset ->
                val selected = preset.meters == selectedRadius
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize().clickable {
                        onRadiusSelected(preset.meters)
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (selected) 14.dp else 10.dp)
                            .background(if (selected) Primary else BgCard, CircleShape)
                            .border(
                                width = 1.dp,
                                color = if (selected) Primary else TextMuted,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        GeofenceRadiusPreset.entries.forEach { preset ->
            Text(
                text = preset.label,
                modifier = Modifier.weight(1f).clickable { onRadiusSelected(preset.meters) },
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = if (preset.meters == selectedRadius) Primary else TextSecondary,
                fontWeight = if (preset.meters == selectedRadius) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        text = "推荐 200 米，适合办公、住宅等常用地点",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
    )
}

private fun formatRadius(radius: Int): String =
    if (radius >= 1000 && radius % 1000 == 0) "${radius / 1000} 公里" else "$radius 米"

@Composable
private fun FrequentSettingCard(checked: Boolean, onCheckedChange: () -> Unit) {
    StandardCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FrequentStatusIcon(isFrequent = checked)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("标记为常用", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text("常用地点会优先显示", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = { onCheckedChange() },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary)
            )
        }
    }
}

@Composable
private fun FrequentSettingSummary(isFrequent: Boolean) {
    StandardCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FrequentStatusIcon(isFrequent = isFrequent)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("常用地点", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (isFrequent) "已标记为常用" else "未标记",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            if (isFrequent) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Warning.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "常用",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Warning,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequentStatusIcon(isFrequent: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (isFrequent) Warning.copy(alpha = 0.12f) else Primary.copy(alpha = 0.10f),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(
                if (isFrequent) R.drawable.icon_geofence_star_filled else R.drawable.icon_geofence_star
            ),
            contentDescription = if (isFrequent) "已标记为常用" else "未标记为常用",
            tint = if (isFrequent) Warning else TextMuted,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun DeleteLocationCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFFFFD8D8)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF5F5))
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFF44336), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("删除此地点", color = Color(0xFFF44336), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StandardCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), content = content)
    }
}

@Composable
private fun SurfaceSaveBar(saving: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    androidx.compose.material3.Surface(color = BgCard, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Border)
            ) { Text("取消", color = TextPrimary) }
            Button(
                onClick = onSave,
                enabled = !saving,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (saving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("保存", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LoadingState(paddingValues: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Primary)
    }
}

@Composable
private fun MissingLocationState(paddingValues: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
        Text("地点不存在", color = TextSecondary)
    }
}

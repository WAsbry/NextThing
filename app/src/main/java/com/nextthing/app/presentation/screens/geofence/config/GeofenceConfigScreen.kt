package com.nextthing.app.presentation.screens.geofence.config

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.nextthing.app.R
import com.nextthing.app.domain.model.GeofenceLocation
import com.nextthing.app.presentation.components.geofence.GeofenceEmptyCopy
import com.nextthing.app.presentation.components.geofence.GeofenceLocationSummary
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
fun GeofenceConfigScreen(
    navController: NavController,
    viewModel: GeofenceConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val activity = context as? android.app.Activity
        viewModel.onLocationPermissionResult(
            granted = granted,
            shouldShowRationale = !granted && activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
        )
    }

    val backgroundLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val activity = context as? android.app.Activity
        viewModel.onBackgroundLocationPermissionResult(
            granted = granted,
            shouldShowRationale = !granted && activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
        )
    }

    fun openAppSettings() {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        )
    }

    fun requestForegroundPermission() {
        if (uiState.locationPermissionState == PermissionState.PERMANENTLY_DENIED) {
            openAppSettings()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun requestBackgroundPermission() {
        if (uiState.backgroundLocationPermissionState == PermissionState.PERMANENTLY_DENIED) {
            openAppSettings()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        uiState.successMessage?.let { snackbarHostState.showSnackbar(it) }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        containerColor = BgPrimary,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "地理围栏",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navController::popBackStack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 5.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                item {
                    SectionLabel("服务状态")
                }

                item {
                    GeofenceServiceCard(
                        uiState = uiState,
                        onRequestForegroundPermission = ::requestForegroundPermission,
                        onRequestBackgroundPermission = ::requestBackgroundPermission,
                        onToggle = viewModel::toggleGlobalEnabled
                    )
                }

                item {
                    LocationsSectionHeader(
                        count = uiState.totalLocationsCount,
                        showAddAction = uiState.locations.isNotEmpty(),
                        onAddClick = { navController.navigate("geofence_location_add") }
                    )
                }

                if (uiState.locations.isEmpty() && !uiState.isLoading) {
                    item {
                        GeofenceEmptyState(
                            onAddClick = { navController.navigate("geofence_location_add") }
                        )
                    }
                } else {
                    items(
                        items = uiState.locations.sortedByDescending { it.isFrequent },
                        key = { it.id }
                    ) { location ->
                        GeofenceLocationItem(
                            location = location,
                            defaultRadius = uiState.defaultRadius,
                            onClick = {
                                navController.navigate("geofence_location_detail/${location.id}")
                            }
                        )
                    }
                }

                item {
                    SectionLabel("围栏设置")
                }

                item {
                    AdvancedSettingsCard(
                        isExpanded = uiState.showAdvancedSettings,
                        onToggle = viewModel::toggleAdvancedSettings,
                        defaultRadius = uiState.defaultRadius,
                        onRadiusChange = viewModel::updateDefaultRadius,
                        accuracyThreshold = uiState.locationAccuracyThreshold,
                        onAccuracyChange = viewModel::updateLocationAccuracyThreshold,
                        notifyWhenOutside = uiState.notifyWhenOutside,
                        onNotifyToggle = viewModel::toggleNotifyWhenOutside
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgPrimary.copy(alpha = 0.78f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }
        }

        if (uiState.showDeleteConfirmation) {
            val locationName = uiState.locationToDelete?.locationInfo?.locationName
                ?.ifEmpty { "未命名地点" }
                ?: "该地点"
            AlertDialog(
                onDismissRequest = viewModel::cancelDelete,
                shape = RoundedCornerShape(8.dp),
                containerColor = BgCard,
                title = { Text("删除地点？", color = TextPrimary) },
                text = {
                    Text(
                        text = "删除“$locationName”后，已关联任务的地理围栏也会被移除。",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = viewModel::confirmDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                    ) { Text("删除") }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::cancelDelete) { Text("取消") }
                }
            )
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
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun LocationsSectionHeader(
    count: Int,
    showAddAction: Boolean,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "地点（$count）",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.weight(1f))
        if (showAddAction) {
            TextButton(onClick = onAddClick) {
                Text("添加地点", color = Primary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun GeofenceServiceCard(
    uiState: GeofenceConfigUiState,
    onRequestForegroundPermission: () -> Unit,
    onRequestBackgroundPermission: () -> Unit,
    onToggle: () -> Unit
) {
    val needsForeground = !uiState.hasLocationPermission
    val needsBackground = uiState.hasLocationPermission &&
        !uiState.hasBackgroundLocationPermission &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val title = when {
        needsForeground -> "需要位置权限"
        needsBackground -> "还需后台定位权限"
        else -> "地理围栏服务"
    }
    val description = when {
        needsForeground -> "开启位置权限后，才能判断你是否到达或离开地点"
        needsBackground -> "继续授权后，应用退到后台也能正常触发提醒"
        uiState.isGlobalEnabled -> "已开启 · 到达和离开地点时提醒"
        else -> "已关闭 · 现有地点和任务绑定会保留"
    }

    val actionLabel = if (needsForeground) "去授权" else "继续授权"
    val serviceEnabled = !needsForeground && !needsBackground && uiState.isGlobalEnabled

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (serviceEnabled) Primary.copy(alpha = 0.45f) else Border),
        colors = CardDefaults.cardColors(
            containerColor = if (serviceEnabled) Primary.copy(alpha = 0.08f) else BgCard
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (needsForeground || needsBackground) {
                                Warning.copy(alpha = 0.12f)
                            } else {
                                Primary.copy(alpha = 0.10f)
                            },
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icon_mine_geofence),
                        contentDescription = null,
                        tint = if (needsForeground || needsBackground) Warning else Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                if (!needsForeground && !needsBackground) {
                    Switch(
                        checked = uiState.isGlobalEnabled,
                        onCheckedChange = { onToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Primary
                        )
                    )
                } else {
                    TextButton(
                        onClick = if (needsForeground) onRequestForegroundPermission else onRequestBackgroundPermission
                    ) {
                        Text(actionLabel, color = Primary, fontWeight = FontWeight.Medium)
                    }
                }
        }
    }
}

@Composable
private fun GeofenceLocationItem(
    location: GeofenceLocation,
    defaultRadius: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Primary.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.icon_mine_geofence),
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(23.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            GeofenceLocationSummary(
                location = location,
                defaultRadius = defaultRadius,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "查看地点详情",
                tint = TextMuted
            )
        }
    }
}

@Composable
private fun GeofenceEmptyState(
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GeofenceEmptyCopy()
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("新建地点")
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
    notifyWhenOutside: Boolean,
    onNotifyToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Border),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Primary.copy(alpha = 0.10f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icon_geofence_settings),
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "高级设置",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "默认半径、定位精度与离开提醒",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (isExpanded) "收起高级设置" else "展开高级设置",
                    tint = TextMuted
                )
            }

            if (isExpanded) {
                HorizontalDivider(color = Border)
                SliderSetting(
                    title = "默认围栏半径",
                    valueText = "$defaultRadius 米",
                    value = defaultRadius.toFloat(),
                    valueRange = 50f..5000f,
                    steps = 98,
                    onValueChange = { onRadiusChange(it.toInt()) }
                )
                HorizontalDivider(color = Border)
                SliderSetting(
                    title = "定位精度阈值",
                    valueText = "$accuracyThreshold 米",
                    value = accuracyThreshold.toFloat(),
                    valueRange = 10f..500f,
                    steps = 48,
                    onValueChange = { onAccuracyChange(it.toInt()) }
                )
                HorizontalDivider(color = Border)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNotifyToggle)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("离开地点时通知", color = TextPrimary)
                        Text(
                            "离开围栏后也发送提醒",
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

@Composable
private fun SliderSetting(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(title, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(valueText, color = Primary, fontWeight = FontWeight.Medium)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
        )
    }
}

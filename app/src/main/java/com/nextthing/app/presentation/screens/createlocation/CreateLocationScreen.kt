package com.nextthing.app.presentation.screens.createlocation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel


import com.nextthing.app.presentation.components.AppToastHost
import com.nextthing.app.presentation.components.AppToastType
import com.nextthing.app.presentation.components.rememberAppToastHostState
import com.nextthing.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLocationScreen(
    onBackPressed: () -> Unit,
    onNavigateToMapPicker: (Double?, Double?) -> Unit,
    onLocationSaved: (CreatedLocationResult) -> Unit,
    viewModel: CreateLocationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val toastState = rememberAppToastHostState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) viewModel.getCurrentLocation()
    }
    val canSave = uiState.locationName.isNotBlank() && !uiState.isSaving && !uiState.isLoadingLocation &&
        (uiState.selectedMode == LocationSelectionMode.REAL_TIME ||
            (uiState.latitude != null && uiState.longitude != null))

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) viewModel.getCurrentLocation() else {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            toastState.showDebounced(it, AppToastType.Error, debounceMillis = 0)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                title = { Text("新建地点", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        bottomBar = {
            Surface(color = BgCard, shadowElevation = 10.dp) {
                Button(
                    onClick = { viewModel.saveLocation(onSuccess = onLocationSaved) },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        disabledContainerColor = Border,
                        disabledContentColor = TextMuted
                    )
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("正在保存")
                    } else {
                        Text("保存地点", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text("为任务设置一个可复用的位置围栏", color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))

            Text("地点信息", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = uiState.locationName,
                onValueChange = viewModel::updateLocationName,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("地点名称") },
                placeholder = { Text("例如：公司、健身房、父母家") },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    focusedLabelColor = Primary,
                    unfocusedBorderColor = Border,
                    focusedContainerColor = BgCard,
                    unfocusedContainerColor = BgCard
                )
            )

            Spacer(Modifier.height(28.dp))
            Text("地点位置", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            CurrentLocationOption(
                selected = uiState.selectedMode == LocationSelectionMode.REAL_TIME,
                loading = uiState.isLoadingLocation,
                address = uiState.address,
                message = uiState.locationMessage,
                onClick = {
                    viewModel.updateSelectedMode(LocationSelectionMode.REAL_TIME)
                    viewModel.getCurrentLocation()
                }
            )
            Spacer(Modifier.height(10.dp))
            LocationSummaryCard(
                state = uiState,
                onSelect = { onNavigateToMapPicker(uiState.latitude, uiState.longitude) }
            )

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = Primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "可在地图中搜索地点、拖动地图选点，或回到实时位置。",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
        }
        AppToastHost(hostState = toastState, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun CurrentLocationOption(
    selected: Boolean,
    loading: Boolean,
    address: String,
    message: String?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Primary.copy(alpha = 0.08f) else BgCard,
        border = BorderStroke(1.dp, if (selected) Primary.copy(alpha = 0.45f) else Border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = Primary))
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text("使用当前位置", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    text = when {
                        loading -> "正在获取当前位置…"
                        selected && address.isNotBlank() -> address
                        message != null -> message
                        else -> "默认使用当前位置，可改为地图选点"
                    },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = Primary, strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun LocationSummaryCard(
    state: CreateLocationUiState,
    onSelect: () -> Unit
) {
    val selected = state.latitude != null && state.longitude != null
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        color = BgCard,
        border = BorderStroke(1.dp, if (selected) Primary.copy(alpha = 0.35f) else Border)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = Primary.copy(alpha = 0.12f)) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.padding(10.dp).size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("选择地点", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (selected && state.selectedMode == LocationSelectionMode.MAP_SELECT) {
                        state.address.ifBlank { "已选择地图坐标" }
                    } else "搜索地点或在地图上拖动选点",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextMuted)
        }
    }
}

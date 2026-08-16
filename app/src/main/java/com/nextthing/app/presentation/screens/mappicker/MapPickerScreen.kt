package com.nextthing.app.presentation.screens.mappicker

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.nextthing.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    onBackPressed: () -> Unit,
    onLocationSelected: (latitude: Double, longitude: Double, address: String) -> Unit,
    viewModel: MapPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) viewModel.getCurrentLocation()
    }
    val mapView = remember(context) {
        MapView(context).apply { onCreate(null) }
    }
    var aMap by remember { mutableStateOf<AMap?>(null) }
    var suppressNextCameraCallback by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.initGeocodeSearch(context)
    }

    LaunchedEffect(uiState.hasSelectedLocation) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        // 从新建地点页带入的坐标就是本页初始点；不要再发起一次强制定位覆盖它。
        if (uiState.hasSelectedLocation) return@LaunchedEffect
        if (hasPermission) viewModel.getCurrentLocation() else {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        mapView.map?.let { map ->
            aMap = map
            map.uiSettings.apply {
                isZoomControlsEnabled = false
                isScaleControlsEnabled = false
                isCompassEnabled = false
            }
            map.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition(
                        LatLng(uiState.latitude ?: 39.9042, uiState.longitude ?: 116.4074),
                        16f,
                        0f,
                        0f
                    )
                )
            )
            map.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
                override fun onCameraChange(position: CameraPosition?) = Unit

                override fun onCameraChangeFinish(position: CameraPosition?) {
                    val target = position?.target ?: return
                    if (suppressNextCameraCallback) {
                        suppressNextCameraCallback = false
                    } else {
                        viewModel.updateLocation(target.latitude, target.longitude)
                    }
                }
            })
        }
    }

    LaunchedEffect(uiState.moveToken, aMap) {
        if (uiState.latitude == null || uiState.longitude == null) return@LaunchedEffect
        suppressNextCameraCallback = true
        aMap?.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition(LatLng(uiState.latitude ?: 39.9042, uiState.longitude ?: 116.4074), 16f, 0f, 0f)
            )
        )
    }

    Box(Modifier.fillMaxSize().background(BgPrimary)) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp)
        )

        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(BgCard)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(start = 0.dp, top = 4.dp, end = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackPressed, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = TextPrimary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(1.dp))
                SearchField(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    onSearch = viewModel::searchPlaces,
                    searching = uiState.isSearching,
                    onClear = viewModel::clearSearch,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                )
            }
            if (uiState.searchResults.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = BgCard,
                    shadowElevation = 8.dp
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        uiState.searchResults.forEach { place ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectSearchResult(place) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, null, tint = Primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(place.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (place.address.isNotBlank()) Text(place.address, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }

        Icon(
            Icons.Default.LocationOn,
            contentDescription = "地图中心选点",
            tint = Primary,
            modifier = Modifier.align(Alignment.Center).size(42.dp).shadow(5.dp, CircleShape)
        )

        LocationConfirmSheet(
            state = uiState,
            onUseCurrentLocation = viewModel::getCurrentLocation,
            onConfirm = {
                onLocationSelected(requireNotNull(uiState.latitude), requireNotNull(uiState.longitude), uiState.address)
            }
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    searching: Boolean,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = BgPrimary, shadowElevation = 0.dp) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxSize(),
            textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
            cursorBrush = SolidColor(Primary),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = Primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isBlank()) {
                            Text("搜索地点、地址或 POI", color = TextSecondary, fontSize = 16.sp)
                        }
                        innerTextField()
                    }
                    when {
                        searching -> CircularProgressIndicator(Modifier.size(20.dp), color = Primary, strokeWidth = 2.dp)
                        query.isNotBlank() -> IconButton(onClick = onClear, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Close, "清空", tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch() })
        )
    }
}

@Composable
private fun BoxScope.LocationConfirmSheet(
    state: MapPickerUiState,
    onUseCurrentLocation: () -> Unit,
    onConfirm: () -> Unit
) {
    Surface(
        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = BgCard,
        shadowElevation = 14.dp
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp),
                shape = CircleShape,
                color = Border
            ) {}
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("已选位置", modifier = Modifier.weight(1f), color = TextSecondary, fontSize = 13.sp)
                TextButton(
                    onClick = onUseCurrentLocation,
                    enabled = !state.isLocating,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    if (state.isLocating) {
                        CircularProgressIndicator(Modifier.size(15.dp), color = Primary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("正在定位", fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("使用当前位置", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                when {
                    state.isLocating -> "正在定位当前位置…"
                    state.isLoadingAddress -> "正在获取地址…"
                    state.hasSelectedLocation -> state.address.ifBlank { "已选择地图坐标" }
                    else -> "等待定位，或拖动地图手动选择"
                },
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            state.addressHint?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onConfirm,
                enabled = state.hasSelectedLocation && !state.isLoadingAddress && !state.isLocating,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text("确认此位置", fontWeight = FontWeight.Bold)
            }
        }
    }
}

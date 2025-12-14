package com.example.nextthingb1.presentation.screens.mappicker

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.CameraUpdateFactory
import com.amap.api.maps2d.MapView
import com.amap.api.maps2d.model.CameraPosition
import com.amap.api.maps2d.model.LatLng
import com.amap.api.maps2d.model.MarkerOptions
import com.example.nextthingb1.presentation.theme.Primary
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    onBackPressed: () -> Unit,
    onLocationSelected: (latitude: Double, longitude: Double, address: String) -> Unit,
    viewModel: MapPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var aMap by remember { mutableStateOf<AMap?>(null) }

    // 初始化：获取当前位置 + 初始化 GeocodeSearch
    LaunchedEffect(Unit) {
        viewModel.getCurrentLocation() // 获取当前位置
        viewModel.initGeocodeSearch(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView?.onDestroy()
        }
    }

    // 监听位置变化，更新地图位置
    LaunchedEffect(uiState.latitude, uiState.longitude) {
        aMap?.let { map ->
            val newPosition = LatLng(uiState.latitude, uiState.longitude)

            // 清除旧标记
            map.clear()

            // 添加新标记
            map.addMarker(
                MarkerOptions()
                    .position(newPosition)
                    .title("当前位置")
            )

            // 移动相机到新位置
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition(newPosition, 15f, 0f, 0f)
                )
            )

            Timber.tag("MapPicker").d("📍 地图已移动到: (${uiState.latitude}, ${uiState.longitude})")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 地图视图
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    mapView = this
                    onCreate(Bundle())

                    // 获取地图控制器
                    map?.let { map ->
                        aMap = map

                        // 设置地图UI样式
                        map.uiSettings.apply {
                            isZoomControlsEnabled = false // 隐藏缩放按钮
                            isScaleControlsEnabled = true // 显示比例尺
                            isCompassEnabled = true // 显示指南针
                        }

                        // 设置初始位置（北京天安门）
                        val initialPosition = LatLng(uiState.latitude, uiState.longitude)
                        map.moveCamera(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition(initialPosition, 15f, 0f, 0f)
                            )
                        )

                        // 地图点击监听
                        map.setOnMapClickListener { latLng ->
                            // 清除之前的标记
                            map.clear()

                            // 添加新标记
                            map.addMarker(
                                MarkerOptions()
                                    .position(latLng)
                                    .title("选中位置")
                            )

                            // 更新ViewModel中的位置
                            viewModel.updateLocation(
                                latitude = latLng.latitude,
                                longitude = latLng.longitude
                            )
                        }

                        // 添加初始标记
                        map.addMarker(
                            MarkerOptions()
                                .position(initialPosition)
                                .title("当前位置")
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 顶部导航栏
        TopAppBar(
            title = {
                Text(
                    "选择地点",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Primary
            ),
            windowInsets = WindowInsets(0, 0, 0, 0)
        )

        // 底部信息卡片
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 位置信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // 标题行
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "选中位置",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 地址信息
                    if (uiState.isLoadingAddress) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "正在获取地址...",
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    } else {
                        Text(
                            text = uiState.address.ifEmpty { "点击地图选择位置" },
                            fontSize = 14.sp,
                            color = if (uiState.address.isEmpty()) Color(0xFF999999) else Color(0xFF424242),
                            lineHeight = 20.sp
                        )
                    }

                    // 坐标信息
                    if (uiState.hasSelectedLocation) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "经度: ${String.format("%.6f", uiState.longitude)}",
                            fontSize = 12.sp,
                            color = Color(0xFF999999),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Text(
                            text = "纬度: ${String.format("%.6f", uiState.latitude)}",
                            fontSize = 12.sp,
                            color = Color(0xFF999999),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 确认按钮
            Button(
                onClick = {
                    if (uiState.hasSelectedLocation) {
                        onLocationSelected(
                            uiState.latitude,
                            uiState.longitude,
                            uiState.address
                        )
                    }
                },
                enabled = uiState.hasSelectedLocation && !uiState.isLoadingAddress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "确认选择",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 错误提示
        uiState.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp, start = 16.dp, end = 16.dp),
                containerColor = Color(0xFFFFEBEE),
                contentColor = Color(0xFFD32F2F)
            ) {
                Text(text = error)
            }
        }
    }

    // 地图生命周期管理
    LaunchedEffect(mapView) {
        mapView?.onResume()
    }

    DisposableEffect(mapView) {
        onDispose {
            mapView?.onPause()
        }
    }
}

package com.nextthing.app.presentation.screens.mappicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.nextthing.app.domain.service.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class MapPickerViewModel @Inject constructor(
    private val locationService: LocationService,
    private val savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapPickerUiState())
    val uiState: StateFlow<MapPickerUiState> = _uiState.asStateFlow()

    private var geocodeSearch: GeocodeSearch? = null

    private var hasInitialLocation = false

    init {
        // 检查是否有初始位置参数（从地点详情页传递）
        val initialLat = savedStateHandle.get<Double>("initial_latitude")
        val initialLng = savedStateHandle.get<Double>("initial_longitude")

        if (initialLat != null && initialLng != null) {
            Timber.tag("MapPicker").d("📍 使用传入的初始位置: ($initialLat, $initialLng)")
            hasInitialLocation = true
            _uiState.update {
                it.copy(
                    latitude = initialLat,
                    longitude = initialLng,
                    hasSelectedLocation = true,
                    isLoadingAddress = true
                )
            }
            // 清除参数，避免下次误用
            savedStateHandle.remove<Double>("initial_latitude")
            savedStateHandle.remove<Double>("initial_longitude")
        }
    }

    /**
     * 获取用户当前位置作为初始位置
     */
    fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                Timber.tag("MapPicker").d("📍 正在获取当前位置...")
                _uiState.update { it.copy(isLoadingAddress = true) }

                val currentLocation = locationService.getCurrentLocation()
                if (currentLocation != null) {
                    Timber.tag("MapPicker").d("✅ 获取当前位置成功: (${currentLocation.latitude}, ${currentLocation.longitude})")
                    _uiState.update {
                        it.copy(
                            latitude = currentLocation.latitude,
                            longitude = currentLocation.longitude,
                            address = currentLocation.address.ifEmpty {
                                "${currentLocation.city}${currentLocation.district}"
                            },
                            isLoadingAddress = false
                        )
                    }
                } else {
                    Timber.tag("MapPicker").w("⚠️ 无法获取当前位置，使用默认位置")
                    _uiState.update { it.copy(isLoadingAddress = false) }
                }
            } catch (e: Exception) {
                Timber.tag("MapPicker").e(e, "❌ 获取当前位置失败")
                _uiState.update {
                    it.copy(
                        isLoadingAddress = false,
                        errorMessage = "获取当前位置失败"
                    )
                }
            }
        }
    }

    /**
     * 初始化地理编码搜索
     */
    fun initGeocodeSearch(context: android.content.Context) {
        try {
            geocodeSearch = GeocodeSearch(context)
            Timber.tag("MapPicker").d("✅ GeocodeSearch 初始化成功")

            // 如果有初始位置，执行逆地理编码
            if (hasInitialLocation) {
                val state = _uiState.value
                performReverseGeocode(state.latitude, state.longitude)
            }
        } catch (e: Exception) {
            Timber.tag("MapPicker").e(e, "❌ GeocodeSearch 初始化失败")
            _uiState.update { it.copy(errorMessage = "地图服务初始化失败") }
        }
    }

    /**
     * 更新选中的位置
     */
    fun updateLocation(latitude: Double, longitude: Double) {
        Timber.tag("MapPicker").d("📍 更新位置: ($latitude, $longitude)")

        _uiState.update {
            it.copy(
                latitude = latitude,
                longitude = longitude,
                hasSelectedLocation = true,
                isLoadingAddress = true,
                errorMessage = null
            )
        }

        // 执行逆地理编码
        performReverseGeocode(latitude, longitude)
    }

    /**
     * 执行逆地理编码（坐标转地址）
     */
    private fun performReverseGeocode(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val address = withContext(Dispatchers.IO) {
                    val search = geocodeSearch
                    if (search == null) {
                        Timber.tag("MapPicker").w("geocodeSearch 未初始化，跳过逆地理编码")
                        return@withContext "获取地址失败"
                    }

                    suspendCoroutine { continuation ->
                        val query = RegeocodeQuery(
                            LatLonPoint(latitude, longitude),
                            200f, // 搜索半径
                            GeocodeSearch.AMAP // 高德坐标系
                        )

                        search.setOnGeocodeSearchListener(
                            object : com.amap.api.services.geocoder.GeocodeSearch.OnGeocodeSearchListener {
                                override fun onRegeocodeSearched(
                                    result: com.amap.api.services.geocoder.RegeocodeResult?,
                                    code: Int
                                ) {
                                    if (code == 1000) {
                                        val regeocodeAddress = result?.regeocodeAddress
                                        val addressStr = regeocodeAddress?.formatAddress ?: "未知地址"
                                        Timber.tag("MapPicker").d("✅ 逆地理编码成功: $addressStr")
                                        continuation.resume(addressStr)
                                    } else {
                                        Timber.tag("MapPicker").e("❌ 逆地理编码失败: code=$code")
                                        continuation.resume("获取地址失败")
                                    }
                                }

                                override fun onGeocodeSearched(
                                    p0: com.amap.api.services.geocoder.GeocodeResult?,
                                    p1: Int
                                ) {
                                    // 不需要处理正向地理编码
                                }
                            }
                        )

                        search.getFromLocationAsyn(query)
                    }
                }

                _uiState.update {
                    it.copy(
                        address = address,
                        isLoadingAddress = false
                    )
                }
            } catch (e: Exception) {
                Timber.tag("MapPicker").e(e, "❌ 逆地理编码异常")
                _uiState.update {
                    it.copy(
                        address = "获取地址失败",
                        isLoadingAddress = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        geocodeSearch = null
        Timber.tag("MapPicker").d("🔄 ViewModel cleared")
    }
}

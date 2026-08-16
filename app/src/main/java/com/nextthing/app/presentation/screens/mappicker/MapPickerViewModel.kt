package com.nextthing.app.presentation.screens.mappicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.poisearch.PoiSearch
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.core.PoiItem
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
    private var appContext: android.content.Context? = null

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
                    isLoadingAddress = true,
                    // 让地图在 MapView 完成创建后也能收到一次明确的镜头更新。
                    moveToken = System.nanoTime()
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
                _uiState.update { it.copy(isLocating = true, addressHint = null) }

                val currentLocation = locationService.getCurrentLocation(forceRefresh = true)
                if (currentLocation != null) {
                    Timber.tag("MapPicker").d("✅ 获取当前位置成功: (${currentLocation.latitude}, ${currentLocation.longitude})")
                    _uiState.update {
                        it.copy(
                            latitude = currentLocation.latitude,
                            longitude = currentLocation.longitude,
                            address = currentLocation.address.ifEmpty {
                                "${currentLocation.city}${currentLocation.district}"
                            },
                            isLoadingAddress = false,
                            isLocating = false,
                            addressHint = null,
                            hasSelectedLocation = true,
                            moveToken = System.nanoTime()
                        )
                    }
                } else {
                    Timber.tag("MapPicker").w("⚠️ 无法获取当前位置，使用默认位置")
                    _uiState.update {
                        it.copy(
                            isLoadingAddress = false,
                            isLocating = false,
                            hasSelectedLocation = false,
                            address = "",
                            addressHint = "未能获取当前位置，请检查系统定位开关和应用定位权限"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.tag("MapPicker").e(e, "❌ 获取当前位置失败")
                _uiState.update {
                    it.copy(
                        isLoadingAddress = false,
                        isLocating = false,
                        hasSelectedLocation = false,
                        address = "",
                        addressHint = "定位请求失败，请稍后重试"
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
            appContext = context.applicationContext
            // 搜索 SDK 的隐私状态必须先于 GeocodeSearch / PoiSearch 初始化。
            com.amap.api.services.core.ServiceSettings.updatePrivacyShow(appContext, true, true)
            com.amap.api.services.core.ServiceSettings.updatePrivacyAgree(appContext, true)
            geocodeSearch = GeocodeSearch(appContext)
            Timber.tag("MapPicker").d("✅ GeocodeSearch 初始化成功")

            // 如果有初始位置，执行逆地理编码
            if (hasInitialLocation) {
                val state = _uiState.value
                performReverseGeocode(state.latitude!!, state.longitude!!)
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
                isLocating = false,
                addressHint = null,
                errorMessage = null
            )
        }

        // 执行逆地理编码
        performReverseGeocode(latitude, longitude)
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, searchResults = emptyList()) }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }

    fun searchPlaces() {
        val keyword = _uiState.value.searchQuery.trim()
        if (keyword.isBlank() || _uiState.value.isSearching) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchResults = emptyList(), errorMessage = null) }
            try {
                val results: List<PlaceSearchResult> = withContext(Dispatchers.IO) {
                    val query = PoiSearch.Query(keyword, "", "")
                    query.pageSize = 6
                    query.pageNum = 0
                    suspendCoroutine<List<PlaceSearchResult>> { continuation ->
                        val context = appContext
                        if (context == null) {
                            continuation.resume(emptyList())
                            return@suspendCoroutine
                        }
                        val search = PoiSearch(context, query)
                        search.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                            override fun onPoiSearched(result: PoiResult?, code: Int) {
                                if (code == 1000) {
                                    continuation.resume(
                                        result?.pois.orEmpty().mapNotNull { poi ->
                                            poi.latLonPoint?.let { point ->
                                                PlaceSearchResult(
                                                    title = poi.title.orEmpty(),
                                                    address = poi.snippet.orEmpty(),
                                                    latitude = point.latitude,
                                                    longitude = point.longitude
                                                )
                                            }
                                        }
                                    )
                                } else {
                                    continuation.resume(emptyList())
                                }
                            }
                            override fun onPoiItemSearched(item: PoiItem?, code: Int) = Unit
                        })
                        search.searchPOIAsyn()
                    }
                }
                _uiState.update { it.copy(isSearching = false, searchResults = results) }
            } catch (e: Exception) {
                Timber.tag("MapPicker").e(e, "POI 搜索失败")
                _uiState.update { it.copy(isSearching = false, errorMessage = "搜索地点失败，请重试") }
            }
        }
    }

    fun selectSearchResult(place: PlaceSearchResult) {
        _uiState.update {
            it.copy(
                latitude = place.latitude,
                longitude = place.longitude,
                address = place.address.ifBlank { place.title },
                hasSelectedLocation = true,
                isLoadingAddress = true,
                isLocating = false,
                searchQuery = place.title,
                searchResults = emptyList(),
                moveToken = System.nanoTime()
            )
        }
        performReverseGeocode(place.latitude, place.longitude)
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
                        return@withContext null
                    }

                    suspendCoroutine<String?> { continuation ->
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
                                        val addressStr = regeocodeAddress?.formatAddress
                                        Timber.tag("MapPicker").d("✅ 逆地理编码成功: $addressStr")
                                        continuation.resume(addressStr)
                                    } else {
                                        Timber.tag("MapPicker").e("❌ 逆地理编码失败: code=$code")
                                        continuation.resume(null)
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
                        address = address ?: it.address,
                        isLoadingAddress = false,
                        addressHint = if (address == null) "未能解析附近地址，仍可确认该坐标" else null
                    )
                }
            } catch (e: Exception) {
                Timber.tag("MapPicker").e(e, "❌ 逆地理编码异常")
                _uiState.update {
                    it.copy(
                        isLoadingAddress = false,
                        addressHint = "未能解析附近地址，仍可确认该坐标",
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

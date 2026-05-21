package com.nextthing.app.data.service

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.nextthing.app.domain.model.LocationInfo
import com.nextthing.app.domain.model.LocationType
import com.nextthing.app.domain.repository.GeofenceConfigRepository
import com.nextthing.app.domain.service.LocationService
import com.nextthing.app.domain.service.LocationServiceStatus
import com.nextthing.app.domain.service.LocationSource
import com.nextthing.app.domain.service.AccuracyLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AmapLocationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fallbackLocationService: LocationServiceImpl, // Google服务作为回退
    private val geofenceConfigRepository: GeofenceConfigRepository // 地理围栏配置
) : LocationService {

    private var amapLocationClient: AMapLocationClient? = null
    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    private var lastLocationUpdateTime: Long = 0
    private var cachedLocation: LocationInfo? = null

    // 位置状态流
    private val _locationUpdates = MutableStateFlow<LocationInfo?>(null)

    // 并发控制：防止同时进行多个定位请求（线程安全）
    private val isLocationInProgress = AtomicBoolean(false)

    // 类级别协程作用域，在 onDestroy 时取消
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    companion object {
        private const val LOCATION_CACHE_DURATION = 3 * 60 * 1000L // 3分钟缓存，更频繁更新
        private const val AMAP_LOCATION_TIMEOUT = 8000L // 高德定位8秒超时，更快响应
        private const val FALLBACK_TIMEOUT = 20000L // Google服务回退超时20秒，给内部多级串联足够时间
    }

    init {
        Timber.d("🏗️ [AmapLocationService] 构造函数被调用，开始初始化...")
        initAmapClient()
    }

        private fun initAmapClient() {
        try {
            Timber.d("🗺️ [AmapLocationService] 🚀 开始初始化高德定位客户端...")
            Timber.d("🗺️ [AmapLocationService] 📍 Context: ${context.javaClass.simpleName}")
            
            // 第一步：设置隐私合规（必须在任何SDK调用之前）
            Timber.d("🔒 [AmapLocationService] 设置隐私合规政策...")
            try {
                // 设置已经向用户展示隐私政策
                AMapLocationClient.updatePrivacyShow(context, true, true)
                Timber.d("✅ [AmapLocationService] updatePrivacyShow 设置成功")
                
                // 设置用户已经同意隐私政策
                AMapLocationClient.updatePrivacyAgree(context, true)
                Timber.d("✅ [AmapLocationService] updatePrivacyAgree 设置成功")
                
                Timber.d("🔒 [AmapLocationService] 隐私合规政策设置完成")
            } catch (e: Exception) {
                Timber.e(e, "❌ [AmapLocationService] 隐私合规设置失败")
                amapLocationClient = null
                return
            }
            
            // 第二步：检查API Key配置
            Timber.d("🗺️ [AmapLocationService] 🔑 检查API Key配置...")
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            val apiKey = applicationInfo.metaData?.getString("com.amap.api.v2.apikey")

            if (apiKey.isNullOrBlank()) {
                Timber.e("❌ [AmapLocationService] 🔑 API Key未配置或为空")
                amapLocationClient = null
                return
            } else {
                Timber.d("✅ [AmapLocationService] 🔑 API Key已配置: ${apiKey.take(8)}...")
            }

            // 第三步：创建定位客户端
            Timber.d("🗺️ [AmapLocationService] 🚀 创建AMapLocationClient...")
            amapLocationClient = AMapLocationClient(context)
            Timber.d("✅ [AmapLocationService] 🎉 高德定位客户端初始化成功")
            Timber.d("🗺️ [AmapLocationService] 📱 SDK已集成")
        } catch (e: Exception) {
            Timber.e(e, "❌ [AmapLocationService] 💥 高德定位客户端初始化失败")
            Timber.e("❌ [AmapLocationService] 🔍 错误类型: ${e.javaClass.simpleName}")
            Timber.e("❌ [AmapLocationService] 📝 错误信息: ${e.message}")
            amapLocationClient = null
        }
    }

    override suspend fun getCurrentLocation(forceRefresh: Boolean): LocationInfo? {
        val startTime = System.currentTimeMillis()
        Timber.d("============================================================")
        Timber.d("🔍 [AmapLocationService] 🚀 开始获取位置流程")
        Timber.d("🔍 [AmapLocationService] 📋 参数 - 强制刷新: $forceRefresh")
        Timber.d("🔍 [AmapLocationService] ⏰ 开始时间: ${java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())}")
        
        // 并发控制检查
        if (isLocationInProgress.get() && !forceRefresh) {
            Timber.d("⏳ [AmapLocationService] 🔄 已有定位请求进行中，跳过重复请求")
            cachedLocation?.let {
                Timber.d("⏳ [AmapLocationService] 📦 返回现有缓存位置: ${it.locationName}")
                return it
            }
            Timber.d("⏳ [AmapLocationService] ❌ 无缓存，等待当前请求完成")
            return null
        }
        
        // 检查权限
        Timber.d("🔐 [AmapLocationService] 🔍 开始检查位置权限...")
        if (!hasLocationPermission()) {
            Timber.w("❌ [AmapLocationService] 🚫 位置权限未授予")
            return null
        }
        Timber.d("✅ [AmapLocationService] 🔓 位置权限检查通过")
        
        // 检查缓存
        Timber.d("📦 [AmapLocationService] 🔍 检查缓存位置...")
        Timber.d("📦 [AmapLocationService] 📊 缓存状态 - 需要刷新: ${shouldRefreshLocation()}")
        if (!forceRefresh && !shouldRefreshLocation()) {
            cachedLocation?.let {
                val cacheAge = (System.currentTimeMillis() - lastLocationUpdateTime) / 1000
                Timber.d("📦 [AmapLocationService] ✅ 使用缓存位置: ${it.locationName}")
                Timber.d("📦 [AmapLocationService] ⏰ 缓存年龄: ${cacheAge}秒")
                return it
            }
        }
        Timber.d("📦 [AmapLocationService] ⏭️ 缓存无效或强制刷新，继续实时定位")
        
        // 尝试快速获取最后已知位置作为临时结果
        if (!forceRefresh) {
            Timber.d("⚡ [AmapLocationService] 🔍 尝试快速定位策略...")
            tryGetQuickLocation()?.let { quickLocation ->
                val quickTime = System.currentTimeMillis() - startTime
                Timber.d("⚡ [AmapLocationService] ✅ 快速定位成功！耗时: ${quickTime}ms")
                Timber.d("⚡ [AmapLocationService] 📍 快速位置: ${quickLocation.locationName}")
                Timber.d("⚡ [AmapLocationService] 🔄 后台启动精确定位...")
                updateLocationCache(quickLocation)
                // 异步在后台获取更精确位置
                serviceScope.launch {
                    try {
                        Timber.d("🎯 [AmapLocationService] 🚀 后台精确定位开始...")
                        val betterResult = getAmapLocation()
                        if (betterResult.isSuccess) {
                            val betterLocation = betterResult.getOrNull()!!
                            updateLocationCache(betterLocation)
                            Timber.d("🎯 [AmapLocationService] ✅ 后台获取到更精确位置: ${betterLocation.locationName}")
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "🎯 [AmapLocationService] ⚠️ 后台精确定位失败")
                    }
                }
                return quickLocation
            }
            Timber.d("⚡ [AmapLocationService] ❌ 快速定位无结果，进入正常定位流程")
        }

        // 优先尝试高德定位
        if (amapLocationClient != null) {
            Timber.d("🗺️ [AmapLocationService] 🎯 开始高德定位尝试...")
            Timber.d("🗺️ [AmapLocationService] 📱 客户端状态: 已初始化")
            
            // 设置定位进行中状态
            isLocationInProgress.set(true)
            
            try {
                val amapStartTime = System.currentTimeMillis()
                val amapResult = getAmapLocation()
                val amapDuration = System.currentTimeMillis() - amapStartTime
                
                if (amapResult.isSuccess) {
                    val locationInfo = amapResult.getOrNull()!!
                    val totalTime = System.currentTimeMillis() - startTime
                    Timber.d("🗺️ [AmapLocationService] ✅ 高德定位成功！")
                    Timber.d("🗺️ [AmapLocationService] ⏱️ 高德耗时: ${amapDuration}ms, 总耗时: ${totalTime}ms")
                    Timber.d("🗺️ [AmapLocationService] 📍 位置结果: ${locationInfo.locationName}")
                    updateLocationCache(locationInfo)
                    
                    // 重置定位状态
                    isLocationInProgress.set(false)
                    
                    Timber.d("============================================================")
                    return locationInfo
                } else {
                    Timber.w("⚠️ [AmapLocationService] 💔 高德定位失败")
                    Timber.w("⚠️ [AmapLocationService] ⏱️ 尝试耗时: ${amapDuration}ms")
                    Timber.w("⚠️ [AmapLocationService] 📝 失败原因: ${amapResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "💥 [AmapLocationService] 🔥 高德定位异常")
            } finally {
                // 确保无论如何都重置状态
                isLocationInProgress.set(false)
            }
        } else {
            Timber.w("🗺️ [AmapLocationService] ⚠️ 高德客户端未初始化，跳过高德定位")
        }

        // 快速回退到Google服务
        Timber.d("🔄 [AmapLocationService] 🔀 开始回退到Google定位服务...")
        Timber.d("🔄 [AmapLocationService] ⏱️ Google服务超时限制: ${FALLBACK_TIMEOUT/1000}秒")
        val fallbackStartTime = System.currentTimeMillis()
        
        val fallbackResult = withTimeoutOrNull(FALLBACK_TIMEOUT) {
            fallbackLocationService.getCurrentLocation(forceRefresh)
        }
        
        val fallbackDuration = System.currentTimeMillis() - fallbackStartTime
        val totalTime = System.currentTimeMillis() - startTime
        
        // 重置状态
        isLocationInProgress.set(false)

        return if (fallbackResult != null) {
            Timber.d("🔄 [AmapLocationService] ✅ Google服务定位成功！")
            Timber.d("🔄 [AmapLocationService] ⏱️ Google耗时: ${fallbackDuration}ms, 总耗时: ${totalTime}ms")
            Timber.d("🔄 [AmapLocationService] 📍 位置结果: ${fallbackResult?.locationName}")
            Timber.d("============================================================")
            fallbackResult
        } else {
            Timber.e("🔄 [AmapLocationService] ⏰ Google服务超时")
            Timber.e("🔄 [AmapLocationService] ⏱️ 超时耗时: ${fallbackDuration}ms, 总耗时: ${totalTime}ms")
            Timber.e("🔄 [AmapLocationService] 💀 所有定位方案均失败")
            Timber.d("============================================================")
            null
        }
    }

    private suspend fun getAmapLocation(): Result<LocationInfo> = withContext(Dispatchers.IO) {
        try {
            val locationClient = amapLocationClient ?: return@withContext Result.failure(
                IllegalStateException("高德定位客户端未初始化")
            )

            Timber.d("🚀 [AmapLocationService] 启动极速定位模式...")
            Timber.d("🚀 [AmapLocationService] 🛠️ 配置定位参数...")

            // 读取地理围栏配置
            val geofenceConfig = try {
                geofenceConfigRepository.getConfigOrDefault()
            } catch (e: Exception) {
                Timber.w(e, "⚠️ [AmapLocationService] 读取地理围栏配置失败，使用默认配置")
                null
            }

            // 始终用省电模式（WiFi + 基站），速度最快，1-2s 内出结果
            // 高精度模式需等 GPS 冷启动，耗时 5s+，对本应用不必要
            val locationMode = AMapLocationClientOption.AMapLocationMode.Battery_Saving
            Timber.d("🔋 [AmapLocationService] 定位模式: 省电模式（WiFi+基站，速度优先）")

            // 配置高德定位参数
            val option = AMapLocationClientOption().apply {
                this.locationMode = locationMode
                isOnceLocationLatest = false // 返回第一个可用结果，不等最优（关键：避免 5s+ 延迟）
                isOnceLocation = true        // 单次定位
                isWifiScan = true            // 被动 WiFi 扫描（室内定位）
                isWifiActiveScan = false     // 关闭主动 WiFi 扫描（减少延迟）
                isNeedAddress = true         // 返回地址文字（需要网络逆地理编码）
                httpTimeOut = 6000L          // 网络超时 6s
                isLocationCacheEnable = true // 允许 SDK 内部缓存（重启后快速返回）
                isMockEnable = false
                isSensorEnable = false       // 关闭传感器融合（省电模式下无意义，还增加延迟）
                locationPurpose = AMapLocationClientOption.AMapLocationPurpose.Sport
            }
            locationClient.setLocationOption(option)
            Timber.d("🚀 [AmapLocationService] ✅ 定位参数配置完成（省电模式，超时 6s）")

            // 使用超时控制的定位获取
            val location = withTimeoutOrNull(AMAP_LOCATION_TIMEOUT) {
                suspendCancellableCoroutine<AMapLocation?> { continuation ->
                    val locationListener = object : AMapLocationListener {
                        override fun onLocationChanged(location: AMapLocation?) {
                            Timber.d("📍 [AmapLocationService] 📱 收到定位回调")
                            if (location != null) {
                                Timber.d("📍 [AmapLocationService] 📊 定位结果详情:")
                                Timber.d("📍 [AmapLocationService] - 错误码: ${location.errorCode}")
                                Timber.d("📍 [AmapLocationService] - 错误信息: ${location.errorInfo}")
                                if (location.errorCode == 0) {
                                    Timber.d("📍 [AmapLocationService] - 经纬度: (${location.latitude}, ${location.longitude})")
                                    Timber.d("📍 [AmapLocationService] - 精度: ${location.accuracy}m")
                                    Timber.d("📍 [AmapLocationService] - 定位类型: ${location.locationType}")
                                    Timber.d("📍 [AmapLocationService] - 地址: ${location.address}")
                                }
                            } else {
                                Timber.w("📍 [AmapLocationService] ⚠️ 收到空定位结果")
                            }
                            locationClient.stopLocation()
                            locationClient.setLocationListener(null)
                            continuation.resume(location)
                        }
                    }

                    locationClient.setLocationListener(locationListener)
                    Timber.d("🔍 [AmapLocationService] 开始高德定位请求...")
                    locationClient.startLocation()

                    continuation.invokeOnCancellation {
                        Timber.d("🛑 [AmapLocationService] 定位请求被取消")
                        locationClient.stopLocation()
                        locationClient.setLocationListener(null)
                    }
                }
            }

            if (location != null && location.errorCode == 0) {
                Timber.d("✅ [AmapLocationService] 高德定位成功")
                Timber.d("   - 经纬度: (${location.latitude}, ${location.longitude})")
                Timber.d("   - 精度: ${location.accuracy}m")
                Timber.d("   - 定位类型: ${location.locationType} (${getLocationTypeDesc(location.locationType)})")
                Timber.d("   - 地址: ${location.address}")
                
                val locationInfo = convertAmapLocationToLocationInfo(location)
                Result.success(locationInfo)
            } else {
                val errorMsg = if (location != null) {
                    when (location.errorCode) {
                        12 -> "网络连接异常或无Wi-Fi热点，建议开启Wi-Fi"
                        13 -> "定位失败，请检查网络连接"
                        14 -> "GPS关闭，无法进行室外定位"
                        else -> "高德定位失败，错误码: ${location.errorCode}, 错误信息: ${location.errorInfo}"
                    }
                } else {
                    "高德定位超时(${AMAP_LOCATION_TIMEOUT/1000}秒)，可能网络较差"
                }
                Timber.w("⚠️ [AmapLocationService] $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Timber.e(e, "💥 [AmapLocationService] 高德定位异常")
            Result.failure(e)
        }
    }

    private suspend fun convertAmapLocationToLocationInfo(location: AMapLocation): LocationInfo = withContext(Dispatchers.IO) {
        val locationName = buildString {
            // 优先从区县开始显示，不包含省市信息
            if (!location.district.isNullOrBlank()) {
                append(location.district)
                if (!location.street.isNullOrBlank()) {
                    append(location.street)
                }
            } else if (!location.address.isNullOrBlank()) {
                // 如果没有区县信息，尝试从完整地址中提取区县部分
                val address = location.address
                // 查找区县关键字的位置，从区县开始截取
                val districtKeywords = listOf("区", "县", "市", "旗", "镇")
                var startIndex = -1
                for (keyword in districtKeywords) {
                    val index = address.indexOf(keyword)
                    if (index > 0) {
                        // 找到区县关键字，从其前面的字符开始截取
                        val possibleStart = maxOf(0, index - 10) // 往前取最多10个字符
                        val beforeKeyword = address.substring(possibleStart, index + 1)
                        // 查找省市分隔符，从区县开始
                        val separators = listOf("省", "市", "自治区")
                        var actualStart = possibleStart
                        for (separator in separators) {
                            val sepIndex = beforeKeyword.lastIndexOf(separator)
                            if (sepIndex >= 0) {
                                actualStart = possibleStart + sepIndex + 1
                                break
                            }
                        }
                        startIndex = actualStart
                        break
                    }
                }

                if (startIndex >= 0) {
                    append(address.substring(startIndex))
                } else {
                    // 没找到区县关键字，使用完整地址
                    append(address)
                }
            } else {
                // 完全没有地址信息时的备用方案
                if (!location.city.isNullOrBlank()) {
                    append(location.city)
                }
            }
        }.takeIf { it.isNotBlank() } ?: "未知位置"

        return@withContext LocationInfo(
            latitude = location.latitude,
            longitude = location.longitude,
            locationName = locationName,
                         locationType = when (location.locationType) {
                 1 -> LocationType.AUTO        // GPS定位
                 2 -> LocationType.AUTO        // 网络定位
                 4 -> LocationType.AUTO        // 缓存定位，归类为网络
                 5 -> LocationType.AUTO        // Wi-Fi定位
                 6 -> LocationType.AUTO        // 手机基站定位
                 8 -> LocationType.AUTO        // 离线定位
                 else -> LocationType.AUTO
             },
             accuracy = location.accuracy,
            city = location.city ?: "",
            district = location.district ?: "",
            province = location.province ?: "",
                         country = location.country ?: "中国",
             addedAt = LocalDateTime.now(),
             updatedAt = LocalDateTime.now(),
             isCurrentLocation = true
        )
    }

    override fun observeLocationUpdates(): Flow<LocationInfo> {
        return _locationUpdates.asStateFlow().filterNotNull()
    }

    override fun shouldRefreshLocation(): Boolean {
        return System.currentTimeMillis() - lastLocationUpdateTime > LOCATION_CACHE_DURATION
    }

    override suspend fun getCachedLocation(): LocationInfo? {
        return if (shouldRefreshLocation()) null else cachedLocation
    }

    override fun stopLocationUpdates() {
        // 停止高德定位监听
        amapLocationClient?.stopLocation()
        Timber.d("🗺️ [AmapLocationService] 停止位置监听")
    }

    fun clearLocationCache() {
        cachedLocation = null
        lastLocationUpdateTime = 0
        _locationUpdates.value = null
    }

    override suspend fun hasLocationPermission(): Boolean {
        // 粗略位置权限足以支持WiFi+基站定位，不需要同时拥有精确位置权限
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun isLocationEnabled(): Boolean {
        // 委托给Google服务检查
        return fallbackLocationService.isLocationEnabled()
    }

    override suspend fun isServiceAvailable(): Boolean {
        return try {
            // 1. 检查权限
            if (!hasLocationPermission()) {
                Timber.d("🔍 [AmapLocationService] 服务不可用: 缺少位置权限")
                return false
            }

            // 2. 检查位置服务是否启用
            if (!isLocationEnabled()) {
                Timber.d("🔍 [AmapLocationService] 服务不可用: 位置服务未启用")
                return false
            }

            // 3. 检查高德地图客户端是否初始化
            if (amapLocationClient == null) {
                Timber.d("🔍 [AmapLocationService] 服务降级: 高德地图未初始化，使用Google服务")
                return true // 返回true因为有Google服务兜底
            }

            Timber.d("✅ [AmapLocationService] 服务可用")
            true
        } catch (e: Exception) {
            Timber.e(e, "❌ [AmapLocationService] 检查服务可用性异常")
            false
        }
    }

    override suspend fun getServiceStatus():  LocationServiceStatus {
        return try {
            val hasPermission = hasLocationPermission()
            val isEnabled = isLocationEnabled()
            val amapInitialized = amapLocationClient != null

            LocationServiceStatus(
                isAvailable = hasPermission && isEnabled,
                amapInitialized = amapInitialized,
                hasPermission = hasPermission,
                isLocationEnabled = isEnabled,
                lastErrorMessage = when {
                    !hasPermission -> "缺少位置权限"
                    !isEnabled -> "位置服务未启用"
                    !amapInitialized -> "高德地图服务未初始化，使用Google服务"
                    else -> null
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "❌ [AmapLocationService] 获取服务状态异常")
            LocationServiceStatus(
                isAvailable = false,
                amapInitialized = false,
                hasPermission = false,
                isLocationEnabled = false,
                lastErrorMessage = "服务状态检查异常: ${e.message}"
            )
        }
    }

    override fun getLocationSource(locationType: Int): LocationSource {
        return when (locationType) {
            1 -> LocationSource.GPS        // GPS定位
            2 -> LocationSource.NETWORK    // 网络定位
            4 -> LocationSource.CACHE      // 缓存定位
            5 -> LocationSource.WIFI       // Wi-Fi定位
            6 -> LocationSource.CELL       // 基站定位
            8 -> LocationSource.CACHE      // 离线定位（归类为缓存）
            else -> LocationSource.UNKNOWN
        }
    }

    override fun getAccuracyLevel(accuracy: Float?): AccuracyLevel {
        return when {
            accuracy == null -> AccuracyLevel.UNAVAILABLE
            accuracy < 10f -> AccuracyLevel.EXCELLENT
            accuracy < 50f -> AccuracyLevel.GOOD
            accuracy < 100f -> AccuracyLevel.FAIR
            accuracy < 500f -> AccuracyLevel.POOR
            else -> AccuracyLevel.UNAVAILABLE
        }
    }

    private fun updateLocationCache(locationInfo: LocationInfo) {
        cachedLocation = locationInfo
        lastLocationUpdateTime = System.currentTimeMillis()
        _locationUpdates.value = locationInfo
        Timber.d("💾 [AmapLocationService] 更新位置缓存: ${locationInfo.locationName}")
    }

    /**
     * 获取定位类型描述
     */
    private fun getLocationTypeDesc(locationType: Int): String {
        return when (locationType) {
            1 -> "GPS定位"
            2 -> "网络定位"
            4 -> "缓存定位"
            5 -> "Wi-Fi定位"
            6 -> "基站定位"
            8 -> "离线定位"
            else -> "未知定位类型"
        }
    }

    /**
     * 尝试快速获取位置（使用最后已知位置）
     */
    private suspend fun tryGetQuickLocation(): LocationInfo? = withContext(Dispatchers.IO) {
        try {
            Timber.d("⚡ [AmapLocationService] 🔍 查找Google服务缓存...")
            val fallbackResult = fallbackLocationService.getCachedLocation()
            if (fallbackResult != null) {
                Timber.d("⚡ [AmapLocationService] ✅ 找到Google服务缓存位置")
                Timber.d("⚡ [AmapLocationService] 📍 缓存位置: ${fallbackResult.locationName}")
                return@withContext fallbackResult
            }
            
            Timber.d("⚡ [AmapLocationService] ❌ 无可用缓存位置")
            return@withContext null
        } catch (e: Exception) {
            Timber.w(e, "⚡ [AmapLocationService] 💥 快速定位异常")
            null
        }
    }

    fun onDestroy() {
        try {
            serviceScope.cancel()
            amapLocationClient?.stopLocation()
            amapLocationClient?.onDestroy()
            amapLocationClient = null
            Timber.d("🗺️ [AmapLocationService] 高德定位客户端已销毁")
        } catch (e: Exception) {
            Timber.e(e, "销毁高德定位客户端时出错")
        }
    }
} 
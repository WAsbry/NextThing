package com.example.nextthingb1.data.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.example.nextthingb1.domain.model.LocationInfo
import com.example.nextthingb1.domain.model.LocationType
import com.example.nextthingb1.domain.service.LocationService
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class LocationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationService {

    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())
    
    private var lastLocationUpdateTime: Long = 0
    private var cachedLocation: LocationInfo? = null
    
    companion object {
        private const val LOCATION_UPDATE_INTERVAL = 5 * 60 * 1000L // 5分钟
        private const val FASTEST_UPDATE_INTERVAL = 1000L // 改为1秒，加快响应
        private const val LOCATION_CACHE_DURATION = 5 * 60 * 1000L // 5分钟缓存
        private const val NETWORK_LOCATION_TIMEOUT = 15000L // 网络定位15秒超时（延长）
        private const val GPS_LOCATION_TIMEOUT = 20000L // GPS定位20秒超时（缩短）
    }

    // 网络定位配置（优先级：高精度确保网络定位生效）
    private val networkLocationRequest: LocationRequest by lazy {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, FASTEST_UPDATE_INTERVAL)
            .setWaitForAccurateLocation(false) // 不等待高精度，接受网络位置
            .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
            .setMaxUpdateDelayMillis(NETWORK_LOCATION_TIMEOUT)
            .setMaxUpdates(1) // 只要一次成功的更新
            .build()
    }
    
    // 粗略网络定位配置（备用方案，只使用网络）
    private val coarseNetworkRequest: LocationRequest by lazy {
        LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, FASTEST_UPDATE_INTERVAL)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
            .setMaxUpdateDelayMillis(8000L) // 8秒快速超时
            .setMaxUpdates(1)
            .build()
    }

    // GPS高精度定位配置
    private val gpsLocationRequest: LocationRequest by lazy {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, FASTEST_UPDATE_INTERVAL)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
            .setMaxUpdateDelayMillis(GPS_LOCATION_TIMEOUT)
            .build()
    }

    override suspend fun getCurrentLocation(forceRefresh: Boolean): LocationInfo? {
        return try {
            Timber.d("🔍 [LocationService] 开始获取位置，强制刷新: $forceRefresh")

            // 检查权限
            if (!hasLocationPermission()) {
                Timber.w("❌ [LocationService] 位置权限未授予")
                return null
            }
            Timber.d("✅ [LocationService] 位置权限检查通过")

            // 检查位置服务
            if (!isLocationEnabled()) {
                Timber.w("❌ [LocationService] 位置服务未启用")
                return null
            }
            Timber.d("✅ [LocationService] 位置服务检查通过")

            // 如果不强制刷新且有缓存，检查缓存是否有效
            if (!forceRefresh && !shouldRefreshLocation()) {
                cachedLocation?.let {
                    Timber.d("✅ [LocationService] 使用缓存位置: ${it.locationName}")
                    return it
                }
            }
            
            Timber.d("🔄 [LocationService] 开始获取实时位置...")

            // 第0步：尝试粗略网络定位（最快，室内友好）
            Timber.d("📶 [LocationService] 第0步：尝试粗略网络定位（8秒超时）")
            val coarseLocation = getLocationByType(
                locationRequest = coarseNetworkRequest,
                timeout = 8000L,
                locationType = "粗略网络定位"
            )
            
            if (coarseLocation != null) {
                Timber.d("✅ [LocationService] 粗略网络定位成功，精度: ${coarseLocation.accuracy}m")
                val locationInfo = convertToLocationInfo(coarseLocation)
                updateLocationCache(locationInfo)
                return locationInfo
            } else {
                Timber.w("⚠️ [LocationService] 粗略网络定位失败，尝试高精度网络定位")
            }

            // 第一步：尝试网络定位（快速但可能精度较低）
            Timber.d("📡 [LocationService] 第一步：尝试网络定位（${NETWORK_LOCATION_TIMEOUT/1000}秒超时）")
            val networkLocation = getLocationByType(
                locationRequest = networkLocationRequest,
                timeout = NETWORK_LOCATION_TIMEOUT,
                locationType = "网络定位"
            )
            
            if (networkLocation != null && networkLocation.accuracy <= 100f) {
                Timber.d("✅ [LocationService] 网络定位成功，精度: ${networkLocation.accuracy}m")
                val locationInfo = convertToLocationInfo(networkLocation)
                updateLocationCache(locationInfo)
                return locationInfo
            } else {
                Timber.w("⚠️ [LocationService] 网络定位失败或精度不够")
                // 先快速尝试最后已知位置作为备选
                Timber.d("📍 [LocationService] 快速尝试最后已知位置...")
                try {
                    val locationInfo = getLastKnownLocationDirect()
                    if (locationInfo != null) {
                        Timber.d("✅ [LocationService] 最后已知位置可用，跳过GPS直接使用")
                        updateLocationCache(locationInfo)
                        return locationInfo
                    }
                } catch (e: Exception) {
                    Timber.w(e, "最后已知位置获取失败，继续GPS定位")
                }
                Timber.d("继续尝试GPS定位...")
            }

            // 第二步：网络定位失败或精度不够，使用GPS高精度定位
            Timber.d("🛰️ [LocationService] 第二步：尝试GPS高精度定位（${GPS_LOCATION_TIMEOUT/1000}秒超时）")
            val gpsLocation = getLocationByType(
                locationRequest = gpsLocationRequest,
                timeout = GPS_LOCATION_TIMEOUT,
                locationType = "GPS定位"
            )

            if (gpsLocation != null) {
                Timber.d("✅ [LocationService] GPS定位成功，精度: ${gpsLocation.accuracy}m")
                val locationInfo = convertToLocationInfo(gpsLocation)
                updateLocationCache(locationInfo)
                return locationInfo
            } else {
                Timber.e("❌ [LocationService] GPS定位也失败了")

                // 第三步：如果都失败了，尝试获取最后已知位置
                Timber.d("📍 [LocationService] 第三步：尝试获取最后已知位置")
                return getLastKnownLocationDirect()
            }

        } catch (e: Exception) {
            Timber.e(e, "💥 [LocationService] 获取位置异常")
            null
        }
    }

    /**
     * 根据类型获取位置（网络定位或GPS定位）
     */
    private suspend fun getLocationByType(
        locationRequest: LocationRequest,
        timeout: Long,
        locationType: String
    ): android.location.Location? = withTimeoutOrNull(timeout) {
        Timber.d("⏱️ [LocationService] $locationType 开始，超时时间: ${timeout/1000}秒")
        
        suspendCancellableCoroutine { continuation ->
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    Timber.d("📍 [LocationService] $locationType 收到位置结果")
                    fusedLocationClient.removeLocationUpdates(this)
                    
                    locationResult.lastLocation?.let { location ->
                        Timber.d("✅ [LocationService] $locationType 成功: 经度=${location.longitude}, 纬度=${location.latitude}, 精度=${location.accuracy}m")
                        continuation.resume(location)
                    } ?: run {
                        Timber.w("⚠️ [LocationService] $locationType 返回空位置")
                        continuation.resume(null)
                    }
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    if (!availability.isLocationAvailable) {
                        Timber.w("❌ [LocationService] $locationType 不可用")
                        fusedLocationClient.removeLocationUpdates(this)
                        continuation.resume(null)
                    }
                }
            }

            try {
                Timber.d("🚀 [LocationService] 发起$locationType 请求")
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                Timber.e(e, "❌ [LocationService] $locationType 权限错误")
                continuation.resumeWithException(e)
            }

            continuation.invokeOnCancellation {
                Timber.d("🛑 [LocationService] $locationType 被取消，移除回调")
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }
    } ?: run {
        Timber.w("⏰ [LocationService] $locationType 超时")
        null
    }

    /**
     * 获取最后已知位置（作为兜底方案）
     */
    private suspend fun getLastKnownLocationDirect(): LocationInfo? = withContext(Dispatchers.IO) {
        try {
            Timber.d("📱 [LocationService] 尝试获取最后已知位置")

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@withContext null
            }

            val lastLocation = fusedLocationClient.lastLocation
            val location = suspendCancellableCoroutine<android.location.Location?> { continuation ->
                lastLocation.addOnSuccessListener { location ->
                    continuation.resume(location)
                }.addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
            }

            if (location != null) {
                Timber.d("✅ [LocationService] 最后已知位置获取成功，精度: ${location.accuracy}m")
                val locationInfo = convertToLocationInfo(location)
                updateLocationCache(locationInfo)
                locationInfo
            } else {
                Timber.e("❌ [LocationService] 最后已知位置也为空")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "💥 [LocationService] 获取最后已知位置异常")
            null
        }
    }

    override suspend fun hasLocationPermission(): Boolean {
        val fineLocationGranted = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseLocationGranted = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val result = fineLocationGranted && coarseLocationGranted
        Timber.d("🔐 [LocationService] 权限检查: 精确位置=$fineLocationGranted, 粗略位置=$coarseLocationGranted, 结果=$result")
        return result
    }

    override suspend fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        
        val result = gpsEnabled || networkEnabled
        Timber.d("🛰️ [LocationService] 位置服务检查: GPS=$gpsEnabled, 网络=$networkEnabled, 结果=$result")
        return result
    }

    override fun observeLocationUpdates(): Flow<LocationInfo> = callbackFlow {
        Timber.d("👁️ [LocationService] 开始监听位置更新")
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Timber.d("📍 [LocationService] 位置更新: ${location.latitude}, ${location.longitude}")
                    val locationInfo = convertToLocationInfo(location)
                    updateLocationCache(locationInfo)
                    trySend(locationInfo)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                networkLocationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Timber.e(e, "❌ [LocationService] 位置更新权限错误")
            close(e)
        }

        awaitClose {
            Timber.d("🛑 [LocationService] 停止位置更新监听")
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun stopLocationUpdates() {
        Timber.d("🛑 [LocationService] 手动停止位置更新")
        // 位置更新会在Flow取消时自动停止
    }

    override fun shouldRefreshLocation(): Boolean {
        val shouldRefresh = System.currentTimeMillis() - lastLocationUpdateTime > LOCATION_CACHE_DURATION
        Timber.d("⏰ [LocationService] 缓存检查: 上次更新=${lastLocationUpdateTime}, 当前=${System.currentTimeMillis()}, 需要刷新=$shouldRefresh")
        return shouldRefresh
    }

    override suspend fun getCachedLocation(): LocationInfo? {
        val cached = if (shouldRefreshLocation()) null else cachedLocation
        Timber.d("📦 [LocationService] 缓存位置: ${cached?.locationName ?: "无缓存"}")
        return cached
    }

    private fun convertToLocationInfo(location: android.location.Location): LocationInfo {
        Timber.d("🔄 [LocationService] 开始地址解析...")
        
        var locationName = "获取地址中..."
        var address = ""
        var city = ""
        var district = ""
        var province = ""
        var subLocality = ""
        var thoroughfare = "" // 街道
        var subThoroughfare = "" // 门牌号

        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                
                Timber.d("🏠 [LocationService] 地址解析成功:")
                Timber.d("  - 国家: ${addr.countryName}")
                Timber.d("  - 省份: ${addr.adminArea}")
                Timber.d("  - 城市: ${addr.locality}")
                Timber.d("  - 区县: ${addr.subLocality}")
                Timber.d("  - 街道: ${addr.thoroughfare}")
                
                // 获取详细地址信息
                address = addr.getAddressLine(0) ?: ""
                province = addr.adminArea ?: "" // 省份
                city = addr.locality ?: addr.subAdminArea ?: "" // 城市
                district = addr.subLocality ?: "" // 区县
                subLocality = addr.thoroughfare ?: "" // 街道/路
                thoroughfare = addr.subThoroughfare ?: "" // 门牌号
                
                // 获取更多详细信息
                val countryName = addr.countryName ?: ""
                val postalCode = addr.postalCode ?: ""
                val featureName = addr.featureName ?: ""
                val premises = addr.premises ?: ""
                
                // 构建精细的位置名称，参考手机天气预报格式
                locationName = when {
                    // 优先显示详细地址
                    district.isNotBlank() && subLocality.isNotBlank() -> {
                        "$district$subLocality"
                    }
                    // 其次显示区县
                    district.isNotBlank() -> district
                    // 再次显示城市
                    city.isNotBlank() -> city
                    // 最后显示省份
                    province.isNotBlank() -> province
                    // 兜底显示完整地址
                    address.isNotBlank() -> address
                    // 无法解析时显示坐标
                    else -> "位置(${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)})"
                }
                
                Timber.d("✅ [LocationService] 地址解析完成: $locationName")
            } else {
                Timber.w("⚠️ [LocationService] 地址解析返回空结果")
                locationName = "位置(${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)})"
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ [LocationService] 地址解析异常")
            locationName = "位置(${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)})"
        }

        return LocationInfo(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            altitude = if (location.hasAltitude()) location.altitude else null,
            locationName = locationName,
            address = address,
            city = city,
            district = district,
            province = province,
            country = "中国",
            addedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            isCurrentLocation = true,
            locationType = LocationType.AUTO // 自动获取的位置
        )
    }

    private fun updateLocationCache(locationInfo: LocationInfo) {
        Timber.d("💾 [LocationService] 更新位置缓存: ${locationInfo.locationName}")
        cachedLocation = locationInfo
        lastLocationUpdateTime = System.currentTimeMillis()
    }
} 
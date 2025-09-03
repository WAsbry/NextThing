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
import timber.log.Timber
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

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
        private const val FASTEST_UPDATE_INTERVAL = 2 * 60 * 1000L // 2分钟
        private const val LOCATION_CACHE_DURATION = 5 * 60 * 1000L // 5分钟缓存
        private const val LOCATION_TIMEOUT = 30000L // 30秒超时，给GPS更多时间
    }

    override suspend fun getCurrentLocation(forceRefresh: Boolean): Result<LocationInfo> {
        return try {
            Timber.d("开始获取位置，强制刷新: $forceRefresh")
            
            // 检查权限
            if (!hasLocationPermission()) {
                Timber.w("位置权限未授予")
                return Result.failure(SecurityException("位置权限未授予"))
            }

            // 检查位置服务
            if (!isLocationEnabled()) {
                Timber.w("位置服务未启用")
                return Result.failure(IllegalStateException("位置服务未启用"))
            }

            // 如果不强制刷新且有缓存，检查缓存是否有效
            if (!forceRefresh && !shouldRefreshLocation()) {
                cachedLocation?.let { 
                    Timber.d("使用缓存位置: ${it.locationName}")
                    return Result.success(it)
                }
            }
            
            Timber.d("开始获取实时位置...")

            // 第一步：尝试快速网络定位
            val quickLocation = getQuickLocation()
            if (quickLocation != null && quickLocation.accuracy <= 100f) {
                Timber.d("快速定位成功，精度: ${quickLocation.accuracy}m")
                val locationInfo = convertToLocationInfo(quickLocation)
                cachedLocation = locationInfo
                lastLocationUpdateTime = System.currentTimeMillis()
                return Result.success(locationInfo)
            }

            // 第二步：如果快速定位失败或精度不够，使用高精度GPS
            Timber.d("快速定位不理想，尝试高精度GPS定位")
            val location = withTimeoutOrNull(LOCATION_TIMEOUT) { // 30秒超时
                suspendCancellableCoroutine<android.location.Location?> { continuation ->
                    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                        .setMaxUpdates(3) // 最多获取3次位置更新
                        .setMinUpdateIntervalMillis(1000) // 最小更新间隔1秒
                        .setMaxUpdateDelayMillis(10000) // 10秒最大延迟
                        .setMinUpdateDistanceMeters(5f) // 最小移动距离5米
                        .setWaitForAccurateLocation(false) // 不等待精确位置，避免无限等待
                        .build()

                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            val location = result.lastLocation
                            Timber.d("收到位置回调: lat=${location?.latitude}, lng=${location?.longitude}, 精度=${location?.accuracy}")
                            
                            // 简化精度策略：接受任何有效位置，优先高精度
                            if (location != null) {
                                if (location.accuracy <= 100f) {
                                    // 高精度位置（100米以内）
                                    Timber.d("获取到高精度位置: ${location.accuracy}m")
                                    fusedLocationClient.removeLocationUpdates(this)
                                    if (continuation.isActive) {
                                        continuation.resume(location)
                                    }
                                } else if (location.accuracy <= 500f) {
                                    // 中等精度位置（500米以内）
                                    Timber.d("获取到中等精度位置: ${location.accuracy}m")
                                    fusedLocationClient.removeLocationUpdates(this)
                                    if (continuation.isActive) {
                                        continuation.resume(location)
                                    }
                                } else {
                                    // 低精度位置，但接受使用
                                    Timber.w("位置精度较低: ${location.accuracy}m，但仍可使用")
                                    fusedLocationClient.removeLocationUpdates(this)
                                    if (continuation.isActive) {
                                        continuation.resume(location)
                                    }
                                }
                            } else {
                                Timber.w("收到空位置信息")
                                fusedLocationClient.removeLocationUpdates(this)
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                        }
                        
                        override fun onLocationAvailability(availability: LocationAvailability) {
                            Timber.d("位置可用性: ${availability.isLocationAvailable}")
                            if (!availability.isLocationAvailable) {
                                Timber.w("位置服务不可用，可能GPS信号弱或位置服务被禁用")
                                fusedLocationClient.removeLocationUpdates(this)
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                        }
                    }

                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        try {
                            fusedLocationClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                Looper.getMainLooper()
                            )
                            
                            // 添加超时保护，避免无限等待
                            GlobalScope.launch {
                                kotlinx.coroutines.delay(LOCATION_TIMEOUT)
                                if (continuation.isActive) {
                                    Timber.w("位置获取超时，强制结束")
                                    fusedLocationClient.removeLocationUpdates(locationCallback)
                                    continuation.resume(null)
                                }
                            }
                            
                            continuation.invokeOnCancellation {
                                fusedLocationClient.removeLocationUpdates(locationCallback)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "请求位置更新失败")
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    } else {
                        continuation.resume(null)
                    }
                }
            }

            if (location != null) {
                val locationInfo = convertToLocationInfo(location)
                // 更新缓存
                cachedLocation = locationInfo
                lastLocationUpdateTime = System.currentTimeMillis()
                
                Timber.d("位置获取成功: ${locationInfo.locationName}")
                Result.success(locationInfo)
            } else {
                Timber.w("精确位置获取超时或失败，尝试使用最后已知位置")
                // 尝试获取最后已知位置
                try {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val lastKnownLocation = fusedLocationClient.lastLocation
                        val lastLocation = suspendCancellableCoroutine<android.location.Location?> { cont ->
                            lastKnownLocation.addOnSuccessListener { loc ->
                                Timber.d("获取到最后已知位置: lat=${loc?.latitude}, lng=${loc?.longitude}, 精度=${loc?.accuracy}")
                                cont.resume(loc)
                            }.addOnFailureListener { exception ->
                                Timber.w(exception, "获取最后已知位置失败")
                                cont.resume(null)
                            }
                        }
                        
                        if (lastLocation != null) {
                            val locationInfo = convertToLocationInfo(lastLocation)
                            cachedLocation = locationInfo
                            lastLocationUpdateTime = System.currentTimeMillis()
                            Timber.d("使用最后已知位置: ${locationInfo.locationName}, 精度: ${lastLocation.accuracy}m")
                            Result.success(locationInfo)
                        } else {
                            Timber.w("最后已知位置也为空，可能是首次使用GPS")
                            Result.failure(Exception("无法获取位置信息\n\n可能原因：\n• 首次使用GPS需要时间定位\n• 请到室外或窗边获取更好的GPS信号\n• 确保位置服务已开启"))
                        }
                    } else {
                        Timber.w("获取最后已知位置时权限检查失败")
                        Result.failure(Exception("位置权限不足"))
                    }
                } catch (e: Exception) {
                    Timber.e(e, "获取最后已知位置失败")
                    Result.failure(Exception("位置服务暂时不可用\n\n请尝试：\n• 重启位置服务\n• 检查网络连接\n• 到室外获取更好信号"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "获取位置失败")
            Result.failure(e)
        }
    }

    /**
     * 快速位置获取策略：先尝试网络定位，再尝试GPS
     */
    private suspend fun getQuickLocation(): android.location.Location? {
        return try {
            Timber.d("尝试快速网络定位")
            
            // 使用网络定位进行快速获取
            val networkLocationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 2000)
                .setMaxUpdates(1)
                .setMaxUpdateDelayMillis(3000) // 3秒快速获取
                .build()
            
            val quickLocation = withTimeoutOrNull(8000) { // 8秒快速超时
                suspendCancellableCoroutine<android.location.Location?> { continuation ->
                    val quickCallback = object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            val location = result.lastLocation
                            Timber.d("快速定位结果: lat=${location?.latitude}, lng=${location?.longitude}, 精度=${location?.accuracy}")
                            fusedLocationClient.removeLocationUpdates(this)
                            if (continuation.isActive) {
                                continuation.resume(location)
                            }
                        }
                        
                        override fun onLocationAvailability(availability: LocationAvailability) {
                            if (!availability.isLocationAvailable) {
                                Timber.w("快速定位服务不可用")
                                fusedLocationClient.removeLocationUpdates(this)
                                if (continuation.isActive) {
                                    continuation.resume(null)
                                }
                            }
                        }
                    }
                    
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        fusedLocationClient.requestLocationUpdates(
                            networkLocationRequest,
                            quickCallback,
                            Looper.getMainLooper()
                        )
                        
                        // 添加超时保护
                        GlobalScope.launch {
                            kotlinx.coroutines.delay(8000)
                            if (continuation.isActive) {
                                Timber.w("快速定位超时，强制结束")
                                fusedLocationClient.removeLocationUpdates(quickCallback)
                                continuation.resume(null)
                            }
                        }
                        
                        continuation.invokeOnCancellation {
                            fusedLocationClient.removeLocationUpdates(quickCallback)
                        }
                    } else {
                        continuation.resume(null)
                    }
                }
            }
            
            quickLocation
        } catch (e: Exception) {
            Timber.w(e, "快速定位失败")
            null
        }
    }

    override suspend fun hasLocationPermission(): Boolean {
        val fineGranted = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val coarseGranted = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasPermission = fineGranted || coarseGranted
        
        Timber.d("LocationService权限检查 - 精确:$fineGranted, 粗略:$coarseGranted, 结果:$hasPermission")
        
        return hasPermission
    }

        override suspend fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        
        Timber.d("位置服务状态 - GPS: $gpsEnabled, 网络: $networkEnabled")
        
        return gpsEnabled || networkEnabled
    }

    override fun observeLocationUpdates(): Flow<LocationInfo> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("位置权限未授予"))
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, LOCATION_UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    val locationInfo = convertToLocationInfo(location)
                    trySend(locationInfo)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun stopLocationUpdates() {
        // 位置更新会在Flow取消时自动停止
    }

    override fun shouldRefreshLocation(): Boolean {
        return System.currentTimeMillis() - lastLocationUpdateTime > LOCATION_CACHE_DURATION
    }

    override suspend fun getCachedLocation(): LocationInfo? {
        return if (shouldRefreshLocation()) null else cachedLocation
    }

    private fun convertToLocationInfo(location: android.location.Location): LocationInfo {
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
                    // 最详细：包含街道和建筑物信息
                    city.isNotBlank() && district.isNotBlank() && subLocality.isNotBlank() && featureName.isNotBlank() -> {
                        "${city}${district}${subLocality}${featureName}"
                    }
                    // 包含街道信息
                    city.isNotBlank() && district.isNotBlank() && subLocality.isNotBlank() -> {
                        "${city}${district}${subLocality}"
                    }
                    // 城市和区域
                    city.isNotBlank() && district.isNotBlank() -> {
                        "${city}${district}"
                    }
                    // 仅城市（去掉"市"字以简化显示）
                    city.isNotBlank() -> {
                        city.replace("市", "").replace("县", "")
                    }
                    // 省份（去掉"省"字）
                    province.isNotBlank() -> {
                        province.replace("省", "").replace("自治区", "").replace("市", "")
                    }
                    // 如果都没有，使用精确坐标（6位小数）
                    else -> "${String.format("%.6f", location.latitude)}°N, ${String.format("%.6f", location.longitude)}°E"
                }
                
                Timber.d("地址解析成功: $locationName")
                Timber.d("详细信息 - 省:$province, 市:$city, 区:$district, 街道:$subLocality")
            } else {
                // 如果地址解析失败，显示坐标
                locationName = "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
                Timber.w("地址解析返回空结果，使用坐标显示")
            }
        } catch (e: Exception) {
            Timber.w(e, "地址解析失败，使用坐标显示")
            locationName = "${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}"
        }

        return LocationInfo(
            locationName = locationName,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            altitude = if (location.hasAltitude()) location.altitude else null,
            address = address,
            city = city,
            district = district,
            province = province,
            country = "中国",
            addedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            isCurrentLocation = true,
            locationType = LocationType.AUTO
        )
    }
} 
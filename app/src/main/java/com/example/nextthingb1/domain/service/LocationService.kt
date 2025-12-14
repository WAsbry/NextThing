package com.example.nextthingb1.domain.service

import com.example.nextthingb1.domain.model.LocationInfo
import kotlinx.coroutines.flow.Flow

/**
 * 定位来源
 */
enum class LocationSource {
    GPS,            // GPS定位
    NETWORK,        // 网络定位
    WIFI,           // WiFi定位
    CELL,           // 基站定位
    CACHE,          // 缓存定位
    UNKNOWN         // 未知来源
}

/**
 * 定位精度等级
 */
enum class AccuracyLevel {
    EXCELLENT,      // 优秀 (<10m)
    GOOD,           // 良好 (10-50m)
    FAIR,           // 一般 (50-100m)
    POOR,           // 较差 (100-500m)
    UNAVAILABLE     // 不可用 (>500m或null)
}

/**
 * 定位服务状态
 */
data class LocationServiceStatus(
    val isAvailable: Boolean,           // 服务是否可用
    val amapInitialized: Boolean,       // 高德地图是否初始化成功
    val hasPermission: Boolean,         // 是否有权限
    val isLocationEnabled: Boolean,     // 位置服务是否启用
    val lastErrorMessage: String? = null // 最后的错误信息
)

interface LocationService {
    /**
     * 获取当前实时位置
     * @param forceRefresh 是否强制刷新位置
     * @return 位置信息结果
     */
    suspend fun getCurrentLocation(forceRefresh: Boolean = false): LocationInfo?

    /**
     * 检查位置权限是否已授予
     */
    suspend fun hasLocationPermission(): Boolean

    /**
     * 检查位置服务是否启用
     */
    suspend fun isLocationEnabled(): Boolean

    /**
     * 检查定位服务是否可用（包括高德地图初始化检查）
     */
    suspend fun isServiceAvailable(): Boolean

    /**
     * 获取定位服务状态详情
     */
    suspend fun getServiceStatus(): LocationServiceStatus

    /**
     * 获取当前定位来源
     */
    fun getLocationSource(locationType: Int): LocationSource

    /**
     * 获取精度等级
     */
    fun getAccuracyLevel(accuracy: Float?): AccuracyLevel

    /**
     * 监听位置变化
     */
    fun observeLocationUpdates(): Flow<LocationInfo>

    /**
     * 停止位置监听
     */
    fun stopLocationUpdates()

    /**
     * 检查是否需要刷新位置（基于5分钟间隔）
     */
    fun shouldRefreshLocation(): Boolean

    /**
     * 获取缓存的位置信息
     */
    suspend fun getCachedLocation(): LocationInfo?
} 
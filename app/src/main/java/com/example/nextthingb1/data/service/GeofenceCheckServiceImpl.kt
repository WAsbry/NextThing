package com.example.nextthingb1.data.service

import com.example.nextthingb1.domain.model.GeofenceCheckResult
import com.example.nextthingb1.domain.model.GeofenceStatus
import com.example.nextthingb1.domain.model.LocationInfo
import com.example.nextthingb1.domain.repository.GeofenceConfigRepository
import com.example.nextthingb1.domain.repository.GeofenceLocationRepository
import com.example.nextthingb1.domain.repository.TaskGeofenceRepository
import com.example.nextthingb1.domain.service.GeofenceCheckService
import com.example.nextthingb1.domain.service.LocationService
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * 地理围栏检查服务实现
 *
 * 提供地理围栏状态检查、距离计算等功能
 * 包含超时、降级、缓存机制
 */
@Singleton
class GeofenceCheckServiceImpl @Inject constructor(
    private val locationService: LocationService,
    private val taskGeofenceRepository: TaskGeofenceRepository,
    private val geofenceLocationRepository: GeofenceLocationRepository,
    private val configRepository: GeofenceConfigRepository
) : GeofenceCheckService {

    companion object {
        private const val TAG = "GeofenceCheck"

        // 超时配置
        private const val LOCATION_TIMEOUT_MS = 10_000L // 10秒

        // 缓存配置
        private const val CACHE_VALIDITY_MS = 60_000L // 1分钟缓存有效期
    }

    // 位置缓存
    private var cachedLocation: LocationInfo? = null
    private var cacheTimestamp: Long = 0L

    // ========== 主要检查方法 ==========

    override suspend fun checkTaskGeofence(taskId: String): GeofenceStatus {
        Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag(TAG).d("开始检查任务地理围栏")
        Timber.tag(TAG).d("  任务ID: $taskId")

        return try {
            // 1. 获取全局配置
            val config = configRepository.getConfigOrDefault()

            // 2. 检查全局开关
            if (!config.isGlobalEnabled) {
                Timber.tag(TAG).d("⏭️ 全局地理围栏未启用，跳过检查")
                return createDisabledStatus()
            }

            // 3. 获取任务的地理围栏关联
            val taskGeofence = taskGeofenceRepository.getByTaskIdOnce(taskId)

            if (taskGeofence == null) {
                Timber.tag(TAG).d("⏭️ 任务未设置地理围栏")
                return createDisabledStatus()
            }

            // 4. 检查任务级别的启用状态
            if (!taskGeofence.isEnabled) {
                Timber.tag(TAG).d("⏭️ 任务地理围栏已禁用")
                return createDisabledStatus()
            }

            // 5. 获取地理围栏地点信息
            val geofenceLocation = geofenceLocationRepository.getLocationByIdOnce(
                taskGeofence.geofenceLocationId
            )

            if (geofenceLocation == null) {
                Timber.tag(TAG).e("❌ 地理围栏地点不存在: ${taskGeofence.geofenceLocationId}")
                return GeofenceStatus(
                    lastCheckTime = LocalDateTime.now(),
                    isInsideGeofence = false,
                    distance = 0.0,
                    userLatitude = 0.0,
                    userLongitude = 0.0,
                    checkResult = GeofenceCheckResult.GEOFENCE_DISABLED,
                    targetLocationName = "",
                    geofenceRadius = taskGeofence.snapshotRadius
                )
            }

            val targetLocation = geofenceLocation.locationInfo

            // 6. 获取用户当前位置（带超时和降级）
            val userLocation = getUserLocationWithFallback(config.locationAccuracyThreshold)

            if (userLocation == null) {
                Timber.tag(TAG).w("⚠️ 无法获取用户位置，降级为普通通知")
                return GeofenceStatus(
                    lastCheckTime = LocalDateTime.now(),
                    isInsideGeofence = false,
                    distance = 0.0,
                    userLatitude = 0.0,
                    userLongitude = 0.0,
                    checkResult = GeofenceCheckResult.LOCATION_UNAVAILABLE,
                    targetLocationName = targetLocation.locationName,
                    geofenceRadius = taskGeofence.snapshotRadius
                )
            }

            // 7. 检查位置精度
            if (userLocation.accuracy != null && userLocation.accuracy > config.locationAccuracyThreshold) {
                Timber.tag(TAG).w("⚠️ 位置精度不足: ${userLocation.accuracy}m > ${config.locationAccuracyThreshold}m")
                // 精度不足时仍然尝试检查，但记录警告
            }

            // 8. 计算距离
            val distance = calculateDistance(
                userLocation.latitude,
                userLocation.longitude,
                targetLocation.latitude,
                targetLocation.longitude
            )

            Timber.tag(TAG).d("📍 距离计算结果: ${String.format("%.2f", distance)}m")
            Timber.tag(TAG).d("  围栏半径: ${taskGeofence.snapshotRadius}m")
            Timber.tag(TAG).d("  用户坐标: (${userLocation.latitude}, ${userLocation.longitude})")
            Timber.tag(TAG).d("  目标坐标: (${targetLocation.latitude}, ${targetLocation.longitude})")

            // 9. 判断是否在围栏内
            val isInside = distance <= taskGeofence.snapshotRadius
            val result = if (isInside) {
                GeofenceCheckResult.INSIDE_GEOFENCE
            } else {
                GeofenceCheckResult.OUTSIDE_GEOFENCE
            }

            // 10. 创建状态对象
            val status = GeofenceStatus(
                lastCheckTime = LocalDateTime.now(),
                isInsideGeofence = isInside,
                distance = distance,
                userLatitude = userLocation.latitude,
                userLongitude = userLocation.longitude,
                checkResult = result,
                targetLocationName = targetLocation.locationName,
                geofenceRadius = taskGeofence.snapshotRadius
            )

            // 11. 更新检查结果到数据库
            taskGeofenceRepository.updateLastCheckResult(
                taskId = taskId,
                result = result,
                distance = distance,
                userLatitude = userLocation.latitude,
                userLongitude = userLocation.longitude
            )

            Timber.tag(TAG).d("✅ 检查完成: ${result.name}")
            Timber.tag(TAG).d("  距离: ${String.format("%.2f", distance)}m")
            Timber.tag(TAG).d("  是否通知: ${result.shouldNotify()}")
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            status
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 检查地理围栏时发生异常")
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            // 异常时降级为位置不可用
            GeofenceStatus(
                lastCheckTime = LocalDateTime.now(),
                isInsideGeofence = false,
                distance = 0.0,
                userLatitude = 0.0,
                userLongitude = 0.0,
                checkResult = GeofenceCheckResult.LOCATION_UNAVAILABLE,
                targetLocationName = "",
                geofenceRadius = 200
            )
        }
    }

    override suspend fun checkMultipleTaskGeofences(taskIds: List<String>): Map<String, GeofenceStatus> {
        Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag(TAG).d("批量检查 ${taskIds.size} 个任务的地理围栏")

        val results = mutableMapOf<String, GeofenceStatus>()

        // 预先获取一次用户位置，避免重复获取
        val config = configRepository.getConfigOrDefault()
        val userLocation = getUserLocationWithFallback(config.locationAccuracyThreshold)

        if (userLocation != null) {
            // 更新缓存，供后续检查使用
            cachedLocation = userLocation
            cacheTimestamp = System.currentTimeMillis()
        }

        // 逐个检查任务
        taskIds.forEach { taskId ->
            try {
                val status = checkTaskGeofence(taskId)
                results[taskId] = status
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "检查任务 $taskId 失败")
                results[taskId] = GeofenceStatus(
                    lastCheckTime = LocalDateTime.now(),
                    isInsideGeofence = false,
                    distance = 0.0,
                    userLatitude = 0.0,
                    userLongitude = 0.0,
                    checkResult = GeofenceCheckResult.LOCATION_UNAVAILABLE,
                    targetLocationName = "",
                    geofenceRadius = 200
                )
            }
        }

        Timber.tag(TAG).d("✅ 批量检查完成")
        Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        return results
    }

    // ========== Haversine 距离计算算法 ==========

    override fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        // 地球半径（米）
        val R = 6371000.0

        // 转换为弧度
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        // Haversine 公式
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1Rad) * cos(lat2Rad) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        // 返回距离（米）
        return R * c
    }

    // ========== 位置获取逻辑（带超时和降级） ==========

    /**
     * 获取用户位置，带超时和缓存降级
     *
     * 策略：
     * 1. 检查缓存是否有效（1分钟内）
     * 2. 检查位置服务状态（权限、启用状态、高德地图初始化）
     * 3. 尝试获取实时位置（10秒超时）
     * 4. 超时则使用缓存位置
     * 5. 缓存也没有则返回 null
     */
    private suspend fun getUserLocationWithFallback(accuracyThreshold: Int): LocationInfo? {
        return try {
            // 1. 检查缓存是否有效
            val now = System.currentTimeMillis()
            if (cachedLocation != null && (now - cacheTimestamp) < CACHE_VALIDITY_MS) {
                Timber.tag(TAG).d("🔄 使用缓存位置（${(now - cacheTimestamp) / 1000}秒前）")
                return cachedLocation
            }

            // 2. 检查位置服务状态（包括权限、启用状态、高德地图初始化）
            val serviceStatus = locationService.getServiceStatus()

            if (!serviceStatus.isAvailable) {
                Timber.tag(TAG).w("⚠️ 位置服务不可用: ${serviceStatus.lastErrorMessage}")

                // 如果高德地图未初始化但权限和位置服务正常，仍可降级使用Google服务
                if (!serviceStatus.amapInitialized && serviceStatus.hasPermission && serviceStatus.isLocationEnabled) {
                    Timber.tag(TAG).d("📡 高德地图未初始化，降级使用Google定位服务")
                } else {
                    // 权限或位置服务问题，无法继续
                    return null
                }
            } else {
                // 服务可用，记录初始化状态
                val initStatus = if (serviceStatus.amapInitialized) "高德地图" else "Google服务"
                Timber.tag(TAG).d("✅ 位置服务可用（$initStatus）")
            }

            // 3. 尝试获取实时位置（带超时）
            Timber.tag(TAG).d("📍 获取实时位置（超时: ${LOCATION_TIMEOUT_MS / 1000}秒）")

            val location = withTimeout(LOCATION_TIMEOUT_MS) {
                locationService.getCurrentLocation(forceRefresh = true)
            }

            if (location != null) {
                // 分析位置精度等级
                val accuracyLevel = locationService.getAccuracyLevel(location.accuracy)
                val accuracyText = when (accuracyLevel) {
                    com.example.nextthingb1.domain.service.AccuracyLevel.EXCELLENT -> "优秀 (<10m)"
                    com.example.nextthingb1.domain.service.AccuracyLevel.GOOD -> "良好 (10-50m)"
                    com.example.nextthingb1.domain.service.AccuracyLevel.FAIR -> "一般 (50-100m)"
                    com.example.nextthingb1.domain.service.AccuracyLevel.POOR -> "较差 (100-500m)"
                    com.example.nextthingb1.domain.service.AccuracyLevel.UNAVAILABLE -> "不可用 (>500m)"
                }

                // 更新缓存
                cachedLocation = location
                cacheTimestamp = System.currentTimeMillis()

                Timber.tag(TAG).d("✅ 获取实时位置成功")
                Timber.tag(TAG).d("  精度: ${location.accuracy}m - $accuracyText")

                // 如果精度不足，记录警告
                if (location.accuracy != null && location.accuracy > accuracyThreshold) {
                    Timber.tag(TAG).w("⚠️ 位置精度不足: ${location.accuracy}m > ${accuracyThreshold}m (阈值)")
                    Timber.tag(TAG).w("  精度等级: $accuracyText")
                    Timber.tag(TAG).w("  建议：移至室外空旷处以获得更好的GPS信号")
                }

                location
            } else {
                Timber.tag(TAG).w("⚠️ 获取实时位置失败，尝试使用缓存")
                // 尝试使用过期缓存
                cachedLocation
            }
        } catch (e: TimeoutCancellationException) {
            Timber.tag(TAG).w("⏱️ 获取位置超时，使用缓存降级")
            // 超时时使用缓存（即使过期）
            cachedLocation
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 获取位置异常")
            // 异常时尝试使用缓存
            cachedLocation
        }
    }

    // ========== 辅助方法 ==========

    /**
     * 创建未启用状态
     */
    private fun createDisabledStatus(): GeofenceStatus {
        return GeofenceStatus(
            lastCheckTime = LocalDateTime.now(),
            isInsideGeofence = false,
            distance = 0.0,
            userLatitude = 0.0,
            userLongitude = 0.0,
            checkResult = GeofenceCheckResult.GEOFENCE_DISABLED,
            targetLocationName = "",
            geofenceRadius = 200
        )
    }

    override fun clearLocationCache() {
        Timber.tag(TAG).d("🗑️ 清除位置缓存")
        cachedLocation = null
        cacheTimestamp = 0L
    }
}

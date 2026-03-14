package com.example.nextthingb1.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * 地理围栏全局配置（领域模型）
 */
data class GeofenceConfig(
    val id: String = "default",
    val isGlobalEnabled: Boolean = false,
    val defaultRadius: Int = 200,
    val locationAccuracyThreshold: Int = 100,
    val autoRefreshInterval: Int = 300,
    val batteryOptimization: Boolean = true,
    val notifyWhenOutside: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * 地理围栏地点（领域模型）
 */
data class GeofenceLocation(
    val id: String = UUID.randomUUID().toString(),
    val locationInfo: LocationInfo, // 复用现有的地点信息
    val customRadius: Int? = null,
    val isFrequent: Boolean = false,
    val usageCount: Int = 0,
    val lastUsed: LocalDateTime? = null,
    val monthlyCheckCount: Int = 0,
    val monthlyHitCount: Int = 0,
    val lastStatisticsResetMonth: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * 获取有效半径
     * @param defaultRadius 全局默认半径
     * @return 自定义半径 或 默认半径
     */
    fun getEffectiveRadius(defaultRadius: Int = 200): Int {
        return customRadius ?: defaultRadius
    }

    /**
     * 计算命中率
     * @return 0.0 ~ 1.0 之间的浮点数
     */
    fun getHitRate(): Float {
        return if (monthlyCheckCount > 0) {
            monthlyHitCount.toFloat() / monthlyCheckCount
        } else {
            0f
        }
    }

    /**
     * 获取格式化的命中率文本
     * @return "85%" 或 "无数据"
     */
    fun getFormattedHitRate(): String {
        return if (monthlyCheckCount > 0) {
            "${(getHitRate() * 100).toInt()}%"
        } else {
            "无数据"
        }
    }
}

/**
 * 任务地理围栏关联（领域模型）
 */
data class TaskGeofence(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val geofenceLocationId: String,
    val geofenceLocation: GeofenceLocation, // 关联的地理围栏地点（完整对象）
    val snapshotRadius: Int, // 快照值：任务创建时的半径
    val isEnabled: Boolean = true, // 是否启用（改名，保持一致性）
    val lastCheckTime: LocalDateTime? = null,
    val lastCheckResult: GeofenceCheckResult? = null,
    val lastCheckDistance: Double? = null,
    val lastCheckUserLatitude: Double? = null,
    val lastCheckUserLongitude: Double? = null,
    val geofenceDeferCount: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * 地理围栏检查结果枚举
 */
enum class GeofenceCheckResult {
    /** 在围栏内 */
    INSIDE_GEOFENCE,

    /** 在围栏外 */
    OUTSIDE_GEOFENCE,

    /** 位置不可用（定位失败） */
    LOCATION_UNAVAILABLE,

    /** 权限被拒绝 */
    PERMISSION_DENIED,

    /** 未启用地理围栏 */
    GEOFENCE_DISABLED;

    /**
     * 是否应该发送通知
     * @return true = 发送通知，false = 不发送
     */
    fun shouldNotify(): Boolean {
        return when (this) {
            INSIDE_GEOFENCE -> true // 在围栏内，正常通知
            LOCATION_UNAVAILABLE -> true // 位置不可用，降级为普通通知
            PERMISSION_DENIED -> true // 权限被拒绝，降级为普通通知
            GEOFENCE_DISABLED -> true // 未启用围栏，正常通知
            OUTSIDE_GEOFENCE -> false // 在围栏外，不发送通知
        }
    }

    /**
     * 获取用户友好的描述文本
     */
    fun getDisplayText(): String {
        return when (this) {
            INSIDE_GEOFENCE -> "您已到达目标地点附近"
            OUTSIDE_GEOFENCE -> "您当前不在目标地点附近"
            LOCATION_UNAVAILABLE -> "无法获取您的位置信息"
            PERMISSION_DENIED -> "未授予位置权限"
            GEOFENCE_DISABLED -> "未启用地理围栏"
        }
    }

    /**
     * 获取对应的图标
     */
    fun getIcon(): String {
        return when (this) {
            INSIDE_GEOFENCE -> "✅"
            OUTSIDE_GEOFENCE -> "⚠️"
            LOCATION_UNAVAILABLE -> "❌"
            PERMISSION_DENIED -> "🔒"
            GEOFENCE_DISABLED -> "⭕"
        }
    }
}

/**
 * 地理围栏检查状态（完整信息）
 */
data class GeofenceStatus(
    val lastCheckTime: LocalDateTime,
    val isInsideGeofence: Boolean,
    val distance: Double, // 与目标点的距离（米）
    val userLatitude: Double,
    val userLongitude: Double,
    val checkResult: GeofenceCheckResult,
    val targetLocationName: String = "",
    val geofenceRadius: Int = 200
) {
    /**
     * 获取格式化的距离文本
     * @return "50米" 或 "3.2公里"
     */
    fun getFormattedDistance(): String {
        return if (distance < 1000) {
            "${distance.toInt()}米"
        } else {
            "%.1f公里".format(distance / 1000)
        }
    }
}

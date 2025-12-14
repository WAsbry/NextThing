package com.example.nextthingb1.presentation.screens.geofence.config

import com.example.nextthingb1.domain.model.GeofenceConfig
import com.example.nextthingb1.domain.model.GeofenceLocation

/**
 * 权限状态
 */
enum class PermissionState {
    GRANTED,                // 已授予
    DENIED,                 // 已拒绝
    NOT_REQUESTED,          // 未请求
    PERMANENTLY_DENIED      // 永久拒绝（用户选择了"不再询问"）
}

/**
 * 地理围栏配置页面的 UI 状态
 */
data class GeofenceConfigUiState(
    // 权限状态
    val locationPermissionState: PermissionState = PermissionState.NOT_REQUESTED,
    val backgroundLocationPermissionState: PermissionState = PermissionState.NOT_REQUESTED,
    val showPermissionRationale: Boolean = false,       // 是否显示权限说明
    val showBackgroundPermissionRationale: Boolean = false,  // 是否显示后台权限说明

    // 全局配置
    val config: GeofenceConfig? = null,
    val isGlobalEnabled: Boolean = false,
    val defaultRadius: Int = 200,
    val locationAccuracyThreshold: Int = 100,
    val batteryOptimization: Boolean = true,
    val notifyWhenOutside: Boolean = false,

    // 系统地理围栏状态
    val systemGeofencesRegistered: Int = 0,          // 系统已注册的地理围栏数量
    val systemGeofencesActive: Boolean = false,       // 系统地理围栏是否活跃

    // 地点列表
    val locations: List<GeofenceLocation> = emptyList(),
    val frequentLocations: List<GeofenceLocation> = emptyList(),

    // 统计信息
    val totalLocationsCount: Int = 0,
    val frequentLocationsCount: Int = 0,
    val activeTasksCount: Int = 0,
    val monthlyCheckCount: Int = 0,      // 本月检查次数
    val averageHitRate: Float = 0f,      // 平均命中率（0.0~1.0）

    // UI 状态
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // 对话框状态
    val showAdvancedSettings: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val locationToDelete: GeofenceLocation? = null
) {
    /**
     * 是否具有基础位置权限
     */
    val hasLocationPermission: Boolean
        get() = locationPermissionState == PermissionState.GRANTED

    /**
     * 是否具有后台位置权限
     */
    val hasBackgroundLocationPermission: Boolean
        get() = backgroundLocationPermissionState == PermissionState.GRANTED

    /**
     * 是否具有完整权限（位置 + 后台位置）
     */
    val hasFullPermissions: Boolean
        get() = hasLocationPermission && hasBackgroundLocationPermission

    /**
     * 是否需要显示权限请求UI
     */
    val shouldShowPermissionRequest: Boolean
        get() = !hasLocationPermission || !hasBackgroundLocationPermission

    /**
     * 功能是否可用（有权限且全局已启用）
     */
    val isFunctional: Boolean
        get() = hasLocationPermission && isGlobalEnabled

    /**
     * 完整功能是否可用（有完整权限且全局已启用）
     */
    val isFullyFunctional: Boolean
        get() = hasFullPermissions && isGlobalEnabled
}

package com.nextthing.app.data.mapper

import com.nextthing.app.data.local.entity.GeofenceConfigEntity
import com.nextthing.app.data.local.entity.GeofenceLocationEntity
import com.nextthing.app.data.local.entity.TaskGeofenceEntity
import com.nextthing.app.data.local.entity.GeofenceLocationStatisticsHistoryEntity
import com.nextthing.app.domain.model.GeofenceConfig
import com.nextthing.app.domain.model.GeofenceLocation
import com.nextthing.app.domain.model.TaskGeofence
import com.nextthing.app.domain.model.GeofenceCheckResult
import com.nextthing.app.domain.model.LocationInfo
import com.nextthing.app.domain.model.GeofenceLocationStatisticsHistory

// ========== GeofenceConfig 映射 ==========

fun GeofenceConfigEntity.toDomain(): GeofenceConfig {
    return GeofenceConfig(
        id = id,
        isGlobalEnabled = isGlobalEnabled,
        defaultRadius = defaultRadius,
        locationAccuracyThreshold = locationAccuracyThreshold,
        autoRefreshInterval = autoRefreshInterval,
        batteryOptimization = batteryOptimization,
        notifyWhenOutside = notifyWhenOutside,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun GeofenceConfig.toEntity(): GeofenceConfigEntity {
    return GeofenceConfigEntity(
        id = id,
        isGlobalEnabled = isGlobalEnabled,
        defaultRadius = defaultRadius,
        locationAccuracyThreshold = locationAccuracyThreshold,
        autoRefreshInterval = autoRefreshInterval,
        batteryOptimization = batteryOptimization,
        notifyWhenOutside = notifyWhenOutside,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// ========== GeofenceLocation 映射 ==========

/**
 * Entity 转 Domain
 * 注意:需要单独提供 LocationInfo，因为 Entity 只存储 locationId
 */
fun GeofenceLocationEntity.toDomain(locationInfo: LocationInfo): GeofenceLocation {
    return GeofenceLocation(
        id = id,
        locationInfo = locationInfo,
        isEnabled = isEnabled,
        customRadius = customRadius,
        isFrequent = isFrequent,
        usageCount = usageCount,
        lastUsed = lastUsed,
        monthlyCheckCount = monthlyCheckCount,
        monthlyHitCount = monthlyHitCount,
        lastStatisticsResetMonth = lastStatisticsResetMonth,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Domain 转 Entity
 * 只存储 locationId，不存储完整的 LocationInfo
 */
fun GeofenceLocation.toEntity(): GeofenceLocationEntity {
    return GeofenceLocationEntity(
        id = id,
        locationId = locationInfo.id, // 只存储ID
        isEnabled = isEnabled,
        customRadius = customRadius,
        isFrequent = isFrequent,
        usageCount = usageCount,
        lastUsed = lastUsed,
        monthlyCheckCount = monthlyCheckCount,
        monthlyHitCount = monthlyHitCount,
        lastStatisticsResetMonth = lastStatisticsResetMonth,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// ========== TaskGeofence 映射 ==========

fun TaskGeofenceEntity.toDomain(geofenceLocation: GeofenceLocation): TaskGeofence {
    return TaskGeofence(
        id = id,
        taskId = taskId,
        geofenceLocationId = geofenceLocationId,
        geofenceLocation = geofenceLocation,
        snapshotRadius = radius,
        isEnabled = enabled,
        lastCheckTime = lastCheckTime,
        lastCheckResult = lastCheckResult?.let {
            try {
                GeofenceCheckResult.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        },
        lastCheckDistance = lastCheckDistance,
        lastCheckUserLatitude = lastCheckUserLatitude,
        lastCheckUserLongitude = lastCheckUserLongitude,
        geofenceDeferCount = geofenceDeferCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun TaskGeofence.toEntity(): TaskGeofenceEntity {
    return TaskGeofenceEntity(
        id = id,
        taskId = taskId,
        geofenceLocationId = geofenceLocationId,
        radius = snapshotRadius,
        enabled = isEnabled,
        lastCheckTime = lastCheckTime,
        lastCheckResult = lastCheckResult?.name, // 存储枚举名称
        lastCheckDistance = lastCheckDistance,
        lastCheckUserLatitude = lastCheckUserLatitude,
        lastCheckUserLongitude = lastCheckUserLongitude,
        geofenceDeferCount = geofenceDeferCount,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// ========== GeofenceLocationStatisticsHistory 映射 ==========

fun GeofenceLocationStatisticsHistoryEntity.toDomain(): GeofenceLocationStatisticsHistory {
    return GeofenceLocationStatisticsHistory(
        id = id,
        geofenceLocationId = geofenceLocationId,
        month = month,
        checkCount = checkCount,
        hitCount = hitCount,
        hitRate = hitRate,
        createdAt = java.time.LocalDateTime.parse(createdAt)
    )
}

fun GeofenceLocationStatisticsHistory.toEntity(): GeofenceLocationStatisticsHistoryEntity {
    return GeofenceLocationStatisticsHistoryEntity(
        id = id,
        geofenceLocationId = geofenceLocationId,
        month = month,
        checkCount = checkCount,
        hitCount = hitCount,
        hitRate = hitRate,
        createdAt = createdAt.toString()
    )
}

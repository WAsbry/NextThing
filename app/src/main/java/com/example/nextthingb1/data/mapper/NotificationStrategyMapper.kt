package com.example.nextthingb1.data.mapper

import com.example.nextthingb1.data.local.entity.NotificationStrategyEntity
import com.example.nextthingb1.domain.model.NotificationStrategy

fun NotificationStrategyEntity.toDomain(): NotificationStrategy {
    return NotificationStrategy(
        id = id,
        name = name,
        isGeofenceEnabled = isGeofenceEnabled,
        vibrationSetting = vibrationSetting,
        soundSetting = soundSetting,
        volume = volume,
        systemNotificationMode = systemNotificationMode,
        createdAt = createdAt,
        updatedAt = updatedAt,
        usageCount = usageCount,
        lastUsedAt = lastUsedAt
    )
}

fun NotificationStrategy.toEntity(): NotificationStrategyEntity {
    return NotificationStrategyEntity(
        id = id,
        name = name,
        isGeofenceEnabled = isGeofenceEnabled,
        vibrationSetting = vibrationSetting,
        soundSetting = soundSetting,
        volume = volume,
        systemNotificationMode = systemNotificationMode,
        createdAt = createdAt,
        updatedAt = updatedAt,
        usageCount = usageCount,
        lastUsedAt = lastUsedAt
    )
}

fun List<NotificationStrategyEntity>.toDomain(): List<NotificationStrategy> = map { it.toDomain() }
fun List<NotificationStrategy>.toEntity(): List<NotificationStrategyEntity> = map { it.toEntity() }
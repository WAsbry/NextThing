package com.nextthing.app.data.mapper

import com.nextthing.app.data.local.entity.NotificationStrategyEntity
import com.nextthing.app.domain.model.NotificationStrategy

fun NotificationStrategyEntity.toDomain(): NotificationStrategy {
    return NotificationStrategy(
        id = id,
        name = name,
        isGeofenceEnabled = isGeofenceEnabled,
        vibrationSetting = vibrationSetting,
        soundSetting = soundSetting,
        volume = volume,
        customAudioPath = customAudioPath,
        customAudioName = customAudioName,
        presetAudioName = presetAudioName,
        systemNotificationMode = systemNotificationMode,
        advanceReminderMinutes = advanceReminderMinutes,
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
        customAudioPath = customAudioPath,
        customAudioName = customAudioName,
        presetAudioName = presetAudioName,
        systemNotificationMode = systemNotificationMode,
        advanceReminderMinutes = advanceReminderMinutes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        usageCount = usageCount,
        lastUsedAt = lastUsedAt
    )
}

fun List<NotificationStrategyEntity>.toDomain(): List<NotificationStrategy> = map { it.toDomain() }
fun List<NotificationStrategy>.toEntity(): List<NotificationStrategyEntity> = map { it.toEntity() }
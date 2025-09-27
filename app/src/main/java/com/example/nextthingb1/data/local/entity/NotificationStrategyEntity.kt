package com.example.nextthingb1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.nextthingb1.domain.model.VibrationSetting
import com.example.nextthingb1.domain.model.SoundSetting
import com.example.nextthingb1.domain.model.SystemNotificationMode
import java.time.LocalDateTime

@Entity(tableName = "notification_strategies")
data class NotificationStrategyEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val isGeofenceEnabled: Boolean,
    val vibrationSetting: VibrationSetting,
    val soundSetting: SoundSetting,
    val volume: Int,
    val systemNotificationMode: SystemNotificationMode,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val usageCount: Int,
    val lastUsedAt: LocalDateTime?
)
package com.nextthing.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nextthing.app.domain.model.VibrationSetting
import com.nextthing.app.domain.model.SoundSetting
import com.nextthing.app.domain.model.SystemNotificationMode
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
    val customAudioPath: String? = null,
    val customAudioName: String? = null,
    val presetAudioName: String? = null,
    val systemNotificationMode: SystemNotificationMode,
    val advanceReminderMinutes: List<Int> = emptyList(),
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val usageCount: Int,
    val lastUsedAt: LocalDateTime?
)
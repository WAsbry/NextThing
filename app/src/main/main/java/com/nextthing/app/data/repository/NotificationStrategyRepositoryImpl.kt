package com.nextthing.app.data.repository

import com.nextthing.app.data.local.dao.NotificationStrategyDao
import com.nextthing.app.data.mapper.toDomain
import com.nextthing.app.data.mapper.toEntity
import com.nextthing.app.domain.model.*
import com.nextthing.app.domain.repository.NotificationStrategyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationStrategyRepositoryImpl @Inject constructor(
    private val notificationStrategyDao: NotificationStrategyDao
) : NotificationStrategyRepository {

    override fun getAllStrategies(): Flow<List<NotificationStrategy>> {
        return notificationStrategyDao.getAllStrategies().map { entities ->
            entities.toDomain()
        }
    }

    override suspend fun getStrategyById(id: String): NotificationStrategy? {
        return notificationStrategyDao.getStrategyById(id)?.toDomain()
    }

    override suspend fun insertStrategy(strategy: NotificationStrategy): String {
        notificationStrategyDao.insertStrategy(strategy.toEntity())
        return strategy.id
    }

    override suspend fun updateStrategy(strategy: NotificationStrategy) {
        notificationStrategyDao.updateStrategy(strategy.toEntity())
    }

    override suspend fun deleteStrategy(id: String) {
        notificationStrategyDao.deleteStrategyById(id)
    }

    override suspend fun deleteAllStrategies() {
        notificationStrategyDao.deleteAllStrategies()
    }

    override suspend fun ensurePresetStrategies() {
        val count = notificationStrategyDao.getStrategyCount()
        if (count > 0) return

        val presets = listOf(
            NotificationStrategy(
                id = "preset_silent",
                name = "无声提醒",
                vibrationSetting = VibrationSetting.LIGHT,
                soundSetting = SoundSetting.NONE,
                volume = 0,
                systemNotificationMode = SystemNotificationMode.STATUS_BAR,
                advanceReminderMinutes = emptyList()
            ),
            NotificationStrategy(
                id = "preset_standard",
                name = "标准提醒",
                vibrationSetting = VibrationSetting.MEDIUM,
                soundSetting = SoundSetting.STANDARD_TONE,
                volume = 50,
                systemNotificationMode = SystemNotificationMode.BANNER,
                advanceReminderMinutes = listOf(5)
            ),
            NotificationStrategy(
                id = "preset_important",
                name = "重要提醒",
                vibrationSetting = VibrationSetting.STRONG,
                soundSetting = SoundSetting.STANDARD_TONE,
                volume = 80,
                systemNotificationMode = SystemNotificationMode.DIALOG,
                advanceReminderMinutes = listOf(5, 15)
            )
        )

        notificationStrategyDao.insertAll(presets.map { it.toEntity() })
    }
}

package com.nextthing.app.domain.repository

import com.nextthing.app.domain.model.NotificationStrategy
import kotlinx.coroutines.flow.Flow

/**
 * 通知策略仓储接口
 */
interface NotificationStrategyRepository {

    fun getAllStrategies(): Flow<List<NotificationStrategy>>
    suspend fun getStrategyById(id: String): NotificationStrategy?
    suspend fun insertStrategy(strategy: NotificationStrategy): String
    suspend fun updateStrategy(strategy: NotificationStrategy)
    suspend fun deleteStrategy(id: String)
    suspend fun deleteAllStrategies()
    suspend fun ensurePresetStrategies()
}

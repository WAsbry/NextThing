package com.example.nextthingb1.domain.repository

import com.example.nextthingb1.domain.model.NotificationStrategy
import kotlinx.coroutines.flow.Flow

/**
 * 通知策略仓储接口
 */
interface NotificationStrategyRepository {

    /**
     * 获取所有通知策略
     */
    fun getAllStrategies(): Flow<List<NotificationStrategy>>

    /**
     * 根据ID获取通知策略
     */
    suspend fun getStrategyById(id: String): NotificationStrategy?

    /**
     * 插入通知策略
     */
    suspend fun insertStrategy(strategy: NotificationStrategy): String

    /**
     * 更新通知策略
     */
    suspend fun updateStrategy(strategy: NotificationStrategy)

    /**
     * 删除通知策略
     */
    suspend fun deleteStrategy(id: String)

    /**
     * 删除所有通知策略
     */
    suspend fun deleteAllStrategies()
}

package com.nextthing.app.data.repository

import com.nextthing.app.data.local.dao.NotificationStrategyDao
import com.nextthing.app.data.mapper.toDomain
import com.nextthing.app.data.mapper.toEntity
import com.nextthing.app.domain.model.NotificationStrategy
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
}

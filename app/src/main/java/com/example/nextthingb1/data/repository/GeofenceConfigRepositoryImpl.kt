package com.example.nextthingb1.data.repository

import com.example.nextthingb1.data.local.dao.GeofenceConfigDao
import com.example.nextthingb1.data.mapper.toDomain
import com.example.nextthingb1.data.mapper.toEntity
import com.example.nextthingb1.domain.model.GeofenceConfig
import com.example.nextthingb1.domain.repository.GeofenceConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofenceConfigRepositoryImpl @Inject constructor(
    private val geofenceConfigDao: GeofenceConfigDao
) : GeofenceConfigRepository {

    override fun getConfig(): Flow<GeofenceConfig?> {
        return geofenceConfigDao.getConfig().map { it?.toDomain() }
    }

    override suspend fun getConfigOrDefault(): GeofenceConfig {
        val existing = geofenceConfigDao.getConfigOnce()
        return if (existing != null) {
            existing.toDomain()
        } else {
            // 不存在则创建默认配置
            val defaultConfig = GeofenceConfig()
            geofenceConfigDao.insertOrReplace(defaultConfig.toEntity())
            defaultConfig
        }
    }

    override suspend fun updateConfig(config: GeofenceConfig) {
        val updatedConfig = config.copy(updatedAt = LocalDateTime.now())
        geofenceConfigDao.update(updatedConfig.toEntity())
    }

    override suspend fun updateGlobalEnabled(enabled: Boolean) {
        geofenceConfigDao.updateGlobalEnabled(
            enabled = enabled,
            updatedAt = LocalDateTime.now().toString()
        )
    }

    override suspend fun updateDefaultRadius(radius: Int) {
        geofenceConfigDao.updateDefaultRadius(
            radius = radius,
            updatedAt = LocalDateTime.now().toString()
        )
    }

    override suspend fun updateLocationAccuracyThreshold(threshold: Int) {
        val config = getConfigOrDefault()
        val updated = config.copy(
            locationAccuracyThreshold = threshold,
            updatedAt = LocalDateTime.now()
        )
        geofenceConfigDao.update(updated.toEntity())
    }

    override suspend fun updateBatteryOptimization(enabled: Boolean) {
        val config = getConfigOrDefault()
        val updated = config.copy(
            batteryOptimization = enabled,
            updatedAt = LocalDateTime.now()
        )
        geofenceConfigDao.update(updated.toEntity())
    }

    override suspend fun updateNotifyWhenOutside(enabled: Boolean) {
        val config = getConfigOrDefault()
        val updated = config.copy(
            notifyWhenOutside = enabled,
            updatedAt = LocalDateTime.now()
        )
        geofenceConfigDao.update(updated.toEntity())
    }

    override suspend fun initializeDefaultConfigIfNeeded() {
        val existing = geofenceConfigDao.getConfigOnce()
        if (existing == null) {
            val defaultConfig = GeofenceConfig()
            geofenceConfigDao.insertOrReplace(defaultConfig.toEntity())
            timber.log.Timber.tag("GeofenceConfig").d("✅ 初始化默认地理围栏配置")
        }
    }
}

package com.example.nextthingb1.data.repository

import com.example.nextthingb1.data.local.dao.GeofenceLocationDao
import com.example.nextthingb1.data.local.dao.LocationDao
import com.example.nextthingb1.data.mapper.toDomain
import com.example.nextthingb1.data.mapper.toEntity
import com.example.nextthingb1.domain.model.GeofenceLocation
import com.example.nextthingb1.domain.repository.GeofenceLocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofenceLocationRepositoryImpl @Inject constructor(
    private val geofenceLocationDao: GeofenceLocationDao,
    private val locationDao: LocationDao
) : GeofenceLocationRepository {

    // ========== 查询操作 ==========

    override fun getAllLocations(): Flow<List<GeofenceLocation>> {
        return geofenceLocationDao.getAllLocations().map { entities ->
            entities.mapNotNull { entity ->
                // 查询关联的 LocationInfo
                val locationInfo = locationDao.getLocationById(entity.locationId)?.toDomain()
                locationInfo?.let { entity.toDomain(it) }
            }
        }
    }

    override suspend fun getAllLocationsOnce(): List<GeofenceLocation> {
        val entities = geofenceLocationDao.getAllLocationsOnce()
        return entities.mapNotNull { entity ->
            val locationInfo = locationDao.getLocationById(entity.locationId)?.toDomain()
            locationInfo?.let { entity.toDomain(it) }
        }
    }

    override fun getLocationById(id: String): Flow<GeofenceLocation?> {
        return geofenceLocationDao.getLocationById(id).map { entity ->
            entity?.let {
                val locationInfo = locationDao.getLocationById(it.locationId)?.toDomain()
                locationInfo?.let { info -> it.toDomain(info) }
            }
        }
    }

    override suspend fun getLocationByIdOnce(id: String): GeofenceLocation? {
        val entity = geofenceLocationDao.getLocationByIdOnce(id) ?: return null
        val locationInfo = locationDao.getLocationById(entity.locationId)?.toDomain() ?: return null
        return entity.toDomain(locationInfo)
    }

    override suspend fun getByLocationId(locationId: String): GeofenceLocation? {
        val entity = geofenceLocationDao.getByLocationId(locationId) ?: return null
        val locationInfo = locationDao.getLocationById(entity.locationId)?.toDomain() ?: return null
        return entity.toDomain(locationInfo)
    }

    override fun getFrequentLocations(): Flow<List<GeofenceLocation>> {
        return geofenceLocationDao.getFrequentLocations().map { entities ->
            entities.mapNotNull { entity ->
                val locationInfo = locationDao.getLocationById(entity.locationId)?.toDomain()
                locationInfo?.let { entity.toDomain(it) }
            }
        }
    }

    override suspend fun getCount(): Int {
        return geofenceLocationDao.getCount()
    }

    override suspend fun getFrequentCount(): Int {
        return geofenceLocationDao.getFrequentCount()
    }

    // ========== 增删改操作 ==========

    override suspend fun insert(location: GeofenceLocation): Result<String> {
        return try {
            geofenceLocationDao.insert(location.toEntity())
            Result.success(location.id)
        } catch (e: Exception) {
            timber.log.Timber.tag("GeofenceLocation").e(e, "❌ 插入地理围栏地点失败")
            Result.failure(e)
        }
    }

    override suspend fun insertAll(locations: List<GeofenceLocation>) {
        geofenceLocationDao.insertAll(locations.map { it.toEntity() })
    }

    override suspend fun update(location: GeofenceLocation) {
        val updated = location.copy(updatedAt = LocalDateTime.now())
        geofenceLocationDao.update(updated.toEntity())
    }

    override suspend fun delete(location: GeofenceLocation): Result<Unit> {
        return try {
            geofenceLocationDao.delete(location.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            timber.log.Timber.tag("GeofenceLocation").e(e, "❌ 删除地理围栏地点失败")
            Result.failure(e)
        }
    }

    override suspend fun deleteById(id: String): Result<Unit> {
        return try {
            geofenceLocationDao.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            timber.log.Timber.tag("GeofenceLocation").e(e, "❌ 删除地理围栏地点失败")
            Result.failure(e)
        }
    }

    // ========== 使用统计操作 ==========

    override suspend fun incrementUsageCount(id: String) {
        val now = LocalDateTime.now()
        geofenceLocationDao.incrementUsageCount(
            id = id,
            lastUsed = now.toString(),
            updatedAt = now.toString()
        )
    }

    override suspend fun updateFrequent(id: String, isFrequent: Boolean) {
        geofenceLocationDao.updateFrequent(
            id = id,
            isFrequent = isFrequent,
            updatedAt = LocalDateTime.now().toString()
        )
    }

    override suspend fun updateFrequentBatch(ids: List<String>, isFrequent: Boolean) {
        if (ids.isEmpty()) return
        geofenceLocationDao.updateFrequentBatch(
            ids = ids,
            isFrequent = isFrequent,
            updatedAt = LocalDateTime.now().toString()
        )
    }

    override suspend fun updateFrequentLocations(): Int {
        val allLocations = getAllLocationsOnce()
        val thirtyDaysAgo = LocalDateTime.now().minusDays(30)

        // 计算哪些地点应该标记为常用
        val frequentIds = allLocations
            .filter { location ->
                location.usageCount >= 3 &&
                location.lastUsed != null &&
                location.lastUsed.isAfter(thirtyDaysAgo)
            }
            .map { it.id }

        // 计算哪些地点应该取消常用标记
        val nonFrequentIds = allLocations
            .filter { location ->
                location.isFrequent && // 当前是常用的
                (location.usageCount < 3 ||
                location.lastUsed == null ||
                location.lastUsed.isBefore(thirtyDaysAgo))
            }
            .map { it.id }

        // 批量更新
        if (frequentIds.isNotEmpty()) {
            updateFrequentBatch(frequentIds, true)
        }
        if (nonFrequentIds.isNotEmpty()) {
            updateFrequentBatch(nonFrequentIds, false)
        }

        val totalUpdated = frequentIds.size + nonFrequentIds.size
        if (totalUpdated > 0) {
            timber.log.Timber.tag("GeofenceLocation")
                .d("✅ 更新常用地点: ${frequentIds.size}个标记为常用, ${nonFrequentIds.size}个取消常用")
        }

        return totalUpdated
    }

    // ========== 月度统计操作 ==========

    override suspend fun incrementCheckStatistics(locationId: String, isHit: Boolean) {
        val currentMonth = java.time.YearMonth.now().toString() // 格式：YYYY-MM
        val updatedAt = LocalDateTime.now().toString()

        geofenceLocationDao.incrementCheckStatistics(
            id = locationId,
            isHit = isHit,
            currentMonth = currentMonth,
            updatedAt = updatedAt
        )

        timber.log.Timber.tag("GeofenceLocation")
            .d("📊 更新统计: locationId=$locationId, isHit=$isHit, month=$currentMonth")
    }

    override suspend fun resetMonthlyStatistics(): Int {
        val currentMonth = java.time.YearMonth.now().toString()
        val updatedAt = LocalDateTime.now().toString()

        val count = geofenceLocationDao.resetMonthlyStatistics(
            currentMonth = currentMonth,
            updatedAt = updatedAt
        )

        timber.log.Timber.tag("GeofenceLocation")
            .d("🔄 重置月度统计: 重置了 $count 个地点")

        return count
    }
}

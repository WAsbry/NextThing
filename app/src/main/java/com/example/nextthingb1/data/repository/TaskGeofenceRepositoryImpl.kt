package com.example.nextthingb1.data.repository

import com.example.nextthingb1.data.local.dao.GeofenceConfigDao
import com.example.nextthingb1.data.local.dao.GeofenceLocationDao
import com.example.nextthingb1.data.local.dao.TaskGeofenceDao
import com.example.nextthingb1.data.local.dao.LocationDao
import com.example.nextthingb1.data.mapper.toDomain
import com.example.nextthingb1.data.mapper.toEntity
import com.example.nextthingb1.domain.model.GeofenceCheckResult
import com.example.nextthingb1.domain.model.TaskGeofence
import com.example.nextthingb1.domain.repository.TaskGeofenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.combine
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskGeofenceRepositoryImpl @Inject constructor(
    private val taskGeofenceDao: TaskGeofenceDao,
    private val geofenceLocationDao: GeofenceLocationDao,
    private val locationDao: LocationDao,
    private val geofenceConfigDao: GeofenceConfigDao
) : TaskGeofenceRepository {

    // ========== 查询操作 ==========

    override fun getByTaskId(taskId: String): Flow<TaskGeofence?> {
        return taskGeofenceDao.getByTaskId(taskId).mapNotNull { entity ->
            entity?.let { taskGeofenceEntity ->
                // 查询 GeofenceLocation
                val geofenceLocationEntity = geofenceLocationDao.getLocationByIdOnce(taskGeofenceEntity.geofenceLocationId)
                if (geofenceLocationEntity != null) {
                    // 查询 LocationInfo
                    val locationEntity = locationDao.getLocationById(geofenceLocationEntity.locationId)
                    if (locationEntity != null) {
                        val geofenceLocation = geofenceLocationEntity.toDomain(locationEntity.toDomain())
                        taskGeofenceEntity.toDomain(geofenceLocation)
                    } else null
                } else null
            }
        }
    }

    override suspend fun getByTaskIdOnce(taskId: String): TaskGeofence? {
        val entity = taskGeofenceDao.getByTaskIdOnce(taskId) ?: return null

        // 查询 GeofenceLocation
        val geofenceLocationEntity = geofenceLocationDao.getLocationByIdOnce(entity.geofenceLocationId) ?: return null

        // 查询 LocationInfo
        val locationEntity = locationDao.getLocationById(geofenceLocationEntity.locationId) ?: return null

        val geofenceLocation = geofenceLocationEntity.toDomain(locationEntity.toDomain())
        return entity.toDomain(geofenceLocation)
    }

    override fun getByLocationId(locationId: String): Flow<List<TaskGeofence>> {
        return taskGeofenceDao.getByLocationId(locationId).map { entities ->
            entities.mapNotNull { entity ->
                // 查询 GeofenceLocation
                val geofenceLocationEntity = geofenceLocationDao.getLocationByIdOnce(entity.geofenceLocationId)
                if (geofenceLocationEntity != null) {
                    // 查询 LocationInfo
                    val locationEntity = locationDao.getLocationById(geofenceLocationEntity.locationId)
                    if (locationEntity != null) {
                        val geofenceLocation = geofenceLocationEntity.toDomain(locationEntity.toDomain())
                        entity.toDomain(geofenceLocation)
                    } else null
                } else null
            }
        }
    }

    override suspend fun getCountByLocationId(locationId: String): Int {
        return taskGeofenceDao.getCountByLocationId(locationId)
    }

    override fun getAllEnabled(): Flow<List<TaskGeofence>> {
        return taskGeofenceDao.getAllEnabled().map { entities ->
            entities.mapNotNull { entity ->
                // 查询 GeofenceLocation
                val geofenceLocationEntity = geofenceLocationDao.getLocationByIdOnce(entity.geofenceLocationId)
                if (geofenceLocationEntity != null) {
                    // 查询 LocationInfo
                    val locationEntity = locationDao.getLocationById(geofenceLocationEntity.locationId)
                    if (locationEntity != null) {
                        val geofenceLocation = geofenceLocationEntity.toDomain(locationEntity.toDomain())
                        entity.toDomain(geofenceLocation)
                    } else null
                } else null
            }
        }
    }

    override suspend fun isGeofenceEnabled(taskId: String): Boolean {
        return taskGeofenceDao.isGeofenceEnabled(taskId)
    }

    override suspend fun getCount(): Int {
        return taskGeofenceDao.getCount()
    }

    // ========== 增删改操作 ==========

    override suspend fun insert(taskGeofence: TaskGeofence): Result<String> {
        return try {
            taskGeofenceDao.insert(taskGeofence.toEntity())
            Result.success(taskGeofence.id)
        } catch (e: Exception) {
            timber.log.Timber.tag("TaskGeofence").e(e, "❌ 插入任务地理围栏关联失败")
            Result.failure(e)
        }
    }

    override suspend fun update(taskGeofence: TaskGeofence) {
        val updated = taskGeofence.copy(updatedAt = LocalDateTime.now())
        taskGeofenceDao.update(updated.toEntity())
    }

    override suspend fun delete(taskGeofence: TaskGeofence) {
        taskGeofenceDao.delete(taskGeofence.toEntity())
    }

    override suspend fun deleteByTaskId(taskId: String) {
        taskGeofenceDao.deleteByTaskId(taskId)
    }

    override suspend fun deleteByLocationId(locationId: String) {
        taskGeofenceDao.deleteByLocationId(locationId)
    }

    // ========== 状态更新操作 ==========

    override suspend fun updateEnabled(taskId: String, enabled: Boolean) {
        taskGeofenceDao.updateEnabled(
            taskId = taskId,
            enabled = enabled,
            updatedAt = LocalDateTime.now().toString()
        )
    }

    override suspend fun updateLastCheckResult(
        taskId: String,
        result: GeofenceCheckResult,
        distance: Double?,
        userLatitude: Double?,
        userLongitude: Double?
    ) {
        val now = LocalDateTime.now()
        taskGeofenceDao.updateLastCheckResult(
            taskId = taskId,
            checkTime = now.toString(),
            result = result.name,
            distance = distance,
            userLat = userLatitude,
            userLon = userLongitude,
            updatedAt = now.toString()
        )
    }

    // ========== 业务逻辑操作 ==========

    override suspend fun createTaskGeofence(
        taskId: String,
        geofenceLocationId: String
    ): Result<String> {
        return try {
            // 1. 查询地理围栏地点
            val geofenceLocationEntity = geofenceLocationDao.getLocationByIdOnce(geofenceLocationId)
                ?: return Result.failure(Exception("地理围栏地点不存在: $geofenceLocationId"))

            // 2. 查询 LocationInfo
            val locationEntity = locationDao.getLocationById(geofenceLocationEntity.locationId)
                ?: return Result.failure(Exception("位置信息不存在"))

            // 3. 获取有效半径（优先使用自定义半径，否则使用全局默认）
            val radius = if (geofenceLocationEntity.customRadius != null) {
                geofenceLocationEntity.customRadius
            } else {
                // 从全局配置获取默认半径
                val config = geofenceConfigDao.getConfigOnce()
                config?.defaultRadius ?: 200
            }

            // 4. 创建 GeofenceLocation domain 对象
            val geofenceLocation = geofenceLocationEntity.toDomain(locationEntity.toDomain())

            // 5. 创建任务地理围栏关联（记录半径快照）
            val taskGeofence = TaskGeofence(
                taskId = taskId,
                geofenceLocationId = geofenceLocationId,
                geofenceLocation = geofenceLocation,
                snapshotRadius = radius, // 快照值
                isEnabled = true
            )

            // 6. 插入数据库
            taskGeofenceDao.insert(taskGeofence.toEntity())

            // 7. 增加地点使用次数
            geofenceLocationDao.incrementUsageCount(
                id = geofenceLocationId,
                lastUsed = LocalDateTime.now().toString(),
                updatedAt = LocalDateTime.now().toString()
            )

            timber.log.Timber.tag("TaskGeofence")
                .d("✅ 创建任务地理围栏关联: taskId=$taskId, locationId=$geofenceLocationId, radius=$radius")

            Result.success(taskGeofence.id)
        } catch (e: Exception) {
            timber.log.Timber.tag("TaskGeofence").e(e, "❌ 创建任务地理围栏关联失败")
            Result.failure(e)
        }
    }

    override suspend fun getEffectiveRadius(taskId: String): Int? {
        // 查询任务的地理围栏关联
        val taskGeofence = taskGeofenceDao.getByTaskIdOnce(taskId) ?: return null

        // 返回快照半径
        return taskGeofence.radius
    }
}



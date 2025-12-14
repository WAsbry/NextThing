package com.example.nextthingb1.domain.usecase

import com.example.nextthingb1.domain.model.GeofenceConfig
import com.example.nextthingb1.domain.model.GeofenceLocation
import com.example.nextthingb1.domain.model.TaskGeofence
import com.example.nextthingb1.domain.repository.GeofenceConfigRepository
import com.example.nextthingb1.domain.repository.GeofenceLocationRepository
import com.example.nextthingb1.domain.repository.TaskGeofenceRepository
import com.example.nextthingb1.domain.service.GeofenceManager
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

/**
 * 地理围栏相关 UseCase 聚合类
 */
data class GeofenceUseCases @Inject constructor(
    val getGeofenceConfig: GetGeofenceConfigUseCase,
    val updateGeofenceConfig: UpdateGeofenceConfigUseCase,
    val getGeofenceLocations: GetGeofenceLocationsUseCase,
    val createGeofenceLocation: CreateGeofenceLocationUseCase,
    val updateGeofenceLocation: UpdateGeofenceLocationUseCase,
    val deleteGeofenceLocation: DeleteGeofenceLocationUseCase,
    val getTaskGeofence: GetTaskGeofenceUseCase,
    val createTaskGeofence: CreateTaskGeofenceUseCase,
    val updateFrequentLocations: UpdateFrequentLocationsUseCase,
    val updateLocationUsage: com.example.nextthingb1.domain.usecase.geofence.UpdateGeofenceLocationUsageUseCase,
    val updateTaskGeofenceCheckResult: com.example.nextthingb1.domain.usecase.geofence.UpdateTaskGeofenceCheckResultUseCase,
    val updateLocationCheckStatistics: com.example.nextthingb1.domain.usecase.geofence.UpdateLocationCheckStatisticsUseCase
)

// ========== 配置相关 UseCase ==========

/**
 * 获取全局地理围栏配置
 */
class GetGeofenceConfigUseCase @Inject constructor(
    private val configRepository: GeofenceConfigRepository
) {
    companion object {
        private const val TAG = "GeofenceConfig"
    }

    /**
     * 获取配置的 Flow,如果不存在则创建默认配置
     */
    operator fun invoke(): Flow<GeofenceConfig?> {
        return configRepository.getConfig()
    }

    /**
     * 获取配置(一次性),如果不存在则创建默认配置
     */
    suspend fun getOrDefault(): GeofenceConfig {
        return configRepository.getConfigOrDefault()
    }
}

/**
 * 更新全局地理围栏配置
 */
class UpdateGeofenceConfigUseCase @Inject constructor(
    private val configRepository: GeofenceConfigRepository
) {
    companion object {
        private const val TAG = "GeofenceConfig"
    }

    /**
     * 更新完整配置
     */
    suspend operator fun invoke(config: GeofenceConfig): Result<Unit> {
        return try {
            configRepository.updateConfig(config)
            Timber.tag(TAG).d("✅ 更新地理围栏配置成功")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 更新地理围栏配置失败")
            Result.failure(e)
        }
    }

    /**
     * 更新全局开关
     */
    suspend fun updateGlobalEnabled(enabled: Boolean): Result<Unit> {
        return try {
            configRepository.updateGlobalEnabled(enabled)
            Timber.tag(TAG).d("✅ 更新全局开关: $enabled")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 更新全局开关失败")
            Result.failure(e)
        }
    }

    /**
     * 更新默认半径
     */
    suspend fun updateDefaultRadius(radius: Int): Result<Unit> {
        return try {
            if (radius < 50 || radius > 5000) {
                return Result.failure(IllegalArgumentException("半径必须在 50-5000 米之间"))
            }
            configRepository.updateDefaultRadius(radius)
            Timber.tag(TAG).d("✅ 更新默认半径: ${radius}m")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 更新默认半径失败")
            Result.failure(e)
        }
    }

    /**
     * 更新位置精度阈值
     */
    suspend fun updateLocationAccuracyThreshold(threshold: Int): Result<Unit> {
        return try {
            if (threshold < 10 || threshold > 500) {
                return Result.failure(IllegalArgumentException("精度阈值必须在 10-500 米之间"))
            }
            configRepository.updateLocationAccuracyThreshold(threshold)
            Timber.tag(TAG).d("✅ 更新精度阈值: ${threshold}m")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 更新精度阈值失败")
            Result.failure(e)
        }
    }

    /**
     * 更新省电模式
     */
    suspend fun updateBatteryOptimization(enabled: Boolean): Result<Unit> {
        return try {
            configRepository.updateBatteryOptimization(enabled)
            Timber.tag(TAG).d("✅ 更新省电模式: $enabled")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 更新省电模式失败")
            Result.failure(e)
        }
    }

    /**
     * 更新离开地点时通知设置
     */
    suspend fun updateNotifyWhenOutside(enabled: Boolean): Result<Unit> {
        return try {
            configRepository.updateNotifyWhenOutside(enabled)
            Timber.tag(TAG).d("✅ 更新离开通知: $enabled")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 更新离开通知失败")
            Result.failure(e)
        }
    }
}

// ========== 地理围栏地点相关 UseCase ==========

/**
 * 获取地理围栏地点列表
 */
class GetGeofenceLocationsUseCase @Inject constructor(
    private val locationRepository: GeofenceLocationRepository
) {
    companion object {
        private const val TAG = "GeofenceLocation"
    }

    /**
     * 获取所有地点
     */
    operator fun invoke(): Flow<List<GeofenceLocation>> {
        return locationRepository.getAllLocations()
    }

    /**
     * 获取所有地点(一次性)
     */
    suspend fun getAllOnce(): List<GeofenceLocation> {
        return locationRepository.getAllLocationsOnce()
    }

    /**
     * 根据 ID 获取地点
     */
    fun getById(id: String): Flow<GeofenceLocation?> {
        return locationRepository.getLocationById(id)
    }

    /**
     * 根据 ID 获取地点(一次性)
     */
    suspend fun getByIdOnce(id: String): GeofenceLocation? {
        return locationRepository.getLocationByIdOnce(id)
    }

    /**
     * 获取常用地点
     */
    fun getFrequent(): Flow<List<GeofenceLocation>> {
        return locationRepository.getFrequentLocations()
    }

    /**
     * 根据 locationId 获取地点
     */
    suspend fun getByLocationId(locationId: String): GeofenceLocation? {
        return locationRepository.getByLocationId(locationId)
    }
}

/**
 * 创建地理围栏地点
 */
class CreateGeofenceLocationUseCase @Inject constructor(
    private val locationRepository: GeofenceLocationRepository,
    private val geofenceManager: GeofenceManager,
    private val configRepository: GeofenceConfigRepository
) {
    companion object {
        private const val TAG = "GeofenceLocation"
    }

    /**
     * 创建新的地理围栏地点
     *
     * @param location GeofenceLocation 对象
     * @return Result<String> 成功返回地点 ID,失败返回异常
     */
    suspend operator fun invoke(location: GeofenceLocation): Result<String> {
        return try {
            // 验证半径范围
            if (location.customRadius != null) {
                if (location.customRadius < 50 || location.customRadius > 5000) {
                    return Result.failure(IllegalArgumentException("自定义半径必须在 50-5000 米之间"))
                }
            }

            // 1. 保存到数据库
            val result = locationRepository.insert(location)

            if (result.isFailure) {
                Timber.tag(TAG).e("❌ 创建地理围栏地点失败: ${result.exceptionOrNull()?.message}")
                return result
            }

            Timber.tag(TAG).d("✅ 创建地理围栏地点成功: ${location.id}")

            // 2. 注册到系统地理围栏
            val config = configRepository.getConfigOrDefault()
            val radius = (location.customRadius ?: config.defaultRadius).toFloat()

            val registerResult = geofenceManager.registerGeofence(
                locationId = location.locationInfo.id,
                latitude = location.locationInfo.latitude,
                longitude = location.locationInfo.longitude,
                radius = radius
            )

            if (registerResult.isSuccess) {
                Timber.tag(TAG).d("✅ 系统地理围栏注册成功")
            } else {
                Timber.tag(TAG).w("⚠️ 系统地理围栏注册失败: ${registerResult.exceptionOrNull()?.message}")
                // 注意: 即使系统围栏注册失败，数据库记录已保存，不回滚
            }

            result
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 创建地理围栏地点异常")
            Result.failure(e)
        }
    }
}

/**
 * 更新地理围栏地点
 */
class UpdateGeofenceLocationUseCase @Inject constructor(
    private val locationRepository: GeofenceLocationRepository,
    private val geofenceManager: GeofenceManager,
    private val configRepository: GeofenceConfigRepository
) {
    companion object {
        private const val TAG = "GeofenceLocation"
    }

    /**
     * 更新地点信息
     */
    suspend operator fun invoke(location: GeofenceLocation): Result<Unit> {
        return try {
            // 验证半径范围
            if (location.customRadius != null) {
                if (location.customRadius < 50 || location.customRadius > 5000) {
                    return Result.failure(IllegalArgumentException("自定义半径必须在 50-5000 米之间"))
                }
            }

            // 获取旧的地点信息,检查半径是否改变
            val oldLocation = locationRepository.getLocationByIdOnce(location.id)
            val radiusChanged = oldLocation?.let {
                val oldRadius = it.customRadius ?: configRepository.getConfigOrDefault().defaultRadius
                val newRadius = location.customRadius ?: configRepository.getConfigOrDefault().defaultRadius
                oldRadius != newRadius
            } ?: false

            // 1. 更新数据库
            locationRepository.update(location)
            Timber.tag(TAG).d("✅ 更新地理围栏地点成功: ${location.id}")

            // 2. 如果半径改变,需要重新注册系统地理围栏
            if (radiusChanged) {
                Timber.tag(TAG).d("📍 半径已改变,重新注册系统地理围栏")

                val config = configRepository.getConfigOrDefault()
                val radius = (location.customRadius ?: config.defaultRadius).toFloat()

                val registerResult = geofenceManager.registerGeofence(
                    locationId = location.locationInfo.id,
                    latitude = location.locationInfo.latitude,
                    longitude = location.locationInfo.longitude,
                    radius = radius
                )

                if (registerResult.isSuccess) {
                    Timber.tag(TAG).d("✅ 系统地理围栏重新注册成功")
                } else {
                    Timber.tag(TAG).w("⚠️ 系统地理围栏重新注册失败: ${registerResult.exceptionOrNull()?.message}")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 更新地理围栏地点失败")
            Result.failure(e)
        }
    }

    /**
     * 更新地点的常用标记
     */
    suspend fun updateFrequent(id: String, isFrequent: Boolean): Result<Unit> {
        return try {
            locationRepository.updateFrequent(id, isFrequent)
            Timber.tag(TAG).d("✅ 更新常用标记: $id -> $isFrequent")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 更新常用标记失败")
            Result.failure(e)
        }
    }

    /**
     * 增加使用次数
     */
    suspend fun incrementUsageCount(id: String): Result<Unit> {
        return try {
            locationRepository.incrementUsageCount(id)
            Timber.tag(TAG).d("✅ 增加使用次数: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 增加使用次数失败")
            Result.failure(e)
        }
    }
}

/**
 * 删除地理围栏地点
 */
class DeleteGeofenceLocationUseCase @Inject constructor(
    private val locationRepository: GeofenceLocationRepository,
    private val taskGeofenceRepository: TaskGeofenceRepository,
    private val geofenceManager: GeofenceManager
) {
    companion object {
        private const val TAG = "GeofenceLocation"
    }

    /**
     * 删除地点
     *
     * 注意:
     * - 会自动级联删除关联的任务地理围栏(数据库外键配置)
     * - 只有当这是最后一个使用该LocationInfo的GeofenceLocation时,才注销系统地理围栏
     */
    suspend operator fun invoke(locationId: String): Result<Unit> {
        return try {
            // 1. 获取要删除的地点信息
            val geofenceLocation = locationRepository.getLocationByIdOnce(locationId)
            if (geofenceLocation == null) {
                Timber.tag(TAG).e("❌ 地点不存在: $locationId")
                return Result.failure(IllegalArgumentException("地点不存在"))
            }

            // 2. 检查是否有任务使用此地点
            val usageCount = taskGeofenceRepository.getCountByLocationId(locationId)
            if (usageCount > 0) {
                Timber.tag(TAG).w("⚠️ 删除地点将影响 $usageCount 个任务的地理围栏")
            }

            // 3. 检查是否有其他 GeofenceLocation 使用相同的 LocationInfo
            val allLocations = locationRepository.getAllLocationsOnce()
            val otherLocationsUsingSameLocationInfo = allLocations.count {
                it.locationInfo.id == geofenceLocation.locationInfo.id && it.id != locationId
            }

            // 4. 只有当这是最后一个使用该 LocationInfo 的 GeofenceLocation 时,才注销系统地理围栏
            if (otherLocationsUsingSameLocationInfo == 0) {
                Timber.tag(TAG).d("📍 这是最后一个使用 LocationInfo(${geofenceLocation.locationInfo.id}) 的地点,注销系统地理围栏")

                val removeResult = geofenceManager.removeGeofence(geofenceLocation.locationInfo.id)
                if (removeResult.isSuccess) {
                    Timber.tag(TAG).d("✅ 系统地理围栏已移除")
                } else {
                    Timber.tag(TAG).w("⚠️ 系统地理围栏移除失败: ${removeResult.exceptionOrNull()?.message}")
                    // 继续删除数据库记录
                }
            } else {
                Timber.tag(TAG).d("📍 还有 $otherLocationsUsingSameLocationInfo 个地点使用相同的 LocationInfo,保留系统地理围栏")
            }

            // 5. 从数据库删除
            val result = locationRepository.deleteById(locationId)

            if (result.isSuccess) {
                Timber.tag(TAG).d("✅ 删除地理围栏地点成功")
            } else {
                Timber.tag(TAG).e("❌ 删除地理围栏地点失败: ${result.exceptionOrNull()?.message}")
            }

            result
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 删除地理围栏地点异常")
            Result.failure(e)
        }
    }
}

// ========== 任务地理围栏关联相关 UseCase ==========

/**
 * 获取任务的地理围栏关联
 */
class GetTaskGeofenceUseCase @Inject constructor(
    private val taskGeofenceRepository: TaskGeofenceRepository
) {
    companion object {
        private const val TAG = "TaskGeofence"
    }

    /**
     * 根据任务 ID 获取地理围栏关联
     */
    operator fun invoke(taskId: String): Flow<TaskGeofence?> {
        return taskGeofenceRepository.getByTaskId(taskId)
    }

    /**
     * 根据任务 ID 获取地理围栏关联(一次性)
     */
    suspend fun getByTaskIdOnce(taskId: String): TaskGeofence? {
        return taskGeofenceRepository.getByTaskIdOnce(taskId)
    }

    /**
     * 根据地点 ID 获取所有关联的任务地理围栏
     */
    fun getByLocationId(locationId: String): Flow<List<TaskGeofence>> {
        return taskGeofenceRepository.getByLocationId(locationId)
    }

    /**
     * 获取所有已启用的任务地理围栏
     */
    fun getAllEnabled(): Flow<List<TaskGeofence>> {
        return taskGeofenceRepository.getAllEnabled()
    }

    /**
     * 检查任务是否启用了地理围栏
     */
    suspend fun isEnabled(taskId: String): Boolean {
        return taskGeofenceRepository.isGeofenceEnabled(taskId)
    }

    /**
     * 获取任务的有效半径
     */
    suspend fun getEffectiveRadius(taskId: String): Int? {
        return taskGeofenceRepository.getEffectiveRadius(taskId)
    }
}

/**
 * 创建任务地理围栏关联
 */
class CreateTaskGeofenceUseCase @Inject constructor(
    private val taskGeofenceRepository: TaskGeofenceRepository,
    private val locationRepository: GeofenceLocationRepository
) {
    companion object {
        private const val TAG = "TaskGeofence"
    }

    /**
     * 为任务创建地理围栏关联
     *
     * @param taskId 任务 ID
     * @param geofenceLocationId 地理围栏地点 ID
     * @return Result<String> 成功返回关联 ID,失败返回异常
     */
    suspend operator fun invoke(
        taskId: String,
        geofenceLocationId: String
    ): Result<String> {
        return try {
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.tag(TAG).d("【UseCase】CreateTaskGeofenceUseCase 开始执行")
            Timber.tag(TAG).d("  taskId: $taskId")
            Timber.tag(TAG).d("  geofenceLocationId: $geofenceLocationId")

            // 检查任务是否已有地理围栏
            val existing = taskGeofenceRepository.getByTaskIdOnce(taskId)
            if (existing != null) {
                Timber.tag(TAG).w("⚠️ 任务已有地理围栏,需先删除旧的关联")
                return Result.failure(IllegalStateException("任务已设置地理围栏"))
            }

            // 检查地点是否存在
            val location = locationRepository.getLocationByIdOnce(geofenceLocationId)
            if (location == null) {
                Timber.tag(TAG).e("❌ 地理围栏地点不存在: $geofenceLocationId")
                return Result.failure(IllegalArgumentException("地理围栏地点不存在"))
            }

            // 调用 Repository 创建关联(包含半径快照逻辑)
            val result = taskGeofenceRepository.createTaskGeofence(taskId, geofenceLocationId)

            if (result.isSuccess) {
                Timber.tag(TAG).d("✅ CreateTaskGeofenceUseCase 执行完成")
                Timber.tag(TAG).d("  关联ID: ${result.getOrNull()}")
            } else {
                Timber.tag(TAG).e("❌ 创建失败: ${result.exceptionOrNull()?.message}")
            }
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            result
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 创建任务地理围栏关联异常")
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Result.failure(e)
        }
    }

    /**
     * 更新任务地理围栏的启用状态
     */
    suspend fun updateEnabled(taskId: String, enabled: Boolean): Result<Unit> {
        return try {
            taskGeofenceRepository.updateEnabled(taskId, enabled)
            Timber.tag(TAG).d("✅ 更新地理围栏启用状态: $taskId -> $enabled")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 更新启用状态失败")
            Result.failure(e)
        }
    }

    /**
     * 删除任务的地理围栏关联
     */
    suspend fun deleteByTaskId(taskId: String): Result<Unit> {
        return try {
            taskGeofenceRepository.deleteByTaskId(taskId)
            Timber.tag(TAG).d("✅ 删除任务地理围栏关联: $taskId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 删除关联失败")
            Result.failure(e)
        }
    }
}

// ========== 业务逻辑相关 UseCase ==========

/**
 * 更新常用地点标记
 *
 * 根据使用频率自动标记/取消标记常用地点:
 * - 使用次数 >= 3 且 30 天内使用过 -> 标记为常用
 * - 不满足条件 -> 取消常用标记
 */
class UpdateFrequentLocationsUseCase @Inject constructor(
    private val locationRepository: GeofenceLocationRepository
) {
    companion object {
        private const val TAG = "GeofenceLocation"
    }

    /**
     * 执行常用地点更新
     *
     * @return Result<Int> 成功返回更新的地点数量,失败返回异常
     */
    suspend operator fun invoke(): Result<Int> {
        return try {
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.tag(TAG).d("开始更新常用地点标记")

            val updatedCount = locationRepository.updateFrequentLocations()

            Timber.tag(TAG).d("✅ 常用地点更新完成")
            Timber.tag(TAG).d("  更新数量: $updatedCount")
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            Result.success(updatedCount)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 更新常用地点失败")
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Result.failure(e)
        }
    }
}

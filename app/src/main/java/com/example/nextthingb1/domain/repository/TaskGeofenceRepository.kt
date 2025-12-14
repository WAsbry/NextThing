package com.example.nextthingb1.domain.repository

import com.example.nextthingb1.domain.model.GeofenceCheckResult
import com.example.nextthingb1.domain.model.TaskGeofence
import kotlinx.coroutines.flow.Flow

/**
 * 任务地理围栏关联仓库接口
 *
 * 管理任务与地理围栏地点的关联关系
 */
interface TaskGeofenceRepository {

    // ========== 查询操作 ==========

    /**
     * 根据任务ID获取地理围栏关联（Flow）
     * @param taskId 任务ID
     * @return 关联对象的 Flow，可能为 null
     */
    fun getByTaskId(taskId: String): Flow<TaskGeofence?>

    /**
     * 根据任务ID获取地理围栏关联（一次性查询）
     * @param taskId 任务ID
     * @return 关联对象，可能为 null
     */
    suspend fun getByTaskIdOnce(taskId: String): TaskGeofence?

    /**
     * 根据地理围栏地点ID获取所有关联的任务
     * @param locationId 地理围栏地点ID
     * @return 关联列表的 Flow
     */
    fun getByLocationId(locationId: String): Flow<List<TaskGeofence>>

    /**
     * 根据地理围栏地点ID获取关联任务数量
     * @param locationId 地理围栏地点ID
     * @return 关联任务数量
     */
    suspend fun getCountByLocationId(locationId: String): Int

    /**
     * 获取所有启用的任务地理围栏
     * @return 启用的关联列表 Flow
     */
    fun getAllEnabled(): Flow<List<TaskGeofence>>

    /**
     * 检查任务是否启用了地理围栏
     * @param taskId 任务ID
     * @return true = 已启用，false = 未启用
     */
    suspend fun isGeofenceEnabled(taskId: String): Boolean

    /**
     * 获取所有任务地理围栏数量
     * @return 总数量
     */
    suspend fun getCount(): Int

    // ========== 增删改操作 ==========

    /**
     * 创建任务地理围栏关联
     * @param taskGeofence 关联对象
     * @return Result.success(关联ID) 或 Result.failure(异常)
     */
    suspend fun insert(taskGeofence: TaskGeofence): Result<String>

    /**
     * 更新任务地理围栏关联
     * @param taskGeofence 关联对象
     */
    suspend fun update(taskGeofence: TaskGeofence)

    /**
     * 删除任务地理围栏关联
     * @param taskGeofence 关联对象
     */
    suspend fun delete(taskGeofence: TaskGeofence)

    /**
     * 根据任务ID删除关联
     * @param taskId 任务ID
     */
    suspend fun deleteByTaskId(taskId: String)

    /**
     * 根据地理围栏地点ID删除所有关联
     * @param locationId 地理围栏地点ID
     */
    suspend fun deleteByLocationId(locationId: String)

    // ========== 状态更新操作 ==========

    /**
     * 更新启用状态
     * @param taskId 任务ID
     * @param enabled 是否启用
     */
    suspend fun updateEnabled(taskId: String, enabled: Boolean)

    /**
     * 更新最后检查结果
     * @param taskId 任务ID
     * @param result 检查结果枚举
     * @param distance 距离（米），可选
     * @param userLatitude 用户纬度，可选
     * @param userLongitude 用户经度，可选
     */
    suspend fun updateLastCheckResult(
        taskId: String,
        result: GeofenceCheckResult,
        distance: Double? = null,
        userLatitude: Double? = null,
        userLongitude: Double? = null
    )

    // ========== 业务逻辑操作 ==========

    /**
     * 为任务创建地理围栏关联（业务方法）
     * 自动获取地点的半径作为快照值
     *
     * @param taskId 任务ID
     * @param geofenceLocationId 地理围栏地点ID
     * @return Result.success(关联ID) 或 Result.failure(异常)
     */
    suspend fun createTaskGeofence(
        taskId: String,
        geofenceLocationId: String
    ): Result<String>

    /**
     * 获取任务的有效围栏半径
     * 优先级：TaskGeofence.radius（快照值）
     *
     * @param taskId 任务ID
     * @return 半径值（米），如果任务未关联地理围栏则返回 null
     */
    suspend fun getEffectiveRadius(taskId: String): Int?
}

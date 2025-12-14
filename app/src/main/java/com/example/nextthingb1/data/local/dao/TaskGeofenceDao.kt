package com.example.nextthingb1.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nextthingb1.data.local.entity.TaskGeofenceEntity
import kotlinx.coroutines.flow.Flow

/**
 * 任务地理围栏关联 DAO
 */
@Dao
interface TaskGeofenceDao {

    /**
     * 根据任务ID获取地理围栏关联（Flow）
     */
    @Query("SELECT * FROM task_geofences WHERE taskId = :taskId")
    fun getByTaskId(taskId: String): Flow<TaskGeofenceEntity?>

    /**
     * 根据任务ID获取地理围栏关联（一次性查询）
     */
    @Query("SELECT * FROM task_geofences WHERE taskId = :taskId")
    suspend fun getByTaskIdOnce(taskId: String): TaskGeofenceEntity?

    /**
     * 根据地理围栏地点ID获取所有关联的任务
     */
    @Query("SELECT * FROM task_geofences WHERE geofenceLocationId = :locationId")
    fun getByLocationId(locationId: String): Flow<List<TaskGeofenceEntity>>

    /**
     * 根据地理围栏地点ID获取关联任务数量
     */
    @Query("SELECT COUNT(*) FROM task_geofences WHERE geofenceLocationId = :locationId")
    suspend fun getCountByLocationId(locationId: String): Int

    /**
     * 获取所有启用的任务地理围栏
     */
    @Query("SELECT * FROM task_geofences WHERE enabled = 1")
    fun getAllEnabled(): Flow<List<TaskGeofenceEntity>>

    /**
     * 插入任务地理围栏关联
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(taskGeofence: TaskGeofenceEntity)

    /**
     * 更新任务地理围栏关联
     */
    @Update
    suspend fun update(taskGeofence: TaskGeofenceEntity)

    /**
     * 删除任务地理围栏关联
     */
    @Delete
    suspend fun delete(taskGeofence: TaskGeofenceEntity)

    /**
     * 根据任务ID删除关联
     */
    @Query("DELETE FROM task_geofences WHERE taskId = :taskId")
    suspend fun deleteByTaskId(taskId: String)

    /**
     * 根据地理围栏地点ID删除所有关联
     */
    @Query("DELETE FROM task_geofences WHERE geofenceLocationId = :locationId")
    suspend fun deleteByLocationId(locationId: String)

    /**
     * 更新启用状态
     */
    @Query("UPDATE task_geofences SET enabled = :enabled, updatedAt = :updatedAt WHERE taskId = :taskId")
    suspend fun updateEnabled(taskId: String, enabled: Boolean, updatedAt: String)

    /**
     * 更新最后检查结果
     */
    @Query("""
        UPDATE task_geofences
        SET lastCheckTime = :checkTime,
            lastCheckResult = :result,
            lastCheckDistance = :distance,
            lastCheckUserLatitude = :userLat,
            lastCheckUserLongitude = :userLon,
            updatedAt = :updatedAt
        WHERE taskId = :taskId
    """)
    suspend fun updateLastCheckResult(
        taskId: String,
        checkTime: String,
        result: String,
        distance: Double?,
        userLat: Double?,
        userLon: Double?,
        updatedAt: String
    )

    /**
     * 检查任务是否启用了地理围栏
     */
    @Query("SELECT EXISTS(SELECT 1 FROM task_geofences WHERE taskId = :taskId AND enabled = 1)")
    suspend fun isGeofenceEnabled(taskId: String): Boolean

    /**
     * 获取所有任务地理围栏数量
     */
    @Query("SELECT COUNT(*) FROM task_geofences")
    suspend fun getCount(): Int
}

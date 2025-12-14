package com.example.nextthingb1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nextthingb1.data.local.entity.GeofenceConfigEntity
import kotlinx.coroutines.flow.Flow

/**
 * 地理围栏全局配置 DAO
 *
 * 这是一个单例表，只有一条记录（id = "default"）
 */
@Dao
interface GeofenceConfigDao {

    /**
     * 获取全局配置（Flow，自动监听变化）
     */
    @Query("SELECT * FROM geofence_config WHERE id = 'default' LIMIT 1")
    fun getConfig(): Flow<GeofenceConfigEntity?>

    /**
     * 获取全局配置（一次性查询）
     */
    @Query("SELECT * FROM geofence_config WHERE id = 'default' LIMIT 1")
    suspend fun getConfigOnce(): GeofenceConfigEntity?

    /**
     * 插入或替换配置
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(config: GeofenceConfigEntity)

    /**
     * 更新配置
     */
    @Update
    suspend fun update(config: GeofenceConfigEntity)

    /**
     * 更新全局开关
     */
    @Query("UPDATE geofence_config SET isGlobalEnabled = :enabled, updatedAt = :updatedAt WHERE id = 'default'")
    suspend fun updateGlobalEnabled(enabled: Boolean, updatedAt: String)

    /**
     * 更新默认半径
     */
    @Query("UPDATE geofence_config SET defaultRadius = :radius, updatedAt = :updatedAt WHERE id = 'default'")
    suspend fun updateDefaultRadius(radius: Int, updatedAt: String)
}

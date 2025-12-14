package com.example.nextthingb1.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.nextthingb1.data.local.entity.GeofenceLocationStatisticsHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 地理围栏地点月度统计历史 DAO
 */
@Dao
interface GeofenceLocationStatisticsHistoryDao {

    /**
     * 插入历史统计记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: GeofenceLocationStatisticsHistoryEntity)

    /**
     * 批量插入历史统计记录
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<GeofenceLocationStatisticsHistoryEntity>)

    /**
     * 获取指定地点的所有历史统计（按月份降序）
     */
    @Query("""
        SELECT * FROM geofence_location_statistics_history
        WHERE geofenceLocationId = :geofenceLocationId
        ORDER BY month DESC
    """)
    fun getHistoryByLocationId(geofenceLocationId: String): Flow<List<GeofenceLocationStatisticsHistoryEntity>>

    /**
     * 获取指定地点的所有历史统计（一次性查询）
     */
    @Query("""
        SELECT * FROM geofence_location_statistics_history
        WHERE geofenceLocationId = :geofenceLocationId
        ORDER BY month DESC
    """)
    suspend fun getHistoryByLocationIdOnce(geofenceLocationId: String): List<GeofenceLocationStatisticsHistoryEntity>

    /**
     * 获取指定地点指定月份的历史统计
     */
    @Query("""
        SELECT * FROM geofence_location_statistics_history
        WHERE geofenceLocationId = :geofenceLocationId AND month = :month
    """)
    suspend fun getHistoryByLocationAndMonth(
        geofenceLocationId: String,
        month: String
    ): GeofenceLocationStatisticsHistoryEntity?

    /**
     * 获取所有地点最近N个月的统计历史
     */
    @Query("""
        SELECT * FROM geofence_location_statistics_history
        ORDER BY month DESC
        LIMIT :limit
    """)
    fun getRecentHistory(limit: Int = 12): Flow<List<GeofenceLocationStatisticsHistoryEntity>>

    /**
     * 删除指定地点的所有历史统计
     */
    @Query("DELETE FROM geofence_location_statistics_history WHERE geofenceLocationId = :geofenceLocationId")
    suspend fun deleteByLocationId(geofenceLocationId: String)

    /**
     * 删除指定月份之前的所有历史数据
     * @param beforeMonth 格式：YYYY-MM
     */
    @Query("DELETE FROM geofence_location_statistics_history WHERE month < :beforeMonth")
    suspend fun deleteBeforeMonth(beforeMonth: String): Int

    /**
     * 获取历史记录数量
     */
    @Query("SELECT COUNT(*) FROM geofence_location_statistics_history")
    suspend fun getCount(): Int
}

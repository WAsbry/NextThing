package com.example.nextthingb1.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nextthingb1.data.local.entity.GeofenceLocationEntity
import kotlinx.coroutines.flow.Flow

/**
 * 地理围栏地点 DAO
 */
@Dao
interface GeofenceLocationDao {

    /**
     * 获取所有地理围栏地点（Flow，自动监听变化）
     * 按常用标记和使用次数排序
     */
    @Query("""
        SELECT * FROM geofence_locations
        ORDER BY isFrequent DESC, usageCount DESC, createdAt DESC
    """)
    fun getAllLocations(): Flow<List<GeofenceLocationEntity>>

    /**
     * 获取所有地理围栏地点（一次性查询）
     */
    @Query("SELECT * FROM geofence_locations ORDER BY isFrequent DESC, usageCount DESC")
    suspend fun getAllLocationsOnce(): List<GeofenceLocationEntity>

    /**
     * 根据ID获取地理围栏地点（Flow）
     */
    @Query("SELECT * FROM geofence_locations WHERE id = :id")
    fun getLocationById(id: String): Flow<GeofenceLocationEntity?>

    /**
     * 根据ID获取地理围栏地点（一次性查询）
     */
    @Query("SELECT * FROM geofence_locations WHERE id = :id")
    suspend fun getLocationByIdOnce(id: String): GeofenceLocationEntity?

    /**
     * 根据 locationId 获取地理围栏地点
     */
    @Query("SELECT * FROM geofence_locations WHERE locationId = :locationId")
    suspend fun getByLocationId(locationId: String): GeofenceLocationEntity?

    /**
     * 获取所有常用地点
     */
    @Query("SELECT * FROM geofence_locations WHERE isFrequent = 1 ORDER BY usageCount DESC")
    fun getFrequentLocations(): Flow<List<GeofenceLocationEntity>>

    /**
     * 插入地理围栏地点
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(location: GeofenceLocationEntity)

    /**
     * 批量插入地理围栏地点
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<GeofenceLocationEntity>)

    /**
     * 更新地理围栏地点
     */
    @Update
    suspend fun update(location: GeofenceLocationEntity)

    /**
     * 删除地理围栏地点
     */
    @Delete
    suspend fun delete(location: GeofenceLocationEntity)

    /**
     * 根据ID删除地理围栏地点
     */
    @Query("DELETE FROM geofence_locations WHERE id = :id")
    suspend fun deleteById(id: String)

    /**
     * 增加使用次数并更新最后使用时间
     */
    @Query("""
        UPDATE geofence_locations
        SET usageCount = usageCount + 1,
            lastUsed = :lastUsed,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun incrementUsageCount(id: String, lastUsed: String, updatedAt: String)

    /**
     * 更新常用标记
     */
    @Query("UPDATE geofence_locations SET isFrequent = :isFrequent, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFrequent(id: String, isFrequent: Boolean, updatedAt: String)

    /**
     * 批量更新常用标记
     */
    @Query("UPDATE geofence_locations SET isFrequent = :isFrequent, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun updateFrequentBatch(ids: List<String>, isFrequent: Boolean, updatedAt: String)

    /**
     * 获取地点数量
     */
    @Query("SELECT COUNT(*) FROM geofence_locations")
    suspend fun getCount(): Int

    /**
     * 获取常用地点数量
     */
    @Query("SELECT COUNT(*) FROM geofence_locations WHERE isFrequent = 1")
    suspend fun getFrequentCount(): Int

    // ========== 统计相关方法 ==========

    /**
     * 增加地点的月度检查次数
     *
     * @param id 地点ID
     * @param isHit 是否命中（用户在围栏内）
     * @param currentMonth 当前月份（格式：YYYY-MM）
     * @param updatedAt 更新时间
     */
    @Query("""
        UPDATE geofence_locations
        SET monthlyCheckCount = CASE
                WHEN lastStatisticsResetMonth = :currentMonth THEN monthlyCheckCount + 1
                ELSE 1
            END,
            monthlyHitCount = CASE
                WHEN lastStatisticsResetMonth = :currentMonth THEN
                    CASE WHEN :isHit = 1 THEN monthlyHitCount + 1 ELSE monthlyHitCount END
                ELSE
                    CASE WHEN :isHit = 1 THEN 1 ELSE 0 END
            END,
            lastStatisticsResetMonth = :currentMonth,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun incrementCheckStatistics(
        id: String,
        isHit: Boolean,
        currentMonth: String,
        updatedAt: String
    )

    /**
     * 重置所有地点的月度统计
     * 用于每月1号批量重置
     *
     * @param currentMonth 当前月份（格式：YYYY-MM）
     * @param updatedAt 更新时间
     */
    @Query("""
        UPDATE geofence_locations
        SET monthlyCheckCount = 0,
            monthlyHitCount = 0,
            lastStatisticsResetMonth = :currentMonth,
            updatedAt = :updatedAt
        WHERE lastStatisticsResetMonth != :currentMonth OR lastStatisticsResetMonth IS NULL
    """)
    suspend fun resetMonthlyStatistics(currentMonth: String, updatedAt: String): Int
}

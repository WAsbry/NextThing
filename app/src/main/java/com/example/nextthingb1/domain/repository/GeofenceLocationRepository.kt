package com.example.nextthingb1.domain.repository

import com.example.nextthingb1.domain.model.GeofenceLocation
import kotlinx.coroutines.flow.Flow

/**
 * 地理围栏地点仓库接口
 *
 * 管理可复用的地理围栏地点配置
 */
interface GeofenceLocationRepository {

    // ========== 查询操作 ==========

    /**
     * 获取所有地理围栏地点（Flow，自动监听变化）
     * 按常用标记和使用次数排序
     * @return 地点列表的 Flow
     */
    fun getAllLocations(): Flow<List<GeofenceLocation>>

    /**
     * 获取所有地理围栏地点（一次性查询）
     * @return 地点列表
     */
    suspend fun getAllLocationsOnce(): List<GeofenceLocation>

    /**
     * 根据ID获取地理围栏地点（Flow）
     * @param id 地理围栏地点ID
     * @return 地点的 Flow，可能为 null
     */
    fun getLocationById(id: String): Flow<GeofenceLocation?>

    /**
     * 根据ID获取地理围栏地点（一次性查询）
     * @param id 地理围栏地点ID
     * @return 地点对象，可能为 null
     */
    suspend fun getLocationByIdOnce(id: String): GeofenceLocation?

    /**
     * 根据原始地点ID获取地理围栏地点
     * @param locationId 原始地点ID（LocationInfo.id）
     * @return 地点对象，可能为 null
     */
    suspend fun getByLocationId(locationId: String): GeofenceLocation?

    /**
     * 获取所有常用地点（Flow）
     * @return 常用地点列表的 Flow
     */
    fun getFrequentLocations(): Flow<List<GeofenceLocation>>

    /**
     * 获取地点数量
     * @return 总数量
     */
    suspend fun getCount(): Int

    /**
     * 获取常用地点数量
     * @return 常用地点数量
     */
    suspend fun getFrequentCount(): Int

    // ========== 增删改操作 ==========

    /**
     * 新增地理围栏地点
     * @param location 地点对象
     * @return Result.success(地点ID) 或 Result.failure(异常)
     */
    suspend fun insert(location: GeofenceLocation): Result<String>

    /**
     * 批量新增地理围栏地点
     * @param locations 地点列表
     */
    suspend fun insertAll(locations: List<GeofenceLocation>)

    /**
     * 更新地理围栏地点
     * @param location 地点对象
     */
    suspend fun update(location: GeofenceLocation)

    /**
     * 删除地理围栏地点
     * @param location 地点对象
     * @return Result.success(Unit) 或 Result.failure(异常)
     */
    suspend fun delete(location: GeofenceLocation): Result<Unit>

    /**
     * 根据ID删除地理围栏地点
     * @param id 地理围栏地点ID
     * @return Result.success(Unit) 或 Result.failure(异常)
     */
    suspend fun deleteById(id: String): Result<Unit>

    // ========== 使用统计操作 ==========

    /**
     * 增加使用次数并更新最后使用时间
     * @param id 地理围栏地点ID
     */
    suspend fun incrementUsageCount(id: String)

    /**
     * 更新常用标记
     * @param id 地理围栏地点ID
     * @param isFrequent 是否为常用地点
     */
    suspend fun updateFrequent(id: String, isFrequent: Boolean)

    /**
     * 批量更新常用标记
     * @param ids 地理围栏地点ID列表
     * @param isFrequent 是否为常用地点
     */
    suspend fun updateFrequentBatch(ids: List<String>, isFrequent: Boolean)

    /**
     * 自动更新常用地点标记
     * 规则：使用次数 >= 3 且最近30天使用过
     * @return 更新的地点数量
     */
    suspend fun updateFrequentLocations(): Int

    // ========== 月度统计操作 ==========

    /**
     * 更新地点的月度检查统计
     *
     * 每次地理围栏检查时调用，自动处理跨月重置逻辑
     *
     * @param locationId 地理围栏地点ID
     * @param isHit 是否命中（用户在围栏内）
     */
    suspend fun incrementCheckStatistics(locationId: String, isHit: Boolean)

    /**
     * 重置所有地点的月度统计
     *
     * 用于每月1号批量重置，或手动重置统计数据
     *
     * @return 重置的地点数量
     */
    suspend fun resetMonthlyStatistics(): Int
}

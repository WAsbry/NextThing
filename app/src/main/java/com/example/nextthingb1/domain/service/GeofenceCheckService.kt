package com.example.nextthingb1.domain.service

import com.example.nextthingb1.domain.model.GeofenceCheckResult
import com.example.nextthingb1.domain.model.GeofenceStatus

/**
 * 地理围栏检查服务接口
 *
 * 负责检查用户当前位置是否在任务的地理围栏内
 */
interface GeofenceCheckService {

    /**
     * 检查任务的地理围栏状态
     *
     * @param taskId 任务 ID
     * @return GeofenceStatus 包含检查结果、距离、位置等信息
     */
    suspend fun checkTaskGeofence(taskId: String): GeofenceStatus

    /**
     * 批量检查多个任务的地理围栏状态
     *
     * @param taskIds 任务 ID 列表
     * @return Map<任务ID, GeofenceStatus>
     */
    suspend fun checkMultipleTaskGeofences(taskIds: List<String>): Map<String, GeofenceStatus>

    /**
     * 计算两点之间的距离（米）
     *
     * 使用 Haversine 公式计算地球表面两点间的大圆距离
     *
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @return 距离（米）
     */
    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double

    /**
     * 清除缓存的位置信息
     *
     * 强制下次检查时重新获取位置
     */
    fun clearLocationCache()
}

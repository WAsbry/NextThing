package com.example.nextthingb1.domain.repository

import com.example.nextthingb1.domain.model.GeofenceConfig
import kotlinx.coroutines.flow.Flow

/**
 * 地理围栏全局配置仓库接口
 *
 * 管理地理围栏的全局设置（单例配置）
 */
interface GeofenceConfigRepository {

    /**
     * 获取全局配置（Flow，自动监听变化）
     * @return 配置的 Flow，可能为 null（首次使用时）
     */
    fun getConfig(): Flow<GeofenceConfig?>

    /**
     * 获取全局配置（一次性查询）
     * @return 配置对象，如果不存在则创建默认配置
     */
    suspend fun getConfigOrDefault(): GeofenceConfig

    /**
     * 更新全局配置
     * @param config 新的配置对象
     */
    suspend fun updateConfig(config: GeofenceConfig)

    /**
     * 更新全局开关
     * @param enabled 是否启用地理围栏功能
     */
    suspend fun updateGlobalEnabled(enabled: Boolean)

    /**
     * 更新默认围栏半径
     * @param radius 默认半径（米），范围：50-1000
     */
    suspend fun updateDefaultRadius(radius: Int)

    /**
     * 更新定位精度阈值
     * @param threshold 精度阈值（米）
     */
    suspend fun updateLocationAccuracyThreshold(threshold: Int)

    /**
     * 更新省电模式开关
     * @param enabled 是否启用省电模式
     */
    suspend fun updateBatteryOptimization(enabled: Boolean)

    /**
     * 更新"不在范围内时通知"开关
     * @param enabled 是否启用
     */
    suspend fun updateNotifyWhenOutside(enabled: Boolean)

    /**
     * 初始化默认配置（如果不存在）
     * 在应用首次启动时调用
     */
    suspend fun initializeDefaultConfigIfNeeded()
}

package com.example.nextthingb1.domain.service

import com.example.nextthingb1.domain.model.LocationInfo
import kotlinx.coroutines.flow.Flow

interface LocationService {
    /**
     * 获取当前实时位置
     * @param forceRefresh 是否强制刷新位置
     * @return 位置信息结果
     */
    suspend fun getCurrentLocation(forceRefresh: Boolean = false): Result<LocationInfo>
    
    /**
     * 检查位置权限是否已授予
     */
    suspend fun hasLocationPermission(): Boolean
    
    /**
     * 检查位置服务是否启用
     */
    suspend fun isLocationEnabled(): Boolean
    
    /**
     * 监听位置变化
     */
    fun observeLocationUpdates(): Flow<LocationInfo>
    
    /**
     * 停止位置监听
     */
    fun stopLocationUpdates()
    
    /**
     * 检查是否需要刷新位置（基于5分钟间隔）
     */
    fun shouldRefreshLocation(): Boolean
    
    /**
     * 获取缓存的位置信息
     */
    suspend fun getCachedLocation(): LocationInfo?
} 
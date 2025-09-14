package com.example.nextthingb1.domain.service

import com.example.nextthingb1.domain.model.WeatherInfo
import com.example.nextthingb1.domain.model.LocationInfo
import kotlinx.coroutines.flow.Flow

interface WeatherService {
    /**
     * 获取当前位置的天气信息
     * @param location 位置信息
     * @param forceRefresh 是否强制刷新
     * @return 天气信息结果
     */
    suspend fun getCurrentWeather(location: LocationInfo, forceRefresh: Boolean = false): Result<WeatherInfo>
    
    /**
     * 监听天气信息变化
     * @param location 位置信息
     * @return 天气信息流
     */
    fun observeWeatherUpdates(location: LocationInfo): Flow<WeatherInfo>
    
    /**
     * 检查是否需要刷新天气数据（基于15分钟间隔）
     */
    fun shouldRefreshWeather(): Boolean
    
    /**
     * 获取缓存的天气信息
     */
    suspend fun getCachedWeather(): WeatherInfo?
    
    /**
     * 清除天气缓存
     */
    suspend fun clearWeatherCache()
} 
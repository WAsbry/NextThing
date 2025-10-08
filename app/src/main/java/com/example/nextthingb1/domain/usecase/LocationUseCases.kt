package com.example.nextthingb1.domain.usecase

import com.example.nextthingb1.domain.model.LocationInfo
import com.example.nextthingb1.domain.model.LocationType
import com.example.nextthingb1.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocationUseCases @Inject constructor(
    private val locationRepository: LocationRepository
) {

    /**
     * 获取所有保存的地点（仅用户手动创建的，不包含自动获取的当前位置）
     *
     * 设计说明：
     * - LocationType.MANUAL: 用户在创建任务时手动添加的位置
     * - LocationType.AUTO: 应用自动获取的当前位置（用于首页显示）
     *
     * 创建任务页面只应该显示用户手动创建的位置，避免大量重复的自动位置记录
     */
    fun getAllSavedLocations(): Flow<List<LocationInfo>> {
        return locationRepository.getLocationsByType(LocationType.MANUAL)
    }

    /**
     * 获取当前位置
     */
    suspend fun getCurrentLocation(): LocationInfo? {
        return locationRepository.getCurrentLocation()
    }

    /**
     * 保存新地点
     */
    suspend fun saveLocation(locationInfo: LocationInfo): Result<LocationInfo> {
        return locationRepository.insertLocation(locationInfo)
    }

    /**
     * 删除地点
     */
    suspend fun deleteLocation(locationId: String): Result<Unit> {
        return locationRepository.deleteLocationById(locationId)
    }

    /**
     * 设置为当前位置
     */
    suspend fun setAsCurrentLocation(locationId: String): Result<Unit> {
        return locationRepository.setAsCurrentLocation(locationId)
    }

    /**
     * 记录地点使用（增加使用频次）
     */
    suspend fun recordLocationUsage(locationId: String): Result<Unit> {
        return locationRepository.incrementUsageCount(locationId)
    }

    /**
     * 获取最常使用的地点
     */
    suspend fun getMostUsedLocations(limit: Int = 10): List<LocationInfo> {
        return locationRepository.getMostUsedLocations(limit)
    }
}
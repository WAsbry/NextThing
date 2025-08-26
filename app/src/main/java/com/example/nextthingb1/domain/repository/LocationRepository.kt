package com.example.nextthingb1.domain.repository

import com.example.nextthingb1.domain.model.LocationInfo
import com.example.nextthingb1.domain.model.LocationStatistics
import com.example.nextthingb1.domain.model.LocationType
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    
    // 基础CRUD操作
    fun getAllLocations(): Flow<List<LocationInfo>>
    suspend fun getCurrentLocation(): LocationInfo?
    suspend fun getLocationById(locationId: String): LocationInfo?
    fun getLocationsByType(type: LocationType): Flow<List<LocationInfo>>
    fun getTodayLocations(): Flow<List<LocationInfo>>
    
    // 位置操作
    suspend fun insertLocation(location: LocationInfo): String
    suspend fun updateLocation(location: LocationInfo)
    suspend fun deleteLocation(locationId: String)
    suspend fun setAsCurrentLocation(locationId: String)
    
    // 地理查询
    suspend fun getLocationsInArea(
        minLat: Double, maxLat: Double,
        minLng: Double, maxLng: Double
    ): List<LocationInfo>
    
    // 统计信息
    suspend fun getLocationStatistics(): LocationStatistics
    
    // 权限和系统定位
    suspend fun requestLocationPermission(): Boolean
    suspend fun getCurrentSystemLocation(): LocationInfo?
    suspend fun isLocationEnabled(): Boolean
} 
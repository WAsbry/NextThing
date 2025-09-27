package com.example.nextthingb1.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import com.example.nextthingb1.data.local.dao.LocationDao
import com.example.nextthingb1.data.mapper.toDomain
import com.example.nextthingb1.data.mapper.toEntity
import com.example.nextthingb1.domain.model.LocationInfo
import com.example.nextthingb1.domain.model.LocationStatistics
import com.example.nextthingb1.domain.model.LocationType
import com.example.nextthingb1.domain.repository.LocationRepository
import java.time.LocalDateTime
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationDao: LocationDao,
    @ApplicationContext private val context: Context
) : LocationRepository {

    override fun getAllLocations(): Flow<List<LocationInfo>> {
        return locationDao.getAllLocations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCurrentLocation(): LocationInfo? {
        return locationDao.getCurrentLocation()?.toDomain()
    }

    override suspend fun getLocationById(locationId: String): LocationInfo? {
        return locationDao.getLocationById(locationId)?.toDomain()
    }

    override fun getLocationsByType(type: LocationType): Flow<List<LocationInfo>> {
        return locationDao.getLocationsByType(type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTodayLocations(): Flow<List<LocationInfo>> {
        return locationDao.getTodayLocations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertLocation(location: LocationInfo): Result<LocationInfo> {
        return try {
            locationDao.insertLocation(location.toEntity())
            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLocation(location: LocationInfo) {
        locationDao.updateLocation(location.toEntity())
    }

    override suspend fun deleteLocationById(locationId: String): Result<Unit> {
        return try {
            locationDao.deleteLocationById(locationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setAsCurrentLocation(locationId: String): Result<Unit> {
        return try {
            locationDao.clearCurrentLocationFlag()
            locationDao.setAsCurrentLocation(locationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun incrementUsageCount(locationId: String): Result<Unit> {
        return try {
            locationDao.incrementUsageCount(locationId, LocalDateTime.now())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMostUsedLocations(limit: Int): List<LocationInfo> {
        return locationDao.getMostUsedLocations(limit).map { it.toDomain() }
    }

    override suspend fun getLocationsInArea(
        minLat: Double, maxLat: Double,
        minLng: Double, maxLng: Double
    ): List<LocationInfo> {
        return locationDao.getLocationsInArea(minLat, maxLat, minLng, maxLng)
            .map { it.toDomain() }
    }

    override suspend fun getLocationStatistics(): LocationStatistics {
        val totalLocations = locationDao.getTotalLocationsCount()
        val todayLocations = locationDao.getTodayLocationsCount()
        val mostVisitedLocation = locationDao.getMostVisitedLocation() ?: ""
        val averageAccuracy = locationDao.getAverageAccuracy() ?: 0f

        return LocationStatistics(
            totalLocations = totalLocations,
            todayLocations = todayLocations,
            mostVisitedLocation = mostVisitedLocation,
            locationAccuracy = averageAccuracy
        )
    }

    override suspend fun requestLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun getCurrentSystemLocation(): LocationInfo? {
        // 这里可以集成LocationService来获取实时位置
        // 暂时返回null，具体实现在LocationService中
        return null
    }

    override suspend fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
} 
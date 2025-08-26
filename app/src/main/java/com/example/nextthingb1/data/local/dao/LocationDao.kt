package com.example.nextthingb1.data.local.dao

import androidx.room.*
import com.example.nextthingb1.data.local.entity.LocationEntity
import com.example.nextthingb1.domain.model.LocationType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface LocationDao {
    
    @Query("SELECT * FROM locations ORDER BY addedAt DESC")
    fun getAllLocations(): Flow<List<LocationEntity>>
    
    @Query("SELECT * FROM locations WHERE isCurrentLocation = 1 LIMIT 1")
    suspend fun getCurrentLocation(): LocationEntity?
    
    @Query("SELECT * FROM locations WHERE id = :locationId")
    suspend fun getLocationById(locationId: String): LocationEntity?
    
    @Query("SELECT * FROM locations WHERE locationType = :type ORDER BY addedAt DESC")
    fun getLocationsByType(type: LocationType): Flow<List<LocationEntity>>
    
    @Query("""
        SELECT * FROM locations 
        WHERE date(addedAt) = date('now', 'localtime')
        ORDER BY addedAt DESC
    """)
    fun getTodayLocations(): Flow<List<LocationEntity>>
    
    @Query("""
        SELECT * FROM locations 
        WHERE latitude BETWEEN :minLat AND :maxLat 
        AND longitude BETWEEN :minLng AND :maxLng
        ORDER BY addedAt DESC
    """)
    suspend fun getLocationsInArea(
        minLat: Double, maxLat: Double,
        minLng: Double, maxLng: Double
    ): List<LocationEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity): Long
    
    @Update
    suspend fun updateLocation(location: LocationEntity)
    
    @Delete
    suspend fun deleteLocation(location: LocationEntity)
    
    @Query("DELETE FROM locations WHERE id = :locationId")
    suspend fun deleteLocationById(locationId: String)
    
    @Query("UPDATE locations SET isCurrentLocation = 0")
    suspend fun clearCurrentLocationFlag()
    
    @Query("UPDATE locations SET isCurrentLocation = 1 WHERE id = :locationId")
    suspend fun setAsCurrentLocation(locationId: String)
    
    // 统计查询
    @Query("SELECT COUNT(*) FROM locations")
    suspend fun getTotalLocationsCount(): Int
    
    @Query("SELECT COUNT(*) FROM locations WHERE date(addedAt) = date('now', 'localtime')")
    suspend fun getTodayLocationsCount(): Int
    
    @Query("""
        SELECT locationName 
        FROM locations 
        GROUP BY locationName 
        ORDER BY COUNT(*) DESC 
        LIMIT 1
    """)
    suspend fun getMostVisitedLocation(): String?
    
    @Query("SELECT AVG(accuracy) FROM locations WHERE accuracy IS NOT NULL")
    suspend fun getAverageAccuracy(): Float?
} 
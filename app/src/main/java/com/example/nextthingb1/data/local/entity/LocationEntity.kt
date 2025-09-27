package com.example.nextthingb1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.nextthingb1.domain.model.LocationType
import java.time.LocalDateTime

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey
    val id: String,
    val locationName: String,           // 地点名字
    val latitude: Double,               // 纬度信息
    val longitude: Double,              // 经度信息
    val usageCount: Int = 0,           // 使用频次
    val lastUsedAt: LocalDateTime? = null, // 最后使用时间
    val accuracy: Float? = null,
    val altitude: Double? = null,
    val address: String = "",
    val city: String = "",
    val district: String = "",
    val province: String = "",
    val country: String = "中国",
    val addedAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isCurrentLocation: Boolean = false,
    val locationType: LocationType = LocationType.MANUAL
) 
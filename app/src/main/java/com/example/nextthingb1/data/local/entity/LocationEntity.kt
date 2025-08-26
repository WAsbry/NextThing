package com.example.nextthingb1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.nextthingb1.domain.model.LocationType
import java.time.LocalDateTime

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey
    val id: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val altitude: Double?,
    val address: String,
    val city: String,
    val district: String,
    val province: String,
    val country: String,
    val addedAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isCurrentLocation: Boolean,
    val locationType: LocationType
) 
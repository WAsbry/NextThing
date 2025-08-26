package com.example.nextthingb1.data.mapper

import com.example.nextthingb1.data.local.entity.LocationEntity
import com.example.nextthingb1.domain.model.LocationInfo

fun LocationEntity.toDomain(): LocationInfo {
    return LocationInfo(
        id = id,
        locationName = locationName,
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        altitude = altitude,
        address = address,
        city = city,
        district = district,
        province = province,
        country = country,
        addedAt = addedAt,
        updatedAt = updatedAt,
        isCurrentLocation = isCurrentLocation,
        locationType = locationType
    )
}

fun LocationInfo.toEntity(): LocationEntity {
    return LocationEntity(
        id = id,
        locationName = locationName,
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        altitude = altitude,
        address = address,
        city = city,
        district = district,
        province = province,
        country = country,
        addedAt = addedAt,
        updatedAt = updatedAt,
        isCurrentLocation = isCurrentLocation,
        locationType = locationType
    )
} 
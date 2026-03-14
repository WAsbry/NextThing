package com.example.nextthingb1.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class LocationInfo(
    val id: String = UUID.randomUUID().toString(),
    val locationName: String = "", // 地点名字
    val latitude: Double, // 纬度信息
    val longitude: Double, // 经度信息
    val usageCount: Int = 0, // 使用频次
    val lastUsedAt: LocalDateTime? = null, // 最后使用时间
    val accuracy: Float? = null, // 定位精度（米）
    val altitude: Double? = null, // 海拔高度（米）
    val address: String = "", // 详细地址
    val city: String = "", // 城市
    val district: String = "", // 区域
    val province: String = "", // 省份
    val country: String = "中国", // 国家
    val addedAt: LocalDateTime = LocalDateTime.now(), // 添加时间
    val updatedAt: LocalDateTime = LocalDateTime.now(), // 更新时间
    val isCurrentLocation: Boolean = false, // 是否为当前位置
    val locationType: LocationType = LocationType.MANUAL // 位置类型
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocationInfo) return false

        if (id != other.id) return false
        if (locationName != other.locationName) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (usageCount != other.usageCount) return false
        if (lastUsedAt != other.lastUsedAt) return false
        if (accuracy != other.accuracy) return false
        if (altitude != other.altitude) return false
        if (address != other.address) return false
        if (city != other.city) return false
        if (district != other.district) return false
        if (province != other.province) return false
        if (country != other.country) return false
        if (addedAt != other.addedAt) return false
        if (updatedAt != other.updatedAt) return false
        if (isCurrentLocation != other.isCurrentLocation) return false
        if (locationType != other.locationType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + locationName.hashCode()
        result = 31 * result + latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + usageCount
        result = 31 * result + (lastUsedAt?.hashCode() ?: 0)
        result = 31 * result + (accuracy?.hashCode() ?: 0)
        result = 31 * result + (altitude?.hashCode() ?: 0)
        result = 31 * result + address.hashCode()
        result = 31 * result + city.hashCode()
        result = 31 * result + district.hashCode()
        result = 31 * result + province.hashCode()
        result = 31 * result + country.hashCode()
        result = 31 * result + addedAt.hashCode()
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + isCurrentLocation.hashCode()
        result = 31 * result + locationType.hashCode()
        return result
    }
}

enum class LocationType {
    MANUAL,     // 手动添加
    AUTO,       // 自动获取
    TASK_BOUND  // 任务绑定位置
}

data class LocationStatistics(
    val totalLocations: Int = 0,
    val todayLocations: Int = 0,
    val mostVisitedLocation: String = "",
    val locationAccuracy: Float = 0f
) 
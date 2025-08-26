package com.example.nextthingb1.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class LocationInfo(
    val id: String = UUID.randomUUID().toString(),
    val locationName: String = "", // 位置名称（如：北京市朝阳区）
    val latitude: Double, // 纬度
    val longitude: Double, // 经度
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
)

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
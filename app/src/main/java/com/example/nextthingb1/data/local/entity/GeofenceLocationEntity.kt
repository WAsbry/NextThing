package com.example.nextthingb1.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 地理围栏地点实体
 *
 * 存储可复用的地理围栏地点配置
 * 一个地点可以被多个任务关联使用
 */
@Entity(
    tableName = "geofence_locations",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE // 地点删除时，级联删除地理围栏配置
        )
    ],
    indices = [
        Index(value = ["locationId"]), // 加速按地点ID查询
        Index(value = ["isFrequent"]), // 加速查询常用地点
        Index(value = ["usageCount"]) // 加速按使用次数排序
    ]
)
data class GeofenceLocationEntity(
    @PrimaryKey
    val id: String,

    /**
     * 关联的地点ID（外键关联 LocationEntity）
     * 复用现有的地点数据（名称、坐标、地址等）
     */
    val locationId: String,

    /**
     * 自定义围栏半径（米）
     * null = 使用全局默认半径（GeofenceConfig.defaultRadius）
     * 非null = 使用此自定义值
     * 范围：50-1000米
     */
    val customRadius: Int? = null,

    /**
     * 是否为常用地点
     * true = 在列表中优先显示，标记⭐图标
     * false = 普通地点
     *
     * 自动更新规则（UpdateFrequentLocationsUseCase）：
     * - 使用次数 >= 3 次
     * - 最近30天内使用过
     */
    val isFrequent: Boolean = false,

    /**
     * 使用次数
     * 每次创建关联此地点的任务时 +1
     * 用于统计和常用地点识别
     */
    val usageCount: Int = 0,

    /**
     * 最后使用时间
     * 记录最近一次创建关联此地点的任务的时间
     * 用于常用地点识别（30天内使用过）
     */
    val lastUsed: LocalDateTime? = null,

    // ========== 统计字段 ==========

    /**
     * 本月检查次数
     * 记录本月对该地点进行地理围栏检查的总次数
     * 每月1号自动重置为0
     */
    val monthlyCheckCount: Int = 0,

    /**
     * 本月命中次数
     * 记录本月检查时用户在围栏内的次数（INSIDE_GEOFENCE）
     * 每月1号自动重置为0
     */
    val monthlyHitCount: Int = 0,

    /**
     * 上次统计重置月份
     * 格式：YYYY-MM（如 "2025-01"）
     * 用于判断是否需要重置月度统计
     */
    val lastStatisticsResetMonth: String? = null,

    // ========== 时间戳 ==========

    /**
     * 创建时间
     */
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * 最后更新时间
     */
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

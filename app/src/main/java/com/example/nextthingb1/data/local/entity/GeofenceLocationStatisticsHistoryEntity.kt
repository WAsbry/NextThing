package com.example.nextthingb1.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 地理围栏地点月度统计历史数据实体
 *
 * 用于保存每月的统计数据，用于历史查看和趋势分析
 */
@Entity(
    tableName = "geofence_location_statistics_history",
    foreignKeys = [
        ForeignKey(
            entity = GeofenceLocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["geofenceLocationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["geofenceLocationId"]),
        Index(value = ["month"]),
        Index(value = ["geofenceLocationId", "month"], unique = true)
    ]
)
data class GeofenceLocationStatisticsHistoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /**
     * 地理围栏地点ID（外键）
     */
    val geofenceLocationId: String,

    /**
     * 统计月份（格式：YYYY-MM，如 "2024-03"）
     */
    val month: String,

    /**
     * 检查次数
     */
    val checkCount: Int,

    /**
     * 命中次数
     */
    val hitCount: Int,

    /**
     * 命中率（0.0~1.0）
     */
    val hitRate: Float,

    /**
     * 创建时间（月末归档时记录）
     */
    val createdAt: String
)

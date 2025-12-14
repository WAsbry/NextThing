package com.example.nextthingb1.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 地理围栏全局配置实体
 *
 * 这是一个单例表，只有一条记录（id = "default"）
 * 用于存储地理围栏的全局设置
 */
@Entity(tableName = "geofence_config")
data class GeofenceConfigEntity(
    @PrimaryKey
    val id: String = "default", // 固定为 "default"，保证单例

    /**
     * 全局开关：是否启用地理围栏功能
     * true = 启用，false = 禁用
     */
    val isGlobalEnabled: Boolean = false,

    /**
     * 默认围栏半径（米）
     * 当用户新增地点时，如果不设置自定义半径，则使用此默认值
     * 范围：50-1000米
     */
    val defaultRadius: Int = 200,

    /**
     * 定位精度阈值（米）
     * 如果定位精度低于此值，降级为普通通知（不检查围栏）
     * 建议值：100米
     */
    val locationAccuracyThreshold: Int = 100,

    /**
     * 自动刷新间隔（秒）
     * 在任务详情页显示围栏状态时，自动刷新的时间间隔
     * 默认：300秒（5分钟）
     */
    val autoRefreshInterval: Int = 300,

    /**
     * 省电模式开关
     * true = 启用省电模式（使用 PRIORITY_BALANCED_POWER_ACCURACY）
     * false = 高精度模式（使用 PRIORITY_HIGH_ACCURACY）
     */
    val batteryOptimization: Boolean = true,

    /**
     * 不在范围内时是否通知
     * true = 发送低优先级提醒
     * false = 不发送任何通知
     */
    val notifyWhenOutside: Boolean = false,

    /**
     * 创建时间
     */
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * 最后更新时间
     */
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

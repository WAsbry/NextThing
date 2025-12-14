package com.example.nextthingb1.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * 任务地理围栏关联实体
 *
 * 将任务与地理围栏地点关联起来
 * 记录快照数据（半径、最后检查结果等），避免追溯修改影响
 */
@Entity(
    tableName = "task_geofences",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE // 任务删除时，级联删除关联
        ),
        ForeignKey(
            entity = GeofenceLocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["geofenceLocationId"],
            onDelete = ForeignKey.CASCADE // 地理围栏地点删除时，级联删除关联
        )
    ],
    indices = [
        Index(value = ["taskId"], unique = true), // 一个任务只能关联一个地理围栏地点
        Index(value = ["geofenceLocationId"]) // 加速查询某地点关联的所有任务
    ]
)
data class TaskGeofenceEntity(
    @PrimaryKey
    val id: String,

    /**
     * 关联的任务ID（外键关联 TaskEntity）
     */
    val taskId: String,

    /**
     * 关联的地理围栏地点ID（外键关联 GeofenceLocationEntity）
     */
    val geofenceLocationId: String,

    /**
     * 快照：围栏半径（米）
     *
     * ⚠️ 重要：这是快照值，记录任务创建时的半径
     * 即使后续修改 GeofenceLocationEntity.customRadius，
     * 此任务的围栏判断仍使用创建时的快照值
     *
     * 好处：避免修改配置导致历史任务行为改变
     */
    val radius: Int,

    /**
     * 是否启用地理围栏
     * true = 启用（到达提醒时间时检查围栏）
     * false = 禁用（正常发送通知，不检查围栏）
     */
    val enabled: Boolean = true,

    /**
     * 最后检查时间
     * 记录最近一次围栏检查的时间（用于调试和统计）
     */
    val lastCheckTime: LocalDateTime? = null,

    /**
     * 最后检查结果（JSON字符串）
     *
     * 存储 GeofenceCheckResult 枚举值的名称：
     * - "INSIDE_GEOFENCE": 在围栏内
     * - "OUTSIDE_GEOFENCE": 在围栏外
     * - "LOCATION_UNAVAILABLE": 位置不可用
     * - "PERMISSION_DENIED": 权限被拒绝
     * - "GEOFENCE_DISABLED": 未启用围栏
     *
     * 用于任务详情页显示围栏状态
     */
    val lastCheckResult: String? = null,

    /**
     * 最后检查时的距离（米）
     * 记录用户位置与目标地点的距离
     * 用于任务详情页显示"距离目标X米"
     */
    val lastCheckDistance: Double? = null,

    /**
     * 最后检查时的用户纬度
     * 用于调试和地图展示
     */
    val lastCheckUserLatitude: Double? = null,

    /**
     * 最后检查时的用户经度
     * 用于调试和地图展示
     */
    val lastCheckUserLongitude: Double? = null,

    /**
     * 创建时间
     */
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * 最后更新时间
     */
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

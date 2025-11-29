package com.example.nextthingb1.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.nextthingb1.domain.model.TaskStatus
import java.time.LocalDateTime

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT // 防止删除正在使用的分类
        )
    ],
    indices = [
        Index(value = ["categoryId"]), // 添加索引提升查询性能
        Index(value = ["templateTaskId"]), // 模板任务ID索引
        Index(value = ["instanceDate"]), // 实例日期索引
        Index(value = ["isTemplate"]) // 模板标志索引
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val categoryId: String, // 外键关联到 CategoryEntity
    val status: TaskStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val dueDate: LocalDateTime?,
    val completedAt: LocalDateTime?,
    val tags: String, // JSON string for tags
    val isUrgent: Boolean,
    val estimatedDuration: Int,
    val actualDuration: Int,
    val subtasksJson: String, // JSON string for subtasks
    val imageUri: String? = null, // 任务图片URI
    val repeatFrequencyJson: String = "{}", // JSON string for repeat frequency
    val locationInfoJson: String? = null, // JSON string for location info
    val importanceUrgencyJson: String? = null, // JSON string for importance urgency
    val notificationStrategyId: String? = null, // 通知策略ID
    val isTemplate: Boolean = false, // 是否为模板任务（用于重复任务）
    val templateTaskId: String? = null, // 如果是实例任务，指向模板任务的ID
    val instanceDate: LocalDateTime? = null // 如果是实例任务，对应的日期
) 
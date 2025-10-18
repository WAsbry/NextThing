package com.example.nextthingb1.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val category: TaskCategory = TaskCategory.WORK,
    val status: TaskStatus = TaskStatus.PENDING,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val dueDate: LocalDateTime? = null,
    val completedAt: LocalDateTime? = null,
    val tags: List<String> = emptyList(),
    val isUrgent: Boolean = false,
    val estimatedDuration: Int = 0, // 分钟
    val actualDuration: Int = 0, // 分钟
    val subtasks: List<Subtask> = emptyList(),
    val imageUri: String? = null, // 任务相关的图片URI
    val repeatFrequency: RepeatFrequency = RepeatFrequency(), // 重复频次
    val locationInfo: LocationInfo? = null, // 地理位置信息
    val importanceUrgency: TaskImportanceUrgency? = null, // 重要程度
    val notificationStrategyId: String? = null // 通知策略ID
)

data class Subtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

// 重要性和闪电紧急性组合（艾森豪威尔矩阵）
enum class TaskImportanceUrgency(
    val displayName: String,
    val description: String,
    val colorHex: String,
    val importance: TaskImportance,
    val urgency: TaskUrgency
) {
    IMPORTANT_URGENT(
        "重要且紧急",
        "需要立即处理的重要事项",
        "#F44336",
        TaskImportance.IMPORTANT,
        TaskUrgency.URGENT
    ),
    IMPORTANT_NOT_URGENT(
        "重要但不紧急",
        "重要的长期目标和计划",
        "#FF9800",
        TaskImportance.IMPORTANT,
        TaskUrgency.NOT_URGENT
    ),
    NOT_IMPORTANT_URGENT(
        "不重要但紧急",
        "需要快速处理的事务性工作",
        "#2196F3",
        TaskImportance.NOT_IMPORTANT,
        TaskUrgency.URGENT
    ),
    NOT_IMPORTANT_NOT_URGENT(
        "不重要且不紧急",
        "可以暂缓或委托的事项",
        "#4CAF50",
        TaskImportance.NOT_IMPORTANT,
        TaskUrgency.NOT_URGENT
    );

    companion object {
        fun fromImportanceAndUrgency(importance: TaskImportance, urgency: TaskUrgency): TaskImportanceUrgency {
            return values().first { it.importance == importance && it.urgency == urgency }
        }
    }
}

enum class TaskImportance {
    IMPORTANT,
    NOT_IMPORTANT
}

enum class TaskUrgency {
    URGENT,
    NOT_URGENT
}

enum class TaskCategory(val displayName: String, val icon: String, val colorHex: String) {
    WORK("工作", "laptop-code", "#42A5F5"),
    STUDY("学习", "book", "#AB47BC"),
    LIFE("生活", "dumbbell", "#66BB6A"),
    HEALTH("健康", "heart", "#E91E63"),
    PERSONAL("个人", "user", "#FF9800"),
    OTHER("其他", "circle", "#9E9E9E")
}

enum class TaskStatus {
    PENDING,     // 未完成：当天需处理但未完成的任务
    COMPLETED,   // 已完成：用户主动标记完成的任务（终态）
    DELAYED,     // 延期：当天未完成手动延期至次日的过渡状态（次日自动转为 PENDING）
    OVERDUE,     // 逾期：当前时间超过任务截止时间5分钟后且未完成的任务
    CANCELLED    // 放弃：用户主动放弃的任务（终态）
}

data class TaskStatistics(
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val pendingTasks: Int = 0,
    val overdueTasks: Int = 0,
    val completionRate: Float = 0f,
    val averageCompletionTime: Int = 0, // 分钟
    val mostProductiveHour: Int = 9,
    val categoryStats: Map<TaskCategory, Int> = emptyMap(),
    val weeklyStats: List<DailyStats> = emptyList()
)

data class DailyStats(
    val date: LocalDateTime,
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val completionRate: Float = 0f,
    val focusTime: Int = 0 // 分钟
) 
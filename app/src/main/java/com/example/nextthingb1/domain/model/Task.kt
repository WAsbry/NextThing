package com.example.nextthingb1.domain.model

import java.time.LocalDateTime
import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
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
    val importanceUrgency: TaskImportanceUrgency? = null // 重要程度
)

data class Subtask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class TaskPriority(val displayName: String, val colorHex: String) {
    LOW("低", "#4CAF50"),
    MEDIUM("中", "#FF9800"),
    HIGH("高", "#F44336")
}

// 重要性和紧急性组合（艾森豪威尔矩阵）
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
    PENDING,    // 待办
    IN_PROGRESS, // 进行中
    COMPLETED,   // 已完成
    CANCELLED,   // 已取消
    OVERDUE     // 已过期
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
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
    val subtasks: List<Subtask> = emptyList()
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
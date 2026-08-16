package com.nextthing.app.data.remote.dto

import com.nextthing.app.data.local.entity.SyncStatus
import com.nextthing.app.domain.model.TaskStatus

/**
 * 任务同步请求DTO
 */
data class TaskSyncRequest(
    val tasks: List<TaskSyncDto>,
    val lastSyncTimestamp: Long?
)

/**
 * 任务同步响应DTO
 */
data class TaskSyncResponse(
    val tasks: List<TaskSyncDto>,
    val serverTimestamp: Long,
    val conflicts: List<SyncConflictDto>?,
    val deduplicatedTaskIds: List<String>? = null
)

/**
 * 任务同步数据传输对象
 */
data class TaskSyncDto(
    val id: String,
    val title: String,
    val description: String,
    val categoryId: String,
    val status: TaskStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val dueDate: Long?,
    val completedAt: Long?,
    val tags: List<String>,
    val isUrgent: Boolean,
    val estimatedDuration: Int,
    val actualDuration: Int,
    val subtasks: List<SubtaskSyncDto>,
    val imageUri: String?,
    val repeatFrequency: RepeatFrequencySyncDto?,
    val locationInfo: LocationInfoSyncDto?,
    val importanceUrgency: ImportanceUrgencySyncDto?,
    val notificationStrategyId: String?,
    val isTemplate: Boolean,
    val templateTaskId: String?,
    val instanceDate: Long?,
    val deleted: Boolean = false // 软删除标记
)

/**
 * 子任务同步DTO
 */
data class SubtaskSyncDto(
    val id: String,
    val title: String,
    val isCompleted: Boolean
)

/**
 * 重复频率同步DTO
 */
data class RepeatFrequencySyncDto(
    val type: String, // DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
    val interval: Int,
    val daysOfWeek: List<Int>?,
    val dayOfMonth: Int?,
    val monthOfYear: Int?,
    val endDate: Long?,
    val occurrences: Int?
)

/**
 * 位置信息同步DTO
 */
data class LocationInfoSyncDto(
    val locationId: String,
    val locationName: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double
)

/**
 * 重要紧急程度同步DTO
 */
data class ImportanceUrgencySyncDto(
    val importance: Int, // 1-5
    val urgency: Int, // 1-5
    val quadrant: String // IMPORTANT_URGENT, IMPORTANT_NOT_URGENT, NOT_IMPORTANT_URGENT, NOT_IMPORTANT_NOT_URGENT
)

/**
 * 同步冲突DTO
 */
data class SyncConflictDto(
    val taskId: String,
    val localVersion: TaskSyncDto,
    val serverVersion: TaskSyncDto,
    val conflictType: ConflictType
)

/**
 * 冲突类型
 */
enum class ConflictType {
    BOTH_MODIFIED,      // 本地和服务器都修改了
    LOCAL_DELETED_SERVER_MODIFIED,  // 本地删除，服务器修改
    LOCAL_MODIFIED_SERVER_DELETED   // 本地修改，服务器删除
}

/**
 * 分类同步DTO
 */
data class CategorySyncDto(
    val id: String,
    val name: String,
    val type: Int,
    val icon: String,
    val colorHex: String,
    val sortOrder: Int,
    val createdAt: Long,
    val isEnabled: Boolean,
    val deleted: Boolean = false
)

/**
 * 分类同步请求
 */
data class CategorySyncRequest(
    val categories: List<CategorySyncDto>,
    val lastSyncTimestamp: Long?
)

/**
 * 分类同步响应
 */
data class CategorySyncResponse(
    val categories: List<CategorySyncDto>,
    val serverTimestamp: Long,
    val conflicts: List<CategoryConflictDto>?
)

/**
 * 分类冲突DTO
 */
data class CategoryConflictDto(
    val categoryId: String,
    val localVersion: CategorySyncDto,
    val serverVersion: CategorySyncDto,
    val conflictType: ConflictType
)

/**
 * 同步状态响应
 */
data class SyncStatusDto(
    val pendingUploads: Int,
    val pendingDownloads: Int,
    val conflicts: Int,
    val lastSyncTimestamp: Long?,
    val isSyncing: Boolean
)

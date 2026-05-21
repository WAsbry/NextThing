package com.nextthing.app.domain.model

/**
 * 同步结果
 */
data class SyncResult(
    val uploadedTasks: Int,
    val downloadedTasks: Int,
    val conflicts: List<SyncConflict>,
    val timestamp: Long
)

/**
 * 同步冲突
 */
data class SyncConflict(
    val taskId: String,
    val taskTitle: String,
    val localModifiedAt: Long,
    val serverModifiedAt: Long,
    val conflictType: ConflictType
)

/**
 * 冲突类型
 */
enum class ConflictType {
    BOTH_MODIFIED,
    LOCAL_DELETED_SERVER_MODIFIED,
    LOCAL_MODIFIED_SERVER_DELETED
}

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

class FullSyncBlockedByPendingChangesException(
    val pendingTaskCount: Int,
    val pendingCategoryCount: Int
) : IllegalStateException(
    "全量同步已阻止：本机仍有 $pendingTaskCount 条任务和 $pendingCategoryCount 个分类尚未上传"
)

class SyncAuthenticationRequiredException :
    IllegalStateException("请先登录云端账号再执行同步")

class SyncAccountMismatchException(
    val boundServerUserId: Long,
    val currentServerUserId: Long
) : IllegalStateException(
    "同步已阻止：本机数据属于账号 $boundServerUserId，当前登录账号为 $currentServerUserId"
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

package com.nextthing.app.domain.repository

import com.nextthing.app.domain.model.SyncResult
import com.nextthing.app.domain.model.SyncConflict
import com.nextthing.app.domain.model.Task
import com.nextthing.app.data.repository.SyncState
import kotlinx.coroutines.flow.Flow

/**
 * 数据同步仓库接口
 */
interface SyncRepository {

    /**
     * 同步状态流
     */
    val syncState: Flow<SyncState>

    /**
     * 执行增量同步
     * 上传本地修改，下载服务器更新
     */
    suspend fun sync(): Result<SyncResult>

    /**
     * 执行全量同步
     * 首次登录或数据恢复时使用
     */
    suspend fun fullSync(): Result<SyncResult>

    /**
     * 标记任务需要同步
     */
    suspend fun markTaskForSync(taskId: String)

    /**
     * 获取所有同步冲突
     */
    suspend fun getConflicts(): List<SyncConflict>

    /**
     * 解决冲突（使用服务器版本）
     */
    suspend fun resolveConflictUseServer(taskId: String): Result<Unit>

    /**
     * 解决冲突（使用本地版本）
     */
    suspend fun resolveConflictUseLocal(task: Task): Result<Unit>
}

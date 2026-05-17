package com.nextthing.app.domain.usecase

import com.nextthing.app.domain.model.SyncResult
import com.nextthing.app.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 同步任务用例 (Stub)
 */
class SyncTasksUseCase @Inject constructor(
    private val repository: SyncRepository
) {
    suspend operator fun invoke(): Result<SyncResult> {
        return Result.success(SyncResult(0, 0, emptyList(), System.currentTimeMillis()))
    }
}

/**
 * 完整同步用例 (Stub)
 */
class FullSyncUseCase @Inject constructor(
    private val repository: SyncRepository
) {
    suspend operator fun invoke(): Result<SyncResult> {
        return Result.success(SyncResult(0, 0, emptyList(), System.currentTimeMillis()))
    }
}

/**
 * 获取同步状态用例 (Stub)
 */
class GetSyncStateUseCase @Inject constructor(
    private val repository: SyncRepository
) {
    operator fun invoke(): Flow<com.nextthing.app.data.repository.SyncState> {
        return repository.syncState
    }
}

/**
 * 获取同步冲突用例 (Stub)
 */
class GetSyncConflictsUseCase @Inject constructor(
    private val repository: SyncRepository
) {
    suspend operator fun invoke(): List<com.nextthing.app.domain.model.SyncConflict> {
        return emptyList()
    }
}

/**
 * 使用服务器版本解决冲突用例 (Stub)
 */
class ResolveConflictUseServerUseCase @Inject constructor(
    private val repository: SyncRepository
) {
    suspend operator fun invoke(taskId: String): Result<Unit> {
        return Result.success(Unit)
    }
}

/**
 * 使用本地版本解决冲突用例 (Stub)
 */
class ResolveConflictUseLocalUseCase @Inject constructor(
    private val repository: SyncRepository
) {
    suspend operator fun invoke(taskId: String): Result<Unit> {
        return Result.success(Unit)
    }
}

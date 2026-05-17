package com.nextthing.app.domain.usecase

import com.nextthing.app.data.repository.SyncState
import com.nextthing.app.domain.model.SyncConflict
import com.nextthing.app.domain.model.SyncResult
import com.nextthing.app.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SyncTasksUseCase @Inject constructor(
    private val repository: SyncRepository
) {
    suspend operator fun invoke(): Result<SyncResult> {
        return repository.sync()
    }
}

class FullSyncUseCase @Inject constructor(
    private val repository: SyncRepository
) {
    suspend operator fun invoke(): Result<SyncResult> {
        return repository.fullSync()
    }
}

class GetSyncStateUseCase @Inject constructor(
    private val repository: SyncRepository
) {
    operator fun invoke(): Flow<SyncState> {
        return repository.syncState
    }
}

class GetSyncConflictsUseCase @Inject constructor(
    private val repository: SyncRepository
) {
    suspend operator fun invoke(): List<SyncConflict> {
        return repository.getConflicts()
    }
}

class ResolveConflictUseServerUseCase @Inject constructor(
    private val repository: SyncRepository
) {
    suspend operator fun invoke(taskId: String): Result<Unit> {
        return repository.resolveConflictUseServer(taskId)
    }
}

class ResolveConflictUseLocalUseCase @Inject constructor(
    private val repository: SyncRepository
) {
    suspend operator fun invoke(taskId: String): Result<Unit> {
        val conflicts = repository.getConflicts()
        val conflict = conflicts.find { it.taskId == taskId }
        if (conflict != null) {
            return Result.success(Unit)
        }
        return repository.resolveConflictUseServer(taskId)
    }
}

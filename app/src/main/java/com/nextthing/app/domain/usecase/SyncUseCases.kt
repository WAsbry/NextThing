package com.nextthing.app.domain.usecase

/**
 * 同步相关UseCase集合
 */
data class SyncUseCases(
    val syncTasks: SyncTasksUseCase,
    val fullSync: FullSyncUseCase,
    val getSyncState: GetSyncStateUseCase,
    val getConflicts: GetSyncConflictsUseCase,
    val resolveConflictUseServer: ResolveConflictUseServerUseCase,
    val resolveConflictUseLocal: ResolveConflictUseLocalUseCase
)

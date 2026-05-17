package com.nextthing.app.domain.usecase

import javax.inject.Inject

data class SyncUseCases @Inject constructor(
    val syncTasks: SyncTasksUseCase,
    val fullSync: FullSyncUseCase,
    val getSyncState: GetSyncStateUseCase,
    val getConflicts: GetSyncConflictsUseCase,
    val resolveConflictUseServer: ResolveConflictUseServerUseCase,
    val resolveConflictUseLocal: ResolveConflictUseLocalUseCase
)

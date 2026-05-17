package com.nextthing.app.presentation.screens.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextthing.app.data.repository.SyncState
import com.nextthing.app.domain.model.SyncConflict
import com.nextthing.app.domain.model.SyncResult
import com.nextthing.app.domain.usecase.SyncUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 同步ViewModel
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncUseCases: SyncUseCases
) : ViewModel() {

    companion object {
        private const val TAG = "同步VM"
    }

    // 同步状态
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // 同步结果
    private val _syncResult = MutableStateFlow<SyncResult?>(null)
    val syncResult: StateFlow<SyncResult?> = _syncResult.asStateFlow()

    // 冲突列表
    private val _conflicts = MutableStateFlow<List<SyncConflict>>(emptyList())
    val conflicts: StateFlow<List<SyncConflict>> = _conflicts.asStateFlow()

    // 是否正在同步
    val isSyncing: StateFlow<Boolean> = syncState.map { it is SyncState.Syncing }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        // 收集同步状态
        viewModelScope.launch {
            syncUseCases.getSyncState().collect { state ->
                _syncState.value = state
            }
        }

        // 加载冲突列表
        loadConflicts()
    }

    /**
     * 执行同步
     */
    fun sync() {
        viewModelScope.launch {
            Timber.tag(TAG).d("🔄 用户触发同步")
            syncUseCases.syncTasks()
                .onSuccess { result ->
                    _syncResult.value = result
                    Timber.tag(TAG).d("✅ 同步成功: 上传${result.uploadedTasks}条, 下载${result.downloadedTasks}条")
                    loadConflicts()
                }
                .onFailure { error ->
                    Timber.tag(TAG).e(error, "❌ 同步失败")
                }
        }
    }

    /**
     * 执行全量同步
     */
    fun fullSync() {
        viewModelScope.launch {
            Timber.tag(TAG).d("🔄 用户触发全量同步")
            syncUseCases.fullSync()
                .onSuccess { result ->
                    _syncResult.value = result
                    Timber.tag(TAG).d("✅ 全量同步成功: 下载${result.downloadedTasks}条")
                    loadConflicts()
                }
                .onFailure { error ->
                    Timber.tag(TAG).e(error, "❌ 全量同步失败")
                }
        }
    }

    /**
     * 加载冲突列表
     */
    private fun loadConflicts() {
        viewModelScope.launch {
            _conflicts.value = syncUseCases.getConflicts()
        }
    }

    /**
     * 解决冲突（使用服务器版本）
     */
    fun resolveConflictUseServer(taskId: String) {
        viewModelScope.launch {
            Timber.tag(TAG).d("解决冲突 - 使用服务器版本: $taskId")
            syncUseCases.resolveConflictUseServer(taskId)
                .onSuccess {
                    Timber.tag(TAG).d("✅ 冲突已解决")
                    loadConflicts()
                }
                .onFailure { error ->
                    Timber.tag(TAG).e(error, "❌ 解决冲突失败")
                }
        }
    }
}

package com.nextthing.app.data.repository

import androidx.room.withTransaction

import com.nextthing.app.data.local.dao.TaskDao
import com.nextthing.app.data.local.dao.CategoryDao
import com.nextthing.app.data.local.database.TaskDatabase
import com.nextthing.app.data.local.entity.SyncStatus
import com.nextthing.app.data.local.entity.TaskEntity
import com.nextthing.app.data.local.entity.CategoryEntity
import com.nextthing.app.data.remote.api.SyncApi
import com.nextthing.app.data.preferences.SyncPreferences
import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.data.remote.dto.*
import com.nextthing.app.data.mapper.toDomain
import com.nextthing.app.data.mapper.toEntity
import com.nextthing.app.data.mapper.toSyncDto
import com.nextthing.app.data.mapper.CategoryMapper.toEntity as categoryToEntity
import com.nextthing.app.domain.repository.SyncRepository
import com.nextthing.app.domain.model.SyncResult
import com.nextthing.app.domain.model.SyncConflict
import com.nextthing.app.domain.model.FullSyncBlockedByPendingChangesException
import com.nextthing.app.domain.model.SyncAccountMismatchException
import com.nextthing.app.domain.model.SyncAuthenticationRequiredException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据同步仓库实现
 */
@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val database: TaskDatabase,
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val syncApi: SyncApi,
    private val syncPreferences: SyncPreferences,
    private val tokenManager: TokenManager
) : SyncRepository {

    companion object {
        private const val TAG = "数据同步"
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: Flow<SyncState> = _syncState.asStateFlow()

    private var lastSyncTimestamp: Long? = null
    private val syncMutex = Mutex()

    /**
     * 执行增量同步
     */
    override suspend fun sync(): Result<SyncResult> = syncMutex.withLock {
        try {
            _syncState.value = SyncState.Syncing
            ensureSyncAccountBinding()
            if (lastSyncTimestamp == null) {
                lastSyncTimestamp = syncPreferences.lastSyncTimestamp.first()
            }
            Timber.tag(TAG).d("🔄 开始数据同步...")

            // 1. 获取本地待同步的任务
            val pendingTasks = getPendingSyncTasks()
            Timber.tag(TAG).d("📤 待上传任务数: ${pendingTasks.size}")

            // Categories are parent rows for tasks and must be applied first.
            val categorySyncResult = syncCategories(
                requiredCategoryIds = pendingTasks.mapTo(linkedSetOf()) { it.categoryId }
            )

            val taskSyncResult = syncTasks(pendingTasks)

            // Categories must run before tasks for the foreign key, but that creates a
            // time window in which a category update could otherwise be skipped. Close
            // that window with one catch-up read, then advance to the task watermark.
            // Keeping the older pre-task watermark caused the next local edit to conflict
            // with the very task version this device had just uploaded.
            val categoryCatchUpResult = if (
                categorySyncResult.serverTimestamp < taskSyncResult.serverTimestamp
            ) {
                fetchCategoryUpdatesSince(categorySyncResult.serverTimestamp)
            } else {
                categorySyncResult
            }
            lastSyncTimestamp = minOf(
                categoryCatchUpResult.serverTimestamp,
                taskSyncResult.serverTimestamp
            )
            syncPreferences.saveLastSyncTimestamp(lastSyncTimestamp!!)

            _syncState.value = SyncState.Success(lastSyncTimestamp!!)

            val result = SyncResult(
                uploadedTasks = taskSyncResult.uploaded,
                downloadedTasks = taskSyncResult.downloaded,
                conflicts = taskSyncResult.conflicts,
                timestamp = lastSyncTimestamp!!
            )

            Timber.tag(TAG).d("✅ 同步完成: 上传${result.uploadedTasks}条, 下载${result.downloadedTasks}条, 冲突${result.conflicts.size}条")
            Result.success(result)

        } catch (e: CancellationException) {
            _syncState.value = SyncState.Idle
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 同步失败")
            _syncState.value = SyncState.Error(e.message ?: "同步失败")
            Result.failure(e)
        }
    }

    /**
     * 全量同步（首次登录使用）
     */
    override suspend fun fullSync(): Result<SyncResult> = syncMutex.withLock {
        try {
            _syncState.value = SyncState.Syncing
            ensureSyncAccountBinding()
            val pendingTasks = taskDao.getPendingSyncTasks()
            val pendingCategories = categoryDao.getPendingSyncCategories()
            if (pendingTasks.isNotEmpty() || pendingCategories.isNotEmpty()) {
                throw FullSyncBlockedByPendingChangesException(
                    pendingTaskCount = pendingTasks.size,
                    pendingCategoryCount = pendingCategories.size
                )
            }

            if (lastSyncTimestamp == null) {
                lastSyncTimestamp = syncPreferences.lastSyncTimestamp.first()
            }
            Timber.tag(TAG).d("🔄 开始全量同步...")

            val response = syncApi.fullSync()
            if (!response.isSuccessful) {
                throw Exception("全量同步失败: ${response.code()}")
            }

            val body = response.body() ?: throw Exception("响应体为空")

            database.withTransaction {
                taskDao.deleteAllTasks()

                // Tasks reference categories through a foreign key, so parents must be restored first.
                body.categories.forEach { categoryDto ->
                    categoryDao.insertCategory(categoryDto.categoryToEntity(SyncStatus.SYNCED))
                }

                body.tasks.forEach { taskDto ->
                    taskDao.insertTask(taskDto.toEntity(SyncStatus.SYNCED))
                }
            }

            lastSyncTimestamp = body.serverTimestamp
            syncPreferences.saveLastSyncTimestamp(lastSyncTimestamp!!)
            _syncState.value = SyncState.Success(lastSyncTimestamp!!)

            val result = SyncResult(
                uploadedTasks = 0,
                downloadedTasks = body.tasks.size,
                conflicts = emptyList(),
                timestamp = lastSyncTimestamp!!
            )

            Timber.tag(TAG).d("✅ 全量同步完成: 下载${result.downloadedTasks}条任务")
            Result.success(result)

        } catch (e: CancellationException) {
            _syncState.value = SyncState.Idle
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 全量同步失败")
            _syncState.value = SyncState.Error(e.message ?: "全量同步失败")
            Result.failure(e)
        }
    }

    /**
     * 标记任务需要同步
     */
    override suspend fun markTaskForSync(taskId: String) {
        Timber.tag(TAG).d("标记任务待同步: $taskId")
        taskDao.updateSyncStatus(taskId, SyncStatus.PENDING, null)
    }

    /**
     * 获取同步冲突列表
     */
    override suspend fun getConflicts(): List<SyncConflict> {
        val conflictTasks = taskDao.getConflictTasks()
        return conflictTasks.map { task ->
            SyncConflict(
                taskId = task.id,
                taskTitle = task.title,
                localModifiedAt = task.updatedAt.toTimestamp(),
                serverModifiedAt = task.serverUpdatedAt ?: 0,
                conflictType = com.nextthing.app.domain.model.ConflictType.BOTH_MODIFIED
            )
        }
    }

    private fun LocalDateTime.toTimestamp(): Long {
        return atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * 解决同步冲突（使用服务器版本）
     */
    override suspend fun resolveConflictUseServer(taskId: String): Result<Unit> {
        return try {
            ensureSyncAccountBinding()
            val response = syncApi.resolveConflictUseServer(taskId)
            if (!response.isSuccessful) {
                throw Exception("解决冲突失败: ${response.code()}")
            }

            val taskDto = response.body() ?: throw Exception("解决冲突响应体为空")
            if (taskDto.deleted) {
                taskDao.deleteTaskById(taskDto.id)
            } else {
                taskDao.insertTask(taskDto.toEntity(SyncStatus.SYNCED))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 解决同步冲突（使用本地版本）
     */
    override suspend fun resolveConflictUseLocal(taskId: String): Result<Unit> {
        return try {
            ensureSyncAccountBinding()
            val task = taskDao.getTaskEntityByIdIncludingDeleted(taskId)
                ?: return Result.failure(IllegalArgumentException("任务不存在"))
            val response = syncApi.resolveConflictUseLocal(task.id, task.toSyncDto())
            if (!response.isSuccessful) {
                throw Exception("解决冲突失败: ${response.code()}")
            }

            val resolvedTask = response.body() ?: throw Exception("解决冲突响应体为空")
            if (resolvedTask.id != taskId) {
                taskDao.deleteTaskById(taskId)
            }
            if (resolvedTask.deleted) {
                taskDao.deleteTaskById(resolvedTask.id)
            } else {
                taskDao.insertTask(resolvedTask.toEntity(SyncStatus.SYNCED))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取待同步的任务
     */
    private suspend fun getPendingSyncTasks(): List<TaskEntity> {
        return taskDao.getPendingSyncTasks()
    }

    private suspend fun ensureSyncAccountBinding() {
        val currentUserId = tokenManager.serverUserId.first()
            ?: throw SyncAuthenticationRequiredException()
        val boundUserId = syncPreferences.boundServerUserId.first()
        when {
            boundUserId == null -> syncPreferences.bindServerUserId(currentUserId)
            boundUserId != currentUserId ->
                throw SyncAccountMismatchException(boundUserId, currentUserId)
        }
    }

    /**
     * 同步任务数据
     */
    private suspend fun syncTasks(pendingTasks: List<TaskEntity>): TaskSyncResult {
        if (pendingTasks.isEmpty() && lastSyncTimestamp != null) {
            // 如果没有待上传的任务，只获取服务器更新
            val response = syncApi.fetchTaskUpdates(lastSyncTimestamp)
            if (!response.isSuccessful) {
                throw Exception("获取任务更新失败: ${response.code()}")
            }

            val body = response.body() ?: throw Exception("任务更新响应体为空")

            // 处理服务器返回的任务
            body.tasks.forEach { taskDto ->
                if (taskDto.deleted) {
                    taskDao.deleteTaskById(taskDto.id)
                } else {
                    taskDao.insertTask(taskDto.toEntity(SyncStatus.SYNCED))
                }
            }

            return TaskSyncResult(
                uploaded = 0,
                downloaded = body.tasks.count { !it.deleted },
                conflicts = body.conflicts?.map { it.toDomain() } ?: emptyList(),
                serverTimestamp = body.serverTimestamp
            )
        }

        val request = TaskSyncRequest(
            tasks = pendingTasks.map { it.toSyncDto() },
            lastSyncTimestamp = lastSyncTimestamp
        )

        val response = syncApi.syncTasks(request)
        if (!response.isSuccessful) {
            // Keep failed writes retryable. ERROR is not queried by getPendingSyncTasks(),
            // so moving the row out of PENDING would make a later worker run report a
            // false success without ever uploading this local change.
            pendingTasks.forEach { task ->
                taskDao.updateSyncError(task.id, SyncStatus.PENDING, "上传失败: ${response.code()}")
            }
            throw Exception("任务同步失败: ${response.code()}")
        }

        val body = response.body() ?: throw Exception("响应体为空")

        val conflictIds = body.conflicts.orEmpty().mapTo(mutableSetOf()) { it.taskId }
        val deduplicatedTaskIds = body.deduplicatedTaskIds.orEmpty().toSet()

        // 冲突任务保留本地版本，等待用户选择；其余服务器更新可安全落库。
        body.tasks.forEach { taskDto ->
            if (taskDto.id in conflictIds) return@forEach
            if (taskDto.deleted) {
                taskDao.deleteTaskById(taskDto.id)
            } else {
                taskDao.insertTask(taskDto.toEntity(SyncStatus.SYNCED))
            }
        }

        // 只确认服务端已接受的任务；删除墓碑在服务端确认后即可本地清理。
        pendingTasks.forEach { task ->
            when {
                task.id in conflictIds ->
                    taskDao.updateSyncError(task.id, SyncStatus.CONFLICT, "本地与服务端均已修改")
                task.id in deduplicatedTaskIds ->
                    taskDao.deleteTaskById(task.id)
                task.deleted ->
                    taskDao.deleteTaskById(task.id)
                else ->
                    taskDao.updateSyncStatus(task.id, SyncStatus.SYNCED, System.currentTimeMillis())
            }
        }

        return TaskSyncResult(
            uploaded = pendingTasks.count { it.id !in conflictIds },
            downloaded = body.tasks.count { !it.deleted },
            conflicts = body.conflicts?.map { it.toDomain() } ?: emptyList(),
            serverTimestamp = body.serverTimestamp
        )
    }

    /**
     * 同步分类数据
     */
    private suspend fun syncCategories(requiredCategoryIds: Set<String>): CategorySyncResult {
        // A category can be locally marked SYNCED even when a new account has no
        // corresponding cloud parent (for example, app-seeded preset categories).
        // Always include the parents referenced by pending tasks before task upload.
        val explicitlyPendingCategories = categoryDao.getPendingSyncCategories()
        val requiredCategories = if (requiredCategoryIds.isEmpty()) {
            emptyList()
        } else {
            categoryDao.getCategoriesByIdsIncludingDeleted(requiredCategoryIds)
        }
        val pendingCategories = (explicitlyPendingCategories + requiredCategories)
            .distinctBy { it.id }

        if (pendingCategories.isEmpty() && lastSyncTimestamp != null) {
            return fetchCategoryUpdatesSince(lastSyncTimestamp!!)
        }

        val request = CategorySyncRequest(
            categories = pendingCategories.map { it.toSyncDto() },
            lastSyncTimestamp = lastSyncTimestamp
        )

        val response = syncApi.syncCategories(request)
        if (!response.isSuccessful) {
            throw Exception("分类同步失败: ${response.code()}")
        }

        val body = response.body() ?: throw Exception("分类同步响应体为空")
        val conflictIds = body.conflicts.orEmpty().mapTo(mutableSetOf()) { it.categoryId }

        // 冲突分类保留本地版本，等待后续冲突处理。
        body.categories.forEach { categoryDto ->
            if (categoryDto.id in conflictIds) return@forEach
            categoryDao.insertCategory(categoryDto.categoryToEntity(SyncStatus.SYNCED))
        }

        // 只确认服务端已接受的分类；已确认的删除墓碑继续保留以满足任务外键。
        pendingCategories.forEach { category ->
            when {
                category.id in conflictIds ->
                    categoryDao.updateSyncStatus(category.id, SyncStatus.CONFLICT, null)
                category.deleted ->
                    categoryDao.updateSyncStatus(
                        category.id,
                        SyncStatus.SYNCED,
                        body.serverTimestamp
                    )
                else ->
                    categoryDao.updateSyncStatus(category.id, SyncStatus.SYNCED, System.currentTimeMillis())
            }
        }

        return CategorySyncResult(
            uploaded = pendingCategories.count { it.id !in conflictIds },
            downloaded = body.categories.count { !it.deleted },
            conflicts = body.conflicts?.map { it.toDomain() } ?: emptyList(),
            serverTimestamp = body.serverTimestamp
        )
    }

    private suspend fun fetchCategoryUpdatesSince(timestamp: Long): CategorySyncResult {
        val response = syncApi.fetchCategoryUpdates(timestamp)
        if (!response.isSuccessful) {
            throw Exception("获取分类更新失败: ${response.code()}")
        }

        val body = response.body() ?: throw Exception("分类更新响应体为空")
        body.categories.forEach { categoryDto ->
            // Keep tombstones because historical tasks may still reference this row
            // through a RESTRICT foreign key. Category queries already hide deleted rows.
            categoryDao.insertCategory(categoryDto.categoryToEntity(SyncStatus.SYNCED))
        }

        return CategorySyncResult(
            uploaded = 0,
            downloaded = body.categories.count { !it.deleted },
            conflicts = body.conflicts?.map { it.toDomain() } ?: emptyList(),
            serverTimestamp = body.serverTimestamp
        )
    }

    // 数据类定义
    private data class TaskSyncResult(
        val uploaded: Int,
        val downloaded: Int,
        val conflicts: List<SyncConflict>,
        val serverTimestamp: Long
    )

    private data class CategorySyncResult(
        val uploaded: Int,
        val downloaded: Int,
        val conflicts: List<SyncConflict>,
        val serverTimestamp: Long
    )
}

/**
 * 同步状态
 */
sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val timestamp: Long) : SyncState()
    data class Error(val message: String) : SyncState()
}

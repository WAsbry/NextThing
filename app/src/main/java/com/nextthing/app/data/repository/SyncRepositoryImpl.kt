package com.nextthing.app.data.repository

import com.nextthing.app.data.local.dao.TaskDao
import com.nextthing.app.data.local.dao.CategoryDao
import com.nextthing.app.data.local.entity.SyncStatus
import com.nextthing.app.data.local.entity.TaskEntity
import com.nextthing.app.data.local.entity.CategoryEntity
import com.nextthing.app.data.remote.api.SyncApi
import com.nextthing.app.data.remote.dto.*
import com.nextthing.app.data.mapper.toDomain
import com.nextthing.app.data.mapper.toEntity
import com.nextthing.app.data.mapper.toSyncDto
import com.nextthing.app.data.mapper.CategoryMapper.toEntity as categoryToEntity
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.repository.SyncRepository
import com.nextthing.app.domain.model.SyncResult
import com.nextthing.app.domain.model.SyncConflict
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val syncApi: SyncApi
) : SyncRepository {

    companion object {
        private const val TAG = "数据同步"
    }

    // 同步状态
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: Flow<SyncState> = _syncState.asStateFlow()

    // 最后同步时间戳
    private var lastSyncTimestamp: Long? = null

    /**
     * 执行增量同步
     */
    override suspend fun sync(): Result<SyncResult> {
        return try {
            _syncState.value = SyncState.Syncing
            Timber.tag(TAG).d("🔄 开始数据同步...")

            // 1. 获取本地待同步的任务
            val pendingTasks = getPendingSyncTasks()
            Timber.tag(TAG).d("📤 待上传任务数: ${pendingTasks.size}")

            // 2. 同步任务数据
            val taskSyncResult = syncTasks(pendingTasks)

            // 3. 同步分类数据
            val categorySyncResult = syncCategories()

            // 4. 更新最后同步时间
            lastSyncTimestamp = System.currentTimeMillis()

            _syncState.value = SyncState.Success(lastSyncTimestamp!!)

            val result = SyncResult(
                uploadedTasks = taskSyncResult.uploaded,
                downloadedTasks = taskSyncResult.downloaded,
                conflicts = taskSyncResult.conflicts,
                timestamp = lastSyncTimestamp!!
            )

            Timber.tag(TAG).d("✅ 同步完成: 上传${result.uploadedTasks}条, 下载${result.downloadedTasks}条, 冲突${result.conflicts.size}条")
            Result.success(result)

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 同步失败")
            _syncState.value = SyncState.Error(e.message ?: "同步失败")
            Result.failure(e)
        }
    }

    /**
     * 全量同步（首次登录使用）
     */
    override suspend fun fullSync(): Result<SyncResult> {
        return try {
            _syncState.value = SyncState.Syncing
            Timber.tag(TAG).d("🔄 开始全量同步...")

            val response = syncApi.fullSync()
            if (!response.isSuccessful) {
                throw Exception("全量同步失败: ${response.code()}")
            }

            val body = response.body() ?: throw Exception("响应体为空")

            // 清空本地数据并导入服务器数据
            taskDao.deleteAllTasks()

            // 保存服务器任务
            body.tasks.forEach { taskDto ->
                taskDao.insertTask(taskDto.toEntity(SyncStatus.SYNCED))
            }

            // 保存分类
            body.categories.forEach { categoryDto ->
                categoryDao.insertCategory(categoryDto.categoryToEntity(SyncStatus.SYNCED))
            }

            lastSyncTimestamp = body.serverTimestamp
            _syncState.value = SyncState.Success(lastSyncTimestamp!!)

            val result = SyncResult(
                uploadedTasks = 0,
                downloadedTasks = body.tasks.size,
                conflicts = emptyList(),
                timestamp = lastSyncTimestamp!!
            )

            Timber.tag(TAG).d("✅ 全量同步完成: 下载${result.downloadedTasks}条任务")
            Result.success(result)

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
        Timber.tag(TAG).d("📝 标记任务待同步: $taskId")
        // 通过DAO更新同步状态为PENDING
        // 注意：需要在TaskDao中添加相应的方法
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
            val response = syncApi.resolveConflictUseServer(taskId)
            if (!response.isSuccessful) {
                throw Exception("解决冲突失败: ${response.code()}")
            }

            response.body()?.let { taskDto ->
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
    override suspend fun resolveConflictUseLocal(task: Task): Result<Unit> {
        return try {
            val taskDto = task.toSyncDto()
            val response = syncApi.resolveConflictUseLocal(task.id, taskDto)
            if (!response.isSuccessful) {
                throw Exception("解决冲突失败: ${response.code()}")
            }

            // 更新本地任务为已同步状态
            val entity = task.toEntity().copy(syncStatus = SyncStatus.SYNCED)
            taskDao.updateTask(entity)

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

            val body = response.body() ?: return TaskSyncResult(0, 0, emptyList())

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
                conflicts = body.conflicts?.map { it.toDomain() } ?: emptyList()
            )
        }

        val request = TaskSyncRequest(
            tasks = pendingTasks.map { it.toSyncDto() },
            lastSyncTimestamp = lastSyncTimestamp
        )

        val response = syncApi.syncTasks(request)
        if (!response.isSuccessful) {
            // 标记上传失败
            pendingTasks.forEach { task ->
                taskDao.updateSyncError(task.id, SyncStatus.ERROR, "上传失败: ${response.code()}")
            }
            throw Exception("任务同步失败: ${response.code()}")
        }

        val body = response.body() ?: throw Exception("响应体为空")

        // 处理服务器返回的任务
        body.tasks.forEach { taskDto ->
            if (taskDto.deleted) {
                taskDao.deleteTaskById(taskDto.id)
            } else {
                taskDao.insertTask(taskDto.toEntity(SyncStatus.SYNCED))
            }
        }

        // 将已上传的任务标记为已同步
        pendingTasks.forEach { task ->
            taskDao.updateSyncStatus(task.id, SyncStatus.SYNCED, System.currentTimeMillis())
        }

        return TaskSyncResult(
            uploaded = pendingTasks.size,
            downloaded = body.tasks.count { !it.deleted },
            conflicts = body.conflicts?.map { it.toDomain() } ?: emptyList()
        )
    }

    /**
     * 同步分类数据
     */
    private suspend fun syncCategories(): CategorySyncResult {
        // 获取待同步的分类
        val pendingCategories = categoryDao.getPendingSyncCategories()

        if (pendingCategories.isEmpty() && lastSyncTimestamp != null) {
            // 只获取更新
            val response = syncApi.fetchCategoryUpdates(lastSyncTimestamp)
            if (!response.isSuccessful) {
                Timber.tag(TAG).w("分类同步失败: ${response.code()}")
                return CategorySyncResult(0, 0, emptyList())
            }

            val body = response.body() ?: return CategorySyncResult(0, 0, emptyList())

            body.categories.forEach { categoryDto ->
                if (categoryDto.deleted) {
                    categoryDao.deleteCategoryById(categoryDto.id)
                } else {
                    categoryDao.insertCategory(categoryDto.categoryToEntity(SyncStatus.SYNCED))
                }
            }

            return CategorySyncResult(
                uploaded = 0,
                downloaded = body.categories.count { !it.deleted },
                conflicts = body.conflicts?.map { it.toDomain() } ?: emptyList()
            )
        }

        val request = CategorySyncRequest(
            categories = pendingCategories.map { it.toSyncDto() },
            lastSyncTimestamp = lastSyncTimestamp
        )

        val response = syncApi.syncCategories(request)
        if (!response.isSuccessful) {
            Timber.tag(TAG).w("分类同步失败: ${response.code()}")
            return CategorySyncResult(0, 0, emptyList())
        }

        val body = response.body() ?: return CategorySyncResult(0, 0, emptyList())

        // 处理服务器返回的分类
        body.categories.forEach { categoryDto ->
            if (categoryDto.deleted) {
                categoryDao.deleteCategoryById(categoryDto.id)
            } else {
                categoryDao.insertCategory(categoryDto.categoryToEntity(SyncStatus.SYNCED))
            }
        }

        // 标记已同步
        pendingCategories.forEach { category ->
            categoryDao.updateSyncStatus(category.id, SyncStatus.SYNCED, System.currentTimeMillis())
        }

        return CategorySyncResult(
            uploaded = pendingCategories.size,
            downloaded = body.categories.count { !it.deleted },
            conflicts = body.conflicts?.map { it.toDomain() } ?: emptyList()
        )
    }

    // 数据类定义
    private data class TaskSyncResult(
        val uploaded: Int,
        val downloaded: Int,
        val conflicts: List<SyncConflict>
    )

    private data class CategorySyncResult(
        val uploaded: Int,
        val downloaded: Int,
        val conflicts: List<SyncConflict>
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

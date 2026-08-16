package com.nextthing.app.repository

import com.nextthing.app.data.local.dao.CategoryDao
import com.nextthing.app.data.local.dao.TaskDao
import com.nextthing.app.data.local.database.TaskDatabase
import com.nextthing.app.data.local.entity.CategoryEntity
import com.nextthing.app.data.local.entity.TaskEntity
import com.nextthing.app.data.mapper.toSyncDto
import com.nextthing.app.data.preferences.SyncPreferences
import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.data.remote.api.SyncApi
import com.nextthing.app.data.remote.dto.CategorySyncDto
import com.nextthing.app.data.remote.dto.CategorySyncResponse
import com.nextthing.app.data.remote.dto.TaskSyncResponse
import com.nextthing.app.data.repository.SyncRepositoryImpl
import com.nextthing.app.domain.model.FullSyncBlockedByPendingChangesException
import com.nextthing.app.domain.model.SyncAccountMismatchException
import com.nextthing.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response
import java.time.LocalDateTime

class SyncRepositoryImplTest {

    private val database = mock<TaskDatabase>()
    private val taskDao = mock<TaskDao>()
    private val categoryDao = mock<CategoryDao>()
    private val syncApi = mock<SyncApi>()
    private val syncPreferences = mock<SyncPreferences>()
    private val tokenManager = mock<TokenManager>()
    private val repository = SyncRepositoryImpl(
        database,
        taskDao,
        categoryDao,
        syncApi,
        syncPreferences,
        tokenManager
    )

    @Before
    fun setUpAccountBinding() {
        runBlocking {
            whenever(tokenManager.serverUserId).thenReturn(flowOf(7L))
            whenever(syncPreferences.boundServerUserId).thenReturn(flowOf(7L))
            whenever(categoryDao.getCategoriesByIdsIncludingDeleted(any())).thenReturn(emptyList())
            whenever(syncApi.fetchCategoryUpdates(any())).thenAnswer { invocation ->
                val since = invocation.getArgument<Long?>(0) ?: 0L
                Response.success(
                    CategorySyncResponse(
                        categories = emptyList(),
                        serverTimestamp = since + 100L,
                        conflicts = emptyList()
                    )
                )
            }
        }
    }

    @Test
    fun `local deletion conflict uploads tombstone and removes acknowledged row`() = runTest {
        val task = deletedTask()
        whenever(taskDao.getTaskEntityByIdIncludingDeleted(task.id)).thenReturn(task)
        whenever(syncApi.resolveConflictUseLocal(eq(task.id), any()))
            .thenReturn(Response.success(task.toSyncDto()))

        val result = repository.resolveConflictUseLocal(task.id)

        assertTrue(result.isSuccess)
        verify(syncApi).resolveConflictUseLocal(
            eq(task.id),
            check { assertTrue(it.deleted) }
        )
        verify(taskDao).deleteTaskById(task.id)
    }

    @Test
    fun `incremental sync closes the category gap and advances to task watermark`() = runTest {
        whenever(syncPreferences.lastSyncTimestamp).thenReturn(flowOf(100L))
        whenever(taskDao.getPendingSyncTasks()).thenReturn(emptyList())
        whenever(categoryDao.getPendingSyncCategories()).thenReturn(emptyList())
        whenever(syncApi.fetchCategoryUpdates(100L)).thenReturn(
            Response.success(
                CategorySyncResponse(
                    categories = listOf(
                        CategorySyncDto(
                            id = "category-deleted",
                            name = "Archived",
                            type = 1,
                            icon = "archive",
                            colorHex = "#999999",
                            sortOrder = 1,
                            createdAt = 50L,
                            isEnabled = false,
                            deleted = true
                        )
                    ),
                    serverTimestamp = 200L,
                    conflicts = emptyList()
                )
            )
        )
        whenever(syncApi.fetchTaskUpdates(100L)).thenReturn(
            Response.success(
                TaskSyncResponse(
                    tasks = emptyList(),
                    serverTimestamp = 250L,
                    conflicts = emptyList()
                )
            )
        )

        val result = repository.sync()

        assertTrue(result.isSuccess)
        inOrder(syncApi) {
            verify(syncApi).fetchCategoryUpdates(100L)
            verify(syncApi).fetchTaskUpdates(100L)
            verify(syncApi).fetchCategoryUpdates(200L)
        }
        verify(syncPreferences).saveLastSyncTimestamp(250L)
        verify(categoryDao).insertCategory(check { assertTrue(it.deleted) })
        verify(categoryDao, never()).deleteCategoryById("category-deleted")
        assertEquals(250L, result.getOrThrow().timestamp)
    }

    @Test
    fun `full sync refuses to overwrite pending local changes`() = runTest {
        whenever(taskDao.getPendingSyncTasks()).thenReturn(listOf(deletedTask()))
        whenever(categoryDao.getPendingSyncCategories()).thenReturn(emptyList())

        val result = repository.fullSync()

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is FullSyncBlockedByPendingChangesException)
        verify(syncApi, never()).fullSync()
        verify(taskDao, never()).deleteAllTasks()
    }

    @Test
    fun `sync refuses to upload local data after cloud account changes`() = runTest {
        whenever(tokenManager.serverUserId).thenReturn(flowOf(8L))
        whenever(syncPreferences.boundServerUserId).thenReturn(flowOf(7L))

        val result = repository.sync()

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is SyncAccountMismatchException)
        verify(syncApi, never()).syncTasks(any())
        verify(syncApi, never()).syncCategories(any())
    }

    @Test
    fun `recurring instance dedup removes local alias and applies canonical server task`() = runTest {
        val local = recurringTask("device-b-instance")
        val canonical = local.toSyncDto().copy(id = "canonical-instance")
        whenever(syncPreferences.lastSyncTimestamp).thenReturn(flowOf(100L))
        whenever(taskDao.getPendingSyncTasks()).thenReturn(listOf(local))
        whenever(categoryDao.getPendingSyncCategories()).thenReturn(emptyList())
        whenever(syncApi.fetchCategoryUpdates(100L)).thenReturn(
            Response.success(
                CategorySyncResponse(
                    categories = emptyList(),
                    serverTimestamp = 200L,
                    conflicts = emptyList()
                )
            )
        )
        whenever(syncApi.syncTasks(any())).thenReturn(
            Response.success(
                TaskSyncResponse(
                    tasks = listOf(canonical),
                    serverTimestamp = 210L,
                    conflicts = emptyList(),
                    deduplicatedTaskIds = listOf(local.id)
                )
            )
        )

        val result = repository.sync()

        assertTrue(result.isSuccess)
        verify(taskDao).deleteTaskById(local.id)
        verify(taskDao).insertTask(check {
            assertEquals("canonical-instance", it.id)
            assertEquals(com.nextthing.app.data.local.entity.SyncStatus.SYNCED, it.syncStatus)
        })
    }

    @Test
    fun `failed task upload stays pending and is retried before sync can succeed`() = runTest {
        val task = recurringTask("retry-task")
        whenever(syncPreferences.lastSyncTimestamp).thenReturn(flowOf(100L))
        whenever(taskDao.getPendingSyncTasks()).thenReturn(listOf(task))
        whenever(categoryDao.getPendingSyncCategories()).thenReturn(emptyList())
        whenever(syncApi.fetchCategoryUpdates(100L)).thenReturn(
            Response.success(
                CategorySyncResponse(
                    categories = emptyList(),
                    serverTimestamp = 200L,
                    conflicts = emptyList()
                )
            )
        )
        whenever(syncApi.syncTasks(any()))
            .thenReturn(Response.error(503, "unavailable".toResponseBody()))
            .thenReturn(
                Response.success(
                    TaskSyncResponse(
                        tasks = emptyList(),
                        serverTimestamp = 210L,
                        conflicts = emptyList()
                    )
                )
            )

        val failed = repository.sync()
        val retried = repository.sync()

        assertFalse(failed.isSuccess)
        assertTrue(retried.isSuccess)
        verify(taskDao).updateSyncError(task.id, com.nextthing.app.data.local.entity.SyncStatus.PENDING, "上传失败: 503")
        verify(syncApi, times(2)).syncTasks(any())
        verify(taskDao).updateSyncStatus(eq(task.id), eq(com.nextthing.app.data.local.entity.SyncStatus.SYNCED), any())
        verify(syncPreferences, never()).saveLastSyncTimestamp(100L)
        verify(syncPreferences).saveLastSyncTimestamp(210L)
    }

    @Test
    fun `task sync uploads its referenced category before the task even when category was locally synced`() = runTest {
        val task = recurringTask("task-with-seeded-category")
        val category = syncedCategory(task.categoryId)
        whenever(syncPreferences.lastSyncTimestamp).thenReturn(flowOf(100L))
        whenever(taskDao.getPendingSyncTasks()).thenReturn(listOf(task))
        whenever(categoryDao.getPendingSyncCategories()).thenReturn(emptyList())
        whenever(categoryDao.getCategoriesByIdsIncludingDeleted(setOf(task.categoryId)))
            .thenReturn(listOf(category))
        whenever(syncApi.syncCategories(any())).thenReturn(
            Response.success(
                CategorySyncResponse(
                    categories = emptyList(),
                    serverTimestamp = 200L,
                    conflicts = emptyList()
                )
            )
        )
        whenever(syncApi.syncTasks(any())).thenReturn(
            Response.success(
                TaskSyncResponse(
                    tasks = emptyList(),
                    serverTimestamp = 210L,
                    conflicts = emptyList()
                )
            )
        )

        val result = repository.sync()

        assertTrue(result.isSuccess)
        inOrder(syncApi) {
            verify(syncApi).syncCategories(check {
                assertEquals(listOf(task.categoryId), it.categories.map { categoryDto -> categoryDto.id })
            })
            verify(syncApi).syncTasks(any())
        }
    }

    private fun deletedTask(): TaskEntity {
        val now = LocalDateTime.of(2026, 7, 28, 20, 0)
        return TaskEntity(
            id = "task-deleted",
            title = "待删除任务",
            description = "",
            categoryId = "category-work",
            status = TaskStatus.PENDING,
            createdAt = now.minusDays(1),
            updatedAt = now,
            dueDate = now.plusDays(1),
            completedAt = null,
            tags = "[]",
            isUrgent = false,
            estimatedDuration = 0,
            actualDuration = 0,
            subtasksJson = "[]",
            deleted = true
        )
    }

    private fun recurringTask(id: String): TaskEntity {
        val now = LocalDateTime.of(2026, 7, 29, 9, 0)
        return TaskEntity(
            id = id,
            title = "每日晨会",
            description = "",
            categoryId = "category-work",
            status = TaskStatus.PENDING,
            createdAt = now.minusDays(1),
            updatedAt = now,
            dueDate = now.plusHours(1),
            completedAt = null,
            tags = "[]",
            isUrgent = true,
            estimatedDuration = 0,
            actualDuration = 0,
            subtasksJson = "[]",
            repeatFrequencyJson = """{"type":"DAILY"}""",
            isTemplate = false,
            templateTaskId = "template-1",
            instanceDate = now.toLocalDate().atStartOfDay()
        )
    }

    private fun syncedCategory(id: String): CategoryEntity {
        return CategoryEntity(
            id = id,
            name = "工作",
            type = 0,
            icon = "drawable:work",
            colorHex = "#42A5F5",
            sortOrder = 0,
            createdAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            isEnabled = true,
            syncStatus = com.nextthing.app.data.local.entity.SyncStatus.SYNCED
        )
    }
}

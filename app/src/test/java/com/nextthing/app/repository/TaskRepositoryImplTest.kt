package com.nextthing.app.repository

import com.nextthing.app.data.local.dao.CategoryDao
import com.nextthing.app.data.local.dao.CategoryTaskCount
import com.nextthing.app.data.local.dao.TaskDao
import com.nextthing.app.data.local.entity.CategoryEntity
import com.nextthing.app.data.local.entity.TaskEntity
import com.nextthing.app.data.local.entity.TaskWithCategory
import com.nextthing.app.data.repository.TaskRepositoryImpl
import com.nextthing.app.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.time.LocalDateTime

class TaskRepositoryImplTest {

    private lateinit var repository: TaskRepositoryImpl
    private lateinit var taskDao: TaskDao
    private lateinit var categoryDao: CategoryDao

    private val now = LocalDateTime.of(2025, 5, 15, 10, 0)
    private val testCategoryEntity = CategoryEntity(
        id = "cat-1", name = "工作", type = 0, icon = "work",
        colorHex = "#42A5F5", sortOrder = 0, createdAt = now
    )
    private val testCategory = Category(
        id = "cat-1", name = "工作", type = CategoryType.PRESET,
        icon = "work", colorHex = "#42A5F5"
    )

    private fun createTaskEntity(
        id: String = "task-1",
        title: String = "测试任务",
        status: TaskStatus = TaskStatus.PENDING,
        categoryId: String = "cat-1"
    ) = TaskEntity(
        id = id, title = title, description = "", categoryId = categoryId,
        status = status, createdAt = now, updatedAt = now,
        dueDate = now.plusHours(2), completedAt = null,
        tags = "[]", isUrgent = false, estimatedDuration = 0, actualDuration = 0,
        subtasksJson = "[]", repeatFrequencyJson = "{}"
    )

    private fun createTaskWithCategory(
        id: String = "task-1",
        title: String = "测试任务",
        status: TaskStatus = TaskStatus.PENDING
    ) = TaskWithCategory(
        task = createTaskEntity(id = id, title = title, status = status),
        category = testCategoryEntity
    )

    @Before
    fun setup() {
        taskDao = mock(TaskDao::class.java)
        categoryDao = mock(CategoryDao::class.java)
        repository = TaskRepositoryImpl(taskDao, categoryDao)
    }

    // ===== insertTask =====

    @Test
    fun `insertTask delegates to DAO and returns id`() = runTest {
        val task = Task(id = "task-1", title = "新任务", category = testCategory)
        `when`(taskDao.insertTask(any())).thenReturn(1L)

        val result = repository.insertTask(task)

        assertEquals("task-1", result)
        verify(taskDao).insertTask(any())
    }

    // ===== updateTask =====

    @Test
    fun `updateTask delegates to DAO`() = runTest {
        val task = Task(id = "task-1", title = "更新后", category = testCategory)

        repository.updateTask(task)

        verify(taskDao).updateTask(any())
    }

    // ===== deleteTask =====

    @Test
    fun `deleteTask calls deleteTaskById`() = runTest {
        repository.deleteTask("task-1")
        verify(taskDao).deleteTaskById("task-1")
    }

    // ===== deleteAllTasks =====

    @Test
    fun `deleteAllTasks delegates to DAO`() = runTest {
        repository.deleteAllTasks()
        verify(taskDao).deleteAllTasks()
    }

    // ===== getTaskById =====

    @Test
    fun `getTaskById returns task when found`() = runTest {
        `when`(taskDao.getTaskById("task-1")).thenReturn(createTaskWithCategory())

        val result = repository.getTaskById("task-1")

        assertNotNull(result)
        assertEquals("task-1", result!!.id)
        assertEquals("测试任务", result.title)
        assertEquals("工作", result.category.name)
    }

    @Test
    fun `getTaskById returns null when not found`() = runTest {
        `when`(taskDao.getTaskById("nonexistent")).thenReturn(null)

        val result = repository.getTaskById("nonexistent")

        assertNull(result)
    }

    // ===== getAllTasks =====

    @Test
    fun `getAllTasks maps entities to domain`() = runTest {
        val entities = listOf(
            createTaskWithCategory(id = "t1", title = "任务1"),
            createTaskWithCategory(id = "t2", title = "任务2")
        )
        `when`(taskDao.getAllTasks()).thenReturn(flowOf(entities))

        val result = repository.getAllTasks().first()

        assertEquals(2, result.size)
        assertEquals("任务1", result[0].title)
        assertEquals("任务2", result[1].title)
    }

    @Test
    fun `getAllTasks returns empty list when no tasks`() = runTest {
        `when`(taskDao.getAllTasks()).thenReturn(flowOf(emptyList()))

        val result = repository.getAllTasks().first()

        assertTrue(result.isEmpty())
    }

    // ===== getTodayTasks =====

    @Test
    fun `getTodayTasks maps correctly`() = runTest {
        val entities = listOf(createTaskWithCategory(title = "今日任务"))
        `when`(taskDao.getTodayTasks()).thenReturn(flowOf(entities))

        val result = repository.getTodayTasks().first()

        assertEquals(1, result.size)
        assertEquals("今日任务", result[0].title)
    }

    // ===== getTaskStatistics =====

    @Test
    fun `getTaskStatistics computes correct rates`() = runTest {
        `when`(taskDao.getTotalTasksCount()).thenReturn(10)
        `when`(taskDao.getCompletedTasksCount()).thenReturn(7)
        `when`(taskDao.getPendingTasksCount()).thenReturn(2)
        `when`(taskDao.getOverdueTasksCount()).thenReturn(1)
        `when`(taskDao.getAverageCompletionTime()).thenReturn(30.0)
        `when`(taskDao.getCategoryTaskCounts()).thenReturn(listOf(CategoryTaskCount("cat-1", 10)))
        `when`(categoryDao.getAllCategoriesList()).thenReturn(listOf(testCategoryEntity))

        val stats = repository.getTaskStatistics()

        assertEquals(10, stats.totalTasks)
        assertEquals(7, stats.completedTasks)
        assertEquals(2, stats.pendingTasks)
        assertEquals(1, stats.overdueTasks)
        assertEquals(0.7f, stats.completionRate, 0.01f)
        assertEquals(30, stats.averageCompletionTime)
    }

    @Test
    fun `getTaskStatistics handles zero tasks`() = runTest {
        `when`(taskDao.getTotalTasksCount()).thenReturn(0)
        `when`(taskDao.getCompletedTasksCount()).thenReturn(0)
        `when`(taskDao.getPendingTasksCount()).thenReturn(0)
        `when`(taskDao.getOverdueTasksCount()).thenReturn(0)
        `when`(taskDao.getAverageCompletionTime()).thenReturn(null)
        `when`(taskDao.getCategoryTaskCounts()).thenReturn(emptyList())
        `when`(categoryDao.getAllCategoriesList()).thenReturn(emptyList())

        val stats = repository.getTaskStatistics()

        assertEquals(0, stats.totalTasks)
        assertEquals(0f, stats.completionRate, 0.01f)
        assertEquals(0, stats.averageCompletionTime)
    }

    // ===== getEarliestTaskDate =====

    @Test
    fun `getEarliestTaskDate returns date when tasks exist`() = runTest {
        val earliest = LocalDateTime.of(2025, 1, 1, 0, 0)
        `when`(taskDao.getEarliestTaskDate()).thenReturn(earliest)

        val result = repository.getEarliestTaskDate()

        assertEquals(earliest.toLocalDate(), result)
    }

    @Test
    fun `getEarliestTaskDate returns null when no tasks`() = runTest {
        `when`(taskDao.getEarliestTaskDate()).thenReturn(null)

        val result = repository.getEarliestTaskDate()

        assertNull(result)
    }

    // ===== getTemplateTasks =====

    @Test
    fun `getTemplateTasks returns template tasks`() = runTest {
        val template = createTaskWithCategory(id = "tpl-1", title = "模板任务").copy(
            task = createTaskEntity(id = "tpl-1", title = "模板任务").copy(isTemplate = true)
        )
        `when`(taskDao.getTemplateTasks()).thenReturn(listOf(template))

        val result = repository.getTemplateTasks()

        assertEquals(1, result.size)
        assertEquals("模板任务", result[0].title)
    }

    // ===== syncTasks / exportTasks / importTasks =====

    @Test
    fun `syncTasks returns success stub`() = runTest {
        val result = repository.syncTasks()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `exportTasks returns success stub`() = runTest {
        val result = repository.exportTasks()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `importTasks returns success stub`() = runTest {
        val result = repository.importTasks("test.json")
        assertTrue(result.isSuccess)
    }

    // ===== deleteCompletedTasks =====

    @Test
    fun `deleteCompletedTasks delegates to DAO`() = runTest {
        repository.deleteCompletedTasks()
        verify(taskDao).deleteCompletedTasks()
    }

    // ===== markTasksAsCompleted =====

    @Test
    fun `markTasksAsCompleted delegates to DAO`() = runTest {
        repository.markTasksAsCompleted(listOf("t1", "t2"))
        verify(taskDao).markTasksAsCompleted(listOf("t1", "t2"))
    }
}

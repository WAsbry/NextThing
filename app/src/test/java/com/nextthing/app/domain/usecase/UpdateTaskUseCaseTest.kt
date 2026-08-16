package com.nextthing.app.domain.usecase

import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.CategoryType
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.model.TaskStatus
import com.nextthing.app.domain.repository.TaskRepository
import com.nextthing.app.util.NotificationHelper
import com.nextthing.app.util.TaskAlarmManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

class UpdateTaskUseCaseTest {

    private val repository = mock<TaskRepository>()
    private val taskAlarmManager = mock<TaskAlarmManager>()
    private val notificationHelper = mock<NotificationHelper>()
    private val useCase = UpdateTaskUseCase(
        repository,
        taskAlarmManager,
        notificationHelper
    )
    private val category = Category(
        id = "category-work",
        name = "工作",
        type = CategoryType.CUSTOM,
        icon = "work",
        colorHex = "#42A5F5"
    )

    @Test
    fun `detail update from pending to completed writes completedAt`() = runTest {
        val existing = task(status = TaskStatus.PENDING)
        whenever(repository.getTaskById(existing.id)).thenReturn(existing)

        val result = useCase(existing.copy(status = TaskStatus.COMPLETED))

        assertTrue(result.isSuccess)
        val updatedTask = argumentCaptor<Task>()
        verify(repository).updateTask(updatedTask.capture())
        assertEquals(TaskStatus.COMPLETED, updatedTask.firstValue.status)
        assertNotNull(updatedTask.firstValue.completedAt)
        verify(taskAlarmManager).cancelTaskAlarm(existing.id)
        verify(notificationHelper).cancelNotification(existing.id)
    }

    @Test
    fun `detail update restores recently completed task and clears completedAt`() = runTest {
        val completedAt = LocalDateTime.now().minusDays(2)
        val existing = task(status = TaskStatus.COMPLETED, completedAt = completedAt)
        whenever(repository.getTaskById(existing.id)).thenReturn(existing)

        val result = useCase(existing.copy(status = TaskStatus.PENDING))

        assertTrue(result.isSuccess)
        val updatedTask = argumentCaptor<Task>()
        verify(repository).updateTask(updatedTask.capture())
        assertEquals(TaskStatus.PENDING, updatedTask.firstValue.status)
        assertEquals(null, updatedTask.firstValue.completedAt)
    }

    @Test
    fun `detail update cannot restore task completed more than seven days ago`() = runTest {
        val existing = task(
            status = TaskStatus.COMPLETED,
            completedAt = LocalDateTime.now().minusDays(8)
        )
        whenever(repository.getTaskById(existing.id)).thenReturn(existing)

        val result = useCase(existing.copy(status = TaskStatus.PENDING))

        assertTrue(result.isFailure)
        verify(repository, never()).updateTask(any())
    }

    @Test
    fun `detail update cannot restore completed task without completedAt`() = runTest {
        val existing = task(status = TaskStatus.COMPLETED, completedAt = null)
        whenever(repository.getTaskById(existing.id)).thenReturn(existing)

        val result = useCase(existing.copy(status = TaskStatus.PENDING))

        assertTrue(result.isFailure)
        verify(repository, never()).updateTask(any())
    }

    @Test
    fun `editing an already completed task preserves original completedAt`() = runTest {
        val completedAt = LocalDateTime.now().minusHours(3)
        val existing = task(status = TaskStatus.COMPLETED, completedAt = completedAt)
        whenever(repository.getTaskById(existing.id)).thenReturn(existing)

        val result = useCase(existing.copy(title = "修改后的标题"))

        assertTrue(result.isSuccess)
        val updatedTask = argumentCaptor<Task>()
        verify(repository).updateTask(updatedTask.capture())
        assertEquals(completedAt, updatedTask.firstValue.completedAt)
    }

    private fun task(
        status: TaskStatus,
        completedAt: LocalDateTime? = null
    ) = Task(
        id = "task-detail",
        title = "详情页状态测试",
        category = category,
        status = status,
        completedAt = completedAt
    )
}

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

class ToggleTaskStatusUseCaseTest {

    private val repository = mock<TaskRepository>()
    private val taskAlarmManager = mock<TaskAlarmManager>()
    private val notificationHelper = mock<NotificationHelper>()
    private val useCase = ToggleTaskStatusUseCase(
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
    fun `pending task completion writes completedAt and cancels reminders`() = runTest {
        val task = Task(
            id = "task-pending",
            title = "待完成任务",
            category = category,
            status = TaskStatus.PENDING,
            completedAt = null
        )
        whenever(repository.getTaskById(task.id)).thenReturn(task)

        val result = useCase(task.id)

        assertTrue(result.isSuccess)
        val updatedTask = argumentCaptor<Task>()
        verify(repository).updateTask(updatedTask.capture())
        assertEquals(TaskStatus.COMPLETED, updatedTask.firstValue.status)
        assertNotNull(updatedTask.firstValue.completedAt)
        verify(taskAlarmManager).cancelTaskAlarm(task.id)
        verify(notificationHelper).cancelNotification(task.id)
    }

    @Test
    fun `completed task without completedAt cannot be restored`() = runTest {
        val task = Task(
            id = "task-invalid-completed",
            title = "缺少完成时间",
            category = category,
            status = TaskStatus.COMPLETED,
            completedAt = null
        )
        whenever(repository.getTaskById(task.id)).thenReturn(task)

        val result = useCase(task.id)

        assertTrue(result.isFailure)
        verify(repository, never()).updateTask(any())
    }

    @Test
    fun `completed task older than seven days cannot be restored`() = runTest {
        val task = completedTask(LocalDateTime.now().minusDays(8))
        whenever(repository.getTaskById(task.id)).thenReturn(task)

        val result = useCase(task.id)

        assertTrue(result.isFailure)
        verify(repository, never()).updateTask(any())
    }

    @Test
    fun `completed task within seven days can be restored`() = runTest {
        val task = completedTask(LocalDateTime.now().minusDays(2))
        whenever(repository.getTaskById(task.id)).thenReturn(task)

        val result = useCase(task.id)

        assertTrue(result.isSuccess)
        val updatedTask = argumentCaptor<Task>()
        verify(repository).updateTask(updatedTask.capture())
        assertEquals(TaskStatus.PENDING, updatedTask.firstValue.status)
        assertEquals(null, updatedTask.firstValue.completedAt)
    }

    private fun completedTask(completedAt: LocalDateTime) = Task(
        id = "task-1",
        title = "已完成任务",
        category = category,
        status = TaskStatus.COMPLETED,
        completedAt = completedAt
    )
}

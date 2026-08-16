package com.nextthing.app.domain.usecase

import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.CategoryType
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.model.TaskStatus
import com.nextthing.app.domain.repository.TaskRepository
import com.nextthing.app.util.TaskAlarmManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime

class DeferTaskUseCaseTest {

    private val repository = mock<TaskRepository>()
    private val taskAlarmManager = mock<TaskAlarmManager>()
    private val useCase = DeferTaskUseCase(repository, taskAlarmManager)
    private val category = Category(
        id = "category-work",
        name = "工作",
        type = CategoryType.CUSTOM,
        icon = "work",
        colorHex = "#42A5F5"
    )

    @Test
    fun `pending task due today is deferred to tomorrow end of day`() = runTest {
        val task = task(
            status = TaskStatus.PENDING,
            dueDate = LocalDate.now().atTime(15, 0)
        )
        whenever(repository.getTaskById(task.id)).thenReturn(task)

        val result = useCase(task.id)

        assertTrue(result.isSuccess)
        val updatedTask = argumentCaptor<Task>()
        verify(repository).updateTask(updatedTask.capture())
        assertEquals(TaskStatus.DELAYED, updatedTask.firstValue.status)
        assertEquals(
            LocalDate.now().plusDays(1).atTime(23, 59, 59),
            updatedTask.firstValue.dueDate
        )
        verify(taskAlarmManager).cancelTaskAlarm(task.id)
    }

    @Test
    fun `deferred task with notification reschedules reminder`() = runTest {
        val task = task(
            status = TaskStatus.PENDING,
            dueDate = LocalDate.now().atTime(18, 0),
            notificationStrategyId = "strategy-1"
        )
        whenever(repository.getTaskById(task.id)).thenReturn(task)

        val result = useCase(task.id)

        assertTrue(result.isSuccess)
        val updatedTask = argumentCaptor<Task>()
        verify(repository).updateTask(updatedTask.capture())
        verify(taskAlarmManager).scheduleTaskAlarm(updatedTask.firstValue)
    }

    @Test
    fun `non pending task cannot be deferred`() = runTest {
        val task = task(
            status = TaskStatus.COMPLETED,
            dueDate = LocalDate.now().atTime(15, 0)
        )
        whenever(repository.getTaskById(task.id)).thenReturn(task)

        val result = useCase(task.id)

        assertTrue(result.isFailure)
        verify(repository, never()).updateTask(any())
    }

    @Test
    fun `past task cannot be deferred`() = runTest {
        val task = task(
            status = TaskStatus.PENDING,
            dueDate = LocalDate.now().minusDays(1).atTime(15, 0)
        )
        whenever(repository.getTaskById(task.id)).thenReturn(task)

        val result = useCase(task.id)

        assertTrue(result.isFailure)
        verify(repository, never()).updateTask(any())
    }

    @Test
    fun `future task cannot be deferred as if it were due today`() = runTest {
        val task = task(
            status = TaskStatus.PENDING,
            dueDate = LocalDate.now().plusDays(2).atTime(15, 0)
        )
        whenever(repository.getTaskById(task.id)).thenReturn(task)

        val result = useCase(task.id)

        assertTrue(result.isFailure)
        verify(repository, never()).updateTask(any())
    }

    @Test
    fun `missing task cannot be deferred`() = runTest {
        whenever(repository.getTaskById("missing")).thenReturn(null)

        val result = useCase("missing")

        assertTrue(result.isFailure)
        verify(repository, never()).updateTask(any())
    }

    private fun task(
        status: TaskStatus,
        dueDate: LocalDateTime?,
        notificationStrategyId: String? = null
    ) = Task(
        id = "task-defer",
        title = "延期测试任务",
        category = category,
        status = status,
        dueDate = dueDate,
        notificationStrategyId = notificationStrategyId
    )
}

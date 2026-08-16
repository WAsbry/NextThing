package com.nextthing.app.domain.usecase

import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.CategoryType
import com.nextthing.app.domain.model.DeleteMode
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.repository.TaskRepository
import com.nextthing.app.util.NotificationHelper
import com.nextthing.app.util.TaskAlarmManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DeleteTaskUseCaseTest {

    private val repository = mock<TaskRepository>()
    private val taskAlarmManager = mock<TaskAlarmManager>()
    private val notificationHelper = mock<NotificationHelper>()
    private val useCase = DeleteTaskUseCase(
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
    fun `ordinary task deletion writes tombstone and cancels reminder side effects`() = runTest {
        val task = task(id = "task-delete")
        whenever(repository.getTaskById(task.id)).thenReturn(task)

        val result = useCase(task.id)

        assertTrue(result.isSuccess)
        verify(repository).deleteTask(task.id)
        verify(taskAlarmManager).cancelTaskAlarm(task.id)
        verify(notificationHelper).cancelNotification(task.id)
    }

    @Test
    fun `missing task deletion fails without changing data or reminders`() = runTest {
        whenever(repository.getTaskById("missing")).thenReturn(null)

        val result = useCase("missing")

        assertTrue(result.isFailure)
        verify(repository, never()).deleteTask(any())
        verify(taskAlarmManager, never()).cancelTaskAlarm(any())
        verify(notificationHelper, never()).cancelNotification(any())
    }

    @Test
    fun `database deletion failure does not report success or cancel reminders`() = runTest {
        val task = task(id = "task-db-failure")
        whenever(repository.getTaskById(task.id)).thenReturn(task)
        whenever(repository.deleteTask(task.id)).thenThrow(IllegalStateException("database unavailable"))

        val result = useCase(task.id)

        assertTrue(result.isFailure)
        verify(taskAlarmManager, never()).cancelTaskAlarm(any())
        verify(notificationHelper, never()).cancelNotification(any())
    }

    @Test
    fun `reminder cleanup failure does not roll back a completed deletion`() = runTest {
        val task = task(id = "task-reminder-failure")
        whenever(repository.getTaskById(task.id)).thenReturn(task)
        whenever(taskAlarmManager.cancelTaskAlarm(task.id))
            .thenThrow(IllegalStateException("alarm unavailable"))
        whenever(notificationHelper.cancelNotification(task.id))
            .thenThrow(IllegalStateException("notification unavailable"))

        val result = useCase(task.id)

        assertTrue(result.isSuccess)
        verify(repository).deleteTask(task.id)
    }

    @Test
    fun `repeated deletion changes data only once and second call fails safely`() = runTest {
        val task = task(id = "task-repeat-delete")
        whenever(repository.getTaskById(task.id)).thenReturn(task, null)

        val first = useCase(task.id)
        val second = useCase(task.id)

        assertTrue(first.isSuccess)
        assertTrue(second.isFailure)
        verify(repository, times(1)).deleteTask(task.id)
        verify(taskAlarmManager, times(1)).cancelTaskAlarm(task.id)
        verify(notificationHelper, times(1)).cancelNotification(task.id)
    }

    @Test
    fun `deleting one recurring instance keeps template and other instances`() = runTest {
        val instance = task(id = "instance-current", templateTaskId = "template-1")
        whenever(repository.getTaskById(instance.id)).thenReturn(instance)

        val result = useCase(instance.id, DeleteMode.DELETE_THIS_ONLY)

        assertTrue(result.isSuccess)
        verify(repository).deleteTask(instance.id)
        verify(repository, never()).deleteTemplateAndAllInstances(any())
        verify(taskAlarmManager).cancelTaskAlarm(instance.id)
        verify(notificationHelper).cancelNotification(instance.id)
    }

    @Test
    fun `deleting all recurring tasks removes template and clears every reminder`() = runTest {
        val templateId = "template-all"
        val current = task(id = "instance-current", templateTaskId = templateId)
        val other = task(id = "instance-other", templateTaskId = templateId)
        whenever(repository.getTaskById(current.id)).thenReturn(current)
        whenever(repository.getInstancesByTemplateId(templateId)).thenReturn(listOf(current, other))

        val result = useCase(current.id, DeleteMode.DELETE_ALL_RECURRING)

        assertTrue(result.isSuccess)
        verify(repository).deleteTemplateAndAllInstances(templateId)
        verify(repository, never()).deleteTask(any())
        listOf(current.id, other.id, templateId).forEach { id ->
            verify(taskAlarmManager).cancelTaskAlarm(id)
            verify(notificationHelper).cancelNotification(id)
        }
    }

    private fun task(
        id: String,
        templateTaskId: String? = null
    ) = Task(
        id = id,
        title = "删除闭环测试任务",
        category = category,
        templateTaskId = templateTaskId
    )
}

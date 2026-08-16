package com.nextthing.app.domain.usecase

import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.CategoryType
import com.nextthing.app.domain.model.RepeatFrequency
import com.nextthing.app.domain.model.RepeatFrequencyType
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.repository.TaskGeofenceRepository
import com.nextthing.app.domain.repository.TaskRepository
import com.nextthing.app.util.TaskAlarmManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

class CreateTaskUseCaseTest {

    private val repository = mock<TaskRepository>()
    private val alarmManager = mock<TaskAlarmManager>()
    private val taskGeofenceRepository = mock<TaskGeofenceRepository>()
    private val recurringGenerator = mock<GenerateRecurringTasksUseCase>()
    private val useCase = CreateTaskUseCase(
        repository,
        alarmManager,
        taskGeofenceRepository,
        recurringGenerator
    )
    private val category = Category(
        id = "category-work",
        name = "工作",
        type = CategoryType.CUSTOM,
        icon = "work",
        colorHex = "#42A5F5"
    )

    @Test
    fun `plain text task is trimmed and persisted`() = runTest {
        whenever(repository.insertTask(any())).thenReturn("task-text-1")

        val result = useCase(
            title = "  完成项目报告  ",
            description = "  整理面试材料  ",
            category = category
        )

        assertTrue(result.isSuccess)
        assertEquals("task-text-1", result.getOrThrow())

        val taskCaptor = argumentCaptor<Task>()
        verify(repository).insertTask(taskCaptor.capture())
        with(taskCaptor.firstValue) {
            assertEquals("完成项目报告", title)
            assertEquals("整理面试材料", description)
            assertEquals(category, this.category)
            assertFalse(isTemplate)
            assertEquals(RepeatFrequencyType.NONE, repeatFrequency.type)
        }
        verify(alarmManager, never()).scheduleTaskAlarm(any())
        verify(taskGeofenceRepository, never()).createTaskGeofence(any(), any())
        verify(recurringGenerator, never()).invoke(any())
    }

    @Test
    fun `blank text task is rejected without persistence`() = runTest {
        val result = useCase(
            title = "   ",
            category = category
        )

        assertTrue(result.isFailure)
        verify(repository, never()).insertTask(any())
    }

    @Test
    fun `recurring create binds template geofence then generates today instance`() = runTest {
        whenever(repository.insertTask(any())).thenAnswer { invocation ->
            invocation.getArgument<com.nextthing.app.domain.model.Task>(0).id
        }
        whenever(taskGeofenceRepository.createTaskGeofence(any(), any()))
            .thenReturn(Result.success("geofence-link"))
        whenever(recurringGenerator.invoke(any())).thenReturn(Result.success(1))

        val result = useCase(
            title = "每日复盘",
            category = category,
            dueDate = LocalDateTime.now().withHour(21).withMinute(0),
            repeatFrequency = RepeatFrequency(RepeatFrequencyType.DAILY),
            notificationStrategyId = "reminder",
            geofenceLocationId = "home"
        )

        assertTrue(result.isSuccess)
        verify(taskGeofenceRepository).createTaskGeofence(result.getOrThrow(), "home")
        verify(recurringGenerator).invoke(LocalDate.now())
        verify(alarmManager, never()).scheduleTaskAlarm(any())
    }

    @Test
    fun `invalid recurring rule is rejected before insert`() = runTest {
        val result = useCase(
            title = "非法周任务",
            category = category,
            repeatFrequency = RepeatFrequency(
                type = RepeatFrequencyType.WEEKLY,
                weekdays = setOf(0, 8)
            )
        )

        assertTrue(result.isFailure)
        verify(repository, never()).insertTask(any())
    }
}

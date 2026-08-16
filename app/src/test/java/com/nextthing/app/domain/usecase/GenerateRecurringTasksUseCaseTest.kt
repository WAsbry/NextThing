package com.nextthing.app.domain.usecase

import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.CategoryType
import com.nextthing.app.domain.model.GeofenceLocation
import com.nextthing.app.domain.model.LocationInfo
import com.nextthing.app.domain.model.RepeatFrequency
import com.nextthing.app.domain.model.RepeatFrequencyType
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.model.TaskGeofence
import com.nextthing.app.domain.repository.TaskGeofenceRepository
import com.nextthing.app.domain.repository.TaskRepository
import com.nextthing.app.util.TaskAlarmManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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

class GenerateRecurringTasksUseCaseTest {

    private val repository = mock<TaskRepository>()
    private val alarmManager = mock<TaskAlarmManager>()
    private val taskGeofenceRepository = mock<TaskGeofenceRepository>()
    private val useCase = GenerateRecurringTasksUseCase(
        repository,
        alarmManager,
        taskGeofenceRepository
    )
    private val category = Category(
        id = "category-work",
        name = "工作",
        type = CategoryType.CUSTOM,
        icon = "work",
        colorHex = "#42A5F5"
    )

    @Test
    fun `generates same-day instance through atomic insert`() = runTest {
        val date = LocalDate.of(2026, 7, 29)
        val template = recurringTemplate(startDate = date)
        whenever(repository.getTemplateTasks()).thenReturn(listOf(template))
        whenever(repository.insertTaskIfAbsent(any())).thenReturn(true)
        whenever(taskGeofenceRepository.getByTaskIdOnce(template.id)).thenReturn(null)

        val result = useCase(date)

        assertEquals(1, result.getOrThrow())
        val instance = argumentCaptor<Task>()
        verify(repository).insertTaskIfAbsent(instance.capture())
        assertEquals(template.id, instance.firstValue.templateTaskId)
        assertEquals(date.atStartOfDay(), instance.firstValue.instanceDate)
        assertEquals(date.atTime(9, 30), instance.firstValue.dueDate)
    }

    @Test
    fun `unique conflict does not replace or reschedule existing instance`() = runTest {
        val date = LocalDate.of(2026, 7, 29)
        whenever(repository.getTemplateTasks()).thenReturn(listOf(recurringTemplate(date)))
        whenever(repository.insertTaskIfAbsent(any())).thenReturn(false)

        val result = useCase(date)

        assertEquals(0, result.getOrThrow())
        verify(alarmManager, never()).scheduleTaskAlarm(any())
        verify(taskGeofenceRepository, never()).insert(any())
    }

    @Test
    fun `future start date does not generate early`() = runTest {
        val targetDate = LocalDate.of(2026, 7, 29)
        whenever(repository.getTemplateTasks())
            .thenReturn(listOf(recurringTemplate(targetDate.plusDays(1))))

        val result = useCase(targetDate)

        assertEquals(0, result.getOrThrow())
        verify(repository, never()).insertTaskIfAbsent(any())
    }

    @Test
    fun `new instance inherits template geofence snapshot`() = runTest {
        val date = LocalDate.of(2026, 7, 29)
        val template = recurringTemplate(date)
        val templateGeofence = TaskGeofence(
            id = "template-geofence",
            taskId = template.id,
            geofenceLocationId = "office-geofence",
            geofenceLocation = GeofenceLocation(
                id = "office-geofence",
                locationInfo = LocationInfo(
                    id = "office",
                    locationName = "办公室",
                    latitude = 31.2,
                    longitude = 121.5
                )
            ),
            snapshotRadius = 350,
            isEnabled = true,
            geofenceDeferCount = 2
        )
        whenever(repository.getTemplateTasks()).thenReturn(listOf(template))
        whenever(repository.insertTaskIfAbsent(any())).thenReturn(true)
        whenever(taskGeofenceRepository.getByTaskIdOnce(template.id))
            .thenReturn(templateGeofence)
        whenever(taskGeofenceRepository.insert(any())).thenReturn(Result.success("instance-geofence"))

        val result = useCase(date)

        assertTrue(result.isSuccess)
        val inherited = argumentCaptor<TaskGeofence>()
        verify(taskGeofenceRepository).insert(inherited.capture())
        assertNotEquals(templateGeofence.id, inherited.firstValue.id)
        assertNotEquals(template.id, inherited.firstValue.taskId)
        assertEquals(350, inherited.firstValue.snapshotRadius)
        assertEquals(0, inherited.firstValue.geofenceDeferCount)
        assertEquals(null, inherited.firstValue.lastCheckResult)
    }

    private fun recurringTemplate(startDate: LocalDate) = Task(
        id = "template-1",
        title = "晨会",
        category = category,
        dueDate = startDate.atTime(9, 30),
        repeatFrequency = RepeatFrequency(RepeatFrequencyType.DAILY),
        notificationStrategyId = "default-reminder",
        isTemplate = true,
        createdAt = LocalDateTime.of(2026, 7, 1, 8, 0)
    )
}

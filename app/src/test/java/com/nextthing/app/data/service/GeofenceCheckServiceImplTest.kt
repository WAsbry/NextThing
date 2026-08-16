package com.nextthing.app.data.service

import com.nextthing.app.domain.model.GeofenceCheckResult
import com.nextthing.app.domain.model.GeofenceConfig
import com.nextthing.app.domain.model.GeofenceLocation
import com.nextthing.app.domain.model.LocationInfo
import com.nextthing.app.domain.model.TaskGeofence
import com.nextthing.app.domain.repository.GeofenceConfigRepository
import com.nextthing.app.domain.repository.GeofenceLocationRepository
import com.nextthing.app.domain.repository.TaskGeofenceRepository
import com.nextthing.app.domain.service.AccuracyLevel
import com.nextthing.app.domain.service.LocationService
import com.nextthing.app.domain.service.LocationServiceStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GeofenceCheckServiceImplTest {

    private val locationService = mock<LocationService>()
    private val taskGeofenceRepository = mock<TaskGeofenceRepository>()
    private val geofenceLocationRepository = mock<GeofenceLocationRepository>()
    private val configRepository = mock<GeofenceConfigRepository>()
    private val service = GeofenceCheckServiceImpl(
        locationService,
        taskGeofenceRepository,
        geofenceLocationRepository,
        configRepository
    )

    @Test
    fun `disabled global config skips location access for batch`() = runTest {
        whenever(configRepository.getConfigOrDefault())
            .thenReturn(GeofenceConfig(isGlobalEnabled = false))

        val results = service.checkMultipleTaskGeofences(listOf("task-1", "task-2"))

        assertEquals(2, results.size)
        results.values.forEach {
            assertEquals(GeofenceCheckResult.GEOFENCE_DISABLED, it.checkResult)
        }
        verify(locationService, never()).getServiceStatus()
        verify(locationService, never()).getCurrentLocation(any())
        verify(taskGeofenceRepository, never()).getByTaskIdOnce(any())
    }

    @Test
    fun `missing location permission has a distinct degradation result`() = runTest {
        val geofence = taskGeofence("task-1")
        whenever(configRepository.getConfigOrDefault())
            .thenReturn(GeofenceConfig(isGlobalEnabled = true))
        whenever(taskGeofenceRepository.getByTaskIdOnce("task-1")).thenReturn(geofence)
        whenever(geofenceLocationRepository.getLocationByIdOnce("office"))
            .thenReturn(geofence.geofenceLocation)
        whenever(locationService.getServiceStatus()).thenReturn(
            LocationServiceStatus(
                isAvailable = false,
                amapInitialized = true,
                hasPermission = false,
                isLocationEnabled = true,
                lastErrorMessage = "permission denied"
            )
        )

        val result = service.checkTaskGeofence("task-1")

        assertEquals(GeofenceCheckResult.PERMISSION_DENIED, result.checkResult)
        assertEquals("办公室", result.targetLocationName)
        verify(locationService, never()).getCurrentLocation(any())
        verify(taskGeofenceRepository, never()).updateLastCheckResult(
            any(),
            any(),
            any(),
            any(),
            any()
        )
    }

    @Test
    fun `batch resolves current location only once`() = runTest {
        val first = taskGeofence("task-1")
        val second = taskGeofence("task-2")
        val current = LocationInfo(
            id = "current",
            locationName = "当前位置",
            latitude = 31.2304,
            longitude = 121.4737,
            accuracy = 12f
        )
        whenever(configRepository.getConfigOrDefault())
            .thenReturn(GeofenceConfig(isGlobalEnabled = true))
        whenever(locationService.getServiceStatus()).thenReturn(
            LocationServiceStatus(
                isAvailable = true,
                amapInitialized = true,
                hasPermission = true,
                isLocationEnabled = true
            )
        )
        whenever(locationService.getCurrentLocation(true)).thenReturn(current)
        whenever(locationService.getAccuracyLevel(12f)).thenReturn(AccuracyLevel.GOOD)
        whenever(taskGeofenceRepository.getByTaskIdOnce("task-1")).thenReturn(first)
        whenever(taskGeofenceRepository.getByTaskIdOnce("task-2")).thenReturn(second)
        whenever(geofenceLocationRepository.getLocationByIdOnce("office"))
            .thenReturn(first.geofenceLocation)

        val results = service.checkMultipleTaskGeofences(listOf("task-1", "task-2"))

        assertEquals(2, results.size)
        verify(locationService).getCurrentLocation(true)
        verify(taskGeofenceRepository).updateLastCheckResult(
            eq("task-1"),
            any(),
            any(),
            any(),
            any()
        )
        verify(taskGeofenceRepository).updateLastCheckResult(
            eq("task-2"),
            any(),
            any(),
            any(),
            any()
        )
    }

    @Test
    fun `caller cancellation is not converted to location unavailable`() = runTest {
        val geofence = taskGeofence("task-1")
        whenever(configRepository.getConfigOrDefault())
            .thenReturn(GeofenceConfig(isGlobalEnabled = true))
        whenever(taskGeofenceRepository.getByTaskIdOnce("task-1")).thenReturn(geofence)
        whenever(geofenceLocationRepository.getLocationByIdOnce("office"))
            .thenReturn(geofence.geofenceLocation)
        whenever(locationService.getServiceStatus()).thenReturn(
            LocationServiceStatus(
                isAvailable = true,
                amapInitialized = true,
                hasPermission = true,
                isLocationEnabled = true
            )
        )
        whenever(locationService.getCurrentLocation(true))
            .thenThrow(CancellationException("worker stopped"))

        try {
            service.checkTaskGeofence("task-1")
            fail("CancellationException expected")
        } catch (_: CancellationException) {
            // expected
        }
    }

    private fun taskGeofence(taskId: String): TaskGeofence {
        val location = GeofenceLocation(
            id = "office",
            locationInfo = LocationInfo(
                id = "office-location",
                locationName = "办公室",
                latitude = 31.2304,
                longitude = 121.4737
            )
        )
        return TaskGeofence(
            id = "geofence-$taskId",
            taskId = taskId,
            geofenceLocationId = location.id,
            geofenceLocation = location,
            snapshotRadius = 200
        )
    }
}

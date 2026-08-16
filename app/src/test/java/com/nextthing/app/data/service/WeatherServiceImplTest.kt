package com.nextthing.app.data.service

import com.nextthing.app.data.remote.api.WeatherApi
import com.nextthing.app.data.remote.api.WeatherNow
import com.nextthing.app.data.remote.api.WeatherNowResponse
import com.nextthing.app.domain.model.LocationInfo
import com.nextthing.app.domain.model.WeatherCondition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

class WeatherServiceImplTest {

    private val weatherApi: WeatherApi = mock()
    private val service = WeatherServiceImpl(weatherApi)

    @Test
    fun `current weather maps supported fields without fabricating unavailable metrics`() = runTest {
        whenever(weatherApi.getCurrentWeather(any(), any())).thenReturn(
            successfulResponse(temp = "31", humidity = "62", windSpeed = "8", text = "晴")
        )

        val result = service.getCurrentWeather(location("杭州", 120.12, 30.28))

        assertTrue(result.isSuccess)
        val weather = result.getOrThrow()
        assertEquals(31, weather.temperature)
        assertEquals(62, weather.humidity)
        assertEquals(8, weather.windSpeed)
        assertEquals(WeatherCondition.SUNNY, weather.condition)
        assertNull(weather.temperatureMax)
        assertNull(weather.temperatureMin)
        assertNull(weather.pm25)
        assertNull(weather.uvIndex)
    }

    @Test
    fun `cache is reused only for the same rounded location`() = runTest {
        whenever(weatherApi.getCurrentWeather(any(), any())).thenReturn(successfulResponse())

        service.getCurrentWeather(location("杭州", 120.123, 30.283))
        service.getCurrentWeather(location("杭州", 120.124, 30.284))
        service.getCurrentWeather(location("上海", 121.47, 31.23))

        verify(weatherApi, times(2)).getCurrentWeather(any(), any())
    }

    @Test
    fun `malformed provider values fail instead of becoming plausible fake data`() = runTest {
        whenever(weatherApi.getCurrentWeather(any(), any())).thenReturn(
            successfulResponse(temp = "unknown")
        )

        val result = service.getCurrentWeather(location("杭州", 120.12, 30.28))

        assertTrue(result.isFailure)
        assertNull(service.getCachedWeather())
    }

    @Test(expected = CancellationException::class)
    fun `request cancellation is propagated`() = runTest {
        whenever(weatherApi.getCurrentWeather(any(), any())).thenThrow(
            CancellationException("cancelled")
        )

        service.getCurrentWeather(location("杭州", 120.12, 30.28))
    }

    private fun successfulResponse(
        temp: String = "26",
        humidity: String = "55",
        windSpeed: String = "6",
        text: String = "多云"
    ): Response<WeatherNowResponse> = Response.success(
        WeatherNowResponse(
            code = "200",
            updateTime = "2026-07-28T12:00+08:00",
            now = WeatherNow(
                obsTime = "2026-07-28T11:55+08:00",
                temp = temp,
                feelsLike = temp,
                icon = "101",
                text = text,
                wind360 = "180",
                windDir = "南风",
                windScale = "2",
                windSpeed = windSpeed,
                humidity = humidity,
                precip = "0.0",
                pressure = "1002",
                vis = "18",
                cloud = null,
                dew = null
            )
        )
    )

    private fun location(
        name: String,
        longitude: Double,
        latitude: Double
    ): LocationInfo = LocationInfo(
        locationName = name,
        latitude = latitude,
        longitude = longitude
    )
}

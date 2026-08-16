package com.nextthing.app.data.service

import com.nextthing.app.data.remote.api.WeatherApi
import com.nextthing.app.data.remote.api.WeatherNow
import com.nextthing.app.domain.model.LocationInfo
import com.nextthing.app.domain.model.WeatherCondition
import com.nextthing.app.domain.model.WeatherInfo
import com.nextthing.app.domain.service.WeatherService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.io.IOException
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class WeatherServiceImpl @Inject constructor(
    private val weatherApi: WeatherApi
) : WeatherService {

    private val refreshMutex = Mutex()
    private val weatherUpdates = MutableStateFlow<WeatherInfo?>(null)

    @Volatile
    private var cachedWeather: WeatherInfo? = null

    @Volatile
    private var cachedLocationKey: String? = null

    @Volatile
    private var lastWeatherUpdateTime: Long = 0

    override suspend fun getCurrentWeather(
        location: LocationInfo,
        forceRefresh: Boolean
    ): Result<WeatherInfo> {
        return try {
            refreshMutex.withLock {
                val locationKey = location.cacheKey()
                cachedWeather
                    ?.takeIf {
                        !forceRefresh &&
                            cachedLocationKey == locationKey &&
                            !shouldRefreshWeather()
                    }
                    ?.let { return@withLock Result.success(it) }

                val response = weatherApi.getCurrentWeather(
                    longitude = location.longitude,
                    latitude = location.latitude
                )
                if (!response.isSuccessful) {
                    return@withLock Result.failure(
                        IOException("天气服务请求失败（${response.code()}）")
                    )
                }

                val payload = response.body()
                    ?: return@withLock Result.failure(IOException("天气服务返回空数据"))
                if (payload.code != "200" || payload.now == null) {
                    return@withLock Result.failure(IOException("天气服务返回异常"))
                }

                val weather = payload.now.toDomain(
                    location = location,
                    updateTime = payload.updateTime
                )
                cachedWeather = weather
                cachedLocationKey = locationKey
                lastWeatherUpdateTime = System.currentTimeMillis()
                weatherUpdates.value = weather
                Result.success(weather)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            Timber.w(e, "获取天气数据失败")
            Result.failure(e)
        }
    }

    override fun observeWeatherUpdates(location: LocationInfo): Flow<WeatherInfo> =
        weatherUpdates.filterNotNull()

    override fun shouldRefreshWeather(): Boolean =
        cachedWeather == null ||
            System.currentTimeMillis() - lastWeatherUpdateTime >= WEATHER_CACHE_DURATION_MS

    override suspend fun getCachedWeather(): WeatherInfo? = cachedWeather

    override suspend fun clearWeatherCache() {
        refreshMutex.withLock {
            cachedWeather = null
            cachedLocationKey = null
            lastWeatherUpdateTime = 0
            weatherUpdates.value = null
        }
    }

    private fun WeatherNow.toDomain(
        location: LocationInfo,
        updateTime: String?
    ): WeatherInfo {
        val temperature = temp.requiredInt("温度")
        val humidityValue = humidity.requiredInt("湿度")
        val windSpeedValue = windSpeed.requiredInt("风速")

        require(humidityValue in 0..100) { "天气服务返回的湿度无效" }
        require(windSpeedValue >= 0) { "天气服务返回的风速无效" }

        val weather = WeatherInfo(
            condition = mapWeatherCondition(text.orEmpty(), icon.orEmpty()),
            temperature = temperature,
            temperatureMax = null,
            temperatureMin = null,
            humidity = humidityValue,
            windSpeed = windSpeedValue,
            pm25 = null,
            uvIndex = null,
            suggestion = null,
            updateTime = parseUpdateTime(updateTime),
            locationName = location.locationName
        )
        return weather.copy(suggestion = weather.getPrioritySuggestion())
    }

    private fun String?.requiredInt(fieldName: String): Int =
        this?.toIntOrNull() ?: throw IOException("天气服务返回的${fieldName}无效")

    private fun parseUpdateTime(value: String?): LocalDateTime {
        if (value.isNullOrBlank()) return LocalDateTime.now()
        return try {
            OffsetDateTime.parse(value).toLocalDateTime()
        } catch (_: DateTimeParseException) {
            try {
                LocalDateTime.parse(value)
            } catch (_: DateTimeParseException) {
                LocalDateTime.now()
            }
        }
    }

    private fun mapWeatherCondition(text: String, icon: String): WeatherCondition =
        when {
            text.contains("雷") -> WeatherCondition.THUNDERSTORM
            text.contains("雪") -> WeatherCondition.SNOWY
            text.contains("雨") -> WeatherCondition.RAINY
            text.contains("雾") || text.contains("霾") -> WeatherCondition.FOGGY
            text.contains("风") -> WeatherCondition.WINDY
            text.contains("多云") -> WeatherCondition.PARTLY_CLOUDY
            text.contains("阴") -> WeatherCondition.CLOUDY
            text.contains("晴") -> WeatherCondition.SUNNY
            icon.startsWith("10") -> WeatherCondition.SUNNY
            icon.startsWith("15") -> WeatherCondition.RAINY
            else -> WeatherCondition.UNKNOWN
        }

    private fun LocationInfo.cacheKey(): String =
        "${(longitude * 100).roundToInt()}:${(latitude * 100).roundToInt()}"

    companion object {
        private const val WEATHER_CACHE_DURATION_MS = 15 * 60 * 1000L
    }
}

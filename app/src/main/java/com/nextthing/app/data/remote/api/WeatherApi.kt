package com.nextthing.app.data.remote.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("api/weather/now")
    suspend fun getCurrentWeather(
        @Query("longitude") longitude: Double,
        @Query("latitude") latitude: Double
    ): Response<WeatherNowResponse>
}

data class WeatherNowResponse(
    @SerializedName("code") val code: String?,
    @SerializedName("now") val now: WeatherNow?,
    @SerializedName("updateTime") val updateTime: String?
)

data class WeatherNow(
    @SerializedName("obsTime") val obsTime: String?,
    @SerializedName("temp") val temp: String?,
    @SerializedName("feelsLike") val feelsLike: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("text") val text: String?,
    @SerializedName("wind360") val wind360: String?,
    @SerializedName("windDir") val windDir: String?,
    @SerializedName("windScale") val windScale: String?,
    @SerializedName("windSpeed") val windSpeed: String?,
    @SerializedName("humidity") val humidity: String?,
    @SerializedName("precip") val precip: String?,
    @SerializedName("pressure") val pressure: String?,
    @SerializedName("vis") val vis: String?,
    @SerializedName("cloud") val cloud: String?,
    @SerializedName("dew") val dew: String?
)

package com.nextthing.app.di

import com.nextthing.app.data.service.WeatherServiceImpl
import com.nextthing.app.domain.service.WeatherService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WeatherModule {

    @Binds
    @Singleton
    abstract fun bindWeatherService(
        weatherServiceImpl: WeatherServiceImpl
    ): WeatherService
}
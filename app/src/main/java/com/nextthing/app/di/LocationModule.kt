package com.nextthing.app.di

import android.content.Context
import com.nextthing.app.data.service.AmapLocationServiceImpl
import com.nextthing.app.data.service.GeofenceCheckServiceImpl
import com.nextthing.app.domain.service.LocationService
import com.nextthing.app.domain.service.GeofenceCheckService
import com.nextthing.app.domain.service.GeofenceManager
import com.nextthing.app.domain.service.WorkerOnlyGeofenceManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindLocationService(
        amapLocationServiceImpl: AmapLocationServiceImpl
    ): LocationService

    @Binds
    @Singleton
    abstract fun bindGeofenceCheckService(
        geofenceCheckServiceImpl: GeofenceCheckServiceImpl
    ): GeofenceCheckService

    @Binds
    @Singleton
    abstract fun bindGeofenceManager(
        geofenceManager: WorkerOnlyGeofenceManager
    ): GeofenceManager
}

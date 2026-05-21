package com.nextthing.app.di

import android.content.Context
import com.nextthing.app.data.service.AmapLocationServiceImpl
import com.nextthing.app.data.service.LocationServiceImpl
import com.nextthing.app.data.service.GeofenceCheckServiceImpl
import com.nextthing.app.domain.service.LocationService
import com.nextthing.app.domain.service.GeofenceCheckService
import com.nextthing.app.domain.service.GeofenceManager
import com.nextthing.app.domain.service.GeofenceManagerImpl
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
        geofenceManagerImpl: GeofenceManagerImpl
    ): GeofenceManager

    companion object {
        @Provides
        @Singleton
        fun provideLocationServiceImpl(
            @ApplicationContext context: Context
        ): LocationServiceImpl {
            return LocationServiceImpl(context)
        }

        @Provides
        @Singleton
        fun provideGeofenceManagerImpl(
            @ApplicationContext context: Context
        ): GeofenceManagerImpl {
            return GeofenceManagerImpl(context)
        }
    }
} 
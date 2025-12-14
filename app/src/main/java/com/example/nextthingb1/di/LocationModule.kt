package com.example.nextthingb1.di

import android.content.Context
import com.example.nextthingb1.data.service.AmapLocationServiceImpl
import com.example.nextthingb1.data.service.LocationServiceImpl
import com.example.nextthingb1.data.service.GeofenceCheckServiceImpl
import com.example.nextthingb1.domain.service.LocationService
import com.example.nextthingb1.domain.service.GeofenceCheckService
import com.example.nextthingb1.domain.service.GeofenceManager
import com.example.nextthingb1.domain.service.GeofenceManagerImpl
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
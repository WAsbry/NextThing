package com.nextthing.app.di

import android.content.Context
import androidx.room.Room
import com.nextthing.app.data.local.dao.AchievementDao
import com.nextthing.app.data.local.dao.CategoryDao
import com.nextthing.app.data.local.dao.LocationDao
import com.nextthing.app.data.local.dao.NotificationStrategyDao
import com.nextthing.app.data.local.dao.TaskDao
import com.nextthing.app.data.local.dao.UserDao
import com.nextthing.app.data.local.dao.GeofenceConfigDao
import com.nextthing.app.data.local.dao.GeofenceLocationDao
import com.nextthing.app.data.local.dao.TaskGeofenceDao
import com.nextthing.app.data.local.dao.StartupTraceDao
import com.nextthing.app.data.local.database.TaskDatabase
import com.nextthing.app.data.repository.AchievementRepositoryImpl
import com.nextthing.app.data.repository.CategoryRepositoryImpl
import com.nextthing.app.data.repository.TaskRepositoryImpl
import com.nextthing.app.data.repository.LocationRepositoryImpl
import com.nextthing.app.data.repository.CustomCategoryRepositoryImpl
import com.nextthing.app.data.repository.NotificationStrategyRepositoryImpl
import com.nextthing.app.data.repository.UserRepositoryImpl
import com.nextthing.app.data.repository.GeofenceConfigRepositoryImpl
import com.nextthing.app.data.repository.GeofenceLocationRepositoryImpl
import com.nextthing.app.data.repository.TaskGeofenceRepositoryImpl
import com.nextthing.app.data.repository.SyncRepositoryImpl
import com.nextthing.app.data.service.CategoryPreferencesManagerImpl
import com.nextthing.app.data.preferences.ThemePreferences
import com.nextthing.app.domain.repository.AchievementRepository
import com.nextthing.app.domain.repository.CategoryRepository
import com.nextthing.app.domain.repository.TaskRepository
import com.nextthing.app.domain.repository.LocationRepository
import com.nextthing.app.domain.repository.CustomCategoryRepository
import com.nextthing.app.domain.repository.NotificationStrategyRepository
import com.nextthing.app.domain.repository.UserRepository
import com.nextthing.app.domain.repository.GeofenceConfigRepository
import com.nextthing.app.domain.repository.GeofenceLocationRepository
import com.nextthing.app.domain.repository.TaskGeofenceRepository
import com.nextthing.app.domain.repository.SyncRepository
import com.nextthing.app.domain.service.CategoryPreferencesManager
import com.nextthing.app.util.NotificationHelper
import com.nextthing.app.util.PermissionManager
import com.nextthing.app.util.TaskAlarmManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTaskDatabase(@ApplicationContext context: Context): TaskDatabase {
        return TaskDatabase.getDatabase(context)
    }

    @Provides
    fun provideAchievementDao(database: TaskDatabase): AchievementDao = database.achievementDao()

    @Provides
    fun provideTaskDao(database: TaskDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideLocationDao(database: TaskDatabase): LocationDao = database.locationDao()

    @Provides
    fun provideUserDao(database: TaskDatabase): UserDao = database.userDao()

    @Provides
    fun provideNotificationStrategyDao(database: TaskDatabase): NotificationStrategyDao = database.notificationStrategyDao()

    @Provides
    fun provideCategoryDao(database: TaskDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideGeofenceConfigDao(database: TaskDatabase): GeofenceConfigDao = database.geofenceConfigDao()

    @Provides
    fun provideGeofenceLocationDao(database: TaskDatabase): GeofenceLocationDao = database.geofenceLocationDao()

    @Provides
    fun provideTaskGeofenceDao(database: TaskDatabase): TaskGeofenceDao = database.taskGeofenceDao()

    @Provides
    fun provideStartupTraceDao(database: TaskDatabase): StartupTraceDao = database.startupTraceDao()

    @Provides
    @Singleton
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper {
        return NotificationHelper(context)
    }

    @Provides
    @Singleton
    fun providePermissionManager(@ApplicationContext context: Context): PermissionManager {
        return PermissionManager(context)
    }

    @Provides
    @Singleton
    fun provideTaskAlarmManager(
        @ApplicationContext context: Context,
        notificationStrategyRepository: NotificationStrategyRepository
    ): TaskAlarmManager {
        return TaskAlarmManager(context, notificationStrategyRepository)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindAchievementRepository(impl: AchievementRepositoryImpl): AchievementRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindCustomCategoryRepository(impl: CustomCategoryRepositoryImpl): CustomCategoryRepository

    @Binds
    @Singleton
    abstract fun bindNotificationStrategyRepository(impl: NotificationStrategyRepositoryImpl): NotificationStrategyRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindGeofenceConfigRepository(impl: GeofenceConfigRepositoryImpl): GeofenceConfigRepository

    @Binds
    @Singleton
    abstract fun bindGeofenceLocationRepository(impl: GeofenceLocationRepositoryImpl): GeofenceLocationRepository

    @Binds
    @Singleton
    abstract fun bindTaskGeofenceRepository(impl: TaskGeofenceRepositoryImpl): TaskGeofenceRepository

    @Binds
    @Singleton
    abstract fun bindSyncRepository(impl: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    abstract fun bindCategoryPreferencesManager(impl: CategoryPreferencesManagerImpl): CategoryPreferencesManager
}

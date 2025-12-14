package com.example.nextthingb1.di

import android.content.Context
import androidx.room.Room
import com.example.nextthingb1.data.local.dao.CategoryDao
import com.example.nextthingb1.data.local.dao.LocationDao
import com.example.nextthingb1.data.local.dao.NotificationStrategyDao
import com.example.nextthingb1.data.local.dao.TaskDao
import com.example.nextthingb1.data.local.dao.UserDao
import com.example.nextthingb1.data.local.dao.GeofenceConfigDao
import com.example.nextthingb1.data.local.dao.GeofenceLocationDao
import com.example.nextthingb1.data.local.dao.TaskGeofenceDao
import com.example.nextthingb1.data.local.database.TaskDatabase
import com.example.nextthingb1.data.repository.CategoryRepositoryImpl
import com.example.nextthingb1.data.repository.TaskRepositoryImpl
import com.example.nextthingb1.data.repository.LocationRepositoryImpl
import com.example.nextthingb1.data.repository.CustomCategoryRepositoryImpl
import com.example.nextthingb1.data.repository.NotificationStrategyRepositoryImpl
import com.example.nextthingb1.data.repository.UserRepositoryImpl
import com.example.nextthingb1.data.repository.GeofenceConfigRepositoryImpl
import com.example.nextthingb1.data.repository.GeofenceLocationRepositoryImpl
import com.example.nextthingb1.data.repository.TaskGeofenceRepositoryImpl
import com.example.nextthingb1.data.service.CategoryPreferencesManagerImpl
import com.example.nextthingb1.domain.repository.CategoryRepository
import com.example.nextthingb1.domain.repository.TaskRepository
import com.example.nextthingb1.domain.repository.LocationRepository
import com.example.nextthingb1.domain.repository.CustomCategoryRepository
import com.example.nextthingb1.domain.repository.NotificationStrategyRepository
import com.example.nextthingb1.domain.repository.UserRepository
import com.example.nextthingb1.domain.repository.GeofenceConfigRepository
import com.example.nextthingb1.domain.repository.GeofenceLocationRepository
import com.example.nextthingb1.domain.repository.TaskGeofenceRepository
import com.example.nextthingb1.domain.service.CategoryPreferencesManager
import com.example.nextthingb1.util.NotificationHelper
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
    fun provideTaskDao(database: TaskDatabase): TaskDao {
        return database.taskDao()
    }
    
    @Provides
    fun provideLocationDao(database: TaskDatabase): LocationDao {
        return database.locationDao()
    }

    @Provides
    fun provideUserDao(database: TaskDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideNotificationStrategyDao(database: TaskDatabase): NotificationStrategyDao {
        return database.notificationStrategyDao()
    }

    @Provides
    fun provideCategoryDao(database: TaskDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideGeofenceConfigDao(database: TaskDatabase): GeofenceConfigDao {
        return database.geofenceConfigDao()
    }

    @Provides
    fun provideGeofenceLocationDao(database: TaskDatabase): GeofenceLocationDao {
        return database.geofenceLocationDao()
    }

    @Provides
    fun provideTaskGeofenceDao(database: TaskDatabase): TaskGeofenceDao {
        return database.taskGeofenceDao()
    }

    @Provides
    @Singleton
    fun provideTaskRepository(taskDao: TaskDao, categoryDao: CategoryDao): TaskRepository {
        return TaskRepositoryImpl(taskDao, categoryDao)
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(categoryDao: CategoryDao): CategoryRepository {
        return CategoryRepositoryImpl(categoryDao)
    }

    @Provides
    @Singleton
    fun provideNotificationStrategyRepository(notificationStrategyDao: NotificationStrategyDao): NotificationStrategyRepository {
        return NotificationStrategyRepositoryImpl(notificationStrategyDao)
    }

    @Provides
    @Singleton
    fun provideUserRepository(userDao: UserDao): UserRepository {
        return UserRepositoryImpl(userDao)
    }
    
    @Provides
    @Singleton
    fun provideLocationRepository(
        locationDao: LocationDao,
        @ApplicationContext context: Context
    ): LocationRepository {
        return LocationRepositoryImpl(locationDao, context)
    }

    @Provides
    @Singleton
    fun provideCustomCategoryRepository(
        @ApplicationContext context: Context
    ): CustomCategoryRepository {
        return CustomCategoryRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideCategoryPreferencesManager(
        @ApplicationContext context: Context
    ): CategoryPreferencesManager {
        return CategoryPreferencesManagerImpl(context)
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(
        @ApplicationContext context: Context
    ): NotificationHelper {
        return NotificationHelper(context)
    }

    @Provides
    @Singleton
    fun providePermissionManager(
        @ApplicationContext context: Context
    ): com.example.nextthingb1.util.PermissionManager {
        return com.example.nextthingb1.util.PermissionManager(context)
    }

    @Provides
    @Singleton
    fun provideTaskAlarmManager(
        @ApplicationContext context: Context
    ): com.example.nextthingb1.util.TaskAlarmManager {
        return com.example.nextthingb1.util.TaskAlarmManager(context)
    }

    @Provides
    @Singleton
    fun provideGeofenceConfigRepository(
        geofenceConfigDao: GeofenceConfigDao
    ): GeofenceConfigRepository {
        return GeofenceConfigRepositoryImpl(geofenceConfigDao)
    }

    @Provides
    @Singleton
    fun provideGeofenceLocationRepository(
        geofenceLocationDao: GeofenceLocationDao,
        locationDao: LocationDao
    ): GeofenceLocationRepository {
        return GeofenceLocationRepositoryImpl(geofenceLocationDao, locationDao)
    }

    @Provides
    @Singleton
    fun provideTaskGeofenceRepository(
        taskGeofenceDao: TaskGeofenceDao,
        geofenceLocationDao: GeofenceLocationDao,
        locationDao: LocationDao,
        geofenceConfigDao: GeofenceConfigDao
    ): TaskGeofenceRepository {
        return TaskGeofenceRepositoryImpl(taskGeofenceDao, geofenceLocationDao, locationDao, geofenceConfigDao)
    }
} 
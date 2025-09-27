package com.example.nextthingb1.di

import android.content.Context
import androidx.room.Room
import com.example.nextthingb1.data.local.dao.LocationDao
import com.example.nextthingb1.data.local.dao.TaskDao
import com.example.nextthingb1.data.local.database.TaskDatabase
import com.example.nextthingb1.data.repository.TaskRepositoryImpl
import com.example.nextthingb1.data.repository.LocationRepositoryImpl
import com.example.nextthingb1.data.repository.CustomCategoryRepositoryImpl
import com.example.nextthingb1.data.service.CategoryPreferencesManagerImpl
import com.example.nextthingb1.domain.repository.TaskRepository
import com.example.nextthingb1.domain.repository.LocationRepository
import com.example.nextthingb1.domain.repository.CustomCategoryRepository
import com.example.nextthingb1.domain.service.CategoryPreferencesManager
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
        return Room.databaseBuilder(
            context.applicationContext,
            TaskDatabase::class.java,
            TaskDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
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
    @Singleton
    fun provideTaskRepository(taskDao: TaskDao): TaskRepository {
        return TaskRepositoryImpl(taskDao)
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
} 
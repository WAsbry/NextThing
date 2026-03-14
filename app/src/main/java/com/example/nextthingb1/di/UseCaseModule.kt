package com.example.nextthingb1.di

import com.example.nextthingb1.domain.repository.TaskRepository
import com.example.nextthingb1.domain.repository.LocationRepository
import com.example.nextthingb1.domain.repository.UserRepository
import com.example.nextthingb1.domain.repository.GeofenceConfigRepository
import com.example.nextthingb1.domain.repository.GeofenceLocationRepository
import com.example.nextthingb1.domain.repository.TaskGeofenceRepository
import com.example.nextthingb1.domain.usecase.*
import com.example.nextthingb1.domain.usecase.AchievementUseCases
import com.example.nextthingb1.domain.usecase.GetAchievementsUseCase
import com.example.nextthingb1.domain.usecase.CheckAndUnlockUseCase
import com.example.nextthingb1.domain.usecase.GetUnlockedCountUseCase
import com.example.nextthingb1.data.service.AchievementChecker
import com.example.nextthingb1.domain.repository.AchievementRepository
import com.example.nextthingb1.data.local.dao.TaskDao
import com.example.nextthingb1.data.local.dao.GeofenceLocationDao
import com.example.nextthingb1.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideAchievementChecker(
        taskDao: TaskDao,
        geofenceLocationDao: GeofenceLocationDao,
        userDao: UserDao
    ): AchievementChecker {
        return AchievementChecker(taskDao, geofenceLocationDao, userDao)
    }

    @Provides
    @Singleton
    fun provideAchievementUseCases(
        repository: AchievementRepository,
        checker: AchievementChecker
    ): AchievementUseCases {
        return AchievementUseCases(
            getAchievements = GetAchievementsUseCase(repository, checker),
            checkAndUnlock = CheckAndUnlockUseCase(repository, checker),
            getUnlockedCount = GetUnlockedCountUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideTaskUseCases(
        repository: TaskRepository,
        locationRepository: LocationRepository,
        taskAlarmManager: com.example.nextthingb1.util.TaskAlarmManager
    ): TaskUseCases {
        return TaskUseCases(
            getAllTasks = GetAllTasksUseCase(repository),
            getTaskById = GetTaskByIdUseCase(repository),
            getTodayTasks = GetTodayTasksUseCase(repository),
            createTask = CreateTaskUseCase(repository, taskAlarmManager),
            updateTask = UpdateTaskUseCase(repository, taskAlarmManager),
            deleteTask = DeleteTaskUseCase(repository, taskAlarmManager),
            deleteAllTasks = DeleteAllTasksUseCase(repository),
            toggleTaskStatus = ToggleTaskStatusUseCase(repository),
            deferTask = DeferTaskUseCase(repository, taskAlarmManager),
            getTaskStatistics = GetTaskStatisticsUseCase(repository),
            searchTasks = SearchTasksUseCase(repository),
            getTasksByCategory = GetTasksByCategoryUseCase(repository),
            getUrgentTasks = GetUrgentTasksUseCase(repository),
            getEarliestTaskDate = GetEarliestTaskDateUseCase(repository),
            generateRecurringTasks = GenerateRecurringTasksUseCase(repository, taskAlarmManager),
            deleteCompletedTasks = DeleteCompletedTasksUseCase(repository),
            locationRepository = locationRepository
        )
    }

    @Provides
    @Singleton
    fun provideLocationUseCases(repository: LocationRepository): LocationUseCases {
        return LocationUseCases(repository)
    }

    @Provides
    @Singleton
    fun provideUserUseCases(repository: UserRepository): UserUseCases {
        return UserUseCases(
            getCurrentUser = GetCurrentUserUseCase(repository),
            createUser = CreateUserUseCase(repository),
            updateUser = UpdateUserUseCase(repository),
            updateNickname = UpdateNicknameUseCase(repository),
            updateAvatar = UpdateAvatarUseCase(repository),
            updatePhoneNumber = UpdatePhoneNumberUseCase(repository),
            updateWechatId = UpdateWechatIdUseCase(repository),
            updateQqId = UpdateQqIdUseCase(repository),
            logout = LogoutUseCase(repository)
        )
    }

    @Provides
    @Singleton
    fun provideGeofenceUseCases(
        configRepository: GeofenceConfigRepository,
        locationRepository: GeofenceLocationRepository,
        taskGeofenceRepository: TaskGeofenceRepository,
        geofenceManager: com.example.nextthingb1.domain.service.GeofenceManager
    ): GeofenceUseCases {
        return GeofenceUseCases(
            getGeofenceConfig = GetGeofenceConfigUseCase(configRepository),
            updateGeofenceConfig = UpdateGeofenceConfigUseCase(configRepository),
            getGeofenceLocations = GetGeofenceLocationsUseCase(locationRepository),
            createGeofenceLocation = CreateGeofenceLocationUseCase(locationRepository, geofenceManager, configRepository),
            updateGeofenceLocation = UpdateGeofenceLocationUseCase(locationRepository, geofenceManager, configRepository),
            deleteGeofenceLocation = DeleteGeofenceLocationUseCase(locationRepository, taskGeofenceRepository, geofenceManager),
            getTaskGeofence = GetTaskGeofenceUseCase(taskGeofenceRepository),
            createTaskGeofence = CreateTaskGeofenceUseCase(taskGeofenceRepository, locationRepository),
            updateFrequentLocations = UpdateFrequentLocationsUseCase(locationRepository),
            updateLocationUsage = com.example.nextthingb1.domain.usecase.geofence.UpdateGeofenceLocationUsageUseCase(locationRepository),
            updateTaskGeofenceCheckResult = com.example.nextthingb1.domain.usecase.geofence.UpdateTaskGeofenceCheckResultUseCase(taskGeofenceRepository),
            updateLocationCheckStatistics = com.example.nextthingb1.domain.usecase.geofence.UpdateLocationCheckStatisticsUseCase(locationRepository)
        )
    }
} 
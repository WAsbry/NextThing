package com.example.nextthingb1.di

import com.example.nextthingb1.domain.repository.TaskRepository
import com.example.nextthingb1.domain.repository.LocationRepository
import com.example.nextthingb1.domain.repository.UserRepository
import com.example.nextthingb1.domain.usecase.*
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
    fun provideTaskUseCases(
        repository: TaskRepository,
        locationRepository: LocationRepository,
        taskAlarmManager: com.example.nextthingb1.util.TaskAlarmManager
    ): TaskUseCases {
        return TaskUseCases(
            getAllTasks = GetAllTasksUseCase(repository),
            getTodayTasks = GetTodayTasksUseCase(repository),
            createTask = CreateTaskUseCase(repository, taskAlarmManager),
            updateTask = UpdateTaskUseCase(repository, taskAlarmManager),
            deleteTask = DeleteTaskUseCase(repository, taskAlarmManager),
            deleteAllTasks = DeleteAllTasksUseCase(repository),
            toggleTaskStatus = ToggleTaskStatusUseCase(repository),
            deferTask = DeferTaskUseCase(repository),
            getTaskStatistics = GetTaskStatisticsUseCase(repository),
            searchTasks = SearchTasksUseCase(repository),
            getTasksByCategory = GetTasksByCategoryUseCase(repository),
            getUrgentTasks = GetUrgentTasksUseCase(repository),
            getEarliestTaskDate = GetEarliestTaskDateUseCase(repository),
            generateRecurringTasks = GenerateRecurringTasksUseCase(repository),
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
} 
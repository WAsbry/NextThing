package com.example.nextthingb1

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.nextthingb1.domain.repository.TaskRepository

import com.example.nextthingb1.util.SyncScheduler

import dagger.hilt.android.HiltAndroidApp

import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class NextThingApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var taskRepository: TaskRepository

    override fun onCreate() {
        super.onCreate()

        // 暂时总是启用调试日志
        Timber.plant(Timber.DebugTree())
        Timber.d("NextThingApplication initialized")

        // 定时同步调度
        SyncScheduler.schedulePeriodicSync(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
} 
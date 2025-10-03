package com.example.nextthingb1

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.nextthingb1.domain.repository.TaskRepository
import com.example.nextthingb1.util.SyncScheduler
import com.example.nextthingb1.work.TaskWorkScheduler

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
        Timber.d("✅ [Application] NextThingApplication 开始初始化...")

        try {
            Timber.d("🔍 [Application] 检查Hilt依赖注入状态...")
            
            // 检查基本依赖
            Timber.d("📋 [Application] TaskRepository: ${taskRepository.javaClass.simpleName}")
            Timber.d("🏭 [Application] WorkerFactory: ${workerFactory.javaClass.simpleName}")
            
            Timber.d("✅ [Application] 基本依赖注入成功")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ [Application] 基本依赖注入失败")
        }

        // 定时同步调度
        try {
            SyncScheduler.schedulePeriodicSync(this)
            Timber.d("✅ [Application] SyncScheduler 初始化成功")
        } catch (e: Exception) {
            Timber.e(e, "❌ [Application] SyncScheduler 初始化失败")
        }

        // 定时逾期检测调度
        try {
            TaskWorkScheduler.scheduleOverdueCheck(this)
            TaskWorkScheduler.triggerImmediateOverdueCheck(this)
            Timber.d("✅ [Application] 逾期检测 TaskWorkScheduler 初始化成功")
        } catch (e: Exception) {
            Timber.e(e, "❌ [Application] 逾期检测 TaskWorkScheduler 初始化失败")
        }

        // 定时延期转待办调度
        try {
            TaskWorkScheduler.scheduleDelayedConversion(this)
            TaskWorkScheduler.triggerImmediateDelayedConversion(this)
            Timber.d("✅ [Application] 延期转待办 TaskWorkScheduler 初始化成功")
        } catch (e: Exception) {
            Timber.e(e, "❌ [Application] 延期转待办 TaskWorkScheduler 初始化失败")
        }

        // 任务通知调度
        try {
            TaskWorkScheduler.scheduleTaskNotifications(this)
            Timber.d("✅ [Application] 任务通知 TaskWorkScheduler 初始化成功")
        } catch (e: Exception) {
            Timber.e(e, "❌ [Application] 任务通知 TaskWorkScheduler 初始化失败")
        }

        Timber.d("🎉 [Application] NextThingApplication 初始化完成")
    }

    override fun onTerminate() {
        super.onTerminate()
        Timber.d("🏁 [Application] NextThingApplication 正在终止...")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
} 
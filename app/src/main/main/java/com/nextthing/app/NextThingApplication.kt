package com.nextthing.app

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.nextthing.app.data.preferences.BriefingPreferences
import com.nextthing.app.data.local.dao.StartupTraceDao
import com.nextthing.app.domain.repository.TaskRepository
import com.nextthing.app.domain.usecase.GeofenceUseCases
import com.nextthing.app.domain.service.GeofenceManager
import com.nextthing.app.domain.service.GeofenceData
import com.nextthing.app.performance.StartupTraceCollector
import com.nextthing.app.performance.StartupTracker
import com.nextthing.app.util.SyncScheduler
import com.nextthing.app.work.TaskWorkScheduler

import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class NextThingApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var startupTraceDao: StartupTraceDao
    @Inject lateinit var geofenceUseCases: GeofenceUseCases
    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var briefingPreferences: BriefingPreferences

    // 应用级协程作用域
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun attachBaseContext(base: Context) {
        StartupTracker.init()
        StartupTracker.record("app_attach")
        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()
        StartupTracker.record("hilt_init_end")
        StartupTracker.record("app_onCreate_start")

        // 暂时总是启用调试日志
        Timber.plant(Timber.DebugTree())
        StartupTracker.record("timber_init")
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
            StartupTracker.record("sync_scheduler")
            Timber.d("✅ [Application] SyncScheduler 初始化成功")
        } catch (e: Exception) {
            Timber.e(e, "❌ [Application] SyncScheduler 初始化失败")
        }

        // 定时逾期检测调度
        try {
            TaskWorkScheduler.scheduleOverdueCheck(this)
            TaskWorkScheduler.triggerImmediateOverdueCheck(this)
            StartupTracker.record("overdue_scheduler")
            Timber.d("✅ [Application] 逾期检测 TaskWorkScheduler 初始化成功")
        } catch (e: Exception) {
            Timber.e(e, "❌ [Application] 逾期检测 TaskWorkScheduler 初始化失败")
        }

        // 定时延期转待办调度
        try {
            TaskWorkScheduler.scheduleDelayedConversion(this)
            TaskWorkScheduler.triggerImmediateDelayedConversion(this)
            StartupTracker.record("delayed_convert")
            Timber.d("✅ [Application] 延期转待办 TaskWorkScheduler 初始化成功")
        } catch (e: Exception) {
            Timber.e(e, "❌ [Application] 延期转待办 TaskWorkScheduler 初始化失败")
        }

        // 任务通知调度
        try {
            TaskWorkScheduler.scheduleTaskNotifications(this)
            StartupTracker.record("notification_sched")
            Timber.d("✅ [Application] 任务通知 TaskWorkScheduler 初始化成功")
        } catch (e: Exception) {
            Timber.e(e, "❌ [Application] 任务通知 TaskWorkScheduler 初始化失败")
        }

        // 倒计时通知更新调度
        try {
            TaskWorkScheduler.scheduleCountdownUpdates(this)
            StartupTracker.record("countdown_sched")
            Timber.d("✅ [Application] 倒计时更新 TaskWorkScheduler 初始化成功")
        } catch (e: Exception) {
            Timber.e(e, "❌ [Application] 倒计时更新 TaskWorkScheduler 初始化失败")
        }

        // 重复任务生成调度
        try {
            TaskWorkScheduler.scheduleRecurringTaskGeneration(this)
            TaskWorkScheduler.triggerImmediateRecurringTaskGeneration(this)
            StartupTracker.record("recurring_sched")
            Timber.d("✅ [Application] 重复任务生成 TaskWorkScheduler 初始化成功")
        } catch (e: Exception) {
            Timber.e(e, "❌ [Application] 重复任务生成 TaskWorkScheduler 初始化失败")
        }

        // 智能早晚报调度
        applicationScope.launch {
            try {
                if (briefingPreferences.isEnabledOnce()) {
                    val mHour = briefingPreferences.getMorningHourOnce()
                    val mMin = briefingPreferences.getMorningMinuteOnce()
                    val eHour = briefingPreferences.getEveningHourOnce()
                    val eMin = briefingPreferences.getEveningMinuteOnce()
                    TaskWorkScheduler.scheduleMorningBriefing(this@NextThingApplication, mHour, mMin)
                    TaskWorkScheduler.scheduleEveningBriefing(this@NextThingApplication, eHour, eMin)
                    Timber.d("✅ [Application] 早晚报调度成功: 早报 %02d:%02d, 晚报 %02d:%02d", mHour, mMin, eHour, eMin)
                } else {
                    Timber.d("ℹ️ [Application] 早晚报未启用")
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ [Application] 早晚报调度失败")
            }
        }

        // 地理围栏初始化
        try {
            initializeGeofences()
            StartupTracker.record("geofence_init")
            Timber.d("✅ [Application] 地理围栏初始化开始")
        } catch (e: Exception) {
            Timber.e(e, "❌ [Application] 地理围栏初始化失败")
        }

        StartupTracker.record("app_onCreate_end")
        Timber.d("🎉 [Application] NextThingApplication 初始化完成")

        // 异步刷启动打点到 Room
        applicationScope.launch {
            try {
                StartupTraceCollector.flushNewToDatabase(startupTraceDao)
                Timber.d("✅ [Application] 启动打点已刷入数据库")
            } catch (e: Exception) {
                Timber.e(e, "❌ [Application] 启动打点刷入失败")
            }
        }
    }

    /**
     * 初始化地理围栏
     * 在应用启动时注册所有已保存的地理围栏到系统
     */
    private fun initializeGeofences() {
        applicationScope.launch {
            try {
                Timber.d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Timber.d("🌍 [Geofence] 开始初始化地理围栏...")

                // 检查位置权限
                if (!geofenceManager.hasLocationPermission()) {
                    Timber.w("⚠️ [Geofence] 缺少位置权限，跳过地理围栏初始化")
                    return@launch
                }

                // 获取所有地理围栏地点
                val geofenceLocations = geofenceUseCases.getGeofenceLocations.getAllOnce()

                if (geofenceLocations.isEmpty()) {
                    Timber.d("ℹ️ [Geofence] 没有需要注册的地理围栏")
                    Timber.d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    return@launch
                }

                Timber.d("📍 [Geofence] 找到 ${geofenceLocations.size} 个地理围栏地点")

                // 获取默认配置
                val config = geofenceUseCases.getGeofenceConfig.getOrDefault()

                // 构建地理围栏数据列表
                val geofenceDataList = geofenceLocations.map { location ->
                    val radius = (location.customRadius ?: config.defaultRadius).toFloat()
                    GeofenceData(
                        locationId = location.locationInfo.id,
                        latitude = location.locationInfo.latitude,
                        longitude = location.locationInfo.longitude,
                        radius = radius
                    )
                }

                // 批量注册地理围栏
                val result = geofenceManager.registerGeofences(geofenceDataList)

                if (result.isSuccess) {
                    val count = result.getOrNull() ?: 0
                    Timber.d("✅ [Geofence] 成功注册 $count 个系统地理围栏")
                } else {
                    Timber.e("❌ [Geofence] 地理围栏注册失败: ${result.exceptionOrNull()?.message}")
                }

                Timber.d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            } catch (e: Exception) {
                Timber.e(e, "❌ [Geofence] 地理围栏初始化异常")
                Timber.d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            }
        }
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
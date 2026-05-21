package com.nextthing.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nextthing.app.domain.model.TaskStatus
import com.nextthing.app.domain.repository.GeofenceConfigRepository
import com.nextthing.app.domain.repository.GeofenceLocationRepository
import com.nextthing.app.domain.repository.TaskRepository
import com.nextthing.app.domain.service.GeofenceData
import com.nextthing.app.domain.service.GeofenceManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime

/**
 * 设备重启后重新调度任务闹钟 + 重新注册地理围栏
 *
 * AlarmManager 的闹钟和系统地理围栏在设备重启后都会全部清除，
 * 需要在 BOOT_COMPLETED 时重新注册。
 */
class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun taskRepository(): TaskRepository
        fun taskAlarmManager(): TaskAlarmManager
        fun geofenceManager(): GeofenceManager
        fun geofenceLocationRepository(): GeofenceLocationRepository
        fun geofenceConfigRepository(): GeofenceConfigRepository
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Timber.d("[BootReceiver] 设备重启，开始恢复闹钟和地理围栏...")

        val entryPoint = try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                BootReceiverEntryPoint::class.java
            )
        } catch (e: Exception) {
            Timber.e(e, "[BootReceiver] 获取EntryPoint失败")
            return
        }

        val taskRepository = entryPoint.taskRepository()
        val taskAlarmManager = entryPoint.taskAlarmManager()
        val geofenceManager = entryPoint.geofenceManager()
        val geofenceLocationRepository = entryPoint.geofenceLocationRepository()
        val geofenceConfigRepository = entryPoint.geofenceConfigRepository()

        val pendingResult = goAsync()

        scope.launch {
            try {
                // ── 1. 重新调度任务闹钟 ──
                val now = LocalDateTime.now()
                val tasks = taskRepository.getAllTasks().first()
                var rescheduled = 0

                tasks.filter { task ->
                    (task.status == TaskStatus.PENDING || task.status == TaskStatus.DELAYED) &&
                    task.dueDate != null &&
                    task.dueDate.isAfter(now) &&
                    task.notificationStrategyId != null
                }.forEach { task ->
                    taskAlarmManager.scheduleTaskAlarm(task)
                    rescheduled++
                }

                Timber.d("[BootReceiver] 已重新调度 $rescheduled 个任务闹钟")

                // ── 2. 重新注册地理围栏 ──
                try {
                    if (!geofenceManager.hasLocationPermission()) {
                        Timber.w("[BootReceiver] 缺少位置权限，跳过地理围栏恢复")
                    } else {
                        val config = geofenceConfigRepository.getConfigOrDefault()
                        if (!config.isGlobalEnabled) {
                            Timber.d("[BootReceiver] 地理围栏全局未启用，跳过恢复")
                        } else {
                            val locations = geofenceLocationRepository.getAllLocationsOnce()
                            if (locations.isEmpty()) {
                                Timber.d("[BootReceiver] 没有地理围栏地点需要恢复")
                            } else {
                                val geofenceDataList = locations.map { location ->
                                    val radius = (location.customRadius ?: config.defaultRadius).toFloat()
                                    GeofenceData(
                                        locationId = location.locationInfo.id,
                                        latitude = location.locationInfo.latitude,
                                        longitude = location.locationInfo.longitude,
                                        radius = radius
                                    )
                                }

                                val result = geofenceManager.registerGeofences(geofenceDataList)
                                if (result.isSuccess) {
                                    Timber.d("[BootReceiver] ✅ 已恢复 ${locations.size} 个地理围栏")
                                } else {
                                    Timber.e("[BootReceiver] ❌ 地理围栏恢复失败: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "[BootReceiver] 恢复地理围栏异常")
                }
            } catch (e: Exception) {
                Timber.e(e, "[BootReceiver] 重启恢复失败")
            } finally {
                pendingResult.finish()
            }
        }
    }
}

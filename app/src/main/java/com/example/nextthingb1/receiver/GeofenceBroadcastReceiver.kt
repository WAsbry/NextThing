package com.example.nextthingb1.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nextthingb1.domain.model.GeofenceCheckResult
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.repository.GeofenceLocationRepository
import com.example.nextthingb1.domain.repository.NotificationStrategyRepository
import com.example.nextthingb1.domain.repository.TaskGeofenceRepository
import com.example.nextthingb1.domain.repository.TaskRepository
import com.example.nextthingb1.domain.usecase.GeofenceUseCases
import com.example.nextthingb1.util.NotificationHelper
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * 地理围栏事件广播接收器
 *
 * 接收系统地理围栏的进入/离开事件，并执行相应处理
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceBroadcast"
    }

    @Inject
    lateinit var taskGeofenceRepository: TaskGeofenceRepository

    @Inject
    lateinit var geofenceLocationRepository: GeofenceLocationRepository

    @Inject
    lateinit var geofenceUseCases: GeofenceUseCases

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var notificationStrategyRepository: NotificationStrategyRepository

    override fun onReceive(context: Context, intent: Intent) {
        Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag(TAG).d("收到地理围栏广播")

        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: run {
            Timber.tag(TAG).e("❌ GeofencingEvent 为 null")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorCode = geofencingEvent.errorCode
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(errorCode)
            Timber.tag(TAG).e("❌ 地理围栏错误: $errorMessage (code: $errorCode)")
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            return
        }

        // 获取触发的地理围栏
        val triggeringGeofences = geofencingEvent.triggeringGeofences
        if (triggeringGeofences.isNullOrEmpty()) {
            Timber.tag(TAG).w("⚠️ 没有触发的地理围栏")
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            return
        }

        // 获取转换类型（进入/离开）
        val geofenceTransition = geofencingEvent.geofenceTransition

        val transitionString = when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "进入围栏"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "离开围栏"
            Geofence.GEOFENCE_TRANSITION_DWELL -> "停留"
            else -> "未知事件($geofenceTransition)"
        }

        Timber.tag(TAG).d("📍 事件类型: $transitionString")
        Timber.tag(TAG).d("📍 触发的围栏数量: ${triggeringGeofences.size}")

        // 使用 goAsync() 延长 BroadcastReceiver 的生命周期
        val pendingResult = goAsync()

        // 在协程中处理（避免阻塞主线程）
        // 使用 SupervisorJob 确保一个任务失败不影响其他任务
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // 处理每个触发的地理围栏
                triggeringGeofences.forEach { geofence ->
                    val locationId = geofence.requestId
                    Timber.tag(TAG).d("  围栏ID: $locationId")

                    handleGeofenceTransition(
                        locationId = locationId,
                        transitionType = geofenceTransition
                    )
                }

                Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            } finally {
                // 完成异步工作
                pendingResult.finish()
            }
        }
    }

    /**
     * 处理地理围栏转换事件
     */
    private suspend fun handleGeofenceTransition(
        locationId: String,
        transitionType: Int
    ) {
        try {
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.tag(TAG).d("处理地理围栏事件")
            Timber.tag(TAG).d("  地点ID: $locationId")
            Timber.tag(TAG).d("  事件类型: $transitionType")

            // 1. 获取地点信息（使用 locationInfo.id 查询，与注册时的 requestId 一致）
            val geofenceLocation = geofenceLocationRepository.getByLocationId(locationId)
            if (geofenceLocation == null) {
                Timber.tag(TAG).e("❌ 地点不存在: $locationId")
                return
            }

            Timber.tag(TAG).d("✅ 地点: ${geofenceLocation.locationInfo.locationName}")

            // 2. 更新地点使用统计
            when (transitionType) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    // 进入围栏 - 更新使用统计（使用 GeofenceLocation 的主键 ID）
                    geofenceUseCases.updateLocationUsage(geofenceLocation.id)
                    Timber.tag(TAG).d("📊 已更新地点使用统计")
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    // 离开围栏 - 可以记录离开时间等
                    Timber.tag(TAG).d("👋 用户离开围栏")
                }
            }

            // 3. 查找关联的任务（使用 GeofenceLocation 的主键 ID 查询 TaskGeofence）
            val relatedTaskGeofences = taskGeofenceRepository.getByLocationId(geofenceLocation.id).first()

            if (relatedTaskGeofences.isEmpty()) {
                Timber.tag(TAG).d("ℹ️ 该地点没有关联任务")
                return
            }

            Timber.tag(TAG).d("📋 找到 ${relatedTaskGeofences.size} 个关联任务")

            // 4. 处理每个关联的任务
            relatedTaskGeofences.forEach { taskGeofence ->
                handleTaskGeofenceEvent(
                    taskGeofence = taskGeofence,
                    transitionType = transitionType,
                    locationName = geofenceLocation.locationInfo.locationName
                )
            }

            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 处理地理围栏事件异常")
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }

    /**
     * 处理任务地理围栏事件
     */
    private suspend fun handleTaskGeofenceEvent(
        taskGeofence: com.example.nextthingb1.domain.model.TaskGeofence,
        transitionType: Int,
        locationName: String
    ) {
        try {
            val taskId = taskGeofence.taskId

            Timber.tag(TAG).d("  → 任务ID: $taskId")

            // 检查任务是否启用地理围栏
            if (!taskGeofence.isEnabled) {
                Timber.tag(TAG).d("  ⏭️ 任务地理围栏未启用，跳过")
                return
            }

            // 根据事件类型处理
            when (transitionType) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    handleEnterGeofence(taskId, locationName, taskGeofence)
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    handleExitGeofence(taskId, locationName, taskGeofence)
                }
            }

            // 更新统计数据
            geofenceUseCases.updateLocationCheckStatistics.invoke(
                locationId = taskGeofence.geofenceLocationId,
                isHit = (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
            )

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 处理任务地理围栏事件异常")
        }
    }

    /**
     * 处理进入围栏事件
     */
    private suspend fun handleEnterGeofence(
        taskId: String,
        locationName: String,
        taskGeofence: com.example.nextthingb1.domain.model.TaskGeofence
    ) {
        Timber.tag(TAG).d("  ✅ 进入围栏: $locationName")

        // 更新检查结果
        taskGeofenceRepository.updateLastCheckResult(
            taskId = taskId,
            result = GeofenceCheckResult.INSIDE_GEOFENCE,
            distance = 0.0, // 系统事件，无法获取精确距离
            userLatitude = 0.0,
            userLongitude = 0.0
        )

        // 检查关联任务是否处于围栏外延期状态，若是则立即发送通知
        try {
            val task = taskRepository.getTaskById(taskId)
            if (task != null && task.status == TaskStatus.DELAYED && task.dueDate != null) {
                val now = LocalDateTime.now()
                Timber.tag(TAG).i("🔔 延期任务用户已进入围栏，立即发送通知: ${task.title}")

                val strategy = task.notificationStrategyId?.let { strategyId ->
                    notificationStrategyRepository.getStrategyById(strategyId)
                }

                if (strategy != null) {
                    val secondsUntilDue = java.time.Duration.between(now, task.dueDate).seconds
                    notificationHelper.showTaskNotificationWithCountdown(
                        task = task,
                        strategy = strategy,
                        secondsUntilDue = secondsUntilDue
                    )
                    // 重置延期次数（任务已被正常通知）
                    taskGeofenceRepository.resetDeferCount(taskId)
                    Timber.tag(TAG).d("  ✅ 已发送补偿通知并重置延期次数")
                } else {
                    Timber.tag(TAG).w("  ⚠️ 找不到通知策略，跳过通知: taskId=$taskId")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "  ❌ 处理延期任务补偿通知失败")
        }

        Timber.tag(TAG).d("  📝 已记录进入事件")
    }

    /**
     * 处理离开围栏事件
     */
    private suspend fun handleExitGeofence(
        taskId: String,
        locationName: String,
        taskGeofence: com.example.nextthingb1.domain.model.TaskGeofence
    ) {
        Timber.tag(TAG).d("  👋 离开围栏: $locationName")

        // 更新检查结果
        taskGeofenceRepository.updateLastCheckResult(
            taskId = taskId,
            result = GeofenceCheckResult.OUTSIDE_GEOFENCE,
            distance = 0.0, // 系统事件，无法获取精确距离
            userLatitude = 0.0,
            userLongitude = 0.0
        )

        Timber.tag(TAG).d("  📝 已记录离开事件")
    }
}

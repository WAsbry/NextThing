package com.nextthing.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nextthing.app.domain.model.GeofenceCheckResult
import com.nextthing.app.domain.model.TaskStatus
import com.nextthing.app.domain.repository.GeofenceConfigRepository
import com.nextthing.app.domain.repository.NotificationStrategyRepository
import com.nextthing.app.domain.repository.TaskRepository
import com.nextthing.app.domain.repository.TaskGeofenceRepository
import com.nextthing.app.domain.service.GeofenceCheckService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime

/**
 * 任务闹钟广播接收器
 * 当闹钟时间到达时，系统会触发此广播接收器
 *
 * 【NotificationTest】通知流程 - 第3步：接收闹钟触发
 * 此类接收AlarmManager发送的广播，然后显示通知
 *
 * 如果任务启用了地理围栏，会先检查用户是否在围栏内：
 * - 在围栏内 → 正常发送通知
 * - 在围栏外 → 延期30分钟并重新调度闹钟
 * - 定位不可用/权限拒绝 → 降级为普通通知
 */
class TaskAlarmReceiver : BroadcastReceiver() {

    /**
     * Hilt EntryPoint接口
     * 因为BroadcastReceiver不能使用@AndroidEntryPoint，所以使用EntryPoint模式获取依赖
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TaskAlarmReceiverEntryPoint {
        fun taskRepository(): TaskRepository
        fun notificationStrategyRepository(): NotificationStrategyRepository
        fun notificationHelper(): NotificationHelper
        fun geofenceCheckService(): GeofenceCheckService
        fun taskGeofenceRepository(): TaskGeofenceRepository
        fun geofenceConfigRepository(): GeofenceConfigRepository
        fun taskAlarmManager(): TaskAlarmManager
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "NotificationTask"
        private const val MAX_DEFER_COUNT = 3
    }

    /**
     * 接收闹钟广播
     * 【NotificationTest】闹钟时间到达时被系统调用
     */
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("taskId")
        val notificationStrategyId = intent.getStringExtra("notificationStrategyId")
        val isAdvanceReminder = intent.getBooleanExtra("isAdvanceReminder", false)
        val advanceMinutes = intent.getIntExtra("advanceMinutes", 0)

        Timber.tag(TAG).d("闹钟触发: taskId=$taskId, 提前提醒=$isAdvanceReminder, 提前${advanceMinutes}分钟")

        if (taskId == null || notificationStrategyId == null) {
            Timber.tag(TAG).e("任务ID或通知策略ID为空")
            return
        }

        val appContext = context.applicationContext
        val entryPoint = try {
            EntryPointAccessors.fromApplication(
                appContext,
                TaskAlarmReceiverEntryPoint::class.java
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e("获取EntryPoint失败: ${e.message}")
            return
        }

        val taskRepository = entryPoint.taskRepository()
        val notificationStrategyRepository = entryPoint.notificationStrategyRepository()
        val notificationHelper = entryPoint.notificationHelper()
        val geofenceCheckService = entryPoint.geofenceCheckService()
        val taskGeofenceRepository = entryPoint.taskGeofenceRepository()
        val geofenceConfigRepository = entryPoint.geofenceConfigRepository()
        val taskAlarmManager = entryPoint.taskAlarmManager()

        val pendingResult = goAsync()

        scope.launch {
            try {
                val task = taskRepository.getTaskById(taskId)
                if (task == null) {
                    Timber.tag(TAG).e("未找到任务: $taskId")
                    pendingResult.finish()
                    return@launch
                }

                if (task.status != TaskStatus.PENDING && task.status != TaskStatus.DELAYED) {
                    Timber.tag(TAG).d("任务状态为 ${task.status}，跳过通知")
                    pendingResult.finish()
                    return@launch
                }

                val strategy = notificationStrategyRepository.getStrategyById(notificationStrategyId)
                if (strategy == null) {
                    Timber.tag(TAG).e("未找到通知策略: $notificationStrategyId")
                    pendingResult.finish()
                    return@launch
                }

                // ── 地理围栏检查 ──
                // 提前提醒不做地理围栏检查（提前提醒是提示性的，不应被静默延期）
                if (!isAdvanceReminder) {
                    val taskGeofence = taskGeofenceRepository.getByTaskIdOnce(taskId)

                    if (taskGeofence != null && taskGeofence.isEnabled) {
                        // 检查全局开关
                        val geofenceConfig = try {
                            geofenceConfigRepository.getConfigOrDefault()
                        } catch (e: Exception) {
                            Timber.tag(TAG).w("读取地理围栏配置失败: ${e.message}")
                            null
                        }

                        if (geofenceConfig != null && geofenceConfig.isGlobalEnabled) {
                            Timber.tag(TAG).d("🛡️ 任务启用了地理围栏，开始检查...")

                            val geofenceStatus = geofenceCheckService.checkTaskGeofence(taskId)

                            when (geofenceStatus.checkResult) {
                                GeofenceCheckResult.INSIDE_GEOFENCE -> {
                                    Timber.tag(TAG).d("✅ 在围栏内，正常发送通知")
                                    // 继续到下面发送通知
                                }

                                GeofenceCheckResult.OUTSIDE_GEOFENCE -> {
                                    val notifyWhenOutside = geofenceConfig.notifyWhenOutside

                                    if (notifyWhenOutside) {
                                        // 发送低优先级提醒
                                        Timber.tag(TAG).i("📢 在围栏外，发送低优先级提醒")
                                        val distanceText = String.format("%.0f", geofenceStatus.distance)
                                        val secondsUntilDue = task.dueDate?.let {
                                            java.time.Duration.between(LocalDateTime.now(), it).seconds
                                        } ?: 0L
                                        val countdownText = when {
                                            secondsUntilDue <= 0 -> "已到期"
                                            secondsUntilDue < 60 -> "${secondsUntilDue}秒后到期"
                                            secondsUntilDue < 3600 -> "${secondsUntilDue / 60}分钟后到期"
                                            else -> "${secondsUntilDue / 3600}小时后到期"
                                        }
                                        notificationHelper.showLowPriorityNotification(
                                            taskId = task.id,
                                            title = "📍 ${task.title}",
                                            content = "⏰ $countdownText\n📍 您不在目标地点范围内\n📏 距离目标地点: ${distanceText}米",
                                            fullContent = buildString {
                                                append("⏰ $countdownText\n")
                                                append("📍 您不在目标地点范围内\n")
                                                append("📏 距离目标地点: ${distanceText}米\n")
                                                if (geofenceStatus.targetLocationName.isNotBlank()) {
                                                    append("🎯 目标地点: ${geofenceStatus.targetLocationName}\n")
                                                }
                                                if (task.description.isNotBlank()) {
                                                    append("\n${task.description}")
                                                }
                                            }
                                        )
                                        pendingResult.finish()
                                        return@launch
                                    } else if (taskGeofence.geofenceDeferCount >= MAX_DEFER_COUNT) {
                                        // 超过最大延期次数，强制发送通知
                                        Timber.tag(TAG).w("⚠️ 已延期${taskGeofence.geofenceDeferCount}次，强制发送通知")
                                        taskGeofenceRepository.resetDeferCount(taskId)
                                        // 继续到下面发送通知
                                    } else {
                                        // 延期30分钟
                                        Timber.tag(TAG).w("⚠️ 在围栏外，延期任务（第${taskGeofence.geofenceDeferCount + 1}次）")
                                        val now = LocalDateTime.now()
                                        val deferredDueDate = now.plusMinutes(30)
                                        val updatedTask = task.copy(
                                            status = TaskStatus.DELAYED,
                                            dueDate = deferredDueDate,
                                            updatedAt = now
                                        )
                                        taskRepository.updateTask(updatedTask)

                                        // 重新调度闹钟
                                        taskAlarmManager.cancelTaskAlarm(taskId)
                                        taskAlarmManager.scheduleTaskAlarm(updatedTask)
                                        taskGeofenceRepository.incrementDeferCount(taskId)

                                        Timber.tag(TAG).i("✅ 任务已延期30分钟: ${task.title}, 新截止: $deferredDueDate")
                                        pendingResult.finish()
                                        return@launch
                                    }
                                }

                                GeofenceCheckResult.LOCATION_UNAVAILABLE,
                                GeofenceCheckResult.PERMISSION_DENIED -> {
                                    // 降级为普通通知
                                    Timber.tag(TAG).w("⚠️ 定位不可用(${geofenceStatus.checkResult})，降级发送通知")
                                    // 继续到下面发送通知
                                }

                                GeofenceCheckResult.GEOFENCE_DISABLED -> {
                                    // 地理围栏已禁用，正常发送
                                    Timber.tag(TAG).d("📢 地理围栏已禁用，正常发送通知")
                                }
                            }
                        }
                    }
                }

                // ── 发送通知 ──
                if (isAdvanceReminder) {
                    notificationHelper.showAdvanceReminderNotification(task, strategy, advanceMinutes)
                    Timber.tag(TAG).d("提前${advanceMinutes}分钟提醒已发送: ${task.title}")
                } else {
                    notificationHelper.showTaskNotification(task, strategy)
                    Timber.tag(TAG).d("到期通知已发送: ${task.title}")
                }

            } catch (e: Exception) {
                Timber.tag(TAG).e("显示通知异常: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}

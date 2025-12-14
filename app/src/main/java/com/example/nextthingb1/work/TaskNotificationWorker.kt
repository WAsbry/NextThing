package com.example.nextthingb1.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.repository.NotificationStrategyRepository
import com.example.nextthingb1.domain.repository.TaskRepository
import com.example.nextthingb1.domain.service.GeofenceCheckService
import com.example.nextthingb1.domain.model.GeofenceCheckResult
import com.example.nextthingb1.domain.usecase.GeofenceUseCases
import com.example.nextthingb1.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDateTime

/**
 * WorkManager worker that checks for tasks that need notifications
 * and triggers them according to their notification strategy
 */
@HiltWorker
class TaskNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val notificationStrategyRepository: NotificationStrategyRepository,
    private val notificationHelper: NotificationHelper,
    private val geofenceCheckService: GeofenceCheckService,
    private val geofenceUseCases: GeofenceUseCases
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TaskNotificationWorker"
        private const val TAG_GEOFENCE = "TaskNotification-Geofence"
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.tag(TAG).d("🔔 开始任务通知检查")

            val now = LocalDateTime.now()
            val tasks = taskRepository.getAllTasks().first()
            val strategies = notificationStrategyRepository.getAllStrategies().first()

            // 读取地理围栏全局配置
            val geofenceConfig = try {
                geofenceUseCases.getGeofenceConfig.getOrDefault()
            } catch (e: Exception) {
                Timber.tag(TAG_GEOFENCE).w(e, "读取地理围栏配置失败，使用默认值")
                null
            }

            var notificationCount = 0
            var geofenceDelayCount = 0

            // 第一步：筛选出需要通知的任务
            val tasksToNotify = tasks.filter { task ->
                task.status == TaskStatus.PENDING &&
                task.dueDate != null &&
                task.notificationStrategyId != null
            }.filter { task ->
                val dueDate = task.dueDate!!
                val minutesUntilDue = java.time.Duration.between(now, dueDate).toMinutes()
                // 在截止时间前3分钟到截止时间这个窗口内发送通知
                minutesUntilDue in 0..3
            }

            if (tasksToNotify.isEmpty()) {
                Timber.tag(TAG).d("📭 没有需要通知的任务")
                Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                return Result.success()
            }

            Timber.tag(TAG).d("📬 找到 ${tasksToNotify.size} 个需要通知的任务")

            // 第二步：识别启用了地理围栏的任务（批量获取配置）
            val taskGeofenceMap = mutableMapOf<String, com.example.nextthingb1.domain.model.TaskGeofence>()
            tasksToNotify.forEach { task ->
                try {
                    val taskGeofence = geofenceUseCases.getTaskGeofence.invoke(task.id).first()
                    if (taskGeofence != null && taskGeofence.isEnabled) {
                        taskGeofenceMap[task.id] = taskGeofence
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG_GEOFENCE).e(e, "获取任务地理围栏失败: ${task.id}")
                }
            }

            // 第三步：批量检查地理围栏（一次位置获取）
            val geofenceResults = if (taskGeofenceMap.isNotEmpty()) {
                Timber.tag(TAG_GEOFENCE).d("🛡️ 批量检查 ${taskGeofenceMap.size} 个任务的地理围栏...")
                geofenceCheckService.checkMultipleTaskGeofences(taskGeofenceMap.keys.toList())
            } else {
                emptyMap()
            }

            // 第四步：根据地理围栏结果处理每个任务
            tasksToNotify.forEach { task ->
                val dueDate = task.dueDate!!
                val minutesUntilDue = java.time.Duration.between(now, dueDate).toMinutes()
                Timber.tag(TAG).d("📋 任务: ${task.title}, 距离截止: ${minutesUntilDue}分钟")

                val geofenceStatus = geofenceResults[task.id]
                val taskGeofence = taskGeofenceMap[task.id]

                if (geofenceStatus != null && taskGeofence != null) {
                    // 启用了地理围栏，根据检查结果处理
                    Timber.tag(TAG_GEOFENCE).d("📍 地理围栏检查结果: ${geofenceStatus.checkResult}")
                    Timber.tag(TAG_GEOFENCE).d("   距离: ${geofenceStatus.distance}米")
                    Timber.tag(TAG_GEOFENCE).d("   用户位置: (${geofenceStatus.userLatitude}, ${geofenceStatus.userLongitude})")

                    when (geofenceStatus.checkResult) {
                        GeofenceCheckResult.INSIDE_GEOFENCE -> {
                            // 在围栏内，正常发送通知
                            Timber.tag(TAG_GEOFENCE).d("✅ 用户在围栏内，发送通知")
                            sendNotification(task, strategies, now, dueDate)
                            notificationCount++

                            // 更新使用统计
                            updateGeofenceStatistics(taskGeofence.geofenceLocation.id, task.id, geofenceStatus.checkResult)
                        }
                        GeofenceCheckResult.OUTSIDE_GEOFENCE -> {
                            // 在围栏外，根据配置决定行为
                            val notifyWhenOutside = geofenceConfig?.notifyWhenOutside ?: false

                            if (notifyWhenOutside) {
                                // 发送低优先级提醒通知
                                Timber.tag(TAG_GEOFENCE).i("📢 用户在围栏外，发送低优先级提醒")
                                sendLowPriorityGeofenceNotification(task, geofenceStatus, strategies, now, dueDate)
                                notificationCount++
                            } else {
                                // 延期任务（原有逻辑）
                                Timber.tag(TAG_GEOFENCE).w("⚠️ 用户在围栏外，延期任务")
                                handleOutsideGeofence(task, geofenceStatus)
                            }

                            geofenceDelayCount++

                            // 注意：检查结果已由 GeofenceCheckService 自动记录，无需重复更新
                        }
                        GeofenceCheckResult.LOCATION_UNAVAILABLE,
                        GeofenceCheckResult.PERMISSION_DENIED -> {
                            // 降级策略：无法获取位置时，仍然发送通知
                            Timber.tag(TAG_GEOFENCE).w("⚠️ 定位不可用(${geofenceStatus.checkResult})，降级发送通知")
                            sendNotification(task, strategies, now, dueDate)
                            notificationCount++

                            // 注意：检查结果已由 GeofenceCheckService 自动记录，无需重复更新
                        }
                        GeofenceCheckResult.GEOFENCE_DISABLED -> {
                            // 地理围栏已禁用，正常发送通知
                            Timber.tag(TAG_GEOFENCE).d("📢 地理围栏已禁用，正常发送通知")
                            sendNotification(task, strategies, now, dueDate)
                            notificationCount++
                        }
                    }
                } else {
                    // 未启用地理围栏，正常发送通知
                    Timber.tag(TAG).d("📢 未启用地理围栏，正常发送通知")
                    sendNotification(task, strategies, now, dueDate)
                    notificationCount++
                }
            }

            Timber.tag(TAG).i("✅ 通知检查完成")
            Timber.tag(TAG).i("   发送通知: $notificationCount 个")
            Timber.tag(TAG).i("   围栏延期: $geofenceDelayCount 个")
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Result.success()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 通知检查失败")
            Result.retry()
        }
    }

    /**
     * 发送任务通知
     */
    private fun sendNotification(
        task: com.example.nextthingb1.domain.model.Task,
        strategies: List<com.example.nextthingb1.domain.model.NotificationStrategy>,
        now: LocalDateTime,
        dueDate: LocalDateTime
    ) {
        val strategy = strategies.find { it.id == task.notificationStrategyId }

        if (strategy != null) {
            // 计算精确的倒计时（秒）
            val secondsUntilDue = java.time.Duration.between(now, dueDate).seconds

            // 显示通知，传递倒计时信息
            notificationHelper.showTaskNotificationWithCountdown(
                task = task,
                strategy = strategy,
                secondsUntilDue = secondsUntilDue
            )
            Timber.tag(TAG).d("📨 通知已发送: ${task.title}")
        } else {
            Timber.tag(TAG).w("⚠️ 找不到通知策略: strategyId=${task.notificationStrategyId}")
        }
    }

    /**
     * 发送低优先级地理围栏提醒通知
     *
     * 当用户不在目标地点范围内时，发送低优先级提醒
     * 与正常通知的区别：
     * - 优先级较低（PRIORITY_DEFAULT）
     * - 不会播放声音和震动
     * - 通知内容提示用户不在范围内
     */
    private fun sendLowPriorityGeofenceNotification(
        task: com.example.nextthingb1.domain.model.Task,
        geofenceStatus: com.example.nextthingb1.domain.model.GeofenceStatus,
        strategies: List<com.example.nextthingb1.domain.model.NotificationStrategy>,
        now: LocalDateTime,
        dueDate: LocalDateTime
    ) {
        try {
            val strategy = strategies.find { it.id == task.notificationStrategyId }

            if (strategy == null) {
                Timber.tag(TAG_GEOFENCE).w("⚠️ 找不到通知策略: strategyId=${task.notificationStrategyId}")
                return
            }

            // 计算距离和倒计时
            val distanceText = String.format("%.0f", geofenceStatus.distance)
            val secondsUntilDue = java.time.Duration.between(now, dueDate).seconds
            val countdownText = formatCountdown(secondsUntilDue)

            // 构建通知内容
            val notificationTitle = "📍 ${task.title}"
            val notificationContent = buildString {
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

            // 使用 NotificationHelper 发送低优先级通知
            notificationHelper.showLowPriorityNotification(
                taskId = task.id,
                title = notificationTitle,
                content = notificationContent,
                fullContent = notificationContent
            )

            Timber.tag(TAG_GEOFENCE).d("📨 低优先级通知已发送: ${task.title}")
            Timber.tag(TAG_GEOFENCE).d("   距离: ${distanceText}米")
            Timber.tag(TAG_GEOFENCE).d("   倒计时: $countdownText")
        } catch (e: Exception) {
            Timber.tag(TAG_GEOFENCE).e(e, "发送低优先级通知失败")
        }
    }

    /**
     * 格式化倒计时文本
     */
    private fun formatCountdown(seconds: Long): String {
        return when {
            seconds <= 0 -> "已到期"
            seconds < 60 -> "${seconds}秒后到期"
            seconds < 3600 -> "${seconds / 60}分钟后到期"
            else -> "${seconds / 3600}小时后到期"
        }
    }

    /**
     * 处理用户在围栏外的情况
     * 将任务延期，并在描述中添加系统提示
     */
    private suspend fun handleOutsideGeofence(
        task: com.example.nextthingb1.domain.model.Task,
        status: com.example.nextthingb1.domain.model.GeofenceStatus
    ) {
        try {
            val distanceText = String.format("%.0f", status.distance)
            val systemNote = "\n\n【系统提示 ${LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"))}】" +
                    "提醒时间时您不在目标地点范围内(距离${distanceText}米)，已自动延期。"

            val updatedTask = task.copy(
                status = TaskStatus.DELAYED,
                description = task.description + systemNote,
                updatedAt = LocalDateTime.now()
            )

            taskRepository.updateTask(updatedTask)
            Timber.tag(TAG_GEOFENCE).i("✅ 任务已延期: ${task.title}")
            Timber.tag(TAG_GEOFENCE).d("   距离: ${distanceText}米")
            Timber.tag(TAG_GEOFENCE).d("   新状态: DELAYED")
        } catch (e: Exception) {
            Timber.tag(TAG_GEOFENCE).e(e, "❌ 处理围栏外任务异常")
        }
    }

    /**
     * 更新地理围栏统计信息
     * - 增加地点使用次数
     * - 更新最后使用时间
     * - 自动识别常用地点
     * - 更新月度检查统计（命中率计算）
     *
     * 注意：检查结果（lastCheckTime, lastCheckResult, distance等）
     * 已由 GeofenceCheckService 在检查时自动记录，无需重复更新
     */
    private suspend fun updateGeofenceStatistics(
        locationId: String,
        taskId: String,
        checkResult: GeofenceCheckResult
    ) {
        try {
            // 1. 更新地点使用统计（累计使用次数）
            geofenceUseCases.updateLocationUsage(locationId).fold(
                onSuccess = {
                    Timber.tag(TAG_GEOFENCE).d("📊 使用统计已更新: $locationId")
                },
                onFailure = { error ->
                    Timber.tag(TAG_GEOFENCE).e("❌ 更新使用统计失败: ${error.message}")
                }
            )

            // 2. 更新月度检查统计（用于计算命中率）
            val isHit = (checkResult == GeofenceCheckResult.INSIDE_GEOFENCE)
            geofenceUseCases.updateLocationCheckStatistics(locationId, isHit).fold(
                onSuccess = {
                    Timber.tag(TAG_GEOFENCE).d("📈 月度统计已更新: $locationId, 命中=$isHit")
                },
                onFailure = { error ->
                    Timber.tag(TAG_GEOFENCE).e("❌ 更新月度统计失败: ${error.message}")
                }
            )
        } catch (e: Exception) {
            Timber.tag(TAG_GEOFENCE).e(e, "更新统计信息异常")
        }
    }
}

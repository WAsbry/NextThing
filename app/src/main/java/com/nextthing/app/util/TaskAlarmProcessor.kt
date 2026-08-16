package com.nextthing.app.util

import com.nextthing.app.domain.model.GeofenceCheckResult
import com.nextthing.app.domain.model.TaskStatus
import com.nextthing.app.domain.repository.GeofenceConfigRepository
import com.nextthing.app.domain.repository.NotificationStrategyRepository
import com.nextthing.app.domain.repository.TaskGeofenceRepository
import com.nextthing.app.domain.repository.TaskRepository
import com.nextthing.app.domain.service.GeofenceCheckService
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes the business logic behind a scheduled task reminder.
 *
 * Both the foreground-service delivery path and the legacy broadcast fallback
 * use this class so reminder behavior stays identical across Android versions.
 */
@Singleton
class TaskAlarmProcessor @Inject constructor(
    private val taskRepository: TaskRepository,
    private val notificationStrategyRepository: NotificationStrategyRepository,
    private val notificationHelper: NotificationHelper,
    private val geofenceCheckService: GeofenceCheckService,
    private val taskGeofenceRepository: TaskGeofenceRepository,
    private val geofenceConfigRepository: GeofenceConfigRepository,
    private val taskAlarmManager: TaskAlarmManager
) {
    suspend fun process(
        taskId: String,
        notificationStrategyId: String,
        isAdvanceReminder: Boolean,
        advanceMinutes: Int
    ) {
        val task = taskRepository.getTaskById(taskId)
        if (task == null) {
            Timber.tag(TAG).e("未找到任务: $taskId")
            return
        }

        if (task.status != TaskStatus.PENDING && task.status != TaskStatus.DELAYED) {
            Timber.tag(TAG).d("任务状态为 ${task.status}，跳过通知")
            return
        }

        val strategy = notificationStrategyRepository.getStrategyById(notificationStrategyId)
        if (strategy == null) {
            Timber.tag(TAG).e("未找到通知策略: $notificationStrategyId")
            return
        }

        // Advance reminders are informational and should not be deferred by geofencing.
        if (!isAdvanceReminder) {
            val taskGeofence = taskGeofenceRepository.getByTaskIdOnce(taskId)
            if (taskGeofence != null && taskGeofence.isEnabled) {
                val geofenceConfig = try {
                    geofenceConfigRepository.getConfigOrDefault()
                } catch (e: Exception) {
                    Timber.tag(TAG).w("读取地理围栏配置失败: ${e.message}")
                    null
                }

                if (geofenceConfig != null && geofenceConfig.isGlobalEnabled) {
                    val geofenceStatus = geofenceCheckService.checkTaskGeofence(taskId)
                    when (geofenceStatus.checkResult) {
                        GeofenceCheckResult.INSIDE_GEOFENCE -> Unit

                        GeofenceCheckResult.OUTSIDE_GEOFENCE -> {
                            if (geofenceConfig.notifyWhenOutside) {
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
                                return
                            }

                            if (taskGeofence.geofenceDeferCount >= MAX_DEFER_COUNT) {
                                taskGeofenceRepository.resetDeferCount(taskId)
                            } else {
                                val now = LocalDateTime.now()
                                val updatedTask = task.copy(
                                    status = TaskStatus.DELAYED,
                                    dueDate = now.plusMinutes(30),
                                    updatedAt = now
                                )
                                taskRepository.updateTask(updatedTask)
                                taskAlarmManager.cancelTaskAlarm(taskId)
                                taskAlarmManager.scheduleTaskAlarm(updatedTask)
                                taskGeofenceRepository.incrementDeferCount(taskId)
                                return
                            }
                        }

                        GeofenceCheckResult.LOCATION_UNAVAILABLE,
                        GeofenceCheckResult.PERMISSION_DENIED,
                        GeofenceCheckResult.GEOFENCE_DISABLED -> Unit
                    }
                }
            }
        }

        if (isAdvanceReminder) {
            notificationHelper.showAdvanceReminderNotification(task, strategy, advanceMinutes)
        } else {
            notificationHelper.showTaskNotification(task, strategy)
        }
    }

    private companion object {
        const val TAG = "NotificationTask"
        const val MAX_DEFER_COUNT = 3
    }
}

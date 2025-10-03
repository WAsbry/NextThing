package com.example.nextthingb1.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.repository.NotificationStrategyRepository
import com.example.nextthingb1.domain.repository.TaskRepository
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
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("TaskNotificationWorker: Starting task notification check")

            val now = LocalDateTime.now()
            val tasks = taskRepository.getAllTasks().first()
            val strategies = notificationStrategyRepository.getAllStrategies().first()
            var notificationCount = 0

            // 遍历所有未完成且有截止时间的任务
            tasks.filter { task ->
                task.status == TaskStatus.PENDING &&
                task.dueDate != null &&
                task.notificationStrategyId != null
            }.forEach { task ->
                val dueDate = task.dueDate!!

                // 检查是否到达通知时间（提前5分钟或已到期）
                val shouldNotify = when {
                    // 已经到期
                    now.isAfter(dueDate) || now.isEqual(dueDate) -> true
                    // 提前5分钟通知
                    now.isAfter(dueDate.minusMinutes(5)) -> true
                    else -> false
                }

                if (shouldNotify) {
                    // 查找对应的通知策略
                    val strategy = strategies.find { it.id == task.notificationStrategyId }

                    if (strategy != null) {
                        // 显示通知，传递完整的任务对象
                        notificationHelper.showTaskNotification(
                            task = task,
                            strategy = strategy
                        )
                        notificationCount++
                        Timber.d("Notification sent for task: ${task.title}")
                    } else {
                        Timber.w("Notification strategy not found for task: ${task.title}, strategyId: ${task.notificationStrategyId}")
                    }
                }
            }

            Timber.i("TaskNotificationWorker: Completed. Sent $notificationCount notification(s)")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "TaskNotificationWorker: Failed to check task notifications")
            Result.retry()
        }
    }
}

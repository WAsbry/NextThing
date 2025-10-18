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
 * Worker that updates countdown notifications every minute
 */
@HiltWorker
class CountdownNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val notificationStrategyRepository: NotificationStrategyRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("CountdownNotificationWorker: Starting countdown update")

            val now = LocalDateTime.now()
            val tasks = taskRepository.getAllTasks().first()
            val strategies = notificationStrategyRepository.getAllStrategies().first()
            var updateCount = 0

            // 遍历所有未完成且有截止时间的任务
            tasks.filter { task ->
                task.status == TaskStatus.PENDING &&
                task.dueDate != null &&
                task.notificationStrategyId != null
            }.forEach { task ->
                val dueDate = task.dueDate!!

                // 计算距离截止时间还有多少分钟
                val minutesUntilDue = java.time.Duration.between(now, dueDate).toMinutes()

                // 只更新在3分钟倒计时窗口内的任务
                if (minutesUntilDue in 0..3) {
                    val strategy = strategies.find { it.id == task.notificationStrategyId }

                    if (strategy != null) {
                        val secondsUntilDue = java.time.Duration.between(now, dueDate).seconds

                        // 更新通知
                        notificationHelper.showTaskNotificationWithCountdown(
                            task = task,
                            strategy = strategy,
                            secondsUntilDue = secondsUntilDue
                        )
                        updateCount++
                        Timber.d("Updated countdown for task: ${task.title}, ${secondsUntilDue}s remaining")
                    }
                }
            }

            Timber.i("CountdownNotificationWorker: Updated $updateCount countdown notification(s)")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "CountdownNotificationWorker: Failed to update countdown notifications")
            Result.retry()
        }
    }
}

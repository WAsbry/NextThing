package com.nextthing.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextthing.app.domain.model.TaskStatus
import com.nextthing.app.domain.repository.NotificationStrategyRepository
import com.nextthing.app.domain.repository.TaskRepository
import com.nextthing.app.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDateTime

/**
 * 周期性通知刷新 Worker（每 15 分钟执行一次，Android WorkManager 最小周期）
 *
 * 职责：
 * 1. 作为 AlarmManager 的兜底机制，确保即将到期的任务不会错过通知
 * 2. 刷新已有通知的倒计时显示（精度为 15 分钟级别，非实时倒计时）
 *
 * 注意：WorkManager 最小周期为 15 分钟，无法实现秒级倒计时更新。
 * 如需秒级倒计时，应使用 ForegroundService。
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
            Timber.d("CountdownNotificationWorker: 开始刷新通知")

            val now = LocalDateTime.now()
            val tasks = taskRepository.getAllTasks().first()
            val strategies = notificationStrategyRepository.getAllStrategies().first()
            var updateCount = 0

            // 遍历所有未完成且有截止时间的任务（包含 DELAYED/OVERDUE，覆盖延期后重新调度的场景）
            tasks.filter { task ->
                (task.status == TaskStatus.PENDING ||
                 task.status == TaskStatus.DELAYED ||
                 task.status == TaskStatus.OVERDUE) &&
                task.dueDate != null &&
                task.notificationStrategyId != null
            }.forEach { task ->
                val dueDate = task.dueDate!!
                val minutesUntilDue = java.time.Duration.between(now, dueDate).toMinutes()

                // 扩大窗口与 TaskNotificationWorker 保持一致：截止前 20 分钟到截止后 5 分钟
                if (minutesUntilDue in -5..20) {
                    val strategy = strategies.find { it.id == task.notificationStrategyId }

                    if (strategy != null) {
                        val secondsUntilDue = java.time.Duration.between(now, dueDate).seconds

                        notificationHelper.showTaskNotificationWithCountdown(
                            task = task,
                            strategy = strategy,
                            secondsUntilDue = secondsUntilDue
                        )
                        updateCount++
                        Timber.d("刷新通知: ${task.title}, 剩余 ${secondsUntilDue}秒")
                    }
                }
            }

            Timber.i("CountdownNotificationWorker: 已刷新 $updateCount 条通知")
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "CountdownNotificationWorker: 刷新通知失败")
            WorkerFailurePolicy.result(TAG, runAttemptCount)
        }
    }

    private companion object {
        const val TAG = "CountdownNotificationWorker"
    }
}

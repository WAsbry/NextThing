package com.example.nextthingb1.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDateTime

/**
 * WorkManager worker that checks for overdue tasks and updates their status automatically.
 *
 * 修复：
 * 1. 删除从未使用的 yesterdayEnd 变量
 * 2. 新增对 DELAYED 任务的兜底处理：若 Worker 某天未执行导致 DELAYED 任务
 *    未被 ConvertDelayedTasksWorker 转换，且 dueDate 已过期，直接标记为 OVERDUE
 */
@HiltWorker
class CheckOverdueTasksWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("CheckOverdueTasksWorker: Starting overdue task check")

            val tasks = taskRepository.getAllTasks().first()
            val now = LocalDateTime.now()
            var updatedCount = 0

            tasks.forEach { task ->
                if (task.dueDate == null) return@forEach

                val isExpired = now.isAfter(task.dueDate.plusMinutes(5))

                when {
                    // PENDING 任务过期 → OVERDUE
                    task.status == TaskStatus.PENDING && isExpired -> {
                        taskRepository.updateTask(
                            task.copy(status = TaskStatus.OVERDUE, updatedAt = LocalDateTime.now())
                        )
                        updatedCount++
                        Timber.d("Updated PENDING task '${task.title}' to OVERDUE")
                    }
                    // 兜底：DELAYED 任务若 dueDate 已过期（ConvertDelayedTasksWorker 未处理）→ OVERDUE
                    task.status == TaskStatus.DELAYED && isExpired -> {
                        taskRepository.updateTask(
                            task.copy(status = TaskStatus.OVERDUE, updatedAt = LocalDateTime.now())
                        )
                        updatedCount++
                        Timber.d("Updated stale DELAYED task '${task.title}' to OVERDUE")
                    }
                }
            }

            Timber.i("CheckOverdueTasksWorker: Completed. Updated $updatedCount task(s) to OVERDUE")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "CheckOverdueTasksWorker: Failed to check overdue tasks")
            Result.retry()
        }
    }
}

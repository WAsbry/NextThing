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
 * This worker runs periodically (typically daily) to scan all tasks and update any
 * PENDING tasks that have passed their due date to OVERDUE status.
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

            // Get all tasks from repository
            val tasks = taskRepository.getAllTasks().first()
            val now = LocalDateTime.now()
            val today = now.toLocalDate()
            val yesterdayEnd = today.minusDays(1).atTime(23, 59, 59)
            var updatedCount = 0

            // Find and update overdue tasks
            tasks.forEach { task ->
                // 逾期的严格定义（根据用户需求）：
                // 1. 任务状态必须是 PENDING（未完成）
                // 2. 任务存在非空的截止时间（dueDate != null）
                // 3. dueDate ≤ 昨天 23:59:59
                //
                // 注意：没有设置 dueDate 的任务永远不会逾期！
                if (task.status == TaskStatus.PENDING &&
                    task.dueDate != null &&
                    task.dueDate.toLocalDate() <= today.minusDays(1)) {

                    val updatedTask = task.copy(
                        status = TaskStatus.OVERDUE,
                        updatedAt = LocalDateTime.now()
                    )
                    taskRepository.updateTask(updatedTask)
                    updatedCount++

                    Timber.d("Updated task '${task.title}' to OVERDUE (dueDate: ${task.dueDate}, today: $today)")
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

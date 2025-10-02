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
 * WorkManager worker that converts DELAYED tasks to PENDING automatically at the start of the next day.
 *
 * 延期转待办 Worker：
 * - 在每天凌晨（00:00:01）运行
 * - 将所有 DELAYED 状态的任务自动转为 PENDING
 * - 确保延期任务在次日成为待办任务
 */
@HiltWorker
class ConvertDelayedTasksWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("ConvertDelayedTasksWorker: Starting delayed task conversion")

            // Get all tasks from repository
            val tasks = taskRepository.getAllTasks().first()
            val now = LocalDateTime.now()
            val today = now.toLocalDate()
            var convertedCount = 0

            // Find and convert delayed tasks
            tasks.forEach { task ->
                // 延期转待办规则（以 dueDate 为核心）：
                // 1. 状态为 DELAYED
                // 2. dueDate = 今天（说明延期到今天，应该转为今天的待办任务）
                if (task.status == TaskStatus.DELAYED &&
                    task.dueDate != null &&
                    task.dueDate.toLocalDate() == today) {

                    val updatedTask = task.copy(
                        status = TaskStatus.PENDING,
                        updatedAt = LocalDateTime.now()
                    )
                    taskRepository.updateTask(updatedTask)
                    convertedCount++

                    Timber.d("Converted delayed task '${task.title}' to PENDING (dueDate is today: ${task.dueDate})")
                }
            }

            Timber.i("ConvertDelayedTasksWorker: Completed. Converted $convertedCount task(s) from DELAYED to PENDING")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "ConvertDelayedTasksWorker: Failed to convert delayed tasks")
            Result.retry()
        }
    }
}

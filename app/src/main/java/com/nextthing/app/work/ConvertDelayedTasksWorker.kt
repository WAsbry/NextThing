package com.nextthing.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextthing.app.domain.model.TaskStatus
import com.nextthing.app.domain.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDateTime

/**
 * WorkManager worker that converts DELAYED tasks to PENDING automatically at the start of the next day.
 *
 * 延期转待办 Worker：
 * - 在每天凌晨（00:00:01）运行
 * - 将所有 DELAYED 状态且 dueDate <= 今天的任务转为 PENDING
 * - 修复：原逻辑 dueDate == today 在 Worker 某天未执行时会导致任务永久卡死，
 *   改为 dueDate <= today（即过期的 DELAYED 也要处理）
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

            val tasks = taskRepository.getAllTasks().first()
            val now = LocalDateTime.now()
            val today = now.toLocalDate()
            var convertedCount = 0

            tasks.forEach { task ->
                // 修复：改用 <= today，而非 == today
                // 若 Worker 某天未执行，dueDate 可能落后于 today，需一并处理
                if (task.status == TaskStatus.DELAYED &&
                    task.dueDate != null &&
                    !task.dueDate.toLocalDate().isAfter(today)) {

                    val updatedTask = task.copy(
                        status = TaskStatus.PENDING,
                        updatedAt = LocalDateTime.now()
                    )
                    taskRepository.updateTask(updatedTask)
                    convertedCount++

                    Timber.d("Converted delayed task '${task.title}' to PENDING (dueDate: ${task.dueDate})")
                }
            }

            Timber.i("ConvertDelayedTasksWorker: Completed. Converted $convertedCount task(s) from DELAYED to PENDING")
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "ConvertDelayedTasksWorker: Failed to convert delayed tasks")
            WorkerFailurePolicy.result(TAG, runAttemptCount)
        }
    }

    private companion object {
        const val TAG = "ConvertDelayedTasksWorker"
    }
}

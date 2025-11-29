package com.example.nextthingb1.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.nextthingb1.domain.usecase.TaskUseCases
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.time.LocalDate

/**
 * Worker to generate recurring task instances daily at midnight.
 *
 * This worker runs every day at 00:00 to generate task instances
 * for all template tasks that need to appear on the current day.
 */
@HiltWorker
class GenerateRecurringTasksWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskUseCases: TaskUseCases
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "RecurringTask"
    }

    override suspend fun doWork(): Result {
        return try {
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Timber.tag(TAG).d("【Worker】GenerateRecurringTasksWorker 开始执行")
            Timber.tag(TAG).d("执行时间: ${LocalDate.now()}")

            // 生成今日的重复任务实例
            taskUseCases.generateRecurringTasks(LocalDate.now()).fold(
                onSuccess = { count ->
                    Timber.tag(TAG).d("✅ Worker执行成功，生成了 $count 个任务实例")
                    Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Result.success()
                },
                onFailure = { error ->
                    Timber.tag(TAG).e("❌ Worker执行失败: ${error.message}")
                    Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Result.retry()
                }
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "💥 Worker执行异常")
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Result.retry()
        }
    }
}

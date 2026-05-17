package com.nextthing.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextthing.app.domain.usecase.SyncTasksUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * 数据同步Worker
 * 定期执行后台数据同步
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncTasksUseCase: SyncTasksUseCase
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "sync_worker"
        const val TAG = "数据同步Worker"
    }

    override suspend fun doWork(): Result {
        Timber.tag(TAG).d("🔄 开始后台数据同步...")

        return try {
            val result = syncTasksUseCase()

            result.fold(
                onSuccess = { syncResult ->
                    Timber.tag(TAG).d("✅ 同步成功: 上传${syncResult.uploadedTasks}条, 下载${syncResult.downloadedTasks}条")
                    Result.success()
                },
                onFailure = { error ->
                    Timber.tag(TAG).e(error, "❌ 同步失败")
                    // 如果失败，返回retry()让WorkManager自动重试
                    Result.retry()
                }
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 同步异常")
            Result.retry()
        }
    }
}

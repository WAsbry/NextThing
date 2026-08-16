package com.nextthing.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.domain.usecase.SyncTasksUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * 数据同步Worker
 * 定期执行后台数据同步
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncTasksUseCase: SyncTasksUseCase,
    private val tokenManager: TokenManager
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "sync_worker"
        const val TAG = "数据同步Worker"
        private const val MAX_RUN_ATTEMPTS = 3
    }

    override suspend fun doWork(): Result {
        Timber.tag(TAG).d("🔄 开始后台数据同步...")

        if (tokenManager.getAccessTokenOnce().isNullOrBlank()) {
            Timber.tag(TAG).d("未登录云端账号，跳过本轮后台同步")
            return Result.success()
        }

        return try {
            val result = syncTasksUseCase()

            result.fold(
                onSuccess = { syncResult ->
                    Timber.tag(TAG).d("✅ 同步成功: 上传${syncResult.uploadedTasks}条, 下载${syncResult.downloadedTasks}条")
                    Result.success()
                },
                onFailure = { error ->
                    Timber.tag(TAG).e(error, "❌ 同步失败")
                    handleFailure()
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ 同步异常")
            handleFailure()
        }
    }

    private suspend fun handleFailure(): Result {
        if (tokenManager.getAccessTokenOnce().isNullOrBlank()) {
            Timber.tag(TAG).w("登录态已失效，停止后台重试")
            return Result.success()
        }

        val currentAttempt = runAttemptCount + 1
        return if (currentAttempt < MAX_RUN_ATTEMPTS) {
            Timber.tag(TAG).w("同步将在退避后重试: attempt=%d/%d", currentAttempt, MAX_RUN_ATTEMPTS)
            Result.retry()
        } else {
            Timber.tag(TAG).e("同步连续失败，结束本轮执行: attempt=%d/%d", currentAttempt, MAX_RUN_ATTEMPTS)
            Result.failure()
        }
    }
}

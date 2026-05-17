package com.nextthing.app.sync

import android.app.Application
import androidx.work.*
import com.nextthing.app.work.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * 同步管理器
 * 负责初始化和管理工作同步
 */
class SyncManager @Inject constructor(
    private val application: Application
) {

    companion object {
        private const val TAG = "同步管理器"
        private const val SYNC_INTERVAL_MINUTES = 15L
    }

    private val workManager = WorkManager.getInstance(application)

    /**
     * 启动定期同步
     */
    fun startPeriodicSync() {
        Timber.tag(TAG).d("启动定期同步，间隔: ${SYNC_INTERVAL_MINUTES}分钟")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(SyncWorker.WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )

        Timber.tag(TAG).d("✅ 定期同步任务已启动")
    }

    /**
     * 停止定期同步
     */
    fun stopPeriodicSync() {
        Timber.tag(TAG).d("停止定期同步")
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
    }

    /**
     * 立即执行一次同步
     */
    fun syncNow() {
        Timber.tag(TAG).d("立即执行同步")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag(SyncWorker.WORK_NAME)
            .build()

        workManager.enqueue(syncWorkRequest)
    }

    /**
     * 检查同步是否正在运行
     */
    fun isSyncRunning(): Boolean {
        val workInfos = workManager.getWorkInfosByTag(SyncWorker.WORK_NAME).get()
        return workInfos.any { it.state == WorkInfo.State.RUNNING }
    }
}

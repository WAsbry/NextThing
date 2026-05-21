package com.nextthing.app.sync

import android.content.Context
import androidx.work.*
import com.nextthing.app.work.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 同步调度器
 * 管理数据同步的定时任务
 */
@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val SYNC_INTERVAL_MINUTES = 15L // 每15分钟同步一次
        private const val SYNC_FLEX_MINUTES = 5L // 灵活时间窗口
    }

    /**
     * 启动定期同步
     */
    fun startPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // 需要网络连接
            .setRequiresBatteryNotLow(true) // 电量不低
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES,
            SYNC_FLEX_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(SyncWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // 如果已存在则保持
            syncWorkRequest
        )
    }

    /**
     * 停止定期同步
     */
    fun stopPeriodicSync() {
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.WORK_NAME)
    }

    /**
     * 执行一次性同步
     */
    fun syncNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .addTag(SyncWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueue(syncWorkRequest)
    }

    /**
     * 检查同步是否正在运行
     */
    fun isSyncRunning(): Boolean {
        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosByTag(SyncWorker.WORK_NAME).get()
        return workInfos.any { it.state == WorkInfo.State.RUNNING }
    }
}

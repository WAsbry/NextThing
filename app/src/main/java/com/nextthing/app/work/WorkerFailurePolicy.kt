package com.nextthing.app.work

import androidx.work.ListenableWorker
import timber.log.Timber

internal enum class WorkerFailureAction {
    RETRY,
    FAIL_CURRENT_RUN
}

/**
 * Keeps background failures bounded. Periodic work will be scheduled again in its next period
 * after a failed run, while one-time work exposes a terminal failure instead of retrying forever.
 */
internal object WorkerFailurePolicy {
    private const val MAX_RUN_ATTEMPTS = 3

    fun decide(runAttemptCount: Int): WorkerFailureAction {
        return if (runAttemptCount + 1 < MAX_RUN_ATTEMPTS) {
            WorkerFailureAction.RETRY
        } else {
            WorkerFailureAction.FAIL_CURRENT_RUN
        }
    }

    fun result(workerName: String, runAttemptCount: Int): ListenableWorker.Result {
        val attempt = runAttemptCount + 1
        return when (decide(runAttemptCount)) {
            WorkerFailureAction.RETRY -> {
                Timber.tag(workerName).w(
                    "后台任务将在退避后重试: attempt=%d/%d",
                    attempt,
                    MAX_RUN_ATTEMPTS
                )
                ListenableWorker.Result.retry()
            }

            WorkerFailureAction.FAIL_CURRENT_RUN -> {
                Timber.tag(workerName).e(
                    "后台任务连续失败，结束本轮执行: attempt=%d/%d",
                    attempt,
                    MAX_RUN_ATTEMPTS
                )
                ListenableWorker.Result.failure()
            }
        }
    }
}

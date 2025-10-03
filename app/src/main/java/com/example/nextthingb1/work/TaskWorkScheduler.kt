package com.example.nextthingb1.work

import android.content.Context
import androidx.work.*
import timber.log.Timber
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Scheduler for all task-related background work.
 *
 * This class is responsible for scheduling periodic WorkManager jobs
 * such as checking for overdue tasks.
 */
object TaskWorkScheduler {

    private const val OVERDUE_CHECK_WORK_NAME = "check_overdue_tasks"
    private const val DELAYED_CONVERT_WORK_NAME = "convert_delayed_tasks"
    private const val TASK_NOTIFICATION_WORK_NAME = "task_notifications"

    /**
     * Schedule daily overdue task check.
     *
     * This schedules a periodic worker that runs once per day at 1:00 AM
     * to check for tasks that have become overdue.
     *
     * @param context Application context
     */
    fun scheduleOverdueCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        // Calculate initial delay to run at 1:00 AM
        val initialDelay = calculateInitialDelay(targetHour = 1, targetMinute = 0)

        val overdueCheckRequest = PeriodicWorkRequestBuilder<CheckOverdueTasksWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            OVERDUE_CHECK_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            overdueCheckRequest
        )

        Timber.i("Scheduled overdue task check to run daily at 1:00 AM (initial delay: ${initialDelay}ms)")
    }

    /**
     * Schedule daily delayed task conversion.
     *
     * This schedules a periodic worker that runs once per day at 00:00:01
     * to convert DELAYED tasks to PENDING automatically.
     *
     * @param context Application context
     */
    fun scheduleDelayedConversion(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        // Calculate initial delay to run at 00:00:01 AM
        val initialDelay = calculateInitialDelay(targetHour = 0, targetMinute = 0)

        val delayedConvertRequest = PeriodicWorkRequestBuilder<ConvertDelayedTasksWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DELAYED_CONVERT_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            delayedConvertRequest
        )

        Timber.i("Scheduled delayed task conversion to run daily at 00:00:01 (initial delay: ${initialDelay}ms)")
    }

    /**
     * Trigger an immediate overdue check (useful for app startup).
     *
     * @param context Application context
     */
    fun triggerImmediateOverdueCheck(context: Context) {
        val overdueCheckRequest = OneTimeWorkRequestBuilder<CheckOverdueTasksWorker>()
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "${OVERDUE_CHECK_WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            overdueCheckRequest
        )

        Timber.d("Triggered immediate overdue task check")
    }

    /**
     * Trigger an immediate delayed task conversion (useful for app startup).
     *
     * @param context Application context
     */
    fun triggerImmediateDelayedConversion(context: Context) {
        val delayedConvertRequest = OneTimeWorkRequestBuilder<ConvertDelayedTasksWorker>()
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "${DELAYED_CONVERT_WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            delayedConvertRequest
        )

        Timber.d("Triggered immediate delayed task conversion")
    }

    /**
     * Schedule periodic task notification check.
     *
     * This schedules a periodic worker that runs every 15 minutes
     * to check for tasks that need notifications.
     *
     * @param context Application context
     */
    fun scheduleTaskNotifications(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val notificationRequest = PeriodicWorkRequestBuilder<TaskNotificationWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TASK_NOTIFICATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            notificationRequest
        )

        Timber.i("Scheduled task notification check to run every 15 minutes")
    }

    /**
     * Cancel all scheduled task work.
     *
     * @param context Application context
     */
    fun cancelAllWork(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(OVERDUE_CHECK_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(DELAYED_CONVERT_WORK_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(TASK_NOTIFICATION_WORK_NAME)
        Timber.i("Cancelled all task work")
    }

    /**
     * Calculate the delay until the next occurrence of the target time.
     *
     * @param targetHour Hour of day (0-23)
     * @param targetMinute Minute of hour (0-59)
     * @return Delay in milliseconds
     */
    private fun calculateInitialDelay(targetHour: Int, targetMinute: Int): Long {
        val now = LocalDateTime.now()
        var targetTime = now.toLocalDate().atTime(LocalTime.of(targetHour, targetMinute))

        // If target time has already passed today, schedule for tomorrow
        if (targetTime.isBefore(now)) {
            targetTime = targetTime.plusDays(1)
        }

        return Duration.between(now, targetTime).toMillis()
    }
}

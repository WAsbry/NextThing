package com.nextthing.app.work

import android.content.Context
import androidx.work.*
import timber.log.Timber
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
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
    private const val COUNTDOWN_UPDATE_WORK_NAME = "countdown_notifications"
    private const val RECURRING_TASKS_WORK_NAME = "generate_recurring_tasks"
    private const val MORNING_BRIEFING_WORK_NAME = "morning_briefing"
    private const val EVENING_BRIEFING_WORK_NAME = "evening_briefing"

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
            ExistingPeriodicWorkPolicy.UPDATE,
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
            ExistingPeriodicWorkPolicy.UPDATE,
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
            ExistingPeriodicWorkPolicy.UPDATE,
            notificationRequest
        )

        Timber.i("Scheduled task notification check to run every 15 minutes")
    }

    /**
     * Schedule periodic notification refresh.
     *
     * This schedules a periodic worker that runs every 15 minutes
     * (Android WorkManager minimum) to refresh existing notifications
     * and serve as a backup for AlarmManager.
     *
     * @param context Application context
     */
    fun scheduleCountdownUpdates(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val countdownRequest = PeriodicWorkRequestBuilder<CountdownNotificationWorker>(
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
            COUNTDOWN_UPDATE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            countdownRequest
        )

        Timber.i("Scheduled notification refresh to run every 15 minutes")
    }

    /**
     * Schedule daily recurring task generation.
     *
     * This schedules a periodic worker that runs once per day at 00:00:00
     * to generate recurring task instances for the current day.
     *
     * @param context Application context
     */
    fun scheduleRecurringTaskGeneration(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        // Calculate initial delay to run at 00:00:00 AM
        val initialDelay = calculateInitialDelay(targetHour = 0, targetMinute = 0)

        val recurringTasksRequest = PeriodicWorkRequestBuilder<GenerateRecurringTasksWorker>(
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
            RECURRING_TASKS_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            recurringTasksRequest
        )

        Timber.i("Scheduled recurring task generation to run daily at 00:00:00 (initial delay: ${initialDelay}ms)")
    }

    /**
     * Trigger an immediate recurring task generation (useful for app startup).
     *
     * @param context Application context
     */
    fun triggerImmediateRecurringTaskGeneration(context: Context) {
        val recurringTasksRequest = OneTimeWorkRequestBuilder<GenerateRecurringTasksWorker>()
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "${RECURRING_TASKS_WORK_NAME}_immediate",
            ExistingWorkPolicy.REPLACE,
            recurringTasksRequest
        )

        Timber.d("Triggered immediate recurring task generation")
    }

    /**
     * Cancel all scheduled task work.
     *
     * @param context Application context
     */
    fun cancelAllWork(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(OVERDUE_CHECK_WORK_NAME)
        wm.cancelUniqueWork("${OVERDUE_CHECK_WORK_NAME}_immediate")
        wm.cancelUniqueWork(DELAYED_CONVERT_WORK_NAME)
        wm.cancelUniqueWork("${DELAYED_CONVERT_WORK_NAME}_immediate")
        wm.cancelUniqueWork(TASK_NOTIFICATION_WORK_NAME)
        wm.cancelUniqueWork(COUNTDOWN_UPDATE_WORK_NAME)
        wm.cancelUniqueWork(RECURRING_TASKS_WORK_NAME)
        wm.cancelUniqueWork("${RECURRING_TASKS_WORK_NAME}_immediate")
        wm.cancelUniqueWork(MORNING_BRIEFING_WORK_NAME)
        wm.cancelUniqueWork(EVENING_BRIEFING_WORK_NAME)
        Timber.i("Cancelled all task work")
    }

    fun scheduleMorningBriefing(context: Context, hour: Int, minute: Int = 0) {
        val initialDelay = calculateInitialDelay(targetHour = hour, targetMinute = minute)

        val request = PeriodicWorkRequestBuilder<DailyBriefingWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInputData(workDataOf(DailyBriefingWorker.KEY_BRIEFING_TYPE to com.nextthing.app.domain.service.AIBriefingGenerator.BriefingType.MORNING.name))
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MORNING_BRIEFING_WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, request
        )
        Timber.i("Scheduled morning briefing at %02d:%02d (delay: ${initialDelay}ms)", hour, minute)
    }

    fun scheduleEveningBriefing(context: Context, hour: Int, minute: Int = 0) {
        val initialDelay = calculateInitialDelay(targetHour = hour, targetMinute = minute)

        val request = PeriodicWorkRequestBuilder<DailyBriefingWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInputData(workDataOf(DailyBriefingWorker.KEY_BRIEFING_TYPE to com.nextthing.app.domain.service.AIBriefingGenerator.BriefingType.EVENING.name))
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            EVENING_BRIEFING_WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, request
        )
        Timber.i("Scheduled evening briefing at %02d:%02d (delay: ${initialDelay}ms)", hour, minute)
    }

    fun cancelBriefingWork(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(MORNING_BRIEFING_WORK_NAME)
        wm.cancelUniqueWork(EVENING_BRIEFING_WORK_NAME)
        Timber.i("Cancelled briefing work")
    }

    fun triggerImmediateBriefing(
        context: Context,
        type: com.nextthing.app.domain.service.AIBriefingGenerator.BriefingType
    ): UUID {
        val request = OneTimeWorkRequestBuilder<DailyBriefingWorker>()
            .setInputData(workDataOf(DailyBriefingWorker.KEY_BRIEFING_TYPE to type.name))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "immediate_briefing_${type.name.lowercase()}",
            ExistingWorkPolicy.REPLACE,
            request
        )
        Timber.d("Triggered immediate ${type.name.lowercase()} briefing")
        return request.id
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

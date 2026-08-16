package com.nextthing.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Legacy broadcast fallback kept for alarms created by older app versions.
 * New alarms target [TaskAlarmService] directly.
 */
class TaskAlarmReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun taskAlarmProcessor(): TaskAlarmProcessor
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(TaskAlarmService.EXTRA_TASK_ID) ?: return
        val strategyId = intent.getStringExtra(TaskAlarmService.EXTRA_STRATEGY_ID) ?: return
        val isAdvance = intent.getBooleanExtra(TaskAlarmService.EXTRA_IS_ADVANCE, false)
        val advanceMinutes = intent.getIntExtra(TaskAlarmService.EXTRA_ADVANCE_MINUTES, 0)
        val processor = try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                ReceiverEntryPoint::class.java
            ).taskAlarmProcessor()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "获取提醒处理器失败")
            return
        }

        val pendingResult = goAsync()
        scope.launch {
            try {
                processor.process(taskId, strategyId, isAdvance, advanceMinutes)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "处理兼容闹钟广播失败")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "NotificationTask"
    }
}

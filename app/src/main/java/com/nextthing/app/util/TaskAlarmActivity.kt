package com.nextthing.app.util

import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Transparent AlarmClock delivery entry for standard and important reminders.
 *
 * Vivo Android 16 can keep broadcast and service deliveries attached to a
 * frozen process. AlarmClock activity delivery is allowed to thaw the app; this
 * activity performs no rendering and exits as soon as reminder processing ends.
 */
@AndroidEntryPoint
class TaskAlarmActivity : ComponentActivity() {
    @Inject lateinit var processor: TaskAlarmProcessor

    private val alarmScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)

        val taskId = intent.getStringExtra(TaskAlarmService.EXTRA_TASK_ID)
        val strategyId = intent.getStringExtra(TaskAlarmService.EXTRA_STRATEGY_ID)
        if (taskId == null || strategyId == null) {
            finishWithoutAnimation()
            return
        }

        val isAdvance = intent.getBooleanExtra(TaskAlarmService.EXTRA_IS_ADVANCE, false)
        val advanceMinutes = intent.getIntExtra(TaskAlarmService.EXTRA_ADVANCE_MINUTES, 0)
        alarmScope.launch {
            try {
                processor.process(taskId, strategyId, isAdvance, advanceMinutes)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "处理 AlarmClock 提醒失败")
            } finally {
                runOnUiThread { finishWithoutAnimation() }
            }
        }
    }

    override fun onDestroy() {
        alarmScope.cancel()
        super.onDestroy()
    }

    private fun finishWithoutAnimation() {
        finish()
        overridePendingTransition(0, 0)
    }

    private companion object {
        const val TAG = "NotificationTask"
    }
}

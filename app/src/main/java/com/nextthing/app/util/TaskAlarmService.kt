package com.nextthing.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nextthing.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Direct AlarmManager target used on modern Android devices.
 *
 * Some vendor Android 16 builds can leave manifest broadcasts queued while the
 * app is backgrounded. A short foreground service avoids that broadcast queue
 * while still completing only the user-requested reminder work.
 */
@AndroidEntryPoint
class TaskAlarmService : Service() {
    @Inject lateinit var processor: TaskAlarmProcessor

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createDeliveryChannel()
        val notification = NotificationCompat.Builder(this, DELIVERY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("NextThing")
            .setContentText("正在处理任务提醒")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                DELIVERY_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(DELIVERY_NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getStringExtra(EXTRA_TASK_ID)
        val strategyId = intent?.getStringExtra(EXTRA_STRATEGY_ID)
        if (taskId == null || strategyId == null) {
            finishStart(startId)
            return START_NOT_STICKY
        }

        val isAdvanceReminder = intent.getBooleanExtra(EXTRA_IS_ADVANCE, false)
        val advanceMinutes = intent.getIntExtra(EXTRA_ADVANCE_MINUTES, 0)
        serviceScope.launch {
            try {
                processor.process(taskId, strategyId, isAdvanceReminder, advanceMinutes)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "处理定时提醒失败")
            } finally {
                finishStart(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun finishStart(startId: Int) {
        if (stopSelfResult(startId)) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun createDeliveryChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(DELIVERY_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            DELIVERY_CHANNEL_ID,
            "提醒处理服务",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_STRATEGY_ID = "notificationStrategyId"
        const val EXTRA_IS_ADVANCE = "isAdvanceReminder"
        const val EXTRA_ADVANCE_MINUTES = "advanceMinutes"

        private const val DELIVERY_CHANNEL_ID = "task_alarm_delivery"
        private const val DELIVERY_NOTIFICATION_ID = 0x4E54
        private const val TAG = "NotificationTask"
    }
}

package com.example.nextthingb1.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.nextthingb1.domain.model.Task
import com.example.nextthingb1.domain.repository.NotificationStrategyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 任务闹钟管理器
 * 负责为任务设置精确时间的闹钟提醒，支持提前提醒
 */
@Singleton
class TaskAlarmManager @Inject constructor(
    private val context: Context,
    private val notificationStrategyRepository: NotificationStrategyRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // 记录每个任务已调度的提前提醒分钟列表，用于取消时清理
    private val prefs = context.getSharedPreferences("task_alarm_advance", Context.MODE_PRIVATE)

    companion object {
        private const val REQUEST_CODE_PREFIX = 10000
        private const val ADVANCE_REQUEST_CODE_PREFIX = 20000
        private const val TAG = "NotificationTask"
    }

    /**
     * 为任务调度闹钟（到期时间闹钟 + 提前提醒闹钟）
     * 自动从数据库获取通知策略中的提前提醒配置
     */
    fun scheduleTaskAlarm(task: Task) {
        val dueDate = task.dueDate ?: return
        val notificationStrategyId = task.notificationStrategyId ?: return
        val now = LocalDateTime.now()

        if (dueDate.isBefore(now)) {
            Timber.tag(TAG).d("任务 ${task.title} 截止时间已过，跳过调度")
            return
        }

        // 调度到期时间闹钟
        scheduleExactAlarm(
            taskId = task.id,
            notificationStrategyId = notificationStrategyId,
            triggerTime = dueDate,
            isAdvanceReminder = false,
            advanceMinutes = 0
        )
        Timber.tag(TAG).d("任务 ${task.title} 到期闹钟已调度: $dueDate")

        // 异步获取策略并调度提前提醒
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val strategy = notificationStrategyRepository.getStrategyById(notificationStrategyId)
                if (strategy != null && strategy.advanceReminderMinutes.isNotEmpty()) {
                    val scheduledAdvanceMinutes = mutableListOf<Int>()
                    for (minutes in strategy.advanceReminderMinutes) {
                        val advanceTriggerTime = dueDate.minusMinutes(minutes.toLong())
                        if (advanceTriggerTime.isAfter(now)) {
                            scheduleExactAlarm(
                                taskId = task.id,
                                notificationStrategyId = notificationStrategyId,
                                triggerTime = advanceTriggerTime,
                                isAdvanceReminder = true,
                                advanceMinutes = minutes
                            )
                            scheduledAdvanceMinutes.add(minutes)
                            Timber.tag(TAG).d("  提前${minutes}分钟提醒已调度: $advanceTriggerTime")
                        }
                    }
                    // 保存已调度的提前提醒列表
                    prefs.edit()
                        .putStringSet("advance_${task.id}", scheduledAdvanceMinutes.map { it.toString() }.toSet())
                        .apply()
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e("获取通知策略失败: ${e.message}")
            }
        }
    }

    /**
     * 取消任务的所有闹钟（到期闹钟 + 全部提前提醒闹钟）
     */
    fun cancelTaskAlarm(taskId: String) {
        // 取消到期时间闹钟
        cancelSingleAlarm(getRequestCode(taskId))

        // 取消所有提前提醒闹钟
        val advanceMinutesSet = prefs.getStringSet("advance_$taskId", emptySet()) ?: emptySet()
        for (minutesStr in advanceMinutesSet) {
            val minutes = minutesStr.toIntOrNull() ?: continue
            cancelSingleAlarm(getAdvanceRequestCode(taskId, minutes))
        }

        // 清理记录
        prefs.edit().remove("advance_$taskId").apply()
    }

    /**
     * 调度一个精确闹钟
     */
    private fun scheduleExactAlarm(
        taskId: String,
        notificationStrategyId: String,
        triggerTime: LocalDateTime,
        isAdvanceReminder: Boolean,
        advanceMinutes: Int
    ) {
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("taskId", taskId)
            putExtra("notificationStrategyId", notificationStrategyId)
            putExtra("isAdvanceReminder", isAdvanceReminder)
            putExtra("advanceMinutes", advanceMinutes)
        }

        val requestCode = if (isAdvanceReminder) {
            getAdvanceRequestCode(taskId, advanceMinutes)
        } else {
            getRequestCode(taskId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                return
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (e2: Exception) {
                Timber.tag(TAG).e("设置闹钟失败: ${e2.message}")
            }
        }
    }

    private fun cancelSingleAlarm(requestCode: Int) {
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun getRequestCode(taskId: String): Int {
        return REQUEST_CODE_PREFIX + taskId.hashCode().and(0x7FFFFFFF) % 100000
    }

    private fun getAdvanceRequestCode(taskId: String, advanceMinutes: Int): Int {
        return ADVANCE_REQUEST_CODE_PREFIX + (taskId + "_adv_$advanceMinutes").hashCode().and(0x7FFFFFFF) % 100000
    }
}

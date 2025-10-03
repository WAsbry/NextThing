package com.example.nextthingb1.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.nextthingb1.domain.model.Task
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 任务闹钟管理器
 * 负责为任务设置精确时间的闹钟提醒
 */
@Singleton
class TaskAlarmManager @Inject constructor(
    private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val REQUEST_CODE_PREFIX = 10000
        private const val TAG = "NotificationTask"
    }

    /**
     * 为任务调度闹钟
     */
    fun scheduleTaskAlarm(task: Task) {
        Timber.tag(TAG).d("━━━━━━ 调度闹钟 ━━━━━━")
        Timber.tag(TAG).d("任务: ${task.title}")

        val dueDate = task.dueDate
        if (dueDate == null) {
            Timber.tag(TAG).w("截止时间为空，跳过")
            return
        }

        val notificationStrategyId = task.notificationStrategyId
        if (notificationStrategyId == null) {
            Timber.tag(TAG).w("通知策略ID为空，跳过")
            return
        }

        val now = LocalDateTime.now()
        if (dueDate.isBefore(now)) {
            Timber.tag(TAG).w("截止时间已过期，跳过")
            return
        }

        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        Timber.tag(TAG).d("截止时间: ${dueDate.format(formatter)}")
        Timber.tag(TAG).d("当前时间: ${now.format(formatter)}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canSchedule = alarmManager.canScheduleExactAlarms()
            Timber.tag(TAG).d("SCHEDULE_EXACT_ALARM权限: ${if (canSchedule) "✅已授权" else "❌未授权"}")
            if (!canSchedule) {
                Timber.tag(TAG).e("缺少SCHEDULE_EXACT_ALARM权限")
            }
        }

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra("taskId", task.id)
            putExtra("taskTitle", task.title)
            putExtra("notificationStrategyId", notificationStrategyId)
        }

        val requestCode = getRequestCode(task.id)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = dueDate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val triggerTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(triggerAtMillis))

        Timber.tag(TAG).d("触发时间: $triggerTime")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Timber.tag(TAG).d("使用 setAlarmClock API")
                val alarmClockInfo = AlarmManager.AlarmClockInfo(
                    triggerAtMillis,
                    pendingIntent
                )
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                Timber.tag(TAG).d("✅ 闹钟设置成功")
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Timber.tag(TAG).d("使用 setExactAndAllowWhileIdle API")
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Timber.tag(TAG).d("✅ 闹钟设置成功")
            } else {
                Timber.tag(TAG).d("使用 setExact API")
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Timber.tag(TAG).d("✅ 闹钟设置成功")
            }
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━")
        } catch (e: SecurityException) {
            Timber.tag(TAG).e("❌ 设置闹钟失败: ${e.message}")
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Timber.tag(TAG).w("⚠️ 使用降级闹钟")
            } catch (e2: Exception) {
                Timber.tag(TAG).e("❌ 降级闹钟失败: ${e2.message}")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e("❌ 设置闹钟异常: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 取消任务的闹钟
     */
    fun cancelTaskAlarm(taskId: String) {
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val requestCode = getRequestCode(taskId)
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

    /**
     * 根据任务ID生成唯一的请求码
     */
    private fun getRequestCode(taskId: String): Int {
        return REQUEST_CODE_PREFIX + taskId.hashCode().and(0x7FFFFFFF) % 100000
    }
}

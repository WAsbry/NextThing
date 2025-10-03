package com.example.nextthingb1.util

import android.content.Context
import android.content.Intent
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知测试辅助工具
 * 用于手动触发通知测试
 *
 * 【NotificationTest】测试工具 - 手动触发闹钟
 */
@Singleton
class NotificationTestHelper @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "NotificationTask"
    }

    /**
     * 手动触发一个任务的闹钟接收器
     * 用于测试通知是否正常工作，不需要等待实际时间
     *
     * @param taskId 任务ID
     * @param taskTitle 任务标题
     * @param notificationStrategyId 通知策略ID
     */
    fun triggerTestAlarm(
        taskId: String,
        taskTitle: String,
        notificationStrategyId: String
    ) {
        Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag(TAG).d("【测试】手动触发闹钟测试")
        Timber.tag(TAG).d("任务ID: $taskId")
        Timber.tag(TAG).d("任务标题: $taskTitle")
        Timber.tag(TAG).d("通知策略ID: $notificationStrategyId")

        try {
            val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
                putExtra("taskId", taskId)
                putExtra("taskTitle", taskTitle)
                putExtra("notificationStrategyId", notificationStrategyId)
            }

            // 发送广播，模拟闹钟触发
            context.sendBroadcast(intent)

            Timber.tag(TAG).d("✅ 测试广播已发送")
            Timber.tag(TAG).d("如果通知系统正常，应该会看到【步骤3】的日志")
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        } catch (e: Exception) {
            Timber.tag(TAG).e("❌ 发送测试广播失败")
            Timber.tag(TAG).e("异常: ${e.message}")
            e.printStackTrace()
            Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        }
    }

    /**
     * 检查设备的Do Not Disturb（勿扰模式）状态
     * 勿扰模式可能会阻止闹钟触发
     */
    fun checkDoNotDisturbStatus(): String {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
            val filter = notificationManager.currentInterruptionFilter

            when (filter) {
                android.app.NotificationManager.INTERRUPTION_FILTER_NONE -> "勿扰模式：完全静音（可能阻止闹钟）"
                android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "勿扰模式：仅优先通知"
                android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS -> "勿扰模式：仅闹钟"
                android.app.NotificationManager.INTERRUPTION_FILTER_ALL -> "正常模式"
                else -> "未知模式"
            }
        } catch (e: Exception) {
            "无法检测: ${e.message}"
        }
    }

    /**
     * 检查应用是否被电池优化
     * 电池优化可能会阻止后台闹钟触发
     */
    fun checkBatteryOptimization(): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE)
                    as android.os.PowerManager
                val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName)

                Timber.tag(TAG).d("电池优化状态: ${if (isIgnoringBatteryOptimizations) "已忽略✅" else "未忽略❌（可能影响后台闹钟）"}")

                isIgnoringBatteryOptimizations
            } else {
                true // Android 6.0以下没有电池优化
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e("无法检测电池优化状态: ${e.message}")
            false
        }
    }

    /**
     * 完整的系统状态检查
     * 检查所有可能影响通知的系统设置
     */
    fun performSystemCheck() {
        Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag(TAG).d("【系统检查】开始检查通知相关的系统状态")

        // 检查勿扰模式
        val dndStatus = checkDoNotDisturbStatus()
        Timber.tag(TAG).d("勿扰模式: $dndStatus")

        // 检查电池优化
        val batteryOptimized = checkBatteryOptimization()

        // 检查通知渠道
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as android.app.NotificationManager
                val channel = notificationManager.getNotificationChannel("task_notifications")

                if (channel != null) {
                    Timber.tag(TAG).d("通知渠道: 存在✅")
                    Timber.tag(TAG).d("  重要性: ${channel.importance}")
                    Timber.tag(TAG).d("  声音: ${channel.sound}")
                    Timber.tag(TAG).d("  震动: ${channel.shouldVibrate()}")
                } else {
                    Timber.tag(TAG).w("⚠️ 通知渠道不存在！")
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e("检查通知渠道失败: ${e.message}")
        }

        Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}

package com.example.nextthingb1.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nextthingb1.domain.repository.NotificationStrategyRepository
import com.example.nextthingb1.domain.repository.TaskRepository
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 任务闹钟广播接收器
 * 当闹钟时间到达时，系统会触发此广播接收器
 *
 * 【NotificationTest】通知流程 - 第3步：接收闹钟触发
 * 此类接收AlarmManager发送的广播，然后显示通知
 */
class TaskAlarmReceiver : BroadcastReceiver() {

    /**
     * Hilt EntryPoint接口
     * 因为BroadcastReceiver不能使用@AndroidEntryPoint，所以使用EntryPoint模式获取依赖
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TaskAlarmReceiverEntryPoint {
        fun taskRepository(): TaskRepository
        fun notificationStrategyRepository(): NotificationStrategyRepository
        fun notificationHelper(): NotificationHelper
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "NotificationTask"
    }

    /**
     * 接收闹钟广播
     * 【NotificationTest】闹钟时间到达时被系统调用
     */
    override fun onReceive(context: Context, intent: Intent) {
        Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Timber.tag(TAG).d("【闹钟触发】TaskAlarmReceiver.onReceive")
        Timber.tag(TAG).d("当前时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")

        val taskId = intent.getStringExtra("taskId")
        val notificationStrategyId = intent.getStringExtra("notificationStrategyId")

        Timber.tag(TAG).d("taskId: $taskId")
        Timber.tag(TAG).d("notificationStrategyId: $notificationStrategyId")

        if (taskId == null || notificationStrategyId == null) {
            Timber.tag(TAG).e("❌ 任务ID或通知策略ID为空")
            return
        }

        val appContext = context.applicationContext
        val entryPoint = try {
            EntryPointAccessors.fromApplication(
                appContext,
                TaskAlarmReceiverEntryPoint::class.java
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e("获取EntryPoint失败: ${e.message}")
            return
        }

        val taskRepository = entryPoint.taskRepository()
        val notificationStrategyRepository = entryPoint.notificationStrategyRepository()
        val notificationHelper = entryPoint.notificationHelper()

        val pendingResult = goAsync()

        scope.launch {
            try {
                Timber.tag(TAG).d("开始加载任务数据...")
                val tasks = taskRepository.getAllTasks().first()
                val task = tasks.find { it.id == taskId }
                if (task == null) {
                    Timber.tag(TAG).e("❌ 未找到任务: $taskId")
                    pendingResult.finish()
                    return@launch
                }
                Timber.tag(TAG).d("✅ 找到任务: ${task.title}")

                Timber.tag(TAG).d("开始加载通知策略...")
                val strategies = notificationStrategyRepository.getAllStrategies().first()
                val strategy = strategies.find { it.id == notificationStrategyId }
                if (strategy == null) {
                    Timber.tag(TAG).e("❌ 未找到通知策略: $notificationStrategyId")
                    pendingResult.finish()
                    return@launch
                }
                Timber.tag(TAG).d("✅ 找到通知策略: ${strategy.name}")

                Timber.tag(TAG).d("准备显示通知...")
                notificationHelper.showTaskNotification(task, strategy)
                Timber.tag(TAG).d("✅ 通知流程完成")
                Timber.tag(TAG).d("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

            } catch (e: Exception) {
                Timber.tag(TAG).e("❌ 显示通知异常: ${e.message}")
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

package com.example.nextthingb1.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nextthingb1.domain.repository.NotificationStrategyRepository
import com.example.nextthingb1.domain.repository.TaskRepository
import com.example.nextthingb1.domain.model.TaskStatus
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
        val taskId = intent.getStringExtra("taskId")
        val notificationStrategyId = intent.getStringExtra("notificationStrategyId")
        val isAdvanceReminder = intent.getBooleanExtra("isAdvanceReminder", false)
        val advanceMinutes = intent.getIntExtra("advanceMinutes", 0)

        Timber.tag(TAG).d("闹钟触发: taskId=$taskId, 提前提醒=$isAdvanceReminder, 提前${advanceMinutes}分钟")

        if (taskId == null || notificationStrategyId == null) {
            Timber.tag(TAG).e("任务ID或通知策略ID为空")
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
                val tasks = taskRepository.getAllTasks().first()
                val task = tasks.find { it.id == taskId }
                if (task == null) {
                    Timber.tag(TAG).e("未找到任务: $taskId")
                    pendingResult.finish()
                    return@launch
                }

                if (task.status != TaskStatus.PENDING) {
                    Timber.tag(TAG).d("任务状态为 ${task.status}，跳过通知")
                    pendingResult.finish()
                    return@launch
                }

                val strategies = notificationStrategyRepository.getAllStrategies().first()
                val strategy = strategies.find { it.id == notificationStrategyId }
                if (strategy == null) {
                    Timber.tag(TAG).e("未找到通知策略: $notificationStrategyId")
                    pendingResult.finish()
                    return@launch
                }

                if (isAdvanceReminder) {
                    notificationHelper.showAdvanceReminderNotification(task, strategy, advanceMinutes)
                    Timber.tag(TAG).d("提前${advanceMinutes}分钟提醒已发送: ${task.title}")
                } else {
                    notificationHelper.showTaskNotification(task, strategy)
                    Timber.tag(TAG).d("到期通知已发送: ${task.title}")
                }

            } catch (e: Exception) {
                Timber.tag(TAG).e("显示通知异常: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}

package com.example.nextthingb1.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nextthingb1.domain.model.TaskStatus
import com.example.nextthingb1.domain.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime

/**
 * 设备重启后重新调度任务闹钟
 * AlarmManager 的闹钟在设备重启后会全部清除，需要在 BOOT_COMPLETED 时重新注册
 */
class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun taskRepository(): TaskRepository
        fun taskAlarmManager(): TaskAlarmManager
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Timber.d("[BootReceiver] 设备重启，开始重新调度任务闹钟...")

        val entryPoint = try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                BootReceiverEntryPoint::class.java
            )
        } catch (e: Exception) {
            Timber.e(e, "[BootReceiver] 获取EntryPoint失败")
            return
        }

        val taskRepository = entryPoint.taskRepository()
        val taskAlarmManager = entryPoint.taskAlarmManager()

        val pendingResult = goAsync()

        scope.launch {
            try {
                val now = LocalDateTime.now()
                val tasks = taskRepository.getAllTasks().first()
                var rescheduled = 0

                tasks.filter { task ->
                    task.status == TaskStatus.PENDING &&
                    task.dueDate != null &&
                    task.dueDate.isAfter(now) &&
                    task.notificationStrategyId != null
                }.forEach { task ->
                    taskAlarmManager.scheduleTaskAlarm(task)
                    rescheduled++
                }

                Timber.d("[BootReceiver] 已重新调度 $rescheduled 个任务闹钟")
            } catch (e: Exception) {
                Timber.e(e, "[BootReceiver] 重新调度任务闹钟失败")
            } finally {
                pendingResult.finish()
            }
        }
    }
}

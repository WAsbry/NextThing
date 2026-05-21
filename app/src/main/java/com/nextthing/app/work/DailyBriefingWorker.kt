package com.nextthing.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextthing.app.data.preferences.BriefingPreferences
import com.nextthing.app.domain.model.TaskStatus
import com.nextthing.app.domain.repository.TaskRepository
import com.nextthing.app.domain.service.AIBriefingGenerator
import com.nextthing.app.domain.service.AIBriefingGenerator.BriefingType
import com.nextthing.app.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@HiltWorker
class DailyBriefingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val briefingPreferences: BriefingPreferences,
    private val briefingGenerator: AIBriefingGenerator,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            if (!briefingPreferences.isEnabledOnce()) {
                Timber.tag("AI-Briefing").d("早晚报未启用，跳过")
                return Result.success()
            }

            val now = LocalDateTime.now()
            val currentHour = now.hour
            val morningHour = briefingPreferences.getMorningHourOnce()
            val eveningHour = briefingPreferences.getEveningHourOnce()

            val type = if (Math.abs(currentHour - morningHour) <= 1) {
                BriefingType.MORNING
            } else {
                BriefingType.EVENING
            }

            Timber.tag("AI-Briefing").d("开始生成${if (type == BriefingType.MORNING) "早报" else "晚报"}")

            val taskData = buildTaskData(type)
            val title = if (type == BriefingType.MORNING) "☀️ 早安简报" else "🌙 晚安简报"

            val content = briefingGenerator.generateBriefing(type, taskData)
                .getOrElse { error ->
                    Timber.tag("AI-Briefing").w(error, "AI 简报失败，使用本地 fallback")
                    buildFallbackBriefing(type, taskData)
                }

            notificationHelper.showBriefingNotification(title, content)
            Result.success()
        } catch (e: Exception) {
            Timber.tag("AI-Briefing").e(e, "DailyBriefingWorker 异常")
            Result.retry()
        }
    }

    private suspend fun buildTaskData(type: BriefingType): String {
        val sb = StringBuilder()
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        if (type == BriefingType.MORNING) {
            val todayTasks = taskRepository.getTodayTasks().first()
            val overdueTasks = taskRepository.getOverdueTasks().first()

            sb.appendLine("=== 今日待办 (${todayTasks.size}件) ===")
            todayTasks.forEach { task ->
                val time = task.dueDate?.format(formatter) ?: "无时间"
                val status = if (task.status == TaskStatus.COMPLETED) "✅" else "⏳"
                val priority = task.importanceUrgency?.displayName?.let { "[$it]" } ?: ""
                sb.appendLine("$status $time ${task.title} ${task.category.name} $priority")
            }

            if (overdueTasks.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("=== 逾期未完成 (${overdueTasks.size}件) ===")
                overdueTasks.forEach { task ->
                    val due = task.dueDate?.format(DateTimeFormatter.ofPattern("MM/dd HH:mm")) ?: ""
                    sb.appendLine("⚠️ ${task.title} (截止: $due)")
                }
            }
        } else {
            val todayTasks = taskRepository.getTodayTasks().first()
            val completed = todayTasks.filter { it.status == TaskStatus.COMPLETED }
            val pending = todayTasks.filter { it.status != TaskStatus.COMPLETED }

            sb.appendLine("=== 今日完成 (${completed.size}件) ===")
            completed.forEach { task ->
                val time = task.completedAt?.format(formatter) ?: ""
                sb.appendLine("✅ $time ${task.title}")
            }

            if (pending.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("=== 今日未完成 (${pending.size}件) ===")
                pending.forEach { task ->
                    val time = task.dueDate?.format(formatter) ?: ""
                    sb.appendLine("⏳ $time ${task.title}")
                }
            }

            val tomorrow = today.plusDays(1)
            val tomorrowTasks = taskRepository.getTasksByDueDate(tomorrow.atStartOfDay()).first()
            if (tomorrowTasks.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("=== 明日待办 (${tomorrowTasks.size}件) ===")
                tomorrowTasks.forEach { task ->
                    val time = task.dueDate?.format(formatter) ?: ""
                    sb.appendLine("📋 $time ${task.title}")
                }
            }
        }

        return sb.toString()
    }

    private fun buildFallbackBriefing(type: BriefingType, taskData: String): String {
        return if (type == BriefingType.MORNING) {
            val todayCount = taskData.lines().count { it.startsWith("⏳") || it.startsWith("⏰") }
            val overdueCount = taskData.lines().count { it.startsWith("⚠️") }
            buildString {
                append("早安！今天有 $todayCount 件待办任务。")
                if (overdueCount > 0) append(" 另有 $overdueCount 件逾期任务需要关注。")
                append(" 加油！")
            }
        } else {
            val completedCount = taskData.lines().count { it.startsWith("✅") }
            val pendingCount = taskData.lines().count { it.startsWith("⏳") }
            buildString {
                append("辛苦了！今天完成了 $completedCount 件任务。")
                if (pendingCount > 0) append(" 还有 $pendingCount 件未完成，记得安排好时间。")
                append(" 晚安！")
            }
        }
    }
}

package com.nextthing.app.data.service

import com.google.gson.Gson
import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.data.remote.api.AIChatApi
import com.nextthing.app.data.remote.dto.AIChatRequest
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.service.AIWeeklyReporter
import com.nextthing.app.domain.service.WeeklyReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIWeeklyReporterService @Inject constructor(
    private val aiChatApi: AIChatApi,
    private val tokenManager: TokenManager,
    private val gson: Gson
) : AIWeeklyReporter {

    override suspend fun generateReport(
        weekTasks: List<Task>,
        completedTasks: List<Task>
    ): Result<WeeklyReport> {
        val userId = tokenManager.serverUserId.first()
        if (userId == null) return Result.failure(IllegalStateException("请先登录"))

        return try {
            val timeFmt = DateTimeFormatter.ofPattern("MM/dd")
            val sb = StringBuilder()
            sb.appendLine("生成本周周报。标题20字以内，总结2-3句话，2-4个亮点，1-3个待改进，2-3条下周建议。语气积极客观。")
            sb.appendLine("请返回JSON：{\"title\":\"...\",\"summary\":\"...\",\"highlights\":[\"...\"],\"improvements\":[\"...\"],\"nextWeekSuggestions\":[\"...\"]}")

            val completedCount = completedTasks.size
            val totalCount = weekTasks.size
            sb.appendLine("\n统计：本周任务$totalCount，已完成$completedCount，完成率${if (totalCount > 0) "%.0f%%".format(completedCount.toDouble() / totalCount * 100) else "N/A"}")

            if (completedTasks.isNotEmpty()) {
                sb.appendLine("\n=== 已完成 ===")
                completedTasks.forEach { t ->
                    sb.appendLine("- ${t.title} | 分类:${t.category.name}")
                }
            }

            val pending = weekTasks.filter { it.status.name !in listOf("DONE", "CANCELLED") }
            if (pending.isNotEmpty()) {
                sb.appendLine("\n=== 待完成 ===")
                pending.forEach { t ->
                    sb.appendLine("- ${t.title} | 状态:${t.status.name} | 截止:${t.dueDate?.format(timeFmt) ?: "无"}")
                }
            }

            val response = withContext(Dispatchers.IO) {
                aiChatApi.chat(AIChatRequest(sb.toString()))
            }
            if (!response.success || response.reply.isNullOrBlank()) {
                return Result.failure(Exception(response.reply ?: "AI 返回内容为空"))
            }

            val obj = AIJsonHelper.parseAIJson(gson, response.reply)
            Result.success(WeeklyReport(
                title = obj.get("title")?.asString ?: "本周周报",
                summary = obj.get("summary")?.asString ?: "",
                highlights = obj.getAsJsonArray("highlights")?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList(),
                improvements = obj.getAsJsonArray("improvements")?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList(),
                nextWeekSuggestions = obj.getAsJsonArray("nextWeekSuggestions")?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList()
            ))
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "周报生成失败")
            Result.failure(Exception("AI 周报生成失败: ${e.message}"))
        }
    }
}

package com.nextthing.app.data.service

import com.google.gson.Gson
import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.data.remote.api.AIChatApi
import com.nextthing.app.data.remote.dto.AIChatRequest
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.service.AIScheduleAdvisor
import com.nextthing.app.domain.service.ScheduleAdvice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIScheduleAdvisorService @Inject constructor(
    private val aiChatApi: AIChatApi,
    private val tokenManager: TokenManager,
    private val gson: Gson
) : AIScheduleAdvisor {

    private val timeFmt = DateTimeFormatter.ofPattern("MM/dd HH:mm")

    override suspend fun analyzeSchedule(
        overdueTasks: List<Task>,
        todayTasks: List<Task>,
        urgentTasks: List<Task>
    ): Result<ScheduleAdvice> {
        val userId = tokenManager.serverUserId.first()
        if (userId == null) return Result.failure(IllegalStateException("请先登录"))

        return try {
            val sb = StringBuilder()
            sb.appendLine("分析用户的日程安排是否合理，检查时间冲突、优先级失衡、过载和逾期堆积，给出3-5条可执行建议。")
            sb.appendLine("请返回JSON：{\"summary\":\"...\",\"suggestions\":[\"...\"]}")

            if (overdueTasks.isNotEmpty()) {
                sb.appendLine("\n=== 逾期任务 ===")
                overdueTasks.forEach { t ->
                    sb.appendLine("- ${t.title} | 截止:${t.dueDate?.format(timeFmt) ?: "无"} | 分类:${t.category.name}")
                }
            }
            if (todayTasks.isNotEmpty()) {
                sb.appendLine("\n=== 今日任务 ===")
                todayTasks.forEach { t ->
                    sb.appendLine("- ${t.title} | 状态:${t.status.name} | 分类:${t.category.name}")
                }
            }
            if (urgentTasks.isNotEmpty()) {
                sb.appendLine("\n=== 紧急任务 ===")
                urgentTasks.forEach { t ->
                    sb.appendLine("- ${t.title} | 截止:${t.dueDate?.format(timeFmt) ?: "无"}")
                }
            }

            val response = withContext(Dispatchers.IO) {
                aiChatApi.chat(AIChatRequest(sb.toString()))
            }
            if (!response.success || response.reply.isNullOrBlank()) {
                return Result.failure(Exception(response.reply ?: "AI 返回内容为空"))
            }

            val obj = AIJsonHelper.parseAIJson(gson, response.reply)
            Result.success(ScheduleAdvice(
                summary = obj.get("summary")?.asString ?: "",
                suggestions = obj.getAsJsonArray("suggestions")?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList()
            ))
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "日程分析失败")
            Result.failure(Exception("AI 日程分析失败: ${e.message}"))
        }
    }
}

package com.nextthing.app.data.service

import com.google.gson.Gson
import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.data.remote.api.AIChatApi
import com.nextthing.app.data.remote.dto.AIChatRequest
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.service.AIBehaviorAnalyzer
import com.nextthing.app.domain.service.BehaviorInsight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIBehaviorAnalyzerService @Inject constructor(
    private val aiChatApi: AIChatApi,
    private val tokenManager: TokenManager,
    private val gson: Gson
) : AIBehaviorAnalyzer {

    override suspend fun analyzeBehavior(
        completedTasks: List<Task>,
        allTasks: List<Task>
    ): Result<BehaviorInsight> {
        val userId = tokenManager.serverUserId.first()
        if (userId == null) return Result.failure(IllegalStateException("请先登录"))

        if (completedTasks.size < 3) {
            return Result.success(BehaviorInsight(
                patterns = listOf("已完成任务不足3个，暂无法分析行为模式"),
                suggestions = listOf("继续完成任务以积累更多数据")
            ))
        }

        return try {
            val timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val sb = StringBuilder()
            sb.appendLine("分析用户的行为模式和习惯规律（时间偏好、分类偏好、工作日vs周末频率、优先级倾向）。")
            sb.appendLine("请返回JSON：{\"patterns\":[\"...\"],\"suggestions\":[\"...\"]}")

            val completedCount = completedTasks.size
            val totalCount = allTasks.size
            sb.appendLine("\n统计：总任务$totalCount，已完成$completedCount，完成率${"%.0f".format(completedCount.toDouble() / totalCount * 100)}%")

            sb.appendLine("\n=== 已完成任务（最近30个） ===")
            completedTasks.take(30).forEach { t ->
                sb.appendLine("- ${t.title} | 分类:${t.category.name} | 完成:${t.updatedAt?.format(timeFmt) ?: "未知"}")
            }

            val response = withContext(Dispatchers.IO) {
                aiChatApi.chat(AIChatRequest(sb.toString()))
            }
            if (!response.success || response.reply.isNullOrBlank()) {
                return Result.failure(Exception(response.reply ?: "AI 返回内容为空"))
            }

            val obj = AIJsonHelper.parseAIJson(gson, response.reply)
            Result.success(BehaviorInsight(
                patterns = obj.getAsJsonArray("patterns")?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList(),
                suggestions = obj.getAsJsonArray("suggestions")?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList()
            ))
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "行为分析失败")
            Result.failure(Exception("AI 行为分析失败: ${e.message}"))
        }
    }
}

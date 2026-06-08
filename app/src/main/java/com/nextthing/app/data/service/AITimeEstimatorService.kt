package com.nextthing.app.data.service

import com.google.gson.Gson
import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.data.remote.api.AIChatApi
import com.nextthing.app.data.remote.dto.AIChatRequest
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.service.AITimeEstimator
import com.nextthing.app.domain.service.TimeEstimate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AITimeEstimatorService @Inject constructor(
    private val aiChatApi: AIChatApi,
    private val tokenManager: TokenManager,
    private val gson: Gson
) : AITimeEstimator {

    override suspend fun estimateTime(
        taskTitle: String,
        taskDescription: String,
        categoryName: String?,
        recentCompletedTasks: List<Task>
    ): Result<TimeEstimate> {
        val userId = tokenManager.serverUserId.first()
        if (userId == null) return Result.failure(IllegalStateException("请先登录"))

        return try {
            val timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val sb = StringBuilder()
            sb.appendLine("预估完成以下任务所需的时间。优先参考历史同类任务的实际耗时。")
            sb.appendLine("请返回JSON：{\"estimatedMinutes\":120,\"reasoning\":\"...\",\"similarTaskCount\":3}")
            sb.appendLine()
            sb.appendLine("=== 当前任务 ===")
            sb.appendLine("标题：$taskTitle")
            sb.appendLine("描述：${taskDescription.ifBlank { "无" }}")
            sb.appendLine("分类：${categoryName ?: "未分类"}")

            if (recentCompletedTasks.isNotEmpty()) {
                sb.appendLine("\n=== 最近完成的任务（供参考） ===")
                recentCompletedTasks.take(20).forEach { t ->
                    val minutes = if (t.createdAt != null && t.updatedAt != null)
                        ChronoUnit.MINUTES.between(t.createdAt, t.updatedAt) else 0L
                    sb.appendLine("- ${t.title} | 分类:${t.category.name} | 耗时:${minutes}分钟")
                }
            }

            val response = withContext(Dispatchers.IO) {
                aiChatApi.chat(AIChatRequest(sb.toString()))
            }
            if (!response.success || response.reply.isNullOrBlank()) {
                return Result.failure(Exception(response.reply ?: "AI 返回内容为空"))
            }

            val obj = AIJsonHelper.parseAIJson(gson, response.reply)
            Result.success(TimeEstimate(
                estimatedMinutes = obj.get("estimatedMinutes")?.asInt ?: 60,
                reasoning = obj.get("reasoning")?.asString ?: "",
                similarTaskCount = obj.get("similarTaskCount")?.asInt ?: 0
            ))
        } catch (e: Exception) {
            Timber.tag("AI-Time").e(e, "时间预估失败")
            Result.failure(Exception("AI 时间预估失败: ${e.message}"))
        }
    }
}

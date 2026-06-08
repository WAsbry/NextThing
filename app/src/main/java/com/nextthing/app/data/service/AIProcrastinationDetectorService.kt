package com.nextthing.app.data.service

import com.google.gson.Gson
import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.data.remote.api.AIChatApi
import com.nextthing.app.data.remote.dto.AIChatRequest
import com.nextthing.app.domain.service.AIProcrastinationDetector
import com.nextthing.app.domain.service.ProcrastinationAdvice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIProcrastinationDetectorService @Inject constructor(
    private val aiChatApi: AIChatApi,
    private val tokenManager: TokenManager,
    private val gson: Gson
) : AIProcrastinationDetector {

    override suspend fun analyze(
        taskTitle: String,
        taskDescription: String,
        delayCount: Int,
        daysSinceCreated: Int,
        category: String
    ): Result<ProcrastinationAdvice> {
        val userId = tokenManager.serverUserId.first()
        if (userId == null) return Result.failure(IllegalStateException("请先登录"))

        return try {
            val message = """分析这个任务是否存在拖延行为，判断严重程度（0-1次延期=low, 2-3次=medium, 4次以上=high）。
标题：$taskTitle
描述：${taskDescription.ifBlank { "无" }}
分类：$category
延期次数：$delayCount
创建至今天数：$daysSinceCreated
请返回JSON：{"severity":"low/medium/high","summary":"...","suggestions":["..."]}""".trimIndent()

            val response = withContext(Dispatchers.IO) {
                aiChatApi.chat(AIChatRequest(message))
            }
            if (!response.success || response.reply.isNullOrBlank()) {
                return Result.failure(Exception(response.reply ?: "AI 返回内容为空"))
            }

            val obj = AIJsonHelper.parseAIJson(gson, response.reply)
            Result.success(ProcrastinationAdvice(
                severity = obj.get("severity")?.asString ?: "low",
                summary = obj.get("summary")?.asString ?: "",
                suggestions = obj.getAsJsonArray("suggestions")?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList()
            ))
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "拖延分析失败")
            Result.failure(Exception("AI 拖延分析失败: ${e.message}"))
        }
    }
}

package com.nextthing.app.data.service

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.data.remote.api.AIChatApi
import com.nextthing.app.data.remote.dto.AIChatRequest
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.service.AITaskSearcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AITaskSearcherService @Inject constructor(
    private val aiChatApi: AIChatApi,
    private val tokenManager: TokenManager,
    private val gson: Gson
) : AITaskSearcher {

    override suspend fun searchByNaturalLanguage(
        query: String,
        allTasks: List<Task>
    ): Result<List<Task>> {
        val userId = tokenManager.serverUserId.first()
        if (userId == null) return Result.failure(IllegalStateException("请先登录"))

        if (allTasks.isEmpty()) return Result.success(emptyList())

        return try {
            val message = buildString {
                appendLine("用户想找这样的任务：$query")
                appendLine("请使用 searchTasks 工具搜索相关任务，然后从结果中找出语义匹配的任务。")
                appendLine("支持模糊匹配（如'开会'匹配'项目会议'）和时间相关查询（'上周'、'最近'）。")
                appendLine("只返回匹配任务的ID列表（JSON字符串数组），如 [\"id1\",\"id2\"]，不要其他文字。")
            }

            val response = withContext(Dispatchers.IO) {
                aiChatApi.chat(AIChatRequest(message))
            }
            if (!response.success || response.reply.isNullOrBlank()) {
                return Result.failure(Exception(response.reply ?: "AI 返回内容为空"))
            }

            val cleanJson = response.reply.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()

            val array = gson.fromJson(cleanJson, JsonArray::class.java)
            val matchedIds = array.mapNotNull { runCatching { it.asString }.getOrNull() }.toSet()

            val results = allTasks.filter { it.id in matchedIds }
            Result.success(results)
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "任务搜索失败")
            Result.failure(Exception("AI 任务搜索失败: ${e.message}"))
        }
    }
}

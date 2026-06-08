package com.nextthing.app.data.service

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.data.remote.api.AIChatApi
import com.nextthing.app.data.remote.dto.AIChatRequest
import com.nextthing.app.domain.service.AISubtaskGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AISubtaskGeneratorService @Inject constructor(
    private val aiChatApi: AIChatApi,
    private val tokenManager: TokenManager,
    private val gson: Gson
) : AISubtaskGenerator {

    override suspend fun generateSubtasks(
        taskTitle: String,
        taskDescription: String
    ): Result<List<String>> {
        val userId = tokenManager.serverUserId.first()
        if (userId == null) return Result.failure(IllegalStateException("请先登录"))

        return try {
            val message = """请将以下任务拆解为3-8个具体可执行的子任务步骤，按执行顺序排列。
任务标题：$taskTitle
任务描述：${taskDescription.ifBlank { "无" }}
请严格返回JSON字符串数组，不要包含任何其他文字。
示例：["步骤1", "步骤2", "步骤3"]""".trimIndent()

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
            val subtasks = array.mapNotNull { runCatching { it.asString }.getOrNull() }

            if (subtasks.isEmpty()) Result.failure(Exception("解析子任务失败"))
            else Result.success(subtasks)
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "子任务生成失败")
            Result.failure(Exception("AI 子任务生成失败: ${e.message}"))
        }
    }
}

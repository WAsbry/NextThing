package com.nextthing.app.data.service

import com.nextthing.app.data.preferences.AIPreferences
import com.nextthing.app.data.remote.ai.ChatCompletionRequest
import com.nextthing.app.data.remote.ai.ChatCompletionResponse
import com.nextthing.app.data.remote.ai.ChatMessage
import com.nextthing.app.data.remote.ai.ResponseFormat
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.service.AITaskSearcher
import com.google.gson.Gson
import com.google.gson.JsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AITaskSearcherService @Inject constructor(
    @Named("ai") private val okHttpClient: OkHttpClient,
    private val aiPreferences: AIPreferences,
    private val gson: Gson
) : AITaskSearcher {

    private val systemPrompt = """
你是一个任务搜索助手。用户会用自然语言描述想找的任务，你需要从任务列表中找出匹配的任务。

要求：
1. 返回 JSON 数组，包含匹配任务的 id 列表
2. 不要包含任何其他文字
3. 如果没有匹配的任务，返回空数组 []
4. 模糊匹配：理解用户意图，比如"开会"匹配"项目会议"、"周会"等
5. 支持时间相关查询："上周"、"最近"、"上个月"等

格式：
[1, 5, 12]
    """.trimIndent()

    override suspend fun searchByNaturalLanguage(
        query: String,
        allTasks: List<Task>
    ): Result<List<Task>> {
        if (!aiPreferences.isConfigured()) {
            return Result.failure(IllegalStateException("请先在设置 → AI 智能助手中配置 API Key"))
        }

        if (allTasks.isEmpty()) {
            return Result.success(emptyList())
        }

        val provider = aiPreferences.getProviderOnce()
        val apiKey = aiPreferences.getApiKeyOnce()
        val modelOverride = aiPreferences.getModelOnce()
        val model = modelOverride.ifBlank { provider.defaultModel }
        val baseUrl = provider.baseUrl

        return try {
            val timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val sb = StringBuilder()
            sb.appendLine("用户搜索：$query")
            sb.appendLine()
            sb.appendLine("=== 所有任务 ===")
            allTasks.forEach { t ->
                val due = t.dueDate?.format(timeFmt) ?: "无截止日期"
                sb.appendLine("[${t.id}] ${t.title} | 状态:${t.status} | 分类:${t.category.name} | 截止:$due | 描述:${t.description ?: "无"}")
            }

            val requestObj = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = sb.toString())
                ),
                temperature = 0.2f,
                responseFormat = ResponseFormat()
            )

            val requestJson = gson.toJson(requestObj)
            val requestBody = requestJson.toRequestBody("application/json".toMediaType())

            val httpRequest = Request.Builder()
                .url("${baseUrl}chat/completions")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()

            Timber.tag("AI-Search").d("搜索任务: query=$query, total=${allTasks.size}")

            val (responseCode, responseBody) = withContext(Dispatchers.IO) {
                val response = okHttpClient.newCall(httpRequest).execute()
                response.code to (response.body?.string() ?: "")
            }

            if (responseCode !in 200..299) {
                return Result.failure(Exception("API 错误 ($responseCode)"))
            }

            val chatResponse = gson.fromJson(responseBody, ChatCompletionResponse::class.java)
            chatResponse.error?.let { err ->
                return Result.failure(Exception("AI 服务错误: ${err.message}"))
            }

            val content = chatResponse.choices?.firstOrNull()?.message?.content
            if (content.isNullOrBlank()) {
                return Result.failure(Exception("AI 返回内容为空"))
            }

            val cleanJson = content.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()

            val idArray = gson.fromJson(cleanJson, JsonArray::class.java)
            val ids = idArray.mapNotNull { runCatching { it.asString }.getOrNull() }.toSet()

            val matchedTasks = allTasks.filter { it.id in ids }

            Timber.tag("AI-Search").d("搜索完成: 匹配 ${matchedTasks.size} 个任务")
            Result.success(matchedTasks)
        } catch (e: Exception) {
            Timber.tag("AI-Search").e(e, "任务搜索异常")
            Result.failure(Exception("AI 搜索失败: ${e.message}"))
        }
    }
}

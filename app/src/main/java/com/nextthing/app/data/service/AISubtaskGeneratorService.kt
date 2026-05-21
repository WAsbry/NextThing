package com.nextthing.app.data.service

import com.nextthing.app.data.preferences.AIPreferences
import com.nextthing.app.data.remote.ai.ChatCompletionRequest
import com.nextthing.app.data.remote.ai.ChatCompletionResponse
import com.nextthing.app.data.remote.ai.ChatMessage
import com.nextthing.app.data.remote.ai.ResponseFormat
import com.nextthing.app.domain.service.AISubtaskGenerator
import com.google.gson.Gson
import com.google.gson.JsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AISubtaskGeneratorService @Inject constructor(
    @Named("ai") private val okHttpClient: OkHttpClient,
    private val aiPreferences: AIPreferences,
    private val gson: Gson
) : AISubtaskGenerator {

    private val systemPrompt = """
你是一个任务分解助手。根据用户给出的任务标题和描述，将其拆解为具体的执行步骤。

要求：
1. 返回 3-8 个具体可执行的子任务步骤
2. 每个步骤简洁明了（10字以内）
3. 按执行顺序排列
4. 步骤之间不重复，覆盖任务的核心环节
5. 严格返回 JSON 字符串数组，不要包含任何其他文字

示例：
["整理上周数据", "准备演示 PPT", "确认会议室预约", "发送会议邀请"]
    """.trimIndent()

    override suspend fun generateSubtasks(
        taskTitle: String,
        taskDescription: String
    ): Result<List<String>> {
        if (!aiPreferences.isConfigured()) {
            return Result.failure(IllegalStateException("请先在设置 → AI 智能助手中配置 API Key"))
        }

        val provider = aiPreferences.getProviderOnce()
        val apiKey = aiPreferences.getApiKeyOnce()
        val modelOverride = aiPreferences.getModelOnce()
        val model = modelOverride.ifBlank { provider.defaultModel }
        val baseUrl = provider.baseUrl

        return try {
            val userContent = buildString {
                append("任务标题：$taskTitle")
                if (taskDescription.isNotBlank()) {
                    append("\n任务描述：$taskDescription")
                }
            }

            val requestObj = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userContent)
                ),
                temperature = 0.4f,
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

            Timber.tag("AI-Subtask").d("请求子任务: title=$taskTitle, model=$model")

            val (responseCode, responseBody) = withContext(Dispatchers.IO) {
                val response = okHttpClient.newCall(httpRequest).execute()
                response.code to (response.body?.string() ?: "")
            }

            if (responseCode !in 200..299) {
                Timber.tag("AI-Subtask").e("API 错误 $responseCode: $responseBody")
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

            val jsonArray = gson.fromJson(cleanJson, JsonArray::class.java)
            val subtasks = jsonArray.mapNotNull { element ->
                runCatching { element.asString }.getOrNull()?.takeIf { it.isNotBlank() }
            }

            Timber.tag("AI-Subtask").d("子任务生成成功: ${subtasks.size} 个")
            Result.success(subtasks)
        } catch (e: Exception) {
            Timber.tag("AI-Subtask").e(e, "子任务生成异常")
            Result.failure(Exception("AI 子任务生成失败: ${e.message}"))
        }
    }
}

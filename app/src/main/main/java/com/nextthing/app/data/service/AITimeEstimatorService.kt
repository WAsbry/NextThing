package com.nextthing.app.data.service

import com.nextthing.app.data.preferences.AIPreferences
import com.nextthing.app.data.remote.ai.ChatCompletionRequest
import com.nextthing.app.data.remote.ai.ChatCompletionResponse
import com.nextthing.app.data.remote.ai.ChatMessage
import com.nextthing.app.data.remote.ai.ResponseFormat
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.service.AITimeEstimator
import com.nextthing.app.domain.service.TimeEstimate
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AITimeEstimatorService @Inject constructor(
    @Named("ai") private val okHttpClient: OkHttpClient,
    private val aiPreferences: AIPreferences,
    private val gson: Gson
) : AITimeEstimator {

    private val systemPrompt = """
你是一个时间管理专家。根据用户的任务描述和历史同类任务的完成情况，预估完成当前任务所需的时间。

要求：
1. 返回 JSON 格式，不要包含任何其他文字
2. estimatedMinutes：预估完成分钟数（整数）
3. reasoning：预估依据（一句话，30字以内）
4. similarTaskCount：参考的同类任务数量

格式：
{
  "estimatedMinutes": 120,
  "reasoning": "类似开发任务平均需要2小时",
  "similarTaskCount": 3
}

预估原则：
- 优先参考历史同类任务的实际耗时
- 考虑任务复杂度、描述中的细节
- 没有历史数据时，根据任务类型合理预估
    """.trimIndent()

    override suspend fun estimateTime(
        taskTitle: String,
        taskDescription: String,
        categoryName: String?,
        recentCompletedTasks: List<Task>
    ): Result<TimeEstimate> {
        if (!aiPreferences.isConfigured()) {
            return Result.failure(IllegalStateException("请先在设置 → AI 智能助手中配置 API Key"))
        }

        val provider = aiPreferences.getProviderOnce()
        val apiKey = aiPreferences.getApiKeyOnce()
        val modelOverride = aiPreferences.getModelOnce()
        val model = modelOverride.ifBlank { provider.defaultModel }
        val baseUrl = provider.baseUrl

        return try {
            val timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val sb = StringBuilder()
            sb.appendLine("=== 当前任务 ===")
            sb.appendLine("标题：$taskTitle")
            sb.appendLine("描述：${taskDescription.ifBlank { "无" }}")
            sb.appendLine("分类：${categoryName ?: "未分类"}")

            if (recentCompletedTasks.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("=== 最近完成的任务（供参考） ===")
                recentCompletedTasks.take(20).forEach { t ->
                    val created = t.createdAt.format(timeFmt)
                    val completed = t.updatedAt.format(timeFmt)
                    val minutes = ChronoUnit.MINUTES.between(t.createdAt, t.updatedAt)
                    sb.appendLine("- ${t.title} | 分类:${t.category.name} | 耗时:${minutes}分钟")
                }
            }

            val requestObj = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = sb.toString())
                ),
                temperature = 0.3f,
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

            Timber.tag("AI-Time").d("预估时间: $taskTitle, 历史=${recentCompletedTasks.size}")

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

            val obj = gson.fromJson(cleanJson, JsonObject::class.java)
            val estimatedMinutes = obj.get("estimatedMinutes")?.asInt ?: 60
            val reasoning = obj.get("reasoning")?.asString ?: ""
            val similarTaskCount = obj.get("similarTaskCount")?.asInt ?: 0

            Timber.tag("AI-Time").d("预估结果: ${estimatedMinutes}分钟, 依据: $reasoning")
            Result.success(TimeEstimate(estimatedMinutes, reasoning, similarTaskCount))
        } catch (e: Exception) {
            Timber.tag("AI-Time").e(e, "时间预估异常")
            Result.failure(Exception("AI 时间预估失败: ${e.message}"))
        }
    }
}

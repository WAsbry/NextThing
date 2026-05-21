package com.nextthing.app.data.service

import com.nextthing.app.data.preferences.AIPreferences
import com.nextthing.app.data.remote.ai.ChatCompletionRequest
import com.nextthing.app.data.remote.ai.ChatCompletionResponse
import com.nextthing.app.data.remote.ai.ChatMessage
import com.nextthing.app.data.remote.ai.ResponseFormat
import com.nextthing.app.domain.service.AIProcrastinationDetector
import com.nextthing.app.domain.service.ProcrastinationAdvice
import com.google.gson.Gson
import com.google.gson.JsonObject
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
class AIProcrastinationDetectorService @Inject constructor(
    @Named("ai") private val okHttpClient: OkHttpClient,
    private val aiPreferences: AIPreferences,
    private val gson: Gson
) : AIProcrastinationDetector {

    private val systemPrompt = """
你是一个拖延症分析专家。根据任务信息和延期数据，判断用户是否在拖延这个任务，并给出针对性建议。

要求：
1. 返回 JSON 格式，不要包含任何其他文字
2. severity 字段：拖延严重程度 "low" / "medium" / "high"
3. summary 字段：一句话分析结论（40字以内）
4. suggestions 字段：2-4 条具体建议

格式：
{
  "severity": "medium",
  "summary": "该任务已延期3次，可能存在畏难情绪",
  "suggestions": ["建议将任务拆分为更小的子任务", "设定一个最小行动目标"]
}

判断依据：
- delayCount（延期次数）：0-1次为 low，2-3次为 medium，4次以上为 high
- daysSinceCreated（创建天数）：结合延期次数综合判断
- 任务复杂度：描述越长的任务越可能因为畏难而拖延
- 分类：某些分类（如健身、学习）更容易拖延
- 建议要具体可操作，比如拆解、设定小目标、改变环境等
    """.trimIndent()

    override suspend fun analyze(
        taskTitle: String,
        taskDescription: String,
        delayCount: Int,
        daysSinceCreated: Int,
        category: String
    ): Result<ProcrastinationAdvice> {
        if (!aiPreferences.isConfigured()) {
            return Result.failure(IllegalStateException("请先在设置 → AI 智能助手中配置 API Key"))
        }

        val provider = aiPreferences.getProviderOnce()
        val apiKey = aiPreferences.getApiKeyOnce()
        val modelOverride = aiPreferences.getModelOnce()
        val model = modelOverride.ifBlank { provider.defaultModel }
        val baseUrl = provider.baseUrl

        return try {
            val sb = StringBuilder()
            sb.appendLine("=== 任务信息 ===")
            sb.appendLine("标题：$taskTitle")
            sb.appendLine("描述：${taskDescription.ifBlank { "无" }}")
            sb.appendLine("分类：$category")
            sb.appendLine("延期次数：$delayCount")
            sb.appendLine("创建至今天数：$daysSinceCreated")

            val requestObj = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = sb.toString())
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

            Timber.tag("AI-Procrastination").d("分析拖延: $taskTitle, delay=$delayCount, days=$daysSinceCreated")

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
            val severity = obj.get("severity")?.asString ?: "low"
            val summary = obj.get("summary")?.asString ?: ""
            val suggestions = obj.getAsJsonArray("suggestions")
                ?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList()

            Timber.tag("AI-Procrastination").d("拖延分析结果: severity=$severity, $summary")
            Result.success(ProcrastinationAdvice(severity, summary, suggestions))
        } catch (e: Exception) {
            Timber.tag("AI-Procrastination").e(e, "拖延分析异常")
            Result.failure(Exception("AI 拖延分析失败: ${e.message}"))
        }
    }
}

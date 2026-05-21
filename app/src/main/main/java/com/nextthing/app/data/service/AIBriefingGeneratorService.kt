package com.nextthing.app.data.service

import com.nextthing.app.data.preferences.AIPreferences
import com.nextthing.app.data.remote.ai.ChatCompletionRequest
import com.nextthing.app.data.remote.ai.ChatCompletionResponse
import com.nextthing.app.data.remote.ai.ChatMessage
import com.nextthing.app.domain.service.AIBriefingGenerator
import com.nextthing.app.domain.service.AIBriefingGenerator.BriefingType
import com.google.gson.Gson
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
class AIBriefingGeneratorService @Inject constructor(
    @Named("ai") private val okHttpClient: OkHttpClient,
    private val aiPreferences: AIPreferences,
    private val gson: Gson
) : AIBriefingGenerator {

    private val morningPrompt = """
你是一个贴心的任务管理助手，正在为用户生成每日早间简报。

根据用户的任务数据，生成一段简洁的中文早间简报。

要求：
1. 控制在 100-200 字以内
2. 先热情打招呼（如"早安！"）
3. 列出今日待办的核心任务（按优先级排序，不重复列举所有）
4. 如有逾期任务，温柔提醒
5. 如有紧急重要任务，重点提示
6. 最后给一句鼓励的话
7. 不要使用 markdown 格式，纯文本即可
    """.trimIndent()

    private val eveningPrompt = """
你是一个贴心的任务管理助手，正在为用户生成每日晚间简报。

根据用户的任务数据，生成一段简洁的中文晚间简报。

要求：
1. 控制在 100-200 字以内
2. 先肯定今日完成的事项（如"辛苦了！"）
3. 对未完成的任务给出建议（是否延期或调整优先级）
4. 简要预告明天的待办
5. 最后给一句温暖的晚安语
6. 不要使用 markdown 格式，纯文本即可
    """.trimIndent()

    override suspend fun generateBriefing(type: BriefingType, taskData: String): Result<String> {
        if (!aiPreferences.isConfigured()) {
            return Result.failure(IllegalStateException("AI 未配置"))
        }

        val provider = aiPreferences.getProviderOnce()
        val apiKey = aiPreferences.getApiKeyOnce()
        val modelOverride = aiPreferences.getModelOnce()
        val model = modelOverride.ifBlank { provider.defaultModel }
        val baseUrl = provider.baseUrl
        val systemPrompt = if (type == BriefingType.MORNING) morningPrompt else eveningPrompt

        return try {
            val requestObj = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = taskData)
                ),
                temperature = 0.5f,
                responseFormat = null
            )

            val requestJson = gson.toJson(requestObj)
            val requestBody = requestJson.toRequestBody("application/json".toMediaType())

            val httpRequest = Request.Builder()
                .url("${baseUrl}chat/completions")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()

            Timber.tag("AI-Briefing").d("请求简报: type=$type, model=$model")

            val (responseCode, responseBody) = withContext(Dispatchers.IO) {
                val response = okHttpClient.newCall(httpRequest).execute()
                response.code to (response.body?.string() ?: "")
            }

            if (responseCode !in 200..299) {
                Timber.tag("AI-Briefing").e("API 错误 $responseCode: $responseBody")
                return Result.failure(Exception("API 错误 ($responseCode)"))
            }

            val chatResponse = gson.fromJson(responseBody, ChatCompletionResponse::class.java)

            chatResponse.error?.let { err ->
                Timber.tag("AI-Briefing").e("API 返回错误: ${err.message}")
                return Result.failure(Exception("AI 服务错误: ${err.message}"))
            }

            val content = chatResponse.choices?.firstOrNull()?.message?.content
            if (content.isNullOrBlank()) {
                return Result.failure(Exception("AI 返回内容为空"))
            }

            Timber.tag("AI-Briefing").d("简报生成成功 (${content.length}字): ${content.take(100)}")
            Result.success(content.trim())
        } catch (e: Exception) {
            Timber.tag("AI-Briefing").e(e, "简报生成异常")
            Result.failure(Exception("AI 简报生成失败: ${e.message}"))
        }
    }
}

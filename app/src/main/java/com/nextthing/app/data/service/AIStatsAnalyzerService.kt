package com.nextthing.app.data.service

import com.nextthing.app.data.preferences.AIPreferences
import com.nextthing.app.data.remote.ai.ChatCompletionRequest
import com.nextthing.app.data.remote.ai.ChatCompletionResponse
import com.nextthing.app.data.remote.ai.ChatMessage
import com.nextthing.app.domain.service.AIStatsAnalyzer
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
class AIStatsAnalyzerService @Inject constructor(
    @Named("ai") private val okHttpClient: OkHttpClient,
    private val aiPreferences: AIPreferences,
    private val gson: Gson
) : AIStatsAnalyzer {

    private val systemPrompt = """
你是一个任务管理分析师。根据用户的任务统计数据，生成一段简洁的中文分析总结。

要求：
1. 总结控制在 100-200 字以内
2. 先概述整体情况（完成率、任务量）
3. 指出亮点（做得好的方面）
4. 指出问题（逾期、延期、放弃等）
5. 给出 1-2 条具体可执行的建议
6. 语气友好积极，像一个贴心的助手
7. 不要使用 markdown 格式，纯文本即可
    """.trimIndent()

    override suspend fun generateSummary(statsData: String): Result<String> {
        if (!aiPreferences.isConfigured()) {
            return Result.failure(IllegalStateException("请先在设置 → AI 智能助手中配置 API Key"))
        }

        val provider = aiPreferences.getProviderOnce()
        val apiKey = aiPreferences.getApiKeyOnce()
        val modelOverride = aiPreferences.getModelOnce()
        val model = modelOverride.ifBlank { provider.defaultModel }
        val baseUrl = provider.baseUrl

        return try {
            val requestObj = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = statsData)
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

            Timber.tag("AI").d("🚀 AI 统计总结请求: provider=${provider.displayName}, model=$model")

            val (responseCode, responseBody) = withContext(Dispatchers.IO) {
                val response = okHttpClient.newCall(httpRequest).execute()
                response.code to (response.body?.string() ?: "")
            }

            if (responseCode !in 200..299) {
                Timber.tag("AI").e("❌ API 错误 $responseCode: $responseBody")
                val detail = when (responseCode) {
                    401 -> "API Key 无效或已过期，请检查设置"
                    402 -> "账户余额不足，请充值"
                    403 -> "无权访问该 API，请检查 Key 权限"
                    429 -> "请求太频繁，请稍后重试"
                    500, 502, 503 -> "AI 服务暂时不可用，请稍后重试"
                    else -> "API 错误 ($responseCode)"
                }
                return Result.failure(Exception(detail))
            }

            Timber.tag("AI").d("✅ AI 统计总结返回 ($responseCode)")

            val chatResponse = gson.fromJson(responseBody, ChatCompletionResponse::class.java)

            chatResponse.error?.let { err ->
                val msg = err.message ?: "未知错误"
                Timber.tag("AI").e("❌ API 返回错误: $msg")
                return Result.failure(Exception("AI 服务错误: $msg"))
            }

            val content = chatResponse.choices?.firstOrNull()?.message?.content

            if (content.isNullOrBlank()) {
                return Result.failure(Exception("AI 返回内容为空"))
            }

            Result.success(content.trim())
        } catch (e: java.net.UnknownHostException) {
            Timber.tag("AI").e(e, "❌ 无法连接到 AI 服务器")
            Result.failure(Exception("网络无法连接，请检查网络设置"))
        } catch (e: java.net.SocketTimeoutException) {
            Timber.tag("AI").e(e, "❌ AI 请求超时")
            Result.failure(Exception("请求超时，AI 响应较慢，请重试"))
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "❌ AI 统计总结异常: ${e.javaClass.simpleName}")
            Result.failure(Exception("AI 分析失败: ${e.message}"))
        }
    }
}

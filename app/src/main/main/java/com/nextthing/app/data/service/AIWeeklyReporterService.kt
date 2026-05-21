package com.nextthing.app.data.service

import com.nextthing.app.data.preferences.AIPreferences
import com.nextthing.app.data.remote.ai.ChatCompletionRequest
import com.nextthing.app.data.remote.ai.ChatCompletionResponse
import com.nextthing.app.data.remote.ai.ChatMessage
import com.nextthing.app.data.remote.ai.ResponseFormat
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.service.AIWeeklyReporter
import com.nextthing.app.domain.service.WeeklyReport
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
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AIWeeklyReporterService @Inject constructor(
    @Named("ai") private val okHttpClient: OkHttpClient,
    private val aiPreferences: AIPreferences,
    private val gson: Gson
) : AIWeeklyReporter {

    private val systemPrompt = """
你是一个周报撰写助手。根据用户本周的任务数据，生成一份简洁有洞察的周报。

要求：
1. 返回 JSON 格式，不要包含任何其他文字
2. title 字段：周报标题（简洁，含本周关键主题，20字以内）
3. summary 字段：本周总体回顾（2-3句话）
4. highlights 字段：2-4 条本周亮点/成就
5. improvements 字段：1-3 条可以改进的地方
6. nextWeekSuggestions 字段：2-3 条下周建议

格式：
{
  "title": "高效推进项目的一周",
  "summary": "本周完成了8个任务，完成率67%，比上周提升了15%...",
  "highlights": ["完成了项目A的核心开发", "连续5天坚持健身"],
  "improvements": ["有2个任务反复延期，需要拆解", "周末任务完成率较低"],
  "nextWeekSuggestions": ["优先处理逾期任务", "周末安排1-2个轻松任务保持节奏"]
}

撰写原则：
- 语气积极但客观
- 用数据说话（完成率、数量变化等）
- 亮点要具体，改进要可操作
- 建议要贴合用户实际情况
    """.trimIndent()

    override suspend fun generateReport(
        weekTasks: List<Task>,
        completedTasks: List<Task>
    ): Result<WeeklyReport> {
        if (!aiPreferences.isConfigured()) {
            return Result.failure(IllegalStateException("请先在设置 → AI 智能助手中配置 API Key"))
        }

        val provider = aiPreferences.getProviderOnce()
        val apiKey = aiPreferences.getApiKeyOnce()
        val modelOverride = aiPreferences.getModelOnce()
        val model = modelOverride.ifBlank { provider.defaultModel }
        val baseUrl = provider.baseUrl

        return try {
            val timeFmt = DateTimeFormatter.ofPattern("MM/dd HH:mm")
            val sb = StringBuilder()

            sb.appendLine("=== 本周任务总览 ===")
            sb.appendLine("总任务数：${weekTasks.size}")
            sb.appendLine("已完成：${completedTasks.size}")
            sb.appendLine("完成率：${if (weekTasks.isNotEmpty()) "%.0f%%".format(completedTasks.size.toFloat() / weekTasks.size * 100) else "0%"}")

            val categoryCount = completedTasks.groupingBy { it.category.name }.eachCount()
            sb.appendLine("分类完成：$categoryCount")

            sb.appendLine()
            sb.appendLine("=== 已完成任务 ===")
            completedTasks.forEach { t ->
                val completed = t.updatedAt.format(timeFmt)
                sb.appendLine("✅ ${t.title} | ${t.category.name} | $completed")
            }

            val pendingTasks = weekTasks.filter { it.status.name != "COMPLETED" && it.status.name != "CANCELLED" }
            if (pendingTasks.isNotEmpty()) {
                sb.appendLine()
                sb.appendLine("=== 未完成任务 ===")
                pendingTasks.forEach { t ->
                    val status = t.status.name
                    val due = t.dueDate?.format(timeFmt) ?: "无截止"
                    sb.appendLine("⏳ ${t.title} | 状态:$status | 截止:$due | ${t.category.name}")
                }
            }

            val requestObj = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = sb.toString())
                ),
                temperature = 0.6f,
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

            Timber.tag("AI-Weekly").d("生成周报: total=${weekTasks.size}, completed=${completedTasks.size}")

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
            val title = obj.get("title")?.asString ?: "本周周报"
            val summary = obj.get("summary")?.asString ?: ""
            val highlights = obj.getAsJsonArray("highlights")
                ?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList()
            val improvements = obj.getAsJsonArray("improvements")
                ?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList()
            val nextWeekSuggestions = obj.getAsJsonArray("nextWeekSuggestions")
                ?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList()

            Timber.tag("AI-Weekly").d("周报生成成功: $title")
            Result.success(WeeklyReport(title, summary, highlights, improvements, nextWeekSuggestions))
        } catch (e: Exception) {
            Timber.tag("AI-Weekly").e(e, "周报生成异常")
            Result.failure(Exception("AI 周报生成失败: ${e.message}"))
        }
    }
}

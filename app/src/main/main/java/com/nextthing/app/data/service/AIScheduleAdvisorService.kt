package com.nextthing.app.data.service

import com.nextthing.app.data.preferences.AIPreferences
import com.nextthing.app.data.remote.ai.ChatCompletionRequest
import com.nextthing.app.data.remote.ai.ChatCompletionResponse
import com.nextthing.app.data.remote.ai.ChatMessage
import com.nextthing.app.data.remote.ai.ResponseFormat
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.service.ScheduleAdvice
import com.nextthing.app.domain.service.AIScheduleAdvisor
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
class AIScheduleAdvisorService @Inject constructor(
    @Named("ai") private val okHttpClient: OkHttpClient,
    private val aiPreferences: AIPreferences,
    private val gson: Gson
) : AIScheduleAdvisor {

    private val systemPrompt = """
你是一个任务管理顾问。根据用户的任务数据，分析当前任务安排是否合理，给出调整建议。

要求：
1. 返回 JSON 格式，不要包含任何其他文字
2. summary 字段：一句话总结当前状况和建议方向（30字以内）
3. suggestions 字段：3-5 条具体可操作的调整建议

格式：
{
  "summary": "当前任务堆积较多，建议优先处理紧急任务",
  "suggestions": ["建议1", "建议2", "建议3"]
}

分析角度：
- 时间冲突：同一时间段是否有多个任务
- 优先级失衡：紧急重要任务是否被低优先级任务挤占
- 负载过重：今日任务数量是否超出合理范围
- 逾期累积：逾期任务是否需要重新安排或放弃
    """.trimIndent()

    override suspend fun analyzeSchedule(
        overdueTasks: List<Task>,
        todayTasks: List<Task>,
        urgentTasks: List<Task>
    ): Result<ScheduleAdvice> {
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

            if (overdueTasks.isNotEmpty()) {
                sb.appendLine("=== 逾期任务 (${overdueTasks.size}件) ===")
                overdueTasks.forEach { t ->
                    val due = t.dueDate?.format(timeFmt) ?: ""
                    sb.appendLine("⚠️ ${t.title} (截止: $due, ${t.category.name})")
                }
            }

            if (todayTasks.isNotEmpty()) {
                sb.appendLine("=== 今日任务 (${todayTasks.size}件) ===")
                todayTasks.forEach { t ->
                    val status = if (t.status.name == "COMPLETED") "✅" else "⏳"
                    val time = t.dueDate?.format(timeFmt) ?: ""
                    val priority = t.importanceUrgency?.displayName ?: ""
                    sb.appendLine("$status ${t.title} $time ${t.category.name} $priority")
                }
            }

            if (urgentTasks.isNotEmpty()) {
                sb.appendLine("=== 紧急重要任务 (${urgentTasks.size}件) ===")
                urgentTasks.forEach { t ->
                    val time = t.dueDate?.format(timeFmt) ?: ""
                    sb.appendLine("🔥 ${t.title} $time")
                }
            }

            if (sb.isEmpty()) {
                sb.appendLine("当前没有待办任务。")
            }

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

            Timber.tag("AI-Schedule").d("请求日程建议: overdue=${overdueTasks.size}, today=${todayTasks.size}, urgent=${urgentTasks.size}")

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
            val summary = obj.get("summary")?.asString ?: ""
            val suggestions = obj.getAsJsonArray("suggestions")
                ?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList()

            Timber.tag("AI-Schedule").d("日程建议生成成功: $summary")
            Result.success(ScheduleAdvice(summary, suggestions))
        } catch (e: Exception) {
            Timber.tag("AI-Schedule").e(e, "日程建议生成异常")
            Result.failure(Exception("AI 日程建议失败: ${e.message}"))
        }
    }
}

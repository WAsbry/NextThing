package com.nextthing.app.data.service

import com.nextthing.app.data.preferences.AIPreferences
import com.nextthing.app.data.remote.ai.ChatCompletionRequest
import com.nextthing.app.data.remote.ai.ChatCompletionResponse
import com.nextthing.app.data.remote.ai.ChatMessage
import com.nextthing.app.data.remote.ai.ResponseFormat
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.service.AIBehaviorAnalyzer
import com.nextthing.app.domain.service.BehaviorInsight
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
class AIBehaviorAnalyzerService @Inject constructor(
    @Named("ai") private val okHttpClient: OkHttpClient,
    private val aiPreferences: AIPreferences,
    private val gson: Gson
) : AIBehaviorAnalyzer {

    private val systemPrompt = """
你是一个行为分析专家。根据用户完成的任务数据，发现用户的行为模式和习惯规律。

要求：
1. 返回 JSON 格式，不要包含任何其他文字
2. patterns 字段：2-4 条行为模式发现（具体且有洞察力）
3. suggestions 字段：2-3 条基于模式的改进建议

格式：
{
  "patterns": [
    "你通常在晚上 8-10 点完成健身类任务",
    "工作日完成任务数比周末多 40%",
    "你更倾向于完成耗时 30 分钟以内的任务"
  ],
  "suggestions": [
    "将大任务拆分为 30 分钟内可完成的小步骤",
    "利用周末上午的高效时段处理重要任务"
  ]
}

分析角度：
- 时间偏好：用户在什么时间段最常完成任务
- 分类偏好：哪类任务完成率高，哪类经常拖延
- 耗时规律：用户倾向于完成多长时间的任务
- 频率规律：工作日 vs 周末的完成习惯
- 优先级：用户更倾向于先做哪类优先级的任务
    """.trimIndent()

    override suspend fun analyzeBehavior(
        completedTasks: List<Task>,
        allTasks: List<Task>
    ): Result<BehaviorInsight> {
        if (!aiPreferences.isConfigured()) {
            return Result.failure(IllegalStateException("请先在设置 → AI 智能助手中配置 API Key"))
        }

        if (completedTasks.size < 3) {
            return Result.success(BehaviorInsight(
                patterns = listOf("完成的任务数量较少（${completedTasks.size}件），暂时无法分析行为模式"),
                suggestions = listOf("继续使用应用积累更多数据后即可获得个性化行为分析")
            ))
        }

        val provider = aiPreferences.getProviderOnce()
        val apiKey = aiPreferences.getApiKeyOnce()
        val modelOverride = aiPreferences.getModelOnce()
        val model = modelOverride.ifBlank { provider.defaultModel }
        val baseUrl = provider.baseUrl

        return try {
            val timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val sb = StringBuilder()

            sb.appendLine("=== 已完成任务 (${completedTasks.size}件) ===")
            completedTasks.take(30).forEach { t ->
                val completed = t.updatedAt.format(timeFmt)
                val hour = t.updatedAt.hour
                val dayOfWeek = t.updatedAt.dayOfWeek.name
                sb.appendLine("- ${t.title} | 分类:${t.category.name} | 完成:$completed ($dayOfWeek ${hour}时)")
            }

            sb.appendLine()
            sb.appendLine("=== 统计数据 ===")
            sb.appendLine("总任务数：${allTasks.size}")
            sb.appendLine("已完成：${completedTasks.size}")
            sb.appendLine("完成率：${if (allTasks.isNotEmpty()) "%.1f%%".format(completedTasks.size.toFloat() / allTasks.size * 100) else "N/A"}")

            val categoryCount = completedTasks.groupingBy { it.category.name }.eachCount()
            sb.appendLine("分类分布：$categoryCount")

            val hourCount = completedTasks.groupingBy { it.updatedAt.hour }.eachCount()
            val topHours = hourCount.entries.sortedByDescending { it.value }.take(5)
            sb.appendLine("热门完成时段：$topHours")

            val requestObj = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = sb.toString())
                ),
                temperature = 0.5f,
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

            Timber.tag("AI-Behavior").d("分析行为: completed=${completedTasks.size}, total=${allTasks.size}")

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
            val patterns = obj.getAsJsonArray("patterns")
                ?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList()
            val suggestions = obj.getAsJsonArray("suggestions")
                ?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList()

            Timber.tag("AI-Behavior").d("行为分析完成: ${patterns.size}条模式, ${suggestions.size}条建议")
            Result.success(BehaviorInsight(patterns, suggestions))
        } catch (e: Exception) {
            Timber.tag("AI-Behavior").e(e, "行为分析异常")
            Result.failure(Exception("AI 行为分析失败: ${e.message}"))
        }
    }
}

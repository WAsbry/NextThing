package com.nextthing.app.data.service

import com.nextthing.app.data.preferences.AIPreferences
import com.nextthing.app.data.remote.ai.ChatCompletionRequest
import com.nextthing.app.data.remote.ai.ChatCompletionResponse
import com.nextthing.app.data.remote.ai.ChatMessage
import com.nextthing.app.data.remote.ai.ResponseFormat
import com.nextthing.app.domain.model.AITaskParseResult
import com.nextthing.app.domain.model.RepeatFrequencyType
import com.nextthing.app.domain.model.TaskImportanceUrgency
import com.nextthing.app.domain.service.AITaskParser
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AITaskParserService @Inject constructor(
    @Named("ai") private val okHttpClient: OkHttpClient,
    private val aiPreferences: AIPreferences,
    private val gson: Gson
) : AITaskParser {

    override suspend fun parseTaskFromText(
        input: String,
        availableCategories: List<String>,
        availableLocations: List<String>
    ): Result<List<AITaskParseResult>> {
        if (!aiPreferences.isConfigured()) {
            Timber.tag("AI-Flow").e("AI 未配置，apiKey 为空")
            return Result.failure(IllegalStateException("请先在设置 → AI 智能助手中配置 API Key"))
        }

        val provider = aiPreferences.getProviderOnce()
        val apiKey = aiPreferences.getApiKeyOnce()
        val modelOverride = aiPreferences.getModelOnce()
        val model = modelOverride.ifBlank { provider.defaultModel }
        val baseUrl = provider.baseUrl
        Timber.tag("AI-Flow").d("AI 配置: provider=${provider.displayName}, model=$model, url=${baseUrl}chat/completions")

        return try {
            val now = LocalDateTime.now()
            val systemPrompt = buildSystemPrompt(now, availableCategories, availableLocations)

            val requestObj = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = input)
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

            Timber.tag("AI-Flow").d("请求 AI: provider=${provider.displayName}, model=$model, input=$input")

            // 必须在 IO 线程执行同步网络调用
            val (responseCode, responseBody) = withContext(Dispatchers.IO) {
                val response = okHttpClient.newCall(httpRequest).execute()
                response.code to (response.body?.string() ?: "")
            }

            if (responseCode !in 200..299) {
                Timber.tag("AI-Flow").e("API 错误 $responseCode: $responseBody")
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

            Timber.tag("AI-Flow").d("AI 返回 ($responseCode): ${responseBody.take(300)}")

            val chatResponse = gson.fromJson(responseBody, ChatCompletionResponse::class.java)

            // 检查 API 层面的错误
            chatResponse.error?.let { err ->
                val msg = err.message ?: "未知错误"
                Timber.tag("AI").e("❌ API 返回错误: $msg")
                return Result.failure(Exception("AI 服务错误: $msg"))
            }

            val content = chatResponse.choices?.firstOrNull()?.message?.content

            if (content.isNullOrBlank()) {
                return Result.failure(Exception("AI 返回内容为空"))
            }

            Result.success(parseJsonToResults(content))
        } catch (e: java.net.UnknownHostException) {
            Timber.tag("AI").e(e, "❌ 无法连接到 AI 服务器")
            Result.failure(Exception("网络无法连接，请检查网络设置"))
        } catch (e: java.net.SocketTimeoutException) {
            Timber.tag("AI").e(e, "❌ AI 请求超时")
            Result.failure(Exception("请求超时，AI 响应较慢，请重试"))
        } catch (e: com.google.gson.JsonSyntaxException) {
            Timber.tag("AI").e(e, "❌ AI 返回格式异常")
            Result.failure(Exception("AI 返回格式异常，请重试"))
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "❌ AI 解析异常: ${e.javaClass.simpleName}")
            Result.failure(Exception("AI 解析失败: ${e.message}"))
        }
    }

    private fun buildSystemPrompt(now: LocalDateTime, categories: List<String>, locations: List<String>): String {
        val nowStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val weekdayName = when (now.dayOfWeek.value) {
            1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"
            5 -> "周五"; 6 -> "周六"; 7 -> "周日"; else -> ""
        }
        val tomorrow = now.toLocalDate().plusDays(1)
        val dayAfterTomorrow = now.toLocalDate().plusDays(2)
        val categoriesStr = if (categories.isNotEmpty()) categories.joinToString("、") else "工作、生活"
        val locationsStr = if (locations.isNotEmpty()) locations.joinToString("、") else ""
        val locationsLine = if (locationsStr.isNotEmpty())
            "\n用户已有的地点: [$locationsStr]" else ""
        val locationFieldComment = if (locations.isNotEmpty())
            "从用户已有地点中选择最匹配的，无匹配则为null"
        else
            "地点名称或null"

        return """
你是一个任务解析助手。从用户的自然语言中提取结构化任务信息。

当前时间: $nowStr ($weekdayName)
明天: $tomorrow
后天: $dayAfterTomorrow
用户已有的分类: [$categoriesStr]$locationsLine

请严格返回 JSON 数组格式，不要包含任何其他文字：
[
  {
    "title": "简洁的任务标题（去掉时间地点等修饰，保留核心动作）",
    "description": "补充描述，没有则为null",
    "dueDate": "yyyy-MM-dd'T'HH:mm:ss 格式，无法确定则为null",
    "categoryName": "从用户分类中选择最匹配的，无法确定则为null",
    "importance": "IMPORTANT_URGENT 或 IMPORTANT_NOT_URGENT 或 NOT_IMPORTANT_URGENT 或 NOT_IMPORTANT_NOT_URGENT 或 null",
    "repeatType": "NONE 或 DAILY 或 WEEKLY 或 MONTHLY",
    "repeatWeekdays": [1,3,5] 或 null（仅 WEEKLY 时需要，1=周一 7=周日）,
    "locationName": "$locationFieldComment",
    "confidence": 0.9
  }
]

如果用户说了多件事（如"上午买菜，下午开会，晚上健身"），每个独立任务一个元素。
如果只识别出一件事，返回只有一个元素的数组。

时间解析规则：
- "今天" → ${now.toLocalDate()}
- "明天" → $tomorrow
- "后天" → $dayAfterTomorrow
- "X点" → 若未说明上午/下午，1-6点默认下午(+12)，7-11点默认上午，12点保持
- "上午X点" → 当天X:00，"下午X点" → 当天(X+12):00（X<=12时）
- "每天/每日" → repeatType=DAILY
- "每周X/每X" → repeatType=WEEKLY，repeatWeekdays=[对应数字]
- 无时间信息 → dueDate 为 null

importance 规则：
- "很重要""紧急""马上""立刻""赶快" → IMPORTANT_URGENT
- "重要" → IMPORTANT_NOT_URGENT
- "尽快""赶紧"但不是重要事务 → NOT_IMPORTANT_URGENT
- 日常琐事或未提及重要性 → null
        """.trimIndent()
    }

    private fun parseJsonToResults(json: String): List<AITaskParseResult> {
        val cleanJson = json.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        Timber.tag("AI-Flow").d("解析 AI 返回 JSON (长度=${cleanJson.length}): ${cleanJson.take(200)}")
        val jsonArray = gson.fromJson(cleanJson, JsonArray::class.java)
        Timber.tag("AI-Flow").d("JSON 数组大小: ${jsonArray.size()}")
        return jsonArray.mapIndexed { index, element ->
            val result = parseSingleResult(element.asJsonObject)
            Timber.tag("AI-Flow").d("  [$index] title=${result.title}, dueDate=${result.dueDate}, category=${result.categoryName}")
            result
        }
    }

    private fun parseSingleResult(obj: JsonObject): AITaskParseResult {

        val title = obj.get("title")?.asString?.takeIf { it.isNotBlank() } ?: "未命名任务"
        val description = obj.get("description")?.takeIf { !it.isJsonNull }?.asString
        val categoryName = obj.get("categoryName")?.takeIf { !it.isJsonNull }?.asString
        val locationName = obj.get("locationName")?.takeIf { !it.isJsonNull }?.asString
        val confidence = obj.get("confidence")?.takeIf { !it.isJsonNull }?.asFloat ?: 0.5f

        // 解析截止时间
        val dueDate = obj.get("dueDate")?.takeIf { !it.isJsonNull }?.asString?.let { raw ->
            try {
                LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            } catch (e1: Exception) {
                try { LocalDateTime.parse(raw) } catch (e2: Exception) {
                    Timber.tag("AI").w("时间解析失败: $raw")
                    null
                }
            }
        }

        // 解析重要性
        val importance = obj.get("importance")?.takeIf { !it.isJsonNull }?.asString?.let {
            try { TaskImportanceUrgency.valueOf(it) } catch (_: Exception) { null }
        }

        // 解析重复类型
        val repeatType = obj.get("repeatType")?.takeIf { !it.isJsonNull }?.asString?.let {
            try { RepeatFrequencyType.valueOf(it) } catch (_: Exception) { RepeatFrequencyType.NONE }
        } ?: RepeatFrequencyType.NONE

        // 解析重复星期
        val repeatWeekdays = obj.get("repeatWeekdays")
            ?.takeIf { !it.isJsonNull && it.isJsonArray }
            ?.asJsonArray?.mapNotNull { runCatching { it.asInt }.getOrNull() }
            ?.toSet()

        return AITaskParseResult(
            title = title,
            description = description,
            dueDate = dueDate,
            categoryName = categoryName,
            importance = importance,
            repeatType = repeatType,
            repeatWeekdays = repeatWeekdays,
            locationName = locationName,
            confidence = confidence
        )
    }
}

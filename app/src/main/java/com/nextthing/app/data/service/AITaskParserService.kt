package com.nextthing.app.data.service

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.data.remote.api.AIChatApi
import com.nextthing.app.data.remote.dto.AIChatRequest
import com.nextthing.app.domain.model.AITaskParseResult
import com.nextthing.app.domain.model.RepeatFrequencyType
import com.nextthing.app.domain.model.TaskImportanceUrgency
import com.nextthing.app.domain.service.AITaskParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AITaskParserService @Inject constructor(
    private val aiChatApi: AIChatApi,
    private val tokenManager: TokenManager,
    private val gson: Gson
) : AITaskParser {

    override suspend fun parseTaskFromText(
        input: String,
        availableCategories: List<String>,
        availableLocations: List<String>
    ): Result<List<AITaskParseResult>> {
        val userId = tokenManager.serverUserId.first()
        if (userId == null) return Result.failure(IllegalStateException("请先登录"))

        return try {
            val now = LocalDateTime.now()
            val message = buildMessage(now, input, availableCategories, availableLocations)

            val response = withContext(Dispatchers.IO) {
                aiChatApi.chat(AIChatRequest(message, textOnly = true))
            }
            if (!response.success || response.reply.isNullOrBlank()) {
                return Result.failure(Exception(response.reply ?: "AI 返回内容为空"))
            }

            Result.success(parseJsonToResults(response.reply))
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "任务解析失败")
            Result.failure(Exception("AI 任务解析失败: ${e.message}"))
        }
    }

    private fun buildMessage(
        now: LocalDateTime,
        input: String,
        categories: List<String>,
        locations: List<String>
    ): String {
        val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val dayNames = arrayOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val tomorrow = now.plusDays(1).toLocalDate()
        val dayAfter = now.plusDays(2).toLocalDate()

        val sb = StringBuilder()
        sb.appendLine("从以下自然语言中提取结构化任务信息。只返回解析结果，不要执行任何操作。")
        sb.appendLine("请返回JSON数组：[{\"title\":\"...\",\"description\":\"...\",\"dueDate\":\"yyyy-MM-ddTHH:mm:ss\",\"categoryName\":\"...\",\"importance\":\"IMPORTANT_URGENT/IMPORTANT_NOT_URGENT/NOT_IMPORTANT_URGENT/NOT_IMPORTANT_NOT_URGENT\",\"repeatType\":\"NONE/DAILY/WEEKDAYS/WEEKENDS/WEEKLY/MONTHLY/YEARLY\",\"repeatWeekdays\":[1,3,5],\"locationName\":\"...\",\"confidence\":0.9}]")
        sb.appendLine()
        sb.appendLine("当前时间：${now.format(dateFmt)} ${dayNames[now.dayOfWeek.value]}")
        sb.appendLine("明天：${tomorrow}  后天：${dayAfter}")
        if (categories.isNotEmpty()) sb.appendLine("可用分类：${categories.joinToString(", ")}")
        if (locations.isNotEmpty()) sb.appendLine("可用地点：${locations.joinToString(", ")}")
        sb.appendLine()
        sb.appendLine("用户输入：$input")
        return sb.toString()
    }

    private fun parseJsonToResults(json: String): List<AITaskParseResult> {
        val cleanJson = json.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        val array = gson.fromJson(cleanJson, JsonArray::class.java)
        return array.mapNotNull { element ->
            runCatching { parseSingleResult(element.asJsonObject) }.getOrNull()
        }
    }

    private fun parseSingleResult(obj: JsonObject): AITaskParseResult {
        val title = obj.get("title")?.takeIf { !it.isJsonNull }?.asString?.takeIf { it.isNotBlank() } ?: "未命名任务"
        val description = obj.get("description")?.takeIf { !it.isJsonNull }?.asString
        val categoryName = obj.get("categoryName")?.takeIf { !it.isJsonNull }?.asString
        val locationName = obj.get("locationName")?.takeIf { !it.isJsonNull }?.asString

        val dueDate = obj.get("dueDate")?.takeIf { !it.isJsonNull }?.asString?.let { ds ->
            runCatching { LocalDateTime.parse(ds, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) }
                .recoverCatching { LocalDateTime.parse(ds) }
                .getOrNull()
        }

        val importance = obj.get("importance")?.takeIf { !it.isJsonNull }?.asString?.let {
            runCatching { TaskImportanceUrgency.valueOf(it) }.getOrNull()
        }

        val repeatType = obj.get("repeatType")?.takeIf { !it.isJsonNull }?.asString?.let {
            runCatching { RepeatFrequencyType.valueOf(it) }.getOrNull() ?: RepeatFrequencyType.NONE
        }

        val repeatWeekdays = obj.get("repeatWeekdays")?.takeIf { !it.isJsonNull }?.asJsonArray?.mapNotNull {
            runCatching { it.asInt }.getOrNull()
        }?.toSet()

        val confidence = obj.get("confidence")?.takeIf { !it.isJsonNull }?.asFloat ?: 0.5f

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

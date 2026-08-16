package com.nextthing.app.data.service

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.nextthing.app.domain.model.AITaskParseResult
import com.nextthing.app.domain.model.RepeatFrequencyType
import com.nextthing.app.domain.model.TaskImportanceUrgency
import com.nextthing.app.domain.service.AITaskParser
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AITaskParserService @Inject constructor(
    private val aiCompletionClient: AICompletionClient,
    private val gson: Gson
) : AITaskParser {

    private companion object {
        const val MAX_PARSED_TASKS = 20
        const val MAX_TITLE_CHARS = 200
        const val MAX_DESCRIPTION_CHARS = 4_000
        const val MAX_NAME_CHARS = 100
    }

    override suspend fun parseTaskFromText(
        input: String,
        availableCategories: List<String>,
        availableLocations: List<String>,
        voiceContext: String?
    ): Result<List<AITaskParseResult>> {
        if (input.isBlank()) {
            return Result.failure(IllegalArgumentException("AI 解析内容不能为空"))
        }
        return try {
            val message = buildMessage(LocalDateTime.now(), input, availableCategories, availableLocations, voiceContext)
            val reply = aiCompletionClient.complete(message, textOnly = true).getOrThrow()
            Result.success(parseJsonToResults(reply))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "Task parsing failed")
            Result.failure(Exception(toUserFacingParseError(e)))
        }
    }

    private fun buildMessage(
        now: LocalDateTime,
        input: String,
        categories: List<String>,
        locations: List<String>,
        voiceContext: String?
    ): String {
        val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val tomorrow = now.plusDays(1).toLocalDate()
        val dayAfter = now.plusDays(2).toLocalDate()

        return buildString {
            appendLine("Extract structured task information from the user's natural-language input.")
            appendLine("Return only a JSON array. Do not add markdown or explanation.")
            appendLine("Schema: [{\"title\":\"...\",\"description\":\"...\",\"dueDate\":\"yyyy-MM-ddTHH:mm:ss\",\"categoryName\":\"...\",\"importance\":\"IMPORTANT_URGENT/IMPORTANT_NOT_URGENT/NOT_IMPORTANT_URGENT/NOT_IMPORTANT_NOT_URGENT\",\"repeatType\":\"NONE/DAILY/WEEKDAYS/WEEKENDS/WEEKLY/MONTHLY/YEARLY\",\"repeatWeekdays\":[1,3,5],\"locationName\":\"...\",\"confidence\":0.9}]")
            appendLine()
            appendLine("Current time: ${now.format(dateFmt)}")
            appendLine("Tomorrow: $tomorrow, day after tomorrow: $dayAfter")
            if (categories.isNotEmpty()) appendLine("Available categories: ${categories.joinToString(", ")}")
            if (locations.isNotEmpty()) appendLine("Available locations: ${locations.joinToString(", ")}")
            if (!voiceContext.isNullOrBlank()) {
                appendLine("Voice context: $voiceContext")
                appendLine("Use voice context only as a weak signal for importance and reminder urgency. Do not copy it into the task title.")
            }
            appendLine()
            appendLine("User input: $input")
        }
    }

    private fun parseJsonToResults(json: String): List<AITaskParseResult> {
        val cleanJson = json.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()

        val array = gson.fromJson(cleanJson, JsonArray::class.java)
        require(array.size() in 1..MAX_PARSED_TASKS) {
            "AI 返回任务数量必须在 1 到 $MAX_PARSED_TASKS 之间"
        }
        return array.mapIndexed { index, element ->
            try {
                require(element.isJsonObject) { "任务必须是 JSON 对象" }
                parseSingleResult(element.asJsonObject)
            } catch (error: Exception) {
                throw IllegalArgumentException("AI 返回的第 ${index + 1} 个任务格式无效", error)
            }
        }
    }

    private fun parseSingleResult(obj: JsonObject): AITaskParseResult {
        val title = obj.get("title")
            ?.takeIf { !it.isJsonNull }
            ?.asString
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("任务标题不能为空")
        require(title.length <= MAX_TITLE_CHARS) { "任务标题过长" }

        val description = obj.get("description")
            ?.takeIf { !it.isJsonNull }
            ?.asString
            ?.trim()
        require(description == null || description.length <= MAX_DESCRIPTION_CHARS) {
            "任务描述过长"
        }

        val categoryName = obj.get("categoryName")
            ?.takeIf { !it.isJsonNull }
            ?.asString
            ?.trim()
        require(categoryName == null || categoryName.length <= MAX_NAME_CHARS) {
            "分类名称过长"
        }
        val locationName = obj.get("locationName")
            ?.takeIf { !it.isJsonNull }
            ?.asString
            ?.trim()
        require(locationName == null || locationName.length <= MAX_NAME_CHARS) {
            "地点名称过长"
        }

        val dueDate = obj.get("dueDate")?.takeIf { !it.isJsonNull }?.asString?.let { ds ->
            runCatching { LocalDateTime.parse(ds, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) }
                .recoverCatching { LocalDateTime.parse(ds) }
                .getOrElse { throw IllegalArgumentException("截止时间格式无效", it) }
        }

        val importance = obj.get("importance")?.takeIf { !it.isJsonNull }?.asString?.let {
            runCatching { TaskImportanceUrgency.valueOf(it) }
                .getOrElse { error -> throw IllegalArgumentException("重要程度无效", error) }
        }

        val repeatType = obj.get("repeatType")?.takeIf { !it.isJsonNull }?.asString?.let {
            runCatching { RepeatFrequencyType.valueOf(it) }
                .getOrElse { error -> throw IllegalArgumentException("重复类型无效", error) }
        }

        val repeatWeekdays = obj.get("repeatWeekdays")?.takeIf { !it.isJsonNull }?.asJsonArray?.mapNotNull {
            runCatching { it.asInt }.getOrNull()
        }?.toSet()
        require(repeatWeekdays == null || repeatWeekdays.all { it in 1..7 }) {
            "重复星期必须在 1 到 7 之间"
        }
        if (repeatType == RepeatFrequencyType.WEEKLY) {
            require(!repeatWeekdays.isNullOrEmpty()) { "每周重复必须指定星期" }
        }

        val confidence = obj.get("confidence")?.takeIf { !it.isJsonNull }?.asFloat ?: 0.5f
        require(confidence in 0f..1f) { "置信度必须在 0 到 1 之间" }

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

    private fun toUserFacingParseError(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("API Key") ||
                message.contains("登录状态") ||
                message.contains("账户余额") ||
                message.contains("请求过于频繁") ||
                message.contains("服务暂时不可用") -> message
            message.contains("401") ->
                "AI 服务认证失败，请检查 DeepSeek API Key，或重新登录后再试。"
            else ->
                "AI 解析失败，请检查 AI 设置或稍后重试。"
        }
    }
}

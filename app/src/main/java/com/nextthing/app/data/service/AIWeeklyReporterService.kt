package com.nextthing.app.data.service

import com.google.gson.Gson
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.service.AIWeeklyReporter
import com.nextthing.app.domain.service.WeeklyReport
import timber.log.Timber
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIWeeklyReporterService @Inject constructor(
    private val aiCompletionClient: AICompletionClient,
    private val gson: Gson
) : AIWeeklyReporter {

    override suspend fun generateReport(
        weekTasks: List<Task>,
        completedTasks: List<Task>
    ): Result<WeeklyReport> {
        return try {
            val reply = aiCompletionClient.complete(buildPrompt(weekTasks, completedTasks)).getOrThrow()
            val obj = AIJsonHelper.parseAIJson(gson, reply)
            Result.success(
                WeeklyReport(
                    title = obj.get("title")?.asString ?: "本周周报",
                    summary = obj.get("summary")?.asString ?: "",
                    highlights = obj.getAsJsonArray("highlights")?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList(),
                    improvements = obj.getAsJsonArray("improvements")?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList(),
                    nextWeekSuggestions = obj.getAsJsonArray("nextWeekSuggestions")?.mapNotNull { runCatching { it.asString }.getOrNull() } ?: emptyList()
                )
            )
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "Weekly report failed")
            Result.failure(Exception("AI weekly report failed: ${e.message}"))
        }
    }

    private fun buildPrompt(weekTasks: List<Task>, completedTasks: List<Task>): String {
        val timeFmt = DateTimeFormatter.ofPattern("MM/dd")
        val totalCount = weekTasks.size
        val completedCount = completedTasks.size
        val pending = weekTasks.filter { it.status.name !in listOf("DONE", "CANCELLED") }
        val completionRate = if (totalCount > 0) {
            "%.0f%%".format(completedCount.toDouble() / totalCount * 100)
        } else {
            "N/A"
        }

        return buildString {
            appendLine("Generate a weekly task report in Chinese.")
            appendLine("Return only JSON with this schema: {\"title\":\"...\",\"summary\":\"...\",\"highlights\":[\"...\"],\"improvements\":[\"...\"],\"nextWeekSuggestions\":[\"...\"]}")
            appendLine("Use an objective but encouraging tone. Keep each list to 1-4 items.")
            appendLine()
            appendLine("Stats: total=$totalCount, completed=$completedCount, completionRate=$completionRate")
            if (completedTasks.isNotEmpty()) {
                appendLine()
                appendLine("Completed tasks:")
                completedTasks.forEach { task -> appendLine("- ${task.title} | category=${task.category.name}") }
            }
            if (pending.isNotEmpty()) {
                appendLine()
                appendLine("Pending tasks:")
                pending.forEach { task -> appendLine("- ${task.title} | status=${task.status.name} | due=${task.dueDate?.format(timeFmt) ?: "none"}") }
            }
        }
    }
}

package com.nextthing.app.data.service

import com.google.gson.Gson
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.service.AIBehaviorAnalyzer
import com.nextthing.app.domain.service.BehaviorInsight
import timber.log.Timber
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIBehaviorAnalyzerService @Inject constructor(
    private val aiCompletionClient: AICompletionClient,
    private val gson: Gson
) : AIBehaviorAnalyzer {

    override suspend fun analyzeBehavior(
        completedTasks: List<Task>,
        allTasks: List<Task>
    ): Result<BehaviorInsight> {
        if (completedTasks.size < 3) {
            return Result.success(
                BehaviorInsight(
                    patterns = listOf("已完成任务不足 3 个，暂时无法稳定分析行为模式"),
                    suggestions = listOf("继续完成任务，积累更多数据后再生成行为洞察")
                )
            )
        }

        return try {
            val reply = aiCompletionClient.complete(buildPrompt(completedTasks, allTasks)).getOrThrow()
            val obj = AIJsonHelper.parseAIJson(gson, reply)
            Result.success(
                BehaviorInsight(
                    patterns = obj.getAsJsonArray("patterns")
                        ?.mapNotNull { runCatching { it.asString }.getOrNull() }
                        ?: emptyList(),
                    suggestions = obj.getAsJsonArray("suggestions")
                        ?.mapNotNull { runCatching { it.asString }.getOrNull() }
                        ?: emptyList()
                )
            )
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "Behavior analysis failed")
            Result.failure(Exception("AI behavior analysis failed: ${e.message}"))
        }
    }

    private fun buildPrompt(completedTasks: List<Task>, allTasks: List<Task>): String {
        val timeFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val totalCount = allTasks.size
        val completedCount = completedTasks.size
        val completionRate = if (totalCount > 0) {
            "%.0f%%".format(completedCount.toDouble() / totalCount * 100)
        } else {
            "N/A"
        }

        return buildString {
            appendLine("Analyze the user's task behavior patterns in Chinese.")
            appendLine("Focus on time preference, category preference, weekday/weekend rhythm, and priority tendency.")
            appendLine("Return only JSON: {\"patterns\":[\"...\"],\"suggestions\":[\"...\"]}")
            appendLine()
            appendLine("Stats: total=$totalCount, completed=$completedCount, completionRate=$completionRate")
            appendLine()
            appendLine("Recent completed tasks:")
            completedTasks.take(30).forEach { task ->
                appendLine("- ${task.title} | category=${task.category.name} | completed=${task.updatedAt?.format(timeFmt) ?: "unknown"}")
            }
        }
    }
}

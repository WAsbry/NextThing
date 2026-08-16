package com.nextthing.app.data.service

import com.google.gson.Gson
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.service.AITimeEstimator
import com.nextthing.app.domain.service.TimeEstimate
import timber.log.Timber
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AITimeEstimatorService @Inject constructor(
    private val aiCompletionClient: AICompletionClient,
    private val gson: Gson
) : AITimeEstimator {

    override suspend fun estimateTime(
        taskTitle: String,
        taskDescription: String,
        categoryName: String?,
        recentCompletedTasks: List<Task>
    ): Result<TimeEstimate> {
        return try {
            val reply = aiCompletionClient.complete(buildPrompt(taskTitle, taskDescription, categoryName, recentCompletedTasks)).getOrThrow()
            val obj = AIJsonHelper.parseAIJson(gson, reply)
            Result.success(
                TimeEstimate(
                    estimatedMinutes = obj.get("estimatedMinutes")?.asInt ?: 60,
                    reasoning = obj.get("reasoning")?.asString ?: "",
                    similarTaskCount = obj.get("similarTaskCount")?.asInt ?: 0
                )
            )
        } catch (e: Exception) {
            Timber.tag("AI-Time").e(e, "Time estimation failed")
            Result.failure(Exception("AI time estimation failed: ${e.message}"))
        }
    }

    private fun buildPrompt(
        taskTitle: String,
        taskDescription: String,
        categoryName: String?,
        recentCompletedTasks: List<Task>
    ): String {
        return buildString {
            appendLine("Estimate how many minutes the task will take. Use similar historical completed tasks when possible.")
            appendLine("Return only JSON: {\"estimatedMinutes\":120,\"reasoning\":\"...\",\"similarTaskCount\":3}")
            appendLine()
            appendLine("Task title: $taskTitle")
            appendLine("Task description: ${taskDescription.ifBlank { "none" }}")
            appendLine("Category: ${categoryName ?: "unknown"}")

            if (recentCompletedTasks.isNotEmpty()) {
                appendLine()
                appendLine("Recent completed tasks:")
                recentCompletedTasks.take(20).forEach { task ->
                    val minutes = if (task.createdAt != null && task.updatedAt != null) {
                        ChronoUnit.MINUTES.between(task.createdAt, task.updatedAt)
                    } else {
                        0L
                    }
                    appendLine("- ${task.title} | category=${task.category.name} | duration=${minutes}min")
                }
            }
        }
    }
}

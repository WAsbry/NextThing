package com.nextthing.app.data.service

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.nextthing.app.domain.service.AISubtaskGenerator
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AISubtaskGeneratorService @Inject constructor(
    private val aiCompletionClient: AICompletionClient,
    private val gson: Gson
) : AISubtaskGenerator {

    override suspend fun generateSubtasks(
        taskTitle: String,
        taskDescription: String
    ): Result<List<String>> {
        return try {
            val message = buildString {
                appendLine("Break the task into 3-8 concrete, executable subtasks in Chinese.")
                appendLine("Return only a JSON string array, without markdown or explanation.")
                appendLine("Example: [\"step 1\", \"step 2\", \"step 3\"]")
                appendLine()
                appendLine("Task title: $taskTitle")
                appendLine("Task description: ${taskDescription.ifBlank { "none" }}")
            }

            val reply = aiCompletionClient.complete(message).getOrThrow()
            val cleanJson = reply.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()

            val array = gson.fromJson(cleanJson, JsonArray::class.java)
            val subtasks = array.mapNotNull { runCatching { it.asString }.getOrNull() }

            if (subtasks.isEmpty()) {
                Result.failure(Exception("Failed to parse subtasks"))
            } else {
                Result.success(subtasks)
            }
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "Subtask generation failed")
            Result.failure(Exception("AI subtask generation failed: ${e.message}"))
        }
    }
}

package com.nextthing.app.data.service

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.service.AITaskSearcher
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AITaskSearcherService @Inject constructor(
    private val aiCompletionClient: AICompletionClient,
    private val gson: Gson
) : AITaskSearcher {

    override suspend fun searchByNaturalLanguage(
        query: String,
        allTasks: List<Task>
    ): Result<List<Task>> {
        if (allTasks.isEmpty()) return Result.success(emptyList())

        return try {
            val reply = aiCompletionClient.complete(buildPrompt(query, allTasks)).getOrThrow()
            val cleanJson = reply.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()
            val array = gson.fromJson(cleanJson, JsonArray::class.java)
            val matchedIds = array.mapNotNull { runCatching { it.asString }.getOrNull() }.toSet()

            Result.success(allTasks.filter { it.id in matchedIds })
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "Task search failed")
            Result.failure(Exception("AI task search failed: ${e.message}"))
        }
    }

    private fun buildPrompt(query: String, allTasks: List<Task>): String {
        return buildString {
            appendLine("The user wants to find tasks matching this natural-language query:")
            appendLine(query)
            appendLine()
            appendLine("Return only a JSON string array containing matched task ids. Do not add explanation.")
            appendLine("Support fuzzy semantic matching and time-related queries when task data is sufficient.")
            appendLine()
            appendLine("Tasks:")
            allTasks.take(120).forEach { task ->
                appendLine("- id=${task.id} | title=${task.title} | status=${task.status.name} | category=${task.category.name} | due=${task.dueDate ?: "none"}")
            }
        }
    }
}

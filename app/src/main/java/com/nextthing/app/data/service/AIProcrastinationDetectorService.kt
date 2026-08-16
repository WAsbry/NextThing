package com.nextthing.app.data.service

import com.google.gson.Gson
import com.nextthing.app.domain.service.AIProcrastinationDetector
import com.nextthing.app.domain.service.ProcrastinationAdvice
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIProcrastinationDetectorService @Inject constructor(
    private val aiCompletionClient: AICompletionClient,
    private val gson: Gson
) : AIProcrastinationDetector {

    override suspend fun analyze(
        taskTitle: String,
        taskDescription: String,
        delayCount: Int,
        daysSinceCreated: Int,
        category: String
    ): Result<ProcrastinationAdvice> {
        return try {
            val reply = aiCompletionClient.complete(buildPrompt(taskTitle, taskDescription, delayCount, daysSinceCreated, category)).getOrThrow()
            val obj = AIJsonHelper.parseAIJson(gson, reply)
            Result.success(
                ProcrastinationAdvice(
                    severity = obj.get("severity")?.asString ?: "low",
                    summary = obj.get("summary")?.asString ?: "",
                    suggestions = obj.getAsJsonArray("suggestions")
                        ?.mapNotNull { runCatching { it.asString }.getOrNull() }
                        ?: emptyList()
                )
            )
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "Procrastination analysis failed")
            Result.failure(Exception("AI procrastination analysis failed: ${e.message}"))
        }
    }

    private fun buildPrompt(
        taskTitle: String,
        taskDescription: String,
        delayCount: Int,
        daysSinceCreated: Int,
        category: String
    ): String {
        return buildString {
            appendLine("Analyze whether this task shows procrastination risk.")
            appendLine("Use severity: low for 0-1 delays, medium for 2-3 delays, high for 4+ delays or obvious long stagnation.")
            appendLine("Return only JSON: {\"severity\":\"low/medium/high\",\"summary\":\"...\",\"suggestions\":[\"...\"]}")
            appendLine()
            appendLine("Title: $taskTitle")
            appendLine("Description: ${taskDescription.ifBlank { "none" }}")
            appendLine("Category: $category")
            appendLine("Delay count: $delayCount")
            appendLine("Days since created: $daysSinceCreated")
        }
    }
}

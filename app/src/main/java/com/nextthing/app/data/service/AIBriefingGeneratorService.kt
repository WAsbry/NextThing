package com.nextthing.app.data.service

import com.nextthing.app.domain.service.AIBriefingGenerator
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIBriefingGeneratorService @Inject constructor(
    private val aiCompletionClient: AICompletionClient
) : AIBriefingGenerator {

    override suspend fun generateBriefing(
        type: AIBriefingGenerator.BriefingType,
        taskData: String
    ): Result<String> {
        return try {
            val typeStr = if (type == AIBriefingGenerator.BriefingType.MORNING) "morning" else "evening"
            val label = if (type == AIBriefingGenerator.BriefingType.MORNING) "morning" else "evening"
            val message = buildString {
                appendLine("Generate a concise $label briefing for the user based on the task data below.")
                appendLine("Focus on what should be done next, risks, and one practical suggestion.")
                appendLine()
                appendLine(taskData)
            }
            val reply = aiCompletionClient.complete(message, sessionId = "briefing-$typeStr").getOrThrow()
            Result.success(reply)
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "Briefing generation failed")
            Result.failure(Exception("AI briefing generation failed: ${e.message}"))
        }
    }
}

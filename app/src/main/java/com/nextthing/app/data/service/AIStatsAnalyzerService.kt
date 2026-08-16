package com.nextthing.app.data.service

import com.nextthing.app.domain.service.AIStatsAnalyzer
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIStatsAnalyzerService @Inject constructor(
    private val aiCompletionClient: AICompletionClient
) : AIStatsAnalyzer {

    override suspend fun generateSummary(statsData: String): Result<String> {
        return try {
            val message = buildString {
                appendLine("Analyze the following task statistics in 100-200 Chinese characters.")
                appendLine("First summarize the overall state, then point out one strength, one risk, and 1-2 actionable suggestions.")
                appendLine()
                appendLine(statsData)
            }
            Result.success(aiCompletionClient.complete(message).getOrThrow())
        } catch (e: Exception) {
            Timber.tag("AI").e(e, "Stats summary failed")
            Result.failure(Exception("AI stats analysis failed: ${e.message}"))
        }
    }
}

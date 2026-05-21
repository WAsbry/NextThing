package com.nextthing.app.domain.service

data class ProcrastinationAdvice(
    val severity: String,       // "low" / "medium" / "high"
    val summary: String,
    val suggestions: List<String>
)

interface AIProcrastinationDetector {
    suspend fun analyze(
        taskTitle: String,
        taskDescription: String,
        delayCount: Int,
        daysSinceCreated: Int,
        category: String
    ): Result<ProcrastinationAdvice>
}

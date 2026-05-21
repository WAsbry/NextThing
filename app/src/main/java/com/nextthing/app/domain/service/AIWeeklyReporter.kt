package com.nextthing.app.domain.service

import com.nextthing.app.domain.model.Task

data class WeeklyReport(
    val title: String,
    val summary: String,
    val highlights: List<String>,
    val improvements: List<String>,
    val nextWeekSuggestions: List<String>
)

interface AIWeeklyReporter {
    suspend fun generateReport(
        weekTasks: List<Task>,
        completedTasks: List<Task>
    ): Result<WeeklyReport>
}
